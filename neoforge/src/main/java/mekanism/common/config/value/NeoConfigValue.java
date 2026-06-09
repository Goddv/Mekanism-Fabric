package mekanism.common.config.value;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoConfigValue<T> implements IConfigValue<T> {

    private final ModConfigSpec.ConfigValue<T> internal;

    public NeoConfigValue(ModConfigSpec.ConfigValue<T> internal) {
        this.internal = internal;
    }

    @Override
    public T get() {
        return internal.get();
    }

    @Override
    public T getDefault() {
        return internal.getDefault();
    }

    @Override
    public void set(T value) {
        internal.set(value);
    }
}
