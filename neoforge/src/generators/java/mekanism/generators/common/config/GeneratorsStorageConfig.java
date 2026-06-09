package mekanism.generators.common.config;

import mekanism.common.config.ConfigType;
import mekanism.common.config.IConfigBuilderFactory;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedLongValue;

public class GeneratorsStorageConfig extends BaseMekanismConfig {

    public final CachedLongValue heatGenerator;
    public final CachedLongValue bioGenerator;
    public final CachedLongValue solarGenerator;
    public final CachedLongValue advancedSolarGenerator;
    public final CachedLongValue windGenerator;

    GeneratorsStorageConfig() {
        IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();

        heatGenerator = CachedLongValue.definedMin(this, builder, GeneratorsConfigTranslations.ENERGY_STORAGE_GENERATOR_HEAT, "heatGenerator",
              12 * 20, 1);
        bioGenerator = CachedLongValue.definedMin(this, builder, GeneratorsConfigTranslations.ENERGY_STORAGE_GENERATOR_BIO, "bioGenerator",
              30 * 20, 1);
        solarGenerator = CachedLongValue.definedMin(this, builder, GeneratorsConfigTranslations.ENERGY_STORAGE_GENERATOR_SOLAR, "solarGenerator",
              2 * 20, 1);
        advancedSolarGenerator = CachedLongValue.definedMin(this, builder, GeneratorsConfigTranslations.ENERGY_STORAGE_GENERATOR_SOLAR_ADVANCED, "advancedSolarGenerator",
              12 * 20, 1);
        windGenerator = CachedLongValue.definedMin(this, builder, GeneratorsConfigTranslations.ENERGY_STORAGE_GENERATOR_WIND, "windGenerator",
              5 * 20, 1);

        this.configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "generator-storage";
    }

    @Override
    public String getTranslation() {
        return "Storage Config";
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.SERVER;
    }
}