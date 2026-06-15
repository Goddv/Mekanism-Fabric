package mekanism.fabric;

import com.mojang.logging.LogUtils;
import mekanism.fabric.client.FabricCompositeModelFlattener;
import mekanism.fabric.content.machine.FabricMachineMenus;
import mekanism.fabric.content.machine.MachineScreen;
import mekanism.fabric.content.machine.gui.MekanismMachineMenus;
import mekanism.fabric.content.machine.gui.MekanismMachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import org.slf4j.Logger;

/**
 * Fabric client entrypoint. Registers client-only bindings — currently the {@link MachineScreen} factory for the machine
 * {@link mekanism.fabric.content.machine.FabricMachineMenus#MACHINE menu type}, so right-clicking a Mekanism machine
 * opens its GUI. Grows as more client rendering is migrated to Fabric.
 */
public final class MekanismFabricClient implements ClientModInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeClient() {
        MenuScreens.register(FabricMachineMenus.MACHINE.get(), MachineScreen::new);
        // The generic chemical/dual-item/generator screen (one screen for all those shapes; it reads the menu's GUI type).
        MenuScreens.register(MekanismMachineMenus.MEKANISM_MACHINE.get(), MekanismMachineScreen::new);
        // Flatten NeoForge composite block models (loader:"neoforge:composite") into vanilla models so they render on
        // Fabric (e.g. the Chemical Crystallizer). Client-only; NeoForge keeps the original composite JSONs.
        FabricCompositeModelFlattener.register();
        LOGGER.info("[Mekanism/Fabric] Client init — machine screen registered.");
        // Dev-only GUI screenshot harness (opens the machine screen + saves a PNG for visual review). Dormant unless
        // MEKANISM_GUI_SHOT=1 is set, so normal dev runs aren't interrupted by an auto-opening screen.
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && "1".equals(System.getenv("MEKANISM_GUI_SHOT"))) {
            MekanismFabricGuiScreenshot.init();
        }
    }
}
