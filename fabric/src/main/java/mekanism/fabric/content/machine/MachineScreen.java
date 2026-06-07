package mekanism.fabric.content.machine;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Transitional Fabric bring-up: a minimal screen for {@link MachineMenu}. It relies on the 26.1
 * {@link AbstractContainerScreen} default render-state pipeline (background panel + slots + labels + tooltips) so the
 * menu is usable in-game without shipping a bespoke GUI atlas; the vanilla menu sync drives the slot contents. The real
 * Mekanism GUI artwork/widgets (energy bar, progress arrow, side-config) arrive with the machine-framework migration to
 * {@code :common}.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
    }
}
