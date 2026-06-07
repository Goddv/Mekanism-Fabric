package mekanism.common.registration.impl;

import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.common.registration.MekanismRegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;

/**
 * Loader-neutral handle to a registered {@link SoundEvent}. Adds the auto-derived subtitle translation key (used by
 * datagen) on top of the shared {@link MekanismRegistryObject} (Holder + Supplier + INamedEntry, backed by Architectury).
 */
@NothingNullByDefault
public class SoundEventRegistryObject<SOUND extends SoundEvent> extends MekanismRegistryObject<SoundEvent> {

    private final String translationKey;

    public SoundEventRegistryObject(RegistrySupplier<SoundEvent> holder) {
        super(holder);
        this.translationKey = Util.makeDescriptionId("sound_event", holder.getId());
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
