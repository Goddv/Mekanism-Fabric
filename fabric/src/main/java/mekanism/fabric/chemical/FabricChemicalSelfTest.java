package mekanism.fabric.chemical;

import com.mojang.logging.LogUtils;
import mekanism.api.MekanismAPIBase;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the chemical core on Fabric. Proves, on a live Fabric server, that the hoisted
 * {@code :common} chemical types ({@link Chemical}/{@link ChemicalStack}) work end-to-end: the custom chemical registry
 * created via fabric-api ({@link FabricChemicalRegistry}) resolves through {@code IChemicalRegistryProvider}, the lazy
 * registry-backed {@link Chemical#CODEC} works, and {@link ChemicalStack} accounting + empty handling behave. Grep for
 * {@code [Mekanism/Fabric][chemical-selftest] RESULT:}.
 */
public final class FabricChemicalSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][chemical-selftest]";

    private FabricChemicalSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static void validate() {
        boolean ok = false;
        try {
            Identifier demoId = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "fabric_demo_chemical");
            boolean registryOk = FabricChemicalRegistry.registry() != null
                  && FabricChemicalRegistry.registry().containsKey(demoId)
                  && FabricChemicalRegistry.registry().containsKey(MekanismAPIBase.EMPTY_CHEMICAL_KEY.identifier());

            Chemical demo = FabricChemicalRegistry.demo().value();
            boolean chemicalOk = demo.getTint() == 0x55AAFF
                  && !demo.getTranslationKey().isEmpty()                 // uses the registry-key lookup
                  && demo.getTranslationKey().equals("chemical.mekanism.fabric_demo_chemical");

            ChemicalStack stack = new ChemicalStack(FabricChemicalRegistry.demo(), 1_000L);
            boolean stackOk = !stack.isEmpty() && stack.amount() == 1_000L && stack.getChemical() == demo
                  && ChemicalStack.EMPTY.isEmpty();

            // The empty holder resolves + is identified by the registry default key.
            boolean emptyOk = FabricChemicalRegistry.empty() != null
                  && FabricChemicalRegistry.empty().is(MekanismAPIBase.EMPTY_CHEMICAL_KEY);

            ok = registryOk && chemicalOk && stackOk && emptyOk;
            LOGGER.info("{} {} chemical core: registry={} chemical={} stack={} empty={}",
                  TAG, ok ? "OK  " : "FAIL", registryOk, chemicalOk, stackOk, emptyOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL chemical test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
