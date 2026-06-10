package mekanism.common.registration;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

/**
 * Loader-neutral deferred-register for {@link DataComponentType}s, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.DataComponentDeferredRegister}. Carries the leaf (vanilla-codec) register
 * conveniences; the NeoForge-only helpers (frequency-aware, attachment-container codecs) stay on the NeoForge register
 * until those neoforge-only payload closures are migrated. Proves the {@code DataComponentType<?>} registry shape
 * (registry type {@code DataComponentType<?>} vs specific {@code DataComponentType<TYPE>}) on Architectury.
 *
 * <p>The leaf helper surface here is kept BYTE-FOR-BYTE aligned with the NeoForge impl so that when the real
 * {@code MekanismDataComponents} eventually migrates its loader-clean components onto this register, each produced
 * {@code DataComponentType} is identical to today's NeoForge component (same persistent/network codecs) — preserving
 * save and protocol compatibility.
 */
@NothingNullByDefault
public class DataComponentDeferredRegister extends MekanismRegister<DataComponentType<?>> {

    private final List<MekanismRegistryObject<? extends DataComponentType<?>>> entries = new ArrayList<>();

    public DataComponentDeferredRegister(String modid) {
        super(modid, Registries.DATA_COMPONENT_TYPE);
    }

    /** Registers a component built by applying {@code operator} to a fresh {@link DataComponentType.Builder}. */
    public <TYPE> MekanismRegistryObject<DataComponentType<TYPE>> simple(String name, UnaryOperator<DataComponentType.Builder<TYPE>> operator) {
        RegistrySupplier<DataComponentType<TYPE>> supplier = doRegister(name, () -> operator.apply(DataComponentType.builder()).build());
        MekanismRegistryObject<DataComponentType<TYPE>> holder = new MekanismRegistryObject<>(supplier);
        entries.add(holder);
        return holder;
    }

    public MekanismRegistryObject<DataComponentType<Boolean>> registerBoolean(String name) {
        return simple(name, builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    }

    public MekanismRegistryObject<DataComponentType<Integer>> registerInt(String name) {
        return simple(name, builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    /**
     * Registers a strictly-POSITIVE int component (minimum 1; 0 is rejected) — aligned with the NeoForge impl's
     * {@code ExtraCodecs.POSITIVE_INT} so the produced component is byte-identical. Despite the "non-negative" name,
     * a value of 0 is NOT storable.
     */
    public MekanismRegistryObject<DataComponentType<Integer>> registerNonNegativeInt(String name) {
        return simple(name, builder -> builder.persistent(ExtraCodecs.POSITIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    public MekanismRegistryObject<DataComponentType<Long>> registerLong(String name) {
        return simple(name, builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public MekanismRegistryObject<DataComponentType<Long>> registerNonNegativeLong(String name) {
        return simple(name, builder -> builder.persistent(SerializerHelper.POSITIVE_LONG_CODEC).networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public MekanismRegistryObject<DataComponentType<UUID>> registerUUID(String name) {
        return simple(name, builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    }

    public MekanismRegistryObject<DataComponentType<Component>> registerComponent(String name) {
        //Copied from DataComponents.CUSTOM_NAME and ITEM_NAME
        return simple(name, builder -> builder.persistent(ComponentSerialization.CODEC).networkSynchronized(ComponentSerialization.STREAM_CODEC).cacheEncoding());
    }

    public <TYPE> MekanismRegistryObject<DataComponentType<ResourceKey<TYPE>>> registerResourceKey(String name, ResourceKey<? extends Registry<TYPE>> registryKey) {
        return simple(name, builder -> builder.persistent(ResourceKey.codec(registryKey)).networkSynchronized(ResourceKey.streamCodec(registryKey)));
    }

    public List<MekanismRegistryObject<? extends DataComponentType<?>>> getEntries() {
        return entries;
    }
}
