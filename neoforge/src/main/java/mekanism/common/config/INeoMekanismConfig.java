package mekanism.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge-only extension of {@link IMekanismConfig} that exposes the wrapped {@link ModConfigSpec} for NeoForge
 * registration ({@code MekanismConfigHelper}). The spec is reached through the loader-neutral {@link #getSpec()} seam,
 * cast to {@link NeoConfigSpec}; the config classes themselves never name {@code ModConfigSpec}.
 */
public interface INeoMekanismConfig extends IMekanismConfig {

    default ModConfigSpec getConfigSpec() {
        return ((NeoConfigSpec) getSpec()).getModConfigSpec();
    }
}
