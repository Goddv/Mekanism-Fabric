package mekanism.common.config.value;

public interface IConfigValue<T> {

    T get();

    T getDefault();

    void set(T value);
}
