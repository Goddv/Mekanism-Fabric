package mekanism.common.config;

import mekanism.common.config.value.CachedValue;

public interface IMekanismConfig {

    String getFileName();

    String getTranslation();

    boolean isLoaded();

    void save();

    void clearCache(boolean unloading);

    void addCachedValue(CachedValue<?> configValue);
}
