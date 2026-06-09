package mekanism.common.config.value;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge {@link IConfigEnumValue} wrapping a {@link ModConfigSpec.EnumValue} (which is itself a {@link ModConfigSpec.ConfigValue}). Delegates verbatim like
 * {@link NeoConfigValue}; exists only so {@code IConfigBuilder.defineEnum} can return the distinct {@link IConfigEnumValue} erasure.
 */
public final class NeoConfigEnumValue<V extends Enum<V>> implements IConfigEnumValue<V> {

    private final ModConfigSpec.ConfigValue<V> internal;

    public NeoConfigEnumValue(ModConfigSpec.ConfigValue<V> internal) {
        this.internal = internal;
    }

    @Override
    public V get() {
        return internal.get();
    }

    @Override
    public V getDefault() {
        return internal.getDefault();
    }

    @Override
    public void set(V value) {
        internal.set(value);
    }
}
