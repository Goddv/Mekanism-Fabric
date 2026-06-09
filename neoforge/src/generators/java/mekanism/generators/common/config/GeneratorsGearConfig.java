package mekanism.generators.common.config;

import mekanism.common.config.ConfigType;
import mekanism.common.config.IConfigBuilderFactory;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.GearConfig;
import mekanism.common.config.MekanismConfigTranslations;
import mekanism.common.config.value.CachedFloatValue;
import mekanism.common.config.value.CachedLongValue;

public class GeneratorsGearConfig extends BaseMekanismConfig {

    //MekaSuit
    public final CachedLongValue mekaSuitGeothermalChargingRate;
    public final CachedFloatValue mekaSuitHeatDamageReductionRatio;

    GeneratorsGearConfig() {
        IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();

        MekanismConfigTranslations.GEAR_MEKA_SUIT.applyToBuilder(builder).push(GearConfig.MEKASUIT_CATEGORY);
        mekaSuitGeothermalChargingRate = CachedLongValue.wrap(this, GeneratorsConfigTranslations.GEAR_MEKA_SUIT_GEOTHERMAL.applyToBuilder(builder)
              .defineInRange("geothermalChargingRate", 10L, 0, Long.MAX_VALUE / 8));

        MekanismConfigTranslations.GEAR_MEKA_SUIT_DAMAGE_ABSORPTION.applyToBuilder(builder).push(GearConfig.MEKASUIT_DAMAGE_CATEGORY);
        mekaSuitHeatDamageReductionRatio = CachedFloatValue.wrap(this, GeneratorsConfigTranslations.GEAR_MEKA_SUIT_HEAT_DAMAGE.applyToBuilder(builder)
              .defineInRange("heatDamageReductionRatio", 0.8, 0, 1));
        builder.pop(2);

        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "generators-gear";
    }

    @Override
    public String getTranslation() {
        return "Gear Config";
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.SERVER;
    }
}