package mekanism.common.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.common.config.value.IConfigEnumValue;
import mekanism.common.config.value.IConfigValue;
import mekanism.common.config.value.NeoConfigEnumValue;
import mekanism.common.config.value.NeoConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge implementation of {@link IConfigBuilder}. Every method delegates VERBATIM to the wrapped {@link ModConfigSpec.Builder} (same call, same args, same
 * order) so the produced {@code .toml} is byte-identical to the original direct-builder code. {@code define*} results are wrapped in {@link NeoConfigValue} and
 * exposed as {@link IConfigValue}; the metadata/section methods return {@code this} to preserve chaining.
 */
public class NeoConfigBuilder implements IConfigBuilder {

    private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

    @Override
    public <T> IConfigValue<T> define(String path, T defaultValue) {
        return new NeoConfigValue<>(builder.define(path, defaultValue));
    }

    @Override
    public <T> IConfigValue<T> define(String path, T defaultValue, Predicate<Object> validator) {
        return new NeoConfigValue<>(builder.define(path, defaultValue, validator));
    }

    @Override
    public <T> IConfigValue<T> define(String path, Supplier<T> defaultSupplier, Predicate<Object> validator) {
        return new NeoConfigValue<>(builder.define(path, defaultSupplier, validator));
    }

    @Override
    public IConfigValue<Integer> defineInRange(String path, int defaultValue, int min, int max) {
        return new NeoConfigValue<>(builder.defineInRange(path, defaultValue, min, max));
    }

    @Override
    public IConfigValue<Long> defineInRange(String path, long defaultValue, long min, long max) {
        return new NeoConfigValue<>(builder.defineInRange(path, defaultValue, min, max));
    }

    @Override
    public IConfigValue<Double> defineInRange(String path, double defaultValue, double min, double max) {
        return new NeoConfigValue<>(builder.defineInRange(path, defaultValue, min, max));
    }

    @Override
    public <V extends Enum<V>> IConfigEnumValue<V> defineEnum(String path, V defaultValue) {
        return new NeoConfigEnumValue<>(builder.defineEnum(path, defaultValue));
    }

    @Override
    public <T> IConfigValue<List<? extends T>> defineListAllowEmpty(String path, Supplier<List<? extends T>> defaultSupplier, Supplier<T> newElementSupplier,
          Predicate<Object> elementValidator) {
        return new NeoConfigValue<>(builder.defineListAllowEmpty(path, defaultSupplier, newElementSupplier, elementValidator));
    }

    @Override
    public IConfigBuilder comment(String... comment) {
        builder.comment(comment);
        return this;
    }

    @Override
    public IConfigBuilder translation(String translationKey) {
        builder.translation(translationKey);
        return this;
    }

    @Override
    public IConfigBuilder push(String path) {
        builder.push(path);
        return this;
    }

    @Override
    public IConfigBuilder pop() {
        builder.pop();
        return this;
    }

    @Override
    public IConfigBuilder pop(int count) {
        builder.pop(count);
        return this;
    }

    @Override
    public IConfigBuilder worldRestart() {
        builder.worldRestart();
        return this;
    }

    @Override
    public IConfigBuilder gameRestart() {
        builder.gameRestart();
        return this;
    }

    public ModConfigSpec buildSpec() {
        return builder.build();
    }
}
