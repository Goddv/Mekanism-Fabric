package mekanism.common.registration;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Minimal loader-neutral registration probe built on Architectury's {@link DeferredRegister}. It registers a single
 * placeholder {@link SoundEvent} ({@code mekanism:arch_registration_probe}) to validate, at runtime on both loaders under
 * archloom no-remap (MC 26.1), that Architectury registration works — the registration analog of the energy slice's
 * {@code BlockApiLookup} proof, and a preview of how Mekanism's registration framework will be rebased onto Architectury.
 *
 * <p>This is dev-only bring-up scaffolding: {@link #init()} is invoked only by the Fabric dev self-test, so the probe
 * never registers in a shipped jar. It will be removed once the real framework migration to Architectury lands.
 */
public final class MekanismArchRegistryProbe {

    public static final Identifier PROBE_ID = Identifier.fromNamespaceAndPath("mekanism", "arch_registration_probe");

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create("mekanism", Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> PROBE = SOUND_EVENTS.register(PROBE_ID, () -> SoundEvent.createVariableRangeEvent(PROBE_ID));

    private MekanismArchRegistryProbe() {
    }

    /** Finalizes the deferred registrations into the active registry (hooks into whichever loader is running). */
    public static void init() {
        SOUND_EVENTS.register();
    }
}
