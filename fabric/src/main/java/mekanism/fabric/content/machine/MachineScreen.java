package mekanism.fabric.content.machine;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Transitional Fabric bring-up: a minimal screen for {@link MachineMenu}. 26.1 replaced the old
 * {@code renderBg(GuiGraphics,...)} hook with a render-state extraction model: the background is drawn by overriding
 * {@link #extractBackground(GuiGraphicsExtractor, int, int, float)} (as vanilla {@code ContainerScreen}/
 * {@code DispenserScreen} do). This draws a plain panel + slot backings via {@link GuiGraphicsExtractor#fill} so the GUI
 * is visible without shipping a bespoke GUI atlas; the vanilla menu sync drives the slot contents. The real Mekanism GUI
 * artwork/widgets (energy bar, progress arrow, side-config) arrive with the machine-framework migration to {@code :common}.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;       // light grey panel (vanilla container tone)
    private static final int PANEL_SHADOW = 0xFF555555; // panel border
    private static final int SLOT = 0xFF8B8B8B;        // slot interior

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick); // dims the world behind the GUI
        int x = this.leftPos;
        int y = this.topPos;
        // Panel with a 1px border so it reads as a window.
        extractor.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, PANEL_SHADOW);
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
        // Slot backings (machine input/output + the full player inventory) so each slot reads as a slot.
        for (Slot slot : this.menu.slots) {
            extractor.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_SHADOW);
            extractor.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT);
        }
    }
}
