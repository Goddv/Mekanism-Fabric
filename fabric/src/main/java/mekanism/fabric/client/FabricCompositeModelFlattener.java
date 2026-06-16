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
 * concatenating every child's RESOLVED {@code elements} (in declaration order), merging the parent textures with every
 * child's RESOLVED textures, forcing {@code parent="minecraft:block/block"} and {@code render_type="minecraft:cutout"}
 * (translucent children render as cutout, acceptable for now) &mdash; then deserializes the rewritten JSON into a vanilla
 * {@link UnbakedModel} via {@link UnbakedModelDeserializer#deserialize(java.io.Reader)}. The {@code initialize} phase
 * registers an {@link ModelModifier.OnLoad} hook (override phase) that substitutes the flattened model for any model id
 * that was a composite. NeoForge is untouched: the {@code :common} composite JSONs stay as-is (its own loader handles
 * them); only the Fabric client rewrites them in memory at load.
 *
 * <p>A composite child supplies its geometry one of two ways: INLINE ({@code elements}/{@code textures} directly on the
 * child &mdash; the ~81 machine/generator composites + the factory {@code front_led} child) or by REFERENCE (a {@code
 * "parent"} model id &mdash; every factory {@code base} child points at {@code factory/&lt;type&gt;/base}, which holds the
 * geometry). For reference children we walk the parent model chain off the {@link ResourceManager}, accumulating textures
 * (descendant-most wins, as in vanilla model inheritance) and taking elements from where they're defined; a parent that
 * is itself composite is resolved recursively (e.g. {@code nutritional_liquifier_base}). Without this, factory blocks
 * flattened to an EMPTY model and rendered untextured/invisible.
 *
 * <p>Grep {@code [Mekanism/Fabric][composite-flatten]}.
 */
public final class FabricCompositeModelFlattener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][composite-flatten]";

    private static final String COMPOSITE_LOADER = "neoforge:composite";
    private static final String MODELS_BLOCK_DIR = "models/block";
    private static final String MODELS_PREFIX = "models/";
    private static final String JSON_SUFFIX = ".json";
    private static final String FLAT_PARENT = "minecraft:block/block";
    private static final String FLAT_RENDER_TYPE = "minecraft:cutout";
    /** Guard against pathological / cyclic parent chains while resolving a child's geometry. */
    private static final int MAX_PARENT_DEPTH = 16;
    /** One factory model we log an element count for, as proof flattening sources real geometry now. */
    private static final String PROOF_MODEL_PATH = "block/factory/enriching/basic";

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
                    JsonObject flat = flatten(json, resourceManager);
                    Identifier modelId = modelIdOf(resourceId);
                    // Proof line (kept quiet): show one factory model now carries real geometry after flattening.
                    if (PROOF_MODEL_PATH.equals(modelId.getPath())) {
                        int count = flat.has("elements") ? flat.getAsJsonArray("elements").size() : 0;
                        LOGGER.info("{} resolved {}: {} element(s) after flatten (was empty before parent-resolution).",
                              TAG, modelId, count);
                    }
                    String flatJson = flat.toString();
                    UnbakedModel model;
                    try (StringReader reader = new StringReader(flatJson)) {
                        model = UnbakedModelDeserializer.deserialize(reader);
                    }
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
     * render_type={@value #FLAT_RENDER_TYPE}, textures = parent textures merged with every child's RESOLVED textures,
     * elements = concatenation of every child's RESOLVED elements in declaration order.
     *
     * <p>A child either carries its geometry INLINE (its own {@code elements}/{@code textures}; e.g. the ~81
     * machine/generator composites and the factory {@code front_led} child) OR references it via a {@code "parent"}
     * (e.g. every factory {@code base} child &rarr; {@code factory/&lt;type&gt;/base}). For the latter we walk the
     * parent model chain off the {@link ResourceManager}, accumulating textures down the chain (a child-most definition
     * wins on key conflict, matching vanilla model inheritance) and taking elements from wherever they are first
     * defined. A child may have BOTH a parent and its own inline elements/textures &mdash; both are merged (inline wins
     * on textures, inline elements appended after resolved ones). A parent that is itself composite is resolved
     * recursively (e.g. {@code nutritional_liquifier}'s {@code base} &rarr; the composite {@code
     * nutritional_liquifier_base}). Texture {@code "#refs"} are left as-is; they bind at bake time against the merged map.
     */
    private static JsonObject flatten(JsonObject composite, ResourceManager resourceManager) {
        JsonObject out = new JsonObject();
        out.addProperty("parent", FLAT_PARENT);
        out.addProperty("render_type", FLAT_RENDER_TYPE);

        ResolvedGeometry merged = new ResolvedGeometry();
        // Parent (top-level) textures first — typically the "particle" entry.
        if (composite.has("textures") && composite.get("textures").isJsonObject()) {
            mergeTextures(merged.textures, composite.getAsJsonObject("textures"));
        }
        if (composite.has("children") && composite.get("children").isJsonObject()) {
            JsonObject children = composite.getAsJsonObject("children");
            for (Map.Entry<String, JsonElement> child : children.entrySet()) {
                if (!child.getValue().isJsonObject()) {
                    continue;
                }
                ResolvedGeometry resolved = resolveChild(child.getValue().getAsJsonObject(), resourceManager, 0);
                // Merge each child's resolved textures over the top-level map (child wins on conflict) + append elements.
                mergeTextures(merged.textures, resolved.textures);
                merged.elements.addAll(resolved.elements);
            }
        }
        out.add("textures", merged.textures);
        out.add("elements", merged.elements);
        return out;
    }

    /**
     * Resolve one composite child to its concrete {elements, textures}. Uses the child's inline geometry if present and,
     * when the child references a {@code "parent"} model, walks that chain via the {@link ResourceManager} to source the
     * elements/textures. Inline definitions on the child win over inherited ones (vanilla inheritance order).
     */
    private static ResolvedGeometry resolveChild(JsonObject child, ResourceManager resourceManager, int depth) {
        ResolvedGeometry result = new ResolvedGeometry();
        // 1) Inherit from the parent chain (lower priority), if the child references one.
        if (child.has("parent") && child.get("parent").isJsonPrimitive()) {
            ResolvedGeometry inherited = resolveModelChain(child.get("parent").getAsString(), resourceManager, depth + 1);
            mergeTextures(result.textures, inherited.textures);
            result.elements.addAll(inherited.elements);
        }
        // 2) The child's own inline textures/elements (higher priority): override textures, append elements.
        if (child.has("textures") && child.get("textures").isJsonObject()) {
            mergeTextures(result.textures, child.getAsJsonObject("textures"));
        }
        if (child.has("elements") && child.get("elements").isJsonArray()) {
            result.elements.addAll(child.getAsJsonArray("elements"));
        }
        return result;
    }

    /**
     * Walk a model-id parent chain ({@code ns:block/foo}), loading each model JSON off the {@link ResourceManager} and
     * accumulating textures (descendant-most wins) until a model defines {@code elements}. If a model in the chain is
     * itself a composite, its children are resolved recursively (each child possibly resolving its own parent chain) and
     * their geometry merged in. Vanilla-namespace ancestors (e.g. {@code minecraft:block/block}) have no geometry to
     * contribute and simply terminate the walk.
     */
    private static ResolvedGeometry resolveModelChain(String modelId, ResourceManager resourceManager, int depth) {
        ResolvedGeometry result = new ResolvedGeometry();
        String current = modelId;
        for (int i = depth; i < MAX_PARENT_DEPTH && current != null; i++) {
            JsonObject model = loadModel(current, resourceManager);
            if (model == null) {
                break; // missing or vanilla model — nothing more to inherit
            }
            // A composite ancestor: fold in each of its children's resolved geometry, then stop (composite has no own elements).
            if (isComposite(model)) {
                if (model.has("textures") && model.get("textures").isJsonObject()) {
                    mergeTextures(result.textures, model.getAsJsonObject("textures"));
                }
                if (model.has("children") && model.get("children").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> child : model.getAsJsonObject("children").entrySet()) {
                        if (!child.getValue().isJsonObject()) {
                            continue;
                        }
                        ResolvedGeometry resolved = resolveChild(child.getValue().getAsJsonObject(), resourceManager, i + 1);
                        mergeTextures(result.textures, resolved.textures);
                        result.elements.addAll(resolved.elements);
                    }
                }
                return result;
            }
            // A normal model: collect its textures; if it has elements we're done; else keep walking its own parent.
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                mergeTextures(result.textures, model.getAsJsonObject("textures"));
            }
            if (model.has("elements") && model.get("elements").isJsonArray() && result.elements.isEmpty()) {
                result.elements.addAll(model.getAsJsonArray("elements"));
                return result;
            }
            current = model.has("parent") && model.get("parent").isJsonPrimitive() ? model.get("parent").getAsString() : null;
        }
        return result;
    }

    /** Load a model JSON ({@code ns:path} -> {@code assets/ns/models/path.json}) off the resource manager; null if absent/malformed. */
    private static JsonObject loadModel(String modelId, ResourceManager resourceManager) {
        Identifier id = Identifier.tryParse(modelId);
        if (id == null) {
            return null;
        }
        Identifier resourceId = Identifier.fromNamespaceAndPath(id.getNamespace(), MODELS_PREFIX + id.getPath() + JSON_SUFFIX);
        Resource resource = resourceManager.getResource(resourceId).orElse(null);
        if (resource == null) {
            return null;
        }
        String raw = read(resource);
        if (raw == null) {
            return null;
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Merge {@code source} into {@code target}, source overriding on key conflict (descendant-most-wins inheritance). */
    private static void mergeTextures(JsonObject target, JsonObject source) {
        for (Map.Entry<String, JsonElement> e : source.entrySet()) {
            target.add(e.getKey(), e.getValue());
        }
    }

    /** A child's (or chain's) resolved geometry: the concatenated elements and the merged texture map. */
    private static final class ResolvedGeometry {
        private final JsonObject textures = new JsonObject();
        private final JsonArray elements = new JsonArray();
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
