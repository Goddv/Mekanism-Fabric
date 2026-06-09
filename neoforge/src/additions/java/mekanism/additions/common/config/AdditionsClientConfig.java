package mekanism.additions.common.config;

import mekanism.common.config.ConfigType;
import mekanism.common.config.IConfigBuilderFactory;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedBooleanValue;

public class AdditionsClientConfig extends BaseMekanismConfig {

    public final CachedBooleanValue pushToTalk;

    AdditionsClientConfig() {
        IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();

        pushToTalk = CachedBooleanValue.wrap(this, AdditionsConfigTranslations.CLIENT_PUSH_TO_TALK.applyToBuilder(builder)
              .define("pushToTalk", true));

        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "additions-client";
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