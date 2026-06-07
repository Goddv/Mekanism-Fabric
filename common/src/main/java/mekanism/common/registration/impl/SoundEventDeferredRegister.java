package mekanism.common.registration.impl;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Loader-neutral {@link SoundEvent} register built on Architectury's {@link DeferredRegister}. Mirrors the old
 * NeoForge-only wrapper's surface ({@code register(name)} + a finalize step) so existing sound holders are unchanged
 * apart from the now loader-neutral finalize call ({@link #register()} replaces the old {@code register(IEventBus)}).
 */
@NothingNullByDefault
public class SoundEventDeferredRegister {

    private final String modid;
    private final DeferredRegister<SoundEvent> registrar;

    public SoundEventDeferredRegister(String modid) {
        this.modid = modid;
        this.registrar = DeferredRegister.create(modid, Registries.SOUND_EVENT);
    }

    public SoundEventRegistryObject<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(modid, name);
        RegistrySupplier<SoundEvent> holder = registrar.register(id, () -> SoundEvent.createVariableRangeEvent(id));
        return new SoundEventRegistryObject<>(holder);
    }

    /** Finalizes the deferred registrations into the active registry (hooks into whichever loader is running). */
    public void register() {
        registrar.register();
    }
}
