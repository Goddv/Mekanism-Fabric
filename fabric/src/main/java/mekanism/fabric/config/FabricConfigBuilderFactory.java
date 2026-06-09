package mekanism.fabric.config;

import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.IConfigBuilderFactory;

/**
 * Fabric implementation of {@link IConfigBuilderFactory}; constructs a {@link DefaultConfigBuilder} (defaults-first, no
 * backing file). Registered via {@code META-INF/services} so {@code IConfigBuilderFactory.INSTANCE} resolves to it on
 * Fabric, the same way {@code NeoConfigBuilderFactory} does on NeoForge.
 */
public class FabricConfigBuilderFactory implements IConfigBuilderFactory {

    @Override
    public IConfigBuilder create() {
        return new DefaultConfigBuilder();
    }
}
