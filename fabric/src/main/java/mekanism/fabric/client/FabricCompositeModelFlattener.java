package mekanism.fabric.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/**
 * Client-only fix: flattens NeoForge {@code "loader":"neoforge:composite"} block models into single vanilla block models
 * at model-load time so they render on Fabric. NeoForge's composite loader nests geometry under a {@code "children"} map
 * (each child carrying its own {@code render_type}, {@code textures} and {@code elements}); vanilla/Fabric cannot parse
 * that loader, so such a model deserializes with NO top-level geometry &mdash; an invisible cube (e.g. the Chemical
 * Crystallizer).
 *
 * <p>This is a {@link PreparableModelLoadingPlugin}: its data-loader phase scans every {@code assets/<ns>/models/block/*.json}
 * resource off-thread &mdash; across ALL namespaces, so it covers both {@code mekanism} and the separate
 * {@code mekanismgenerators} module (e.g. the Bio Generator's composite model) &mdash; and for each whose JSON has
 * {@code "loader":"neoforge:composite"} it FLATTENS it &mdash;
 * concatenating every child's {@code elements} (in declaration order), merging the parent textures with every child's
 * textures, forcing {@code parent="minecraft:block/block"} and {@code render_type="minecraft:cutout"} (translucent
 * children render as cutout, acceptable for now) &mdash; then deserializes the rewritten JSON into a vanilla
 * {@link UnbakedModel} via {@link UnbakedModelDeserializer#deserialize(java.io.Reader)}. The {@code initialize} phase
 * registers an {@link ModelModifier.OnLoad} hook (override phase) that substitutes the flattened model for any model id
 * that was a composite. NeoForge is untouched: the {@code :common} composite JSONs stay as-is (its own loader handles
 * them); only the Fabric client rewrites them in memory at load.
 *
 * <p>Grep {@code [Mekanism/Fabric][composite-flatten]}.
 */
public final class FabricCompositeModelFlattener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][composite-flatten]";

    private static final String COMPOSITE_LOADER = "neoforge:composite";
    private static final String MODELS_BLOCK_DIR = "models/block";
    private static final String JSON_SUFFIX = ".json";
    private static final String FLAT_PARENT = "minecraft:block/block";
    private static final String FLAT_RENDER_TYPE = "minecraft:cutout";

    private FabricCompositeModelFlattener() {
    }

    /** Register the plugin from the Fabric client entrypoint. */
    public static void register() {
        PreparableModelLoadingPlugin.register(FabricCompositeModelFlattener::loadFlattenedModels, (flattened, ctx) -> {
            LOGGER.info("{} active: {} composite block model(s) flattened to vanilla.", TAG, flattened.size());
            ctx.modifyModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (original, modCtx) -> {
                UnbakedModel replacement = flattened.get(modCtx.id());
                return replacement != null ? replacement : original;
            });
        });
    }

    /**
     * Data-loader phase (off-thread): scan all block models, flatten the composites, return a map keyed by MODEL id
     * (namespace:block/&lt;name&gt;) &mdash; the same id {@link ModelModifier.OnLoad.Context#id()} reports.
     */
    private static CompletableFuture<Map<Identifier, UnbakedModel>> loadFlattenedModels(
          net.minecraft.server.packs.resources.PreparableReloadListener.SharedState sharedState, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            ResourceManager resourceManager = sharedState.resourceManager();
            Map<Identifier, UnbakedModel> flattened = new HashMap<>();
            Map<String, Integer> perNamespace = new HashMap<>();
            Map<Identifier, Resource> blockModels = resourceManager.listResources(
                  MODELS_BLOCK_DIR, id -> id.getPath().endsWith(JSON_SUFFIX));
            for (Map.Entry<Identifier, Resource> entry : blockModels.entrySet()) {
                Identifier resourceId = entry.getKey();
                String raw = read(entry.getValue());
                if (raw == null) {
                    continue;
                }
                JsonObject json;
                try {
                    json = JsonParser.parseString(raw).getAsJsonObject();
                } catch (RuntimeException e) {
                    continue; // not an object / malformed — let vanilla handle (and error) as usual
                }
                if (!isComposite(json)) {
                    continue;
                }
                try {
                    String flatJson = flatten(json).toString();
                    UnbakedModel model;
                    try (StringReader reader = new StringReader(flatJson)) {
                        model = UnbakedModelDeserializer.deserialize(reader);
                    }
                    Identifier modelId = modelIdOf(resourceId);
                    flattened.put(modelId, model);
                    perNamespace.merge(modelId.getNamespace(), 1, Integer::sum);
                } catch (RuntimeException e) {
                    LOGGER.error("{} failed to flatten composite model {}", TAG, resourceId, e);
                }
            }
            // Break the flatten count down per namespace so the mekanismgenerators composites (e.g. bio_generator) are
            // visibly accounted for alongside the mekanism ones.
            LOGGER.info("{} scanned {} block model(s); flattened {} composite(s) {}.",
                  TAG, blockModels.size(), flattened.size(), perNamespace);
            return flattened;
        }, executor);
    }

    /** A composite model is identified by its {@code "loader":"neoforge:composite"} field. */
    private static boolean isComposite(JsonObject json) {
        JsonElement loader = json.get("loader");
        return loader != null && loader.isJsonPrimitive() && COMPOSITE_LOADER.equals(loader.getAsString());
    }

    /**
     * Flatten a composite model JSON into a vanilla block model JSON: parent={@value #FLAT_PARENT},
     * render_type={@value #FLAT_RENDER_TYPE}, textures = parent textures merged with every child's textures, elements =
     * concatenation of every child's elements in declaration order.
     */
    private static JsonObject flatten(JsonObject composite) {
        JsonObject out = new JsonObject();
        out.addProperty("parent", FLAT_PARENT);
        out.addProperty("render_type", FLAT_RENDER_TYPE);

        JsonObject textures = new JsonObject();
        // Parent (top-level) textures first — typically the "particle" entry.
        if (composite.has("textures") && composite.get("textures").isJsonObject()) {
            mergeInto(textures, composite.getAsJsonObject("textures"));
        }
        JsonArray elements = new JsonArray();
        if (composite.has("children") && composite.get("children").isJsonObject()) {
            JsonObject children = composite.getAsJsonObject("children");
            for (Map.Entry<String, JsonElement> child : children.entrySet()) {
                if (!child.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject childObj = child.getValue().getAsJsonObject();
                if (childObj.has("textures") && childObj.get("textures").isJsonObject()) {
                    mergeInto(textures, childObj.getAsJsonObject("textures"));
                }
                if (childObj.has("elements") && childObj.get("elements").isJsonArray()) {
                    elements.addAll(childObj.getAsJsonArray("elements"));
                }
            }
        }
        out.add("textures", textures);
        out.add("elements", elements);
        return out;
    }

    private static void mergeInto(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> e : source.entrySet()) {
            target.add(e.getKey(), e.getValue());
        }
    }

    /** Resource id ({@code ns:models/block/foo.json}) -> model id ({@code ns:block/foo}). */
    private static Identifier modelIdOf(Identifier resourceId) {
        String path = resourceId.getPath();
        // strip leading "models/" and trailing ".json"
        String modelPath = path.substring("models/".length(), path.length() - JSON_SUFFIX.length());
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), modelPath);
    }

    private static String read(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
