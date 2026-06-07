package mekanism.fabric.registration;

import com.mojang.logging.LogUtils;
import mekanism.common.registries.MekanismSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation that Mekanism's real content registers on Fabric through the Architectury-based
 * registration framework. Gated behind {@code isDevelopmentEnvironment()} by its caller; the actual registration
 * (MekanismSounds.SOUND_EVENTS.register()) happens unconditionally in the entrypoint. This confirms a representative
 * Mekanism {@link SoundEvent} is resolvable via its {@code SoundEventRegistryObject} and present in
 * {@link BuiltInRegistries#SOUND_EVENT}. Grep for {@code RESULT:} to see PASS/FAIL.
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
            SoundEvent viaSupplier = MekanismSounds.ENRICHMENT_CHAMBER.get();
            var id = MekanismSounds.ENRICHMENT_CHAMBER.getId();
            boolean inRegistry = BuiltInRegistries.SOUND_EVENT.containsKey(id);
            SoundEvent viaRegistry = BuiltInRegistries.SOUND_EVENT.getValue(id);
            int total = BuiltInRegistries.SOUND_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).toArray().length;
            ok = viaSupplier != null && inRegistry && viaRegistry != null;
            LOGGER.info("{} {} real Mekanism sounds via Architectury: probe={} inRegistry={} supplierResolved={} mekanismSounds={}",
                  TAG, ok ? "OK  " : "FAIL", id, inRegistry, viaSupplier != null, total);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL registration test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
