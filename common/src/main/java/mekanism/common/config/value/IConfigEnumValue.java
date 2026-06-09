package mekanism.common.config.value;

/**
 * Marker sub-interface of {@link IConfigValue} for enum-typed config values produced by {@code IConfigBuilder.defineEnum}.
 * <p>
 * Its sole purpose is to give {@code CachedEnumValue.wrap} a distinct erasure from {@code CachedConfigValue.wrap} (both of which would otherwise take a plain
 * {@link IConfigValue} and clash as static methods with the same erasure across the subclass/superclass relationship).
 */
public interface IConfigEnumValue<V extends Enum<V>> extends IConfigValue<V> {
}
