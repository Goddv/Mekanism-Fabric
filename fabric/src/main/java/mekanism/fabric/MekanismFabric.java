package mekanism.fabric;

import mekanism.fabric.energy.FabricEnergySelfTest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint for the Mekanism multi-loader port.
 *
 * <p>Still an early bring-up build: real content is wired up as loader-neutral code is hoisted into {@code :common}
 * (see docs/FABRIC_PORT_PLAN.md, Phase 2 energy slice). In a development environment it runs
 * {@link FabricEnergySelfTest} to validate the energy slice (hoisted common energy API + the Fabric strict-energy
 * {@code BlockApiLookup}) on a live server; this is skipped in production.
 */
public final class MekanismFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[Mekanism/Fabric] Fabric build initialized — Architectury no-remap toolchain OK.");
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            FabricEnergySelfTest.run();
        }
    }
}
