package mekanism.common.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public interface INeoMekanismConfig extends IMekanismConfig {

    ModConfigSpec getConfigSpec();

    ModConfig.Type getConfigType();

    @Override
    default boolean isLoaded() {
        return getConfigSpec().isLoaded();
    }
}
