package mekanism.common.config.value;

import mekanism.common.config.IMekanismConfig;

public class CachedConfigValue<T> extends CachedResolvableConfigValue<T, T> {

    protected CachedConfigValue(IMekanismConfig config, IConfigValue<T> internal) {
        super(config, internal);
    }

    public static <T> CachedConfigValue<T> wrap(IMekanismConfig config, IConfigValue<T> internal) {
        return new CachedConfigValue<>(config, internal);
    }

    public static <T> CachedConfigValue<T> wrap(IMekanismConfig config, net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<T> internal) {
        return wrap(config, new NeoConfigValue<>(internal));
    }

    @Override
    protected T resolve(T encoded) {
        return encoded;
    }

    @Override
    protected T encode(T value) {
        return value;
    }
}