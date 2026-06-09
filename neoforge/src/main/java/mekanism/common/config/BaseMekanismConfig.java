package mekanism.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mekanism.common.Mekanism;
import mekanism.common.config.value.CachedValue;

public abstract class BaseMekanismConfig implements INeoMekanismConfig {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final List<CachedValue<?>> cachedConfigValues = new ArrayList<>();

    /** The built spec handle; assigned by each subclass ctor from {@code builder.build()}. */
    protected IConfigSpec configSpec;

    @Override
    public IConfigSpec getSpec() {
        return configSpec;
    }

    @Override
    public boolean isLoaded() {
        return configSpec != null && configSpec.isLoaded();
    }

    @Override
    public void clearCache(boolean unloading) {
        for (CachedValue<?> cachedConfigValue : cachedConfigValues) {
            cachedConfigValue.clearCache(unloading);
        }
    }

    @Override
    public void addCachedValue(CachedValue<?> configValue) {
        cachedConfigValues.add(configValue);
    }

    @Override
    public void save() {
        EXECUTOR.submit(new ConfigSaver(configSpec));
    }

    private static class ConfigSaver implements Runnable {

        private final IConfigSpec configSpec;
        private int retries = 0;

        private ConfigSaver(IConfigSpec configSpec) {
            this.configSpec = configSpec;
        }

        @Override
        public void run() {
            try {
                configSpec.save();
            } catch (Exception e) {
                Mekanism.logger.error("Failed to save config", e);
                if (retries++ < 3) {
                    EXECUTOR.submit(this);
                } else {
                    Mekanism.logger.error("Giving up");
                }
            }
        }
    }
}
