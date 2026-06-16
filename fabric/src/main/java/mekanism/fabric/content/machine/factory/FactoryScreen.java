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
 * from the menu's {@link FactorySlotLayout} (N input + N output slot frames, +secondary column for sawing). Draws: the
 * window panel, each factory slot frame, a single recipe-progress arrow (aggregate), the vertical energy bar, and (for
 * chemical factories) one vertical shared-tank bar. The player inventory is shifted down so it sits below the (taller for
 * higher tiers) factory slot grid.
 */
public class FactoryScreen extends AbstractContainerScreen<FactoryMenu> {

    private static final Identifier BASE = Identifier.fromNamespaceAndPath("mekanism", "base");
    private static final Identifier BAR_FRAME = Identifier.fromNamespaceAndPath("mekanism", "bar/base");
    private static final Identifier SLOT_INPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/input.png");
    private static final Identifier SLOT_OUTPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/output.png");
    private static final Identifier UP_ARROW = Identifier.fromNamespaceAndPath("mekanism", "gui/up_arrow.png");
    private static final Identifier PROGRESS_BAR = Identifier.fromNamespaceAndPath("mekanism", "gui/progress/bar.png");

    private static final int ENERGY_X = 164;
    private static final int ENERGY_Y = 16;
    private static final int BAR_INNER_H = 52;
    private static final int TANK_X = 8;
    private static final int TANK_Y = 16;

    public FactoryScreen(FactoryMenu menu, Inventory playerInventory, Component title) {
        // Window sized to the factory grid (tier-dependent height): the player inventory top + its 4 rows + a margin.
        super(menu, playerInventory, title, 176, menu.playerInventoryTop() + 4 * 18 + 8);
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
        // Single aggregate progress arrow + bar between the input and output columns.
        graphics.blit(RenderPipelines.GUI_TEXTURED, UP_ARROW, x + 60, y + 24, 0.0F, 0.0F, 8, 10, 8, 10);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 78, y + 24, 0.0F, 0.0F, 25, 9, 25, 27);
        int progressWidth = Math.round(25 * (this.menu.getProgressPermille() / 1000.0F));
        if (progressWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 78, y + 24, 0.0F, 9.0F, progressWidth, 9, 25, 27);
        }
        // Energy bar (right).
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x + ENERGY_X, y + ENERGY_Y, 6, BAR_INNER_H + 2);
        int energyFill = Math.round(BAR_INNER_H * (this.menu.getEnergyPermille() / 1000.0F));
        if (energyFill > 0) {
            int fillTop = y + ENERGY_Y + 1 + (BAR_INNER_H - energyFill);
            graphics.fillGradient(x + ENERGY_X + 1, fillTop, x + ENERGY_X + 5, y + ENERGY_Y + 1 + BAR_INNER_H,
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
        if (inBar(mouseX, mouseY, x + ENERGY_X, y + ENERGY_Y)) {
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
