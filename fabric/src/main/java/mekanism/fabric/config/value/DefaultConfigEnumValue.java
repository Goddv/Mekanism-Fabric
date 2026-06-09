package mekanism.fabric.config.value;

import mekanism.common.config.value.IConfigEnumValue;

/**
 * Fabric (defaults-first) implementation of {@link IConfigEnumValue}. {@link IConfigEnumValue} is a pure marker over
 * {@link mekanism.common.config.value.IConfigValue} (it exists only to give {@code CachedEnumValue.wrap} a distinct
 * erasure), so this is a thin {@link DefaultConfigValue} subclass that supplies the marker type for
 * {@code IConfigBuilder.defineEnum}.
 *
 * @param <V> the enum type
 */
public class DefaultConfigEnumValue<V extends Enum<V>> extends DefaultConfigValue<V> implements IConfigEnumValue<V> {

    public DefaultConfigEnumValue(V defaultValue) {
        super(defaultValue);
    }
}
