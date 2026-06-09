package mekanism.common.config.value;

import mekanism.common.config.IMekanismConfig;

public class CachedEnumValue<T extends Enum<T>> extends CachedConfigValue<T> {

    private CachedEnumValue(IMekanismConfig config, IConfigValue<T> internal) {
        super(config, internal);
    }

    //Note: Ensure that we provide a nice translated name for any enum value based configs we have
    public static <T extends Enum<T>> CachedEnumValue<T> wrap(IMekanismConfig config, IConfigEnumValue<T> internal) {
        return new CachedEnumValue<>(config, internal);
    }
}