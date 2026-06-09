package mekanism.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge implementation of {@link IConfigSpec}. Wraps a {@link ModConfigSpec} and delegates {@link #isLoaded()} and
 * {@link #save()} VERBATIM to it, so the {@code .toml} load/save lifecycle is byte-identical to the original
 * direct-{@code ModConfigSpec} code. {@link #getModConfigSpec()} exposes the wrapped spec for NeoForge registration.
 */
public class NeoConfigSpec implements IConfigSpec {

    private final ModConfigSpec spec;

    public NeoConfigSpec(ModConfigSpec spec) {
        this.spec = spec;
    }

    @Override
    public boolean isLoaded() {
        return spec.isLoaded();
    }

    @Override
    public void save() {
        spec.save();
    }

    public ModConfigSpec getModConfigSpec() {
        return spec;
    }
}
