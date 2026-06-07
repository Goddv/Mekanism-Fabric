package mekanism.common.registration;

import com.mojang.datafixers.util.Either;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

/**
 * Loader-neutral registry handle wrapping an Architectury {@link RegistrySupplier}. Implements {@link Holder} (by
 * delegation — the RegistrySupplier is itself a holder) plus {@link Supplier} and {@link INamedEntry}, so it drops into
 * the same call sites as the old NeoForge {@code DeferredHolder}-based objects. Subclass to add registry-specific helpers
 * (e.g. translation keys). This is the Architectury-based counterpart of {@link MekanismDeferredHolder}; registries are
 * migrated onto it one at a time, after which the NeoForge base pair can be retired.
 */
@NothingNullByDefault
public class MekanismRegistryObject<T> implements Holder<T>, Supplier<T>, INamedEntry {

    private final RegistrySupplier<T> holder;

    public MekanismRegistryObject(RegistrySupplier<T> holder) {
        this.holder = holder;
    }

    @Override
    public T get() {
        return holder.get();
    }

    @Override
    public Identifier getId() {
        return holder.getId();
    }

    // ---- Holder<T> delegation ----

    @Override
    public T value() {
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
    public boolean is(ResourceKey<T> key) {
        return holder.is(key);
    }

    @Override
    public boolean is(Predicate<ResourceKey<T>> predicate) {
        return holder.is(predicate);
    }

    @Override
    public boolean is(TagKey<T> tag) {
        return holder.is(tag);
    }

    @Override
    public boolean is(Holder<T> other) {
        return holder.is(other);
    }

    @Override
    public Stream<TagKey<T>> tags() {
        return holder.tags();
    }

    @Override
    public DataComponentMap components() {
        return holder.components();
    }

    @Override
    public Either<ResourceKey<T>, T> unwrap() {
        return holder.unwrap();
    }

    @Override
    public Optional<ResourceKey<T>> unwrapKey() {
        return holder.unwrapKey();
    }

    @Override
    public Holder.Kind kind() {
        return holder.kind();
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> owner) {
        return holder.canSerializeIn(owner);
    }
}
