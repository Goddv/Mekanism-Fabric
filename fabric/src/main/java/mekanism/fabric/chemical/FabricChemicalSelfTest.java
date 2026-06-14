package mekanism.fabric.chemical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import mekanism.api.Action;
import mekanism.api.MekanismAPIBase;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.recipes.codec.MekanismExtraCodecs;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

            ok = registryOk && chemicalOk && stackOk && emptyOk && capabilityOk && aliasShimOk;
            LOGGER.info("{} {} chemical core+capability: registry={} chemical={} stack={} empty={} capability={} aliasShim={}",
                  TAG, ok ? "OK  " : "FAIL", registryOk, chemicalOk, stackOk, emptyOk, capabilityOk, aliasShimOk);
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
}
