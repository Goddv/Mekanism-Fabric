package mekanism.fabric.chemical;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.MekanismAPIBase;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
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

            ok = registryOk && chemicalOk && stackOk && emptyOk && capabilityOk;
            LOGGER.info("{} {} chemical core+capability: registry={} chemical={} stack={} empty={} capability={}",
                  TAG, ok ? "OK  " : "FAIL", registryOk, chemicalOk, stackOk, emptyOk, capabilityOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL chemical test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
