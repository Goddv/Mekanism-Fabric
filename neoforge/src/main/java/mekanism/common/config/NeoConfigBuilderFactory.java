package mekanism.common.config;

/**
 * NeoForge implementation of {@link IConfigBuilderFactory}; constructs a {@link NeoConfigBuilder} (which wraps a fresh
 * {@code ModConfigSpec.Builder}). Registered via {@code META-INF/services} so {@code IConfigBuilderFactory.INSTANCE}
 * resolves to it.
 */
public class NeoConfigBuilderFactory implements IConfigBuilderFactory {

    @Override
    public IConfigBuilder create() {
        return new NeoConfigBuilder();
    }
}
