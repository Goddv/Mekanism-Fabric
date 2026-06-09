package mekanism.fabric.config.value;

import mekanism.common.config.value.IConfigValue;

/**
 * Fabric (defaults-first) implementation of {@link IConfigValue}.
 * <p>
 * Holds the declared default and an in-memory current value (seeded to the default). {@link #get()} returns the current
 * value, {@link #getDefault()} the declared default, and {@link #set(Object)} mutates only the in-memory current value.
 * There is no backing file yet on Fabric, so {@link #set(Object)} edits are lost on restart (known Fabric limitation,
 * documented in {@code DefaultConfigSpec#save()}); on a fresh launch every value reads back its declared default — which
 * is exactly what the {@code Cached*Value} read path expects via {@code IConfigValue.get()}/{@code getDefault()}.
 *
 * @param <T> the value type
 */
public class DefaultConfigValue<T> implements IConfigValue<T> {

    private final T defaultValue;
    private T current;

    public DefaultConfigValue(T defaultValue) {
        this.defaultValue = defaultValue;
        this.current = defaultValue;
    }

    @Override
    public T get() {
        return current;
    }

    @Override
    public T getDefault() {
        return defaultValue;
    }

    @Override
    public void set(T value) {
        this.current = value;
    }
}
