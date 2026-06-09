package mekanism.fabric.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.IConfigSpec;
import mekanism.common.config.value.IConfigEnumValue;
import mekanism.common.config.value.IConfigValue;
import mekanism.fabric.config.value.DefaultConfigEnumValue;
import mekanism.fabric.config.value.DefaultConfigValue;

/**
 * Fabric (defaults-first) implementation of {@link IConfigBuilder}.
 * <p>
 * Every {@code define*}/{@code defineEnum}/{@code defineListAllowEmpty} captures the declared default (invoking the
 * {@link Supplier} for the supplier-based variants and for list defaults) and returns a {@link DefaultConfigValue} seeded
 * to that default. The metadata/section methods ({@link #comment}, {@link #translation}, {@link #push}, {@link #pop},
 * {@link #worldRestart}, {@link #gameRestart}) are no-ops that return {@code this} so the call chains used by the config
 * classes keep working. {@link #build()} returns a {@link DefaultConfigSpec} (which reports loaded + no-op save). There is
 * no {@code .toml} produced — Fabric reads back the declared defaults at runtime.
 */
public class DefaultConfigBuilder implements IConfigBuilder {

    @Override
    public <T> IConfigValue<T> define(String path, T defaultValue) {
        return new DefaultConfigValue<>(defaultValue);
    }

    @Override
    public <T> IConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
        return new DefaultConfigValue<>(defaultValue);
    }

    @Override
    public <T> IConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
        return new DefaultConfigValue<>(defaultSupplier.get());
    }

    @Override
    public IConfigValue<Integer> defineInRange(String path, int defaultValue, int min, int max) {
        return new DefaultConfigValue<>(defaultValue);
    }

    @Override
    public IConfigValue<Long> defineInRange(String path, long defaultValue, long min, long max) {
        return new DefaultConfigValue<>(defaultValue);
    }

    @Override
    public IConfigValue<Double> defineInRange(String path, double defaultValue, double min, double max) {
        return new DefaultConfigValue<>(defaultValue);
    }

    @Override
    public <V extends Enum<V>> IConfigEnumValue<V> defineEnum(String path, V defaultValue) {
        return new DefaultConfigEnumValue<>(defaultValue);
    }

    @Override
    public <T> IConfigValue<List<? extends T>> defineListAllowEmpty(String path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier,
          Predicate<Object> elementValidator) {
        return new DefaultConfigValue<>(defaultSupplier.get());
    }

    @Override
    public IConfigBuilder comment(String... comment) {
        return this;
    }

    @Override
    public IConfigBuilder translation(String translationKey) {
        return this;
    }

    @Override
    public IConfigBuilder push(String path) {
        return this;
    }

    @Override
    public IConfigBuilder pop() {
        return this;
    }

    @Override
    public IConfigBuilder pop(int count) {
        return this;
    }

    @Override
    public IConfigBuilder worldRestart() {
        return this;
    }

    @Override
    public IConfigBuilder gameRestart() {
        return this;
    }

    @Override
    public IConfigSpec build() {
        return new DefaultConfigSpec();
    }
}
