package mekanism.common.config;

import java.nio.file.Path;
import java.util.Map;
import mekanism.common.Mekanism;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;

public class MekanismConfigHelper {

    private MekanismConfigHelper() {
    }

    public static final Path CONFIG_DIR = FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(Mekanism.MOD_NAME));

    /**
     * Creates and register a mod config, and track it so that we can properly clear cached values.
     */
    public static void registerConfig(Map<net.neoforged.fml.config.IConfigSpec, IMekanismConfig> knownConfigs, ModContainer modContainer, INeoMekanismConfig config) {
        modContainer.registerConfig(toModConfigType(config.getConfigType()), config.getConfigSpec(), Mekanism.MOD_NAME + "/" + config.getFileName() + ".toml");
        knownConfigs.put(config.getConfigSpec(), config);
    }

    /**
     * Maps the loader-neutral {@link ConfigType} back to the NeoForge {@link ModConfig.Type} used for registration, so
     * each config still registers under exactly the same type it always has.
     */
    private static ModConfig.Type toModConfigType(ConfigType type) {
        return switch (type) {
            case CLIENT -> ModConfig.Type.CLIENT;
            case COMMON -> ModConfig.Type.COMMON;
            case SERVER -> ModConfig.Type.SERVER;
            case STARTUP -> ModConfig.Type.STARTUP;
        };
    }

    public static void onConfigLoad(ModConfigEvent event, String modid, Map<net.neoforged.fml.config.IConfigSpec, IMekanismConfig> knownConfigs) {
        //Note: We listen to both the initial load and the reload, to make sure that we fix any accidentally
        // cached values from calls before the initial loading
        ModConfig config = event.getConfig();
        //Make sure it is for the same modid as us
        if (config.getModId().equals(modid)) {
            IMekanismConfig mekanismConfig = knownConfigs.get(config.getSpec());
            if (mekanismConfig != null) {
                mekanismConfig.clearCache(event instanceof ModConfigEvent.Unloading);
            }
        }
    }
}