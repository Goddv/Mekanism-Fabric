package mekanism.common.registration.impl;

import com.mojang.datafixers.util.Either;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.common.registration.INamedEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;

/**
 * Loader-neutral handle to a registered {@link SoundEvent}, backing onto Architectury's {@link RegistrySupplier}. It is
 * both a {@link Holder} and a {@link Supplier} of the sound (delegating to the supplier, which is itself a registry
 * holder) so it drops into the same call sites as the old NeoForge {@code DeferredHolder}-based object, and an
 * {@link INamedEntry} for pre-registration name access. Also exposes the auto-derived subtitle translation key for datagen.
 */
@NothingNullByDefault
public class SoundEventRegistryObject<SOUND extends SoundEvent> implements Holder<SoundEvent>, Supplier<SoundEvent>, INamedEntry {

    private final RegistrySupplier<SoundEvent> holder;
    private final String translationKey;

    public SoundEventRegistryObject(RegistrySupplier<SoundEvent> holder) {
        this.holder = holder;
        this.translationKey = Util.makeDescriptionId("sound_event", holder.getId());
    }

    @Override
    public SoundEvent get() {
        return holder.get();
    }

    @Override
    public Identifier getId() {
        return holder.getId();
    }

    public String getTranslationKey() {
        return translationKey;
    }

    // ---- Holder<SoundEvent> delegation (the Architectury RegistrySupplier is itself a Holder) ----

    @Override
    public SoundEvent value() {
        return holder.value();
    }

    @Override
    public boolean isBound() {
        return holder.isBound();
    }

    @Override
    public boolean areComponentsBound() {
        return holder.areComponentsBound();
    }

    @Override
    public boolean is(Identifier id) {
        return holder.is(id);
    }

    @Override
    public boolean is(ResourceKey<SoundEvent> key) {
        return holder.is(key);
    }

    @Override
    public boolean is(Predicate<ResourceKey<SoundEvent>> predicate) {
        return holder.is(predicate);
    }

    @Override
    public boolean is(TagKey<SoundEvent> tag) {
        return holder.is(tag);
    }

    @Override
    public boolean is(Holder<SoundEvent> other) {
        return holder.is(other);
    }

    @Override
    public Stream<TagKey<SoundEvent>> tags() {
        return holder.tags();
    }

    @Override
    public DataComponentMap components() {
        return holder.components();
    }

    @Override
    public Either<ResourceKey<SoundEvent>, SoundEvent> unwrap() {
        return holder.unwrap();
    }

    @Override
    public Optional<ResourceKey<SoundEvent>> unwrapKey() {
        return holder.unwrapKey();
    }

    @Override
    public Holder.Kind kind() {
        return holder.kind();
    }

    @Override
    public boolean canSerializeIn(HolderOwner<SoundEvent> owner) {
        return holder.canSerializeIn(owner);
    }
}
