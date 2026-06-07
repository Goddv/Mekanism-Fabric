package mekanism.fabric.registration;

import com.mojang.logging.LogUtils;
import mekanism.common.registries.MekanismGameEvents;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.registries.MekanismSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation that Mekanism's real content registers on Fabric through the Architectury-based
 * registration framework. Gated behind {@code isDevelopmentEnvironment()} by its caller; the actual registration happens
 * unconditionally in the entrypoint. Confirms representative entries from each migrated registry (sounds, game events)
 * are present in their vanilla registries and resolvable via their registry objects. Grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricRegistrationSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][registration-selftest]";

    private FabricRegistrationSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static void validate() {
        boolean ok = false;
        try {
            boolean soundOk = check("sound", BuiltInRegistries.SOUND_EVENT.containsKey(MekanismSounds.ENRICHMENT_CHAMBER.getId()),
                  MekanismSounds.ENRICHMENT_CHAMBER.get() != null, MekanismSounds.ENRICHMENT_CHAMBER.getId());
            boolean gameEventOk = check("game_event", BuiltInRegistries.GAME_EVENT.containsKey(MekanismGameEvents.JETPACK_BURN.getId()),
                  MekanismGameEvents.JETPACK_BURN.get() != null, MekanismGameEvents.JETPACK_BURN.getId());
            boolean particleOk = check("particle", BuiltInRegistries.PARTICLE_TYPE.containsKey(MekanismParticleTypes.LASER.getId()),
                  MekanismParticleTypes.LASER.get() != null, MekanismParticleTypes.LASER.getId());
            long mekSounds = BuiltInRegistries.SOUND_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekGameEvents = BuiltInRegistries.GAME_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekParticles = BuiltInRegistries.PARTICLE_TYPE.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            ok = soundOk && gameEventOk && particleOk;
            LOGGER.info("{} {} Architectury registration: mekanismSounds={} mekanismGameEvents={} mekanismParticles={}",
                  TAG, ok ? "OK  " : "FAIL", mekSounds, mekGameEvents, mekParticles);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL registration test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }

    private static boolean check(String kind, boolean inRegistry, boolean supplierResolved, Object id) {
        boolean ok = inRegistry && supplierResolved;
        LOGGER.info("{} {} {} probe={} inRegistry={} supplierResolved={}", TAG, ok ? "OK  " : "FAIL", kind, id, inRegistry, supplierResolved);
        return ok;
    }
}
