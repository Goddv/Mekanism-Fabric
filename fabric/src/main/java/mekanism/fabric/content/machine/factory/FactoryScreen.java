package mekanism.fabric.content.machine.factory;

import mekanism.fabric.content.machine.factory.FactorySlotLayout.SlotSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Transitional Fabric bring-up: ONE generic screen for {@link FactoryMenu}, rendering with Mekanism's REAL GUI textures
 * (the same 9-sliced {@code base} window + slot/arrow/bar sprites the other transitional machine screens use) but laid out
 * from the menu's {@link FactorySlotLayout}: input slots in a horizontal ROW across the top, an output ROW directly below
 * (a SECOND output row for sawing), and a recipe-progress DOWN-arrow per process column between each input and its output.
 * The window width grows with the process count (ultimate=9 → wide), so the energy bar tracks the right edge and the
 * player inventory + shared chemical-tank bar are positioned relative to that width. The player inventory sits below the
 * (tier-dependent) factory grid, centered under the window.
 */
public class FactoryScreen extends AbstractContainerScreen<FactoryMenu> {

    private static final Identifier BASE = Identifier.fromNamespaceAndPath("mekanism", "base");
    private static final Identifier BAR_FRAME = Identifier.fromNamespaceAndPath("mekanism", "bar/base");
    private static final Identifier SLOT_INPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/input.png");
    private static final Identifier SLOT_OUTPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/output.png");
    private static final Identifier UP_ARROW = Identifier.fromNamespaceAndPath("mekanism", "gui/up_arrow.png");

    /** Energy bar inset from the RIGHT edge of the (tier-dependent width) window. */
    private static final int ENERGY_RIGHT_INSET = 12;
    private static final int ENERGY_Y = 16;
    private static final int BAR_INNER_H = 52;
    private static final int TANK_X = 8;
    private static final int TANK_Y = 16;
    private static final int DOWN_ARROW_W = 8;
    private static final int DOWN_ARROW_H = 10;

    public FactoryScreen(FactoryMenu menu, Inventory playerInventory, Component title) {
        // Window sized to the factory grid: width grows with the process count; height = player-inv top + its 4 rows + margin.
        super(menu, playerInventory, title, menu.contentWidth(), menu.playerInventoryTop() + 4 * 18 + 8);
    }

    /** Energy bar x for this (tier-dependent width) window. */
    private int energyX() {
        return this.imageWidth - ENERGY_RIGHT_INSET;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = menu.playerInventoryTop() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BASE, x, y, this.imageWidth, this.imageHeight);
        for (SlotSpec spec : this.menu.layout().slots()) {
            Identifier tex = spec.type() == FactorySlotLayout.SlotType.INPUT ? SLOT_INPUT : SLOT_OUTPUT;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + spec.x() - 1, y + spec.y() - 1, 0.0F, 0.0F, 18, 18, 18, 18);
        }
        // One arrow per process column, between the input row and its output, with a green progress overlay (top-down)
        // scaled to the aggregate progress. (up_arrow.png is an 8x10 single-frame sprite; the overlay shows progress.)
        int progressPermille = this.menu.getProgressPermille();
        int fillH = Math.round(DOWN_ARROW_H * (progressPermille / 1000.0F));
        for (FactorySlotLayout.ArrowSpec arrow : this.menu.layout().arrows()) {
            int ax = x + arrow.x();
            int ay = y + arrow.y();
            graphics.blit(RenderPipelines.GUI_TEXTURED, UP_ARROW, ax, ay, 0.0F, 0.0F,
                  DOWN_ARROW_W, DOWN_ARROW_H, DOWN_ARROW_W, DOWN_ARROW_H);
            if (fillH > 0) {
                // Tint the upper portion of the arrow cell to indicate progress flowing input -> output.
                graphics.fillGradient(ax, ay, ax + DOWN_ARROW_W, ay + fillH, 0x805BE05B, 0x802FA82F);
            }
        }
        // Energy bar (right edge, tracks the tier-dependent window width).
        int energyX = energyX();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x + energyX, y + ENERGY_Y, 6, BAR_INNER_H + 2);
        int energyFill = Math.round(BAR_INNER_H * (this.menu.getEnergyPermille() / 1000.0F));
        if (energyFill > 0) {
            int fillTop = y + ENERGY_Y + 1 + (BAR_INNER_H - energyFill);
            graphics.fillGradient(x + energyX + 1, fillTop, x + energyX + 5, y + ENERGY_Y + 1 + BAR_INNER_H,
                  0xFF5BE05B, 0xFF2FA82F);
        }
        // Shared chemical tank bar (left), chemical factories only.
        if (this.menu.layout().hasTank()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x + TANK_X, y + TANK_Y, 6, BAR_INNER_H + 2);
            int tankFill = Math.round(BAR_INNER_H * (this.menu.getTankPermille() / 1000.0F));
            if (tankFill > 0) {
                int fillTop = y + TANK_Y + 1 + (BAR_INNER_H - tankFill);
                graphics.fillGradient(x + TANK_X + 1, fillTop, x + TANK_X + 5, y + TANK_Y + 1 + BAR_INNER_H, 0xFF4FC3F7, 0xFF0277BD);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        if (inBar(mouseX, mouseY, x + energyX(), y + ENERGY_Y)) {
            graphics.setTooltipForNextFrame(Component.literal("Energy: " + percent(this.menu.getEnergyPermille())), mouseX, mouseY);
            return;
        }
        if (this.menu.layout().hasTank() && inBar(mouseX, mouseY, x + TANK_X, y + TANK_Y)) {
            graphics.setTooltipForNextFrame(Component.literal("Chemical: " + percent(this.menu.getTankPermille())), mouseX, mouseY);
        }
    }

    private static boolean inBar(int mouseX, int mouseY, int barX, int barY) {
        return mouseX >= barX && mouseX < barX + 6 && mouseY >= barY && mouseY < barY + BAR_INNER_H + 2;
    }

    private static String percent(int permille) {
        return String.format("%.1f%%", permille / 10.0F);
    }
}
