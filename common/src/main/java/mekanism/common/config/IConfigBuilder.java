package mekanism.common.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.common.config.value.IConfigEnumValue;
import mekanism.common.config.value.IConfigValue;

/**
 * Loader-neutral abstraction over a config spec builder (NeoForge's {@code ModConfigSpec.Builder}).
 * <p>
 * Each method mirrors exactly one {@code ModConfigSpec.Builder} call so that the NeoForge implementation can delegate verbatim (same call, same args, same
 * order) and produce a byte-identical {@code .toml}. The {@code define*}/{@code defineEnum}/{@code defineList*} methods return an {@link IConfigValue} (the
 * read-side seam); the metadata/section methods ({@link #comment}, {@link #translation}, {@link #push}, {@link #pop}, {@link #worldRestart},
 * {@link #gameRestart}) return {@code this} so call chains keep working.
 */
public interface IConfigBuilder {

    <T> IConfigValue<T> define(String path, T defaultValue);

    <T> IConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator);

    <T> IConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator);

    IConfigValue<Integer> defineInRange(String path, int defaultValue, int min, int max);

    IConfigValue<Long> defineInRange(String path, long defaultValue, long min, long max);

    IConfigValue<Double> defineInRange(String path, double defaultValue, double min, double max);

    <V extends Enum<V>> IConfigEnumValue<V> defineEnum(String path, V defaultValue);

    <T> IConfigValue<List<? extends T>> defineListAllowEmpty(String path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier,
          Predicate<Object> elementValidator);

    IConfigBuilder comment(String... comment);

    IConfigBuilder translation(String translationKey);

    IConfigBuilder push(String path);

    IConfigBuilder pop();

    IConfigBuilder pop(int count);

    IConfigBuilder worldRestart();

    IConfigBuilder gameRestart();

    /**
     * Finalizes the spec being built and returns the loader-neutral {@link IConfigSpec} handle. The NeoForge
     * implementation builds the underlying {@code ModConfigSpec} (verbatim) and wraps it.
     */
    IConfigSpec build();
}
