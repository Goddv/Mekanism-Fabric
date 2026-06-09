package mekanism.common.config;

/**
 * Loader-neutral handle for a built config spec. Produced by {@link IConfigBuilder#build()} and held by
 * {@code BaseMekanismConfig} as the lifecycle handle (load-state + persistence).
 * <p>
 * The NeoForge implementation ({@code NeoConfigSpec}) wraps a {@code ModConfigSpec} and delegates {@link #isLoaded()}
 * and {@link #save()} verbatim to it, so the {@code .toml} lifecycle is byte-identical to the original direct-spec code.
 */
public interface IConfigSpec {

    /** Whether the underlying spec has been loaded (a config file bound to it). */
    boolean isLoaded();

    /** Persists the spec's current values to disk. */
    void save();
}
