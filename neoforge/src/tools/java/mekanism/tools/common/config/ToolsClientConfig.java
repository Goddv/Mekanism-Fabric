package mekanism.tools.common.config;

import mekanism.common.config.ConfigType;
import mekanism.common.config.IConfigBuilderFactory;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedBooleanValue;

public class ToolsClientConfig extends BaseMekanismConfig {

    public final CachedBooleanValue displayDurabilityTooltips;

    public ToolsClientConfig() {
        IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();

        this.displayDurabilityTooltips = CachedBooleanValue.wrap(this, ToolsConfigTranslations.CLIENT_DURABILITY_TOOLTIPS.applyToBuilder(builder)
              .define("displayDurabilityTooltips", true));

        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "tools-client";
    }

    @Override
    public String getTranslation() {
        return "Client Config";
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.CLIENT;
    }
}