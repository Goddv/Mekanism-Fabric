package mekanism.fabric.registration;

import com.mojang.logging.LogUtils;
import mekanism.common.registration.MekanismArchRegistryProbe;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the registration slice. Gated behind {@code isDevelopmentEnvironment()} by its caller,
 * so it never runs for end users. It proves, on a live Fabric server, that Architectury's {@link MekanismArchRegistryProbe
 * DeferredRegister} registers content under archloom no-remap on MC 26.1: it finalizes the deferred registration and then
 * confirms the registered {@link SoundEvent} is both resolvable through the Architectury {@code RegistrySupplier} and
 * present in the vanilla {@link BuiltInRegistries#SOUND_EVENT} registry.
 *
 * <p>Grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricRegistrationSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][registration-selftest]";

    private FabricRegistrationSelfTest() {
    }

    public static void run() {
        // Finalize the Architectury deferred registrations into the active registry (during mod init, before freeze).
        MekanismArchRegistryProbe.init();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static void validate() {
        boolean ok = false;
        try {
            SoundEvent viaSupplier = MekanismArchRegistryProbe.PROBE.get();
            boolean inRegistry = BuiltInRegistries.SOUND_EVENT.containsKey(MekanismArchRegistryProbe.PROBE_ID);
            SoundEvent viaRegistry = BuiltInRegistries.SOUND_EVENT.getValue(MekanismArchRegistryProbe.PROBE_ID);
            ok = viaSupplier != null && inRegistry && viaRegistry != null;
            LOGGER.info("{} {} Architectury DeferredRegister: id={} inRegistry={} supplierResolved={} sameInstance={}",
                  TAG, ok ? "OK  " : "FAIL", MekanismArchRegistryProbe.PROBE_ID, inRegistry, viaSupplier != null, viaRegistry == viaSupplier);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL registration test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
