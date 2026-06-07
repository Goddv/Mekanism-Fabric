package mekanism.fabric;

import com.mojang.logging.LogUtils;
import mekanism.fabric.content.machine.MachineMenu;
import mekanism.fabric.content.machine.MachineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Dev-only autonomous visual check for the machine GUI: once the client is in a world, it opens {@link MachineScreen}
 * (client-only menu, for layout/texture inspection) and saves a screenshot to {@code run/screenshots/}, logging a
 * {@code [gui-screenshot]} marker. Lets the GUI look be verified from the saved PNG without manual in-game interaction.
 * Removed once the GUI is finalized.
 */
public final class MekanismFabricGuiScreenshot {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static int ticks = -1;
    private static boolean done;

    private MekanismFabricGuiScreenshot() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (done || mc.player == null || mc.level == null) {
                return;
            }
            ticks++;
            if (ticks == 40) {
                mc.setScreen(new MachineScreen(new MachineMenu(0, mc.player.getInventory()),
                      mc.player.getInventory(), Component.literal("Enrichment Chamber")));
                LOGGER.info("[gui-screenshot] opened MachineScreen");
            } else if (ticks == 70) {
                Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(),
                      msg -> LOGGER.info("[gui-screenshot] {}", msg.getString()));
                LOGGER.info("[gui-screenshot] grabbed screenshot to run/screenshots/");
                done = true;
            }
        });
    }
}
