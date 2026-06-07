package mekanism.fabric;

import com.mojang.logging.LogUtils;
import mekanism.fabric.content.machine.FabricMachineMenus;
import mekanism.fabric.content.machine.MachineScreen;
import net.fabricmc.api.ClientModInitializer;
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
        LOGGER.info("[Mekanism/Fabric] Client init — machine screen registered.");
    }
}
