package mekanism.fabric.config;

import mekanism.common.config.IConfigSpec;

/**
 * Fabric (defaults-first) implementation of {@link IConfigSpec}.
 * <p>
 * Reports {@link #isLoaded()} as {@code true} so the {@code getOrDefault}/clear-cache read path in {@code BaseMekanismConfig}
 * + the {@code Cached*Value} classes treats the spec as live and returns the in-memory values (which are seeded to the
 * declared defaults via {@link mekanism.fabric.config.value.DefaultConfigValue}). {@link #save()} is a no-op: there is no
 * backing {@code .toml} on Fabric yet, so client-GUI config edits are not persisted across restarts (known Fabric
 * limitation until a real Fabric config backend lands behind these same interfaces).
 */
public class DefaultConfigSpec implements IConfigSpec {

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void save() {
        //No-op: defaults-first Fabric impl has no backing file (see class javadoc).
    }
}
