package mekanism.common.registration.impl;

import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.common.registration.MekanismRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Loader-neutral {@link SoundEvent} register (Architectury-backed via {@link MekanismRegister}). {@code register(name)}
 * creates a variable-range sound under {@code modid:name}; {@link #register()} finalizes per loader.
 */
@NothingNullByDefault
public class SoundEventDeferredRegister extends MekanismRegister<SoundEvent> {

    public SoundEventDeferredRegister(String modid) {
        super(modid, Registries.SOUND_EVENT);
    }

    public SoundEventRegistryObject<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(modid, name);
        RegistrySupplier<SoundEvent> holder = registrar.register(id, () -> SoundEvent.createVariableRangeEvent(id));
        return new SoundEventRegistryObject<>(holder);
    }
}
