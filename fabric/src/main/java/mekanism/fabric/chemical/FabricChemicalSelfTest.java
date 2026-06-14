package mekanism.fabric.chemical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.MekanismAPIBase;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.recipes.codec.MekanismExtraCodecs;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.CompoundChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.SingleChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.TagChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.CommonIngredientCreatorAccess;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the chemical core + capability on Fabric. Proves, on a live Fabric server: (1) the
 * hoisted {@code :common} chemical types ({@link Chemical}/{@link ChemicalStack}) work end-to-end via the custom
 * chemical registry created with fabric-api ({@link FabricChemicalRegistry}) + the {@code IChemicalRegistryProvider}
 * service; (2) the {@link MekanismFabricChemical#SIDED} {@code BlockApiLookup<IChemicalHandler>} resolves a real
 * chemical handler from a world position and stores a {@link ChemicalStack} (the chemical capability, mirroring
 * energy/heat). Grep for {@code [Mekanism/Fabric][chemical-selftest] RESULT:}.
 */
public final class FabricChemicalSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][chemical-selftest]";

    private FabricChemicalSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        try {
            Identifier demoId = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "fabric_demo_chemical");
            boolean registryOk = FabricChemicalRegistry.registry() != null
                  && FabricChemicalRegistry.registry().containsKey(demoId)
                  && FabricChemicalRegistry.registry().containsKey(MekanismAPIBase.EMPTY_CHEMICAL_KEY.identifier());

            Chemical demo = FabricChemicalRegistry.demo().value();
            boolean chemicalOk = demo.getTint() == 0x55AAFF
                  && demo.getTranslationKey().equals("chemical.mekanism.fabric_demo_chemical");

            ChemicalStack stack = new ChemicalStack(FabricChemicalRegistry.demo(), 1_000L);
            boolean stackOk = !stack.isEmpty() && stack.amount() == 1_000L && stack.getChemical() == demo
                  && ChemicalStack.EMPTY.isEmpty();

            boolean emptyOk = FabricChemicalRegistry.empty() != null
                  && FabricChemicalRegistry.empty().is(MekanismAPIBase.EMPTY_CHEMICAL_KEY);

            // Chemical CAPABILITY end-to-end: place the demo block-entity, resolve its IChemicalHandler via the
            // BlockApiLookup, and store a chemical through it.
            boolean capabilityOk = false;
            BlockPos pos = new BlockPos(0, 64, 16);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricEnergyBlockDemo.BLOCK.get().defaultBlockState(), 3);
            IChemicalHandler handler = MekanismFabricChemical.getChemicalHandler(level, pos, null);
            if (handler != null && handler.getChemicalTanks() > 0) {
                ChemicalStack toInsert = new ChemicalStack(FabricChemicalRegistry.demo(), 5_000L);
                ChemicalStack leftover = handler.insertChemical(0, toInsert, Action.EXECUTE);
                ChemicalStack stored = handler.getChemicalInTank(0);
                capabilityOk = leftover.isEmpty() && stored.amount() == 5_000L && stored.getChemical() == demo;
            }
            level.removeBlock(pos, false);

            // aliasedFieldOf shim wire-parity: the hoisted CompoundChemicalIngredient.CODEC now routes through
            // MekanismExtraCodecs.aliasedFieldOf on BOTH loaders, so it MUST reproduce NeoForge's exact
            // encode-first-name / decode-any-name behavior. Exercise the shim directly (NOT via the throwing stub
            // creator) on a representative String codec keyed by the same names CompoundChemicalIngredient uses.
            boolean aliasShimOk = validateAliasShim();

            // REAL Fabric chemical-ingredient creator + dispatch codec: now that the two creator impls are hoisted to
            // :common and the chemical_ingredient_type registry is built on Fabric, exercise creation + the dispatch
            // round-trip through CommonIngredientCreatorAccess (NOT a throwing stub).
            boolean creatorOk = validateRealCreator(level);

            ok = registryOk && chemicalOk && stackOk && emptyOk && capabilityOk && aliasShimOk && creatorOk;
            LOGGER.info("{} {} chemical core+capability: registry={} chemical={} stack={} empty={} capability={} aliasShim={} creator={}",
                  TAG, ok ? "OK  " : "FAIL", registryOk, chemicalOk, stackOk, emptyOk, capabilityOk, aliasShimOk, creatorOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL chemical test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }

    /**
     * Validates the {@link MekanismExtraCodecs#aliasedFieldOf} shim reproduces NeoForge's encode-first-name /
     * decode-any-name semantics, matching {@code CompoundChemicalIngredient.CODEC}'s use of
     * ({@link SerializationConstants#CHILDREN}, {@link SerializationConstants#INGREDIENTS}). Three assertions:
     * (a) ENCODE writes only the FIRST name key ("children"), never the alias; (b) DECODE accepts the FIRST name key;
     * (c) DECODE accepts the ALIAS name key ("ingredients"). Wire-format guard for recipe JSON / network sync without
     * needing the (NeoForge-only) dispatch codec.
     */
    private static boolean validateAliasShim() {
        try {
            MapCodec<String> mapCodec = MekanismExtraCodecs.aliasedFieldOf(Codec.STRING, SerializationConstants.CHILDREN, SerializationConstants.INGREDIENTS);
            Codec<String> codec = mapCodec.codec();

            // (a) ENCODE: must write the FIRST name ("children") and NOT the alias ("ingredients").
            JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, "foo").getOrThrow();
            boolean encodeOk = encoded.isJsonObject();
            if (encodeOk) {
                JsonObject obj = encoded.getAsJsonObject();
                encodeOk = obj.has(SerializationConstants.CHILDREN)
                      && !obj.has(SerializationConstants.INGREDIENTS)
                      && obj.get(SerializationConstants.CHILDREN).getAsString().equals("foo");
            }

            // (b) DECODE: must accept the FIRST name key.
            JsonObject firstNameJson = new JsonObject();
            firstNameJson.addProperty(SerializationConstants.CHILDREN, "bar");
            boolean decodeFirstOk = "bar".equals(codec.parse(JsonOps.INSTANCE, firstNameJson).getOrThrow());

            // (c) DECODE: must accept the ALIAS name key.
            JsonObject aliasJson = new JsonObject();
            aliasJson.addProperty(SerializationConstants.INGREDIENTS, "baz");
            boolean decodeAliasOk = "baz".equals(codec.parse(JsonOps.INSTANCE, aliasJson).getOrThrow());

            boolean shimOk = encodeOk && decodeFirstOk && decodeAliasOk;
            LOGGER.info("{} {} aliasedFieldOf shim parity: encodeFirstName={} decodeFirstName={} decodeAlias={}",
                  TAG, shimOk ? "OK  " : "FAIL", encodeOk, decodeFirstOk, decodeAliasOk);
            return shimOk;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL aliasedFieldOf shim parity threw", TAG, t);
            return false;
        }
    }

    /**
     * Validates the REAL Fabric chemical-ingredient creator + dispatch codec (the goal of this increment). Proves, via
     * the loader-neutral {@link CommonIngredientCreatorAccess} routed through {@code FabricMekanismAccess} →
     * {@link mekanism.common.recipe.ingredients.ChemicalIngredientCreator}: (a) creation works ({@code of}/{@code tag}/
     * {@code compound}) and {@code test(demo)} behaves; (b) the {@code dispatchMapOrElse} codec round-trips a
     * {@link SingleChemicalIngredient} (no {@code type} key → fallback branch) and a {@link CompoundChemicalIngredient}
     * (has {@code type} key → dispatch into the Fabric {@code chemical_ingredient_type} registry) through JSON and back;
     * (c) a {@link ChemicalStackIngredient} built via {@code chemicalStack().from(single, 1000L)} round-trips its CODEC.
     * Encoding uses a registry-aware serialization context so the chemical Holder/Tag references resolve.
     */
    private static boolean validateRealCreator(ServerLevel level) {
        try {
            IChemicalIngredientCreator chemical = CommonIngredientCreatorAccess.chemical();
            IChemicalStackIngredientCreator chemicalStack = CommonIngredientCreatorAccess.chemicalStack();
            // Registry-aware ops: SingleChemicalIngredient/TagChemicalIngredient codecs reference the chemical registry.
            DynamicOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            // (a) creation: of(demo) is a SingleChemicalIngredient; tag(...); compound(List.of(two ingredients)).
            // (demo is the only non-empty chemical on Fabric; SingleChemicalIngredient rejects the empty chemical by
            // design, so the compound mixes the single + the tag ingredient to get two distinct children.)
            ChemicalIngredient single = chemical.of(FabricChemicalRegistry.demo());
            ChemicalIngredient tag = chemical.tag(TagKey.create(MekanismAPIBase.CHEMICAL_REGISTRY_NAME,
                  Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "selftest_tag")));
            ChemicalIngredient compound = chemical.compound(List.of(single, tag));
            boolean buildOk = single instanceof SingleChemicalIngredient
                  && tag instanceof TagChemicalIngredient
                  && compound instanceof CompoundChemicalIngredient compoundIngredient
                  && compoundIngredient.children().size() == 2
                  // test(demo) must be true for the single matching the demo chemical.
                  && single.test(FabricChemicalRegistry.demo());

            // (b) DISPATCH round-trip: a single ingredient serializes WITHOUT a type key (fallback branch).
            Codec<ChemicalIngredient> codec = chemical.codec();
            JsonElement singleJson = codec.encodeStart(ops, single).getOrThrow();
            boolean singleNoTypeKey = singleJson.isJsonObject() && !singleJson.getAsJsonObject().has("type");
            ChemicalIngredient singleBack = codec.parse(ops, singleJson).getOrThrow();
            boolean singleRoundTripOk = singleNoTypeKey && single.equals(singleBack);

            // a compound ingredient serializes WITH a type key → dispatched through the Fabric type registry.
            JsonElement compoundJson = codec.encodeStart(ops, compound).getOrThrow();
            ChemicalIngredient compoundBack = codec.parse(ops, compoundJson).getOrThrow();
            boolean compoundRoundTripOk = compound.equals(compoundBack);

            // (c) ChemicalStackIngredient via chemicalStack().from(single, amount), round-trip its CODEC.
            ChemicalStackIngredient stackIngredient = chemicalStack.from(single, 1_000L);
            Codec<ChemicalStackIngredient> stackCodec = chemicalStack.codec();
            JsonElement stackJson = stackCodec.encodeStart(ops, stackIngredient).getOrThrow();
            ChemicalStackIngredient stackBack = stackCodec.parse(ops, stackJson).getOrThrow();
            boolean stackRoundTripOk = stackIngredient.equals(stackBack) && stackBack.amount() == 1_000L;

            boolean creatorOk = buildOk && singleRoundTripOk && compoundRoundTripOk && stackRoundTripOk;
            LOGGER.info("{} {} real creator+dispatch: build={} singleRoundTrip(noType)={} compoundRoundTrip(dispatch)={} stackRoundTrip={}",
                  TAG, creatorOk ? "OK  " : "FAIL", buildOk, singleRoundTripOk, compoundRoundTripOk, stackRoundTripOk);
            return creatorOk;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL real creator+dispatch threw", TAG, t);
            return false;
        }
    }
}
