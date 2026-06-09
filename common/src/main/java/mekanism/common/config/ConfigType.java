package mekanism.common.config;

/**
 * Loader-neutral config type. Mirrors the {@code net.neoforged.fml.config.ModConfig.Type} values the config classes use
 * (CLIENT/COMMON/SERVER/STARTUP); the NeoForge registration path ({@code MekanismConfigHelper}) maps these back to the
 * corresponding {@code ModConfig.Type} so registration is unchanged.
 */
public enum ConfigType {
    CLIENT,
    COMMON,
    SERVER,
    STARTUP
}
