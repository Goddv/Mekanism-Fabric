package mekanism.common.config;

import mekanism.api.MekanismAPIBase;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-neutral factory for {@link IConfigBuilder} instances. Lets the config classes build a spec without naming the
 * loader-specific builder type (NeoForge's {@code NeoConfigBuilder}); each loader registers an implementation that
 * constructs its own builder.
 * <p>
 * Resolved via {@link MekanismAPIBase#getService} (same precedent as {@code ITileSyncService}); every loader MUST
 * register an impl (getService throws otherwise).
 */
@Internal
public interface IConfigBuilderFactory {

    IConfigBuilderFactory INSTANCE = MekanismAPIBase.getService(IConfigBuilderFactory.class);

    IConfigBuilder create();
}
