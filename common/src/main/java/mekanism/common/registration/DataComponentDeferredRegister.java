package mekanism.common.registration;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Loader-neutral deferred-register for {@link DataComponentType}s, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.DataComponentDeferredRegister}. Carries the leaf (vanilla-codec) register
 * conveniences; the NeoForge-only helpers (frequency-aware, attachment-container codecs) stay on the NeoForge register
 * until the chemical/fluid data-type closure is migrated. Proves the {@code DataComponentType<?>} registry shape
 * (registry type {@code DataComponentType<?>} vs specific {@code DataComponentType<TYPE>}) on Architectury.
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

    public MekanismRegistryObject<DataComponentType<Integer>> registerNonNegativeInt(String name) {
        return simple(name, builder -> builder.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    public MekanismRegistryObject<DataComponentType<Long>> registerLong(String name) {
        return simple(name, builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public List<MekanismRegistryObject<? extends DataComponentType<?>>> getEntries() {
        return entries;
    }
}
