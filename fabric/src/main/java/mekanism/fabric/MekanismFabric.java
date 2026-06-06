package mekanism.fabric;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint for the Mekanism multi-loader port.
 *
 * <p>Currently a minimal toolchain-validation stub: it proves the Architectury no-remap Fabric build
 * produces a loadable mod on Minecraft 26.1. Real behaviour is wired up as loader-neutral code is
 * hoisted into {@code :common} (see docs/FABRIC_PORT_PLAN.md, Phase 2 energy slice).
 */
public final class MekanismFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        System.out.println("[Mekanism/Fabric] Fabric test build initialized — Architectury no-remap toolchain OK.");
    }
}
