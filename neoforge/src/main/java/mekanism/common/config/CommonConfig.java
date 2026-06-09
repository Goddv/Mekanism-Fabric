package mekanism.common.config;

import mekanism.common.config.value.CachedBooleanValue;
import mekanism.common.config.value.CachedEnumValue;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;

public class CommonConfig extends BaseMekanismConfig {

    public final CachedEnumValue<EnergyUnit> energyUnit;
    public final CachedEnumValue<TemperatureUnit> tempUnit;
    public final CachedBooleanValue enableDecayTimers;
    public final CachedBooleanValue copyBlockData;
    public final CachedBooleanValue holidays;

    CommonConfig() {
        IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();

        energyUnit = CachedEnumValue.wrap(this, MekanismConfigTranslations.COMMON_UNIT_ENERGY.applyToBuilder(builder)
              .defineEnum("energyType", EnergyUnit.FORGE_ENERGY));
        tempUnit = CachedEnumValue.wrap(this, MekanismConfigTranslations.COMMON_UNIT_TEMPERATURE.applyToBuilder(builder)
              .defineEnum("temperatureUnit", TemperatureUnit.KELVIN));
        enableDecayTimers = CachedBooleanValue.wrap(this, MekanismConfigTranslations.COMMON_DECAY_TIMERS.applyToBuilder(builder)
              .define("enableDecayTimers", true));
        copyBlockData = CachedBooleanValue.wrap(this, MekanismConfigTranslations.COMMON_COPY_BLOCK_DATA.applyToBuilder(builder)
              .define("copyBlockData", true));
        holidays = CachedBooleanValue.wrap(this, MekanismConfigTranslations.COMMON_HOLIDAYS.applyToBuilder(builder)
              .define("holidays", true));

        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "common";
    }

    @Override
    public String getTranslation() {
        return "Common Config";
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.COMMON;
    }
}