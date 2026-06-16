package mekanism.fabric.content.machine.gui;

import mekanism.fabric.content.machine.gui.MachineGuiType.SlotSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Transitional Fabric bring-up: ONE generic screen for {@link MekanismMachineMenu}, rendering with Mekanism's REAL GUI
 * textures (the same 9-sliced {@code base} window + slot/arrow/bar sprites the original {@link
 * mekanism.fabric.content.machine.MachineScreen} uses) but laid out from the menu's {@link MachineGuiType} shape. Draws:
 * the window panel, each machine slot's frame (input/output texture by spec type), a recipe-progress arrow (only when the
 * shape has one), the vertical energy bar (synced energy permille), and one vertical chemical-tank bar per tank (synced
 * tank permille). Hover tooltips report energy / each tank as a permille percentage (full ChemicalStack identity sync is
 * deferred to the machine-framework migration).
 *
 * <p>26.1 draws GUI via the render-state extraction model: {@link #extractBackground} for the panel/bars and {@link
 * #extractTooltip} (calling {@link GuiGraphicsExtractor#setTooltipForNextFrame}) for the bar tooltips — there is no
 * {@code GuiGraphics}/{@code render} path in this version.
 */
public class MekanismMachineScreen extends AbstractContainerScreen<MekanismMachineMenu> {

    // Atlas sprites (assets/mekanism/textures/gui/sprites/), drawn via blitSprite.
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("mekanism", "base");
    private static final Identifier BAR_FRAME = Identifier.fromNamespaceAndPath("mekanism", "bar/base");
    // Direct textures (assets/mekanism/gui/...), drawn via blit (path used as-is under assets/<ns>/).
    private static final Identifier SLOT_INPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/input.png");
    private static final Identifier SLOT_OUTPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/output.png");
    private static final Identifier SLOT_NORMAL = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/normal.png");
    private static final Identifier UP_ARROW = Identifier.fromNamespaceAndPath("mekanism", "gui/up_arrow.png");
    private static final Identifier PROGRESS_BAR = Identifier.fromNamespaceAndPath("mekanism", "gui/progress/bar.png");

    // Energy bar geometry (right edge), matching MachineScreen.
    private static final int ENERGY_X = 164;
    private static final int ENERGY_Y = 16;
    private static final int BAR_INNER_H = 52;
    // Chemical tank bar geometry (left side). One per tank, spaced rightward.
    private static final int TANK_BASE_X = 8;
    private static final int TANK_Y = 16;
    private static final int TANK_SPACING = 10;

    public MekanismMachineScreen(MekanismMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick); // dims the world behind the GUI
        int x = this.leftPos;
        int y = this.topPos;
        // Mekanism's real window panel (9-sliced atlas sprite).
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BASE, x, y, this.imageWidth, this.imageHeight);
        // Slot frames at each machine slot position, using the real Mekanism slot textures (18x18, frame offset -1,-1).
        for (SlotSpec spec : this.menu.guiType().slots()) {
            Identifier tex = switch (spec.type()) {
                case INPUT -> SLOT_INPUT;
                case OUTPUT -> SLOT_OUTPUT;
                case NORMAL -> SLOT_NORMAL;
            };
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + spec.x() - 1, y + spec.y() - 1, 0.0F, 0.0F, 18, 18, 18, 18);
        }
        // Recipe-progress arrow + bar (machines only). Up-arrow (8x10) + progress bar (25x9), between input and output.
        if (this.menu.guiType().hasProgress()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, UP_ARROW, x + 68, y + 38, 0.0F, 0.0F, 8, 10, 8, 10);
            // bar.png is 25x27 (empty=v0 / filled=v9 / warning=v18). Draw empty, then overlay filled clipped to progress.
            graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 86, y + 38, 0.0F, 0.0F, 25, 9, 25, 27);
            int progressWidth = Math.round(25 * (this.menu.getProgressPermille() / 1000.0F));
            if (progressWidth > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 86, y + 38, 0.0F, 9.0F, progressWidth, 9, 25, 27);
            }
        }
        // Energy bar frame on the right (bar/base sprite, 6x54), with a bottom-up energy fill inside the 1px border.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x + ENERGY_X, y + ENERGY_Y, 6, BAR_INNER_H + 2);
        int energyFill = Math.round(BAR_INNER_H * (this.menu.getEnergyPermille() / 1000.0F));
        if (energyFill > 0) {
            int fillTop = y + ENERGY_Y + 1 + (BAR_INNER_H - energyFill);
            graphics.fillGradient(x + ENERGY_X + 1, fillTop, x + ENERGY_X + 5, y + ENERGY_Y + 1 + BAR_INNER_H,
                  0xFF5BE05B, 0xFF2FA82F);
        }
        // Chemical tank bars (left side), one per tank, colored a generic cyan so they read as fluid/chemical.
        int tankCount = this.menu.guiType().tankCount();
        for (int t = 0; t < tankCount; t++) {
            int tx = x + TANK_BASE_X + t * TANK_SPACING;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, tx, y + TANK_Y, 6, BAR_INNER_H + 2);
            int tankFill = Math.round(BAR_INNER_H * (this.menu.getTankPermille(t) / 1000.0F));
            if (tankFill > 0) {
                int fillTop = y + TANK_Y + 1 + (BAR_INNER_H - tankFill);
                graphics.fillGradient(tx + 1, fillTop, tx + 5, y + TANK_Y + 1 + BAR_INNER_H, 0xFF4FC3F7, 0xFF0277BD);
            }
        }
    }

    /**
     * Hover tooltips: the vanilla path first (item-slot tooltips), then our energy/tank bars. {@link
     * GuiGraphicsExtractor#setTooltipForNextFrame} only keeps the LAST one set this frame, so checking the bars after the
     * vanilla call lets a bar tooltip win when the cursor is over a bar (bars never overlap slots here).
     */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = this.leftPos;
        int y = this.topPos;
        if (inBar(mouseX, mouseY, x + ENERGY_X, y + ENERGY_Y)) {
            graphics.setTooltipForNextFrame(Component.literal("Energy: " + percent(this.menu.getEnergyPermille())), mouseX, mouseY);
            return;
        }
        int tankCount = this.menu.guiType().tankCount();
        for (int t = 0; t < tankCount; t++) {
            int tx = x + TANK_BASE_X + t * TANK_SPACING;
            if (inBar(mouseX, mouseY, tx, y + TANK_Y)) {
                graphics.setTooltipForNextFrame(Component.literal(this.menu.guiType().tankLabel() + ": " + percent(this.menu.getTankPermille(t))), mouseX, mouseY);
                return;
            }
        }
    }

    private static boolean inBar(int mouseX, int mouseY, int barX, int barY) {
        return mouseX >= barX && mouseX < barX + 6 && mouseY >= barY && mouseY < barY + BAR_INNER_H + 2;
    }

    private static String percent(int permille) {
        return String.format("%.1f%%", permille / 10.0F);
    }
}
