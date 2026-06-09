package mekanism.common.config;

import mekanism.common.config.value.CachedValue;

public interface IMekanismConfig {

    String getFileName();

    String getTranslation();

    /** Loader-neutral config type, used by the (loader-specific) registration path to bind this config to a file. */
    ConfigType getConfigType();

    /** The built spec handle for this config (load-state + persistence). */
    IConfigSpec getSpec();

    boolean isLoaded();

    void save();

    void clearCache(boolean unloading);

    void addCachedValue(CachedValue<?> configValue);
}
