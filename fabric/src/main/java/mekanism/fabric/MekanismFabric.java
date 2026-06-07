package mekanism.fabric;

import com.mojang.logging.LogUtils;
import mekanism.common.registries.MekanismGameEvents;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.registries.MekanismSounds;
import mekanism.fabric.content.FabricBringUpContent;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import mekanism.fabric.energy.FabricEnergySelfTest;
import mekanism.fabric.registration.FabricRegistrationSelfTest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Fabric entrypoint for the Mekanism multi-loader port.
 *
 * <p>Early bring-up build: real content is wired up as loader-neutral code is hoisted into {@code :common}. The first
 * real registry — Mekanism's {@link MekanismSounds sound events} — now registers here through the Architectury-based
 * registration framework shared with NeoForge. In a development environment it additionally runs the energy and
 * registration self-tests on a live server; those are skipped in production.
 */
public final class MekanismFabric implements ModInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("[Mekanism/Fabric] Fabric build initialized — Architectury no-remap toolchain OK.");
        // Real content registration (shared loader-neutral path; finalized per-loader).
        MekanismSounds.SOUND_EVENTS.register();
        MekanismGameEvents.GAME_EVENTS.register();
        MekanismParticleTypes.PARTICLE_TYPES.register();
        // Transitional: a slice of real simple items + a Mekanism creative tab, so content is visible
        // in-game on Fabric. Removed once the full item/block framework is migrated to :common.
        FabricBringUpContent.init();
        // Transitional: a functional energy block (block-entity that stores energy + exposes the strict-energy
        // capability via BlockApiLookup) — the foundation pattern for real Mekanism machines on Fabric.
        FabricEnergyBlockDemo.init();
        // Dev-only bring-up validation; skipped in production.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            FabricEnergySelfTest.run();
            FabricRegistrationSelfTest.run();
        }
    }
}
