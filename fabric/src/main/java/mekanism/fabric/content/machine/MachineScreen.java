package mekanism.fabric.content.machine;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Transitional Fabric bring-up: a screen for {@link MachineMenu} that renders with Mekanism's REAL GUI textures + layout,
 * reproducing the enrichment-chamber look (GuiElectricMachine) without the full GuiElement widget framework. Draws:
 * Mekanism's 9-sliced {@code base} window sprite, the real slot textures (input/output/normal), the up-arrow, the
 * progress bar, and the energy-bar frame ({@code bar/base} sprite) — all at the real electric-machine positions
 * (input 64,17 / output 116,35 / up-arrow 68,38 / progress 86,38 / energy-bar 164,16).
 *
 * <p>26.1 draws GUI backgrounds via the render-state extraction model: {@link #extractBackground} +
 * {@link GuiGraphicsExtractor} (blitSprite for atlas sprites at {@code textures/gui/sprites/}, blit for the direct
 * textures at {@code assets/mekanism/gui/...}). Live values (energy fill, recipe progress) need the container sync
 * framework — that, plus the real GuiElement widgets/tabs, arrives with the machine-framework migration to {@code :common}.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    // Atlas sprites (assets/mekanism/textures/gui/sprites/), drawn via blitSprite.
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("mekanism", "base");
    private static final Identifier BAR_FRAME = Identifier.fromNamespaceAndPath("mekanism", "bar/base");
    // Direct textures (assets/mekanism/gui/...), drawn via blit (path used as-is under assets/<ns>/).
    private static final Identifier SLOT_INPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/input.png");
    private static final Identifier SLOT_OUTPUT = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/output.png");
    private static final Identifier SLOT_NORMAL = Identifier.fromNamespaceAndPath("mekanism", "gui/slot/normal.png");
    private static final Identifier UP_ARROW = Identifier.fromNamespaceAndPath("mekanism", "gui/up_arrow.png");
    private static final Identifier PROGRESS_BAR = Identifier.fromNamespaceAndPath("mekanism", "gui/progress/bar.png");

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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick); // dims the world behind the GUI
        int x = this.leftPos;
        int y = this.topPos;
        // Mekanism's real window panel (9-sliced atlas sprite) — same call GuiMekanism makes.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BASE, x, y, this.imageWidth, this.imageHeight);
        // Slot frames at each slot position, using the real Mekanism slot textures (18x18, frame offset -1,-1).
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            Identifier tex = i == 0 ? SLOT_INPUT : i == 1 ? SLOT_OUTPUT : SLOT_NORMAL;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, x + slot.x - 1, y + slot.y - 1, 0.0F, 0.0F, 18, 18, 18, 18);
        }
        // Up-arrow (8x10) + progress bar (25x9) between input and output (GuiUpArrow 68,38 / GuiProgress BAR 86,38).
        graphics.blit(RenderPipelines.GUI_TEXTURED, UP_ARROW, x + 68, y + 38, 0.0F, 0.0F, 8, 10, 8, 10);
        // bar.png is 25x27 (3 stacked frames: empty=v0 / filled=v9 / warning=v18). Draw the empty frame, then overlay
        // the filled frame clipped to the current recipe progress (left-to-right fill).
        graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 86, y + 38, 0.0F, 0.0F, 25, 9, 25, 27);
        int progressWidth = Math.round(25 * (this.menu.getProgressPermille() / 1000.0F));
        if (progressWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_BAR, x + 86, y + 38, 0.0F, 9.0F, progressWidth, 9, 25, 27);
        }
        // Energy-bar frame on the right (GuiVerticalPowerBar 164,16; frame = bar/base sprite, texWidth+2 x texHeight+2),
        // with a bottom-up energy fill inside the 1px border (inner area 4x52 at +1,+1).
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x + 164, y + 16, 6, 54);
        int fillHeight = Math.round(52 * (this.menu.getEnergyPermille() / 1000.0F));
        if (fillHeight > 0) {
            int fillTop = y + 16 + 1 + (52 - fillHeight);
            graphics.fillGradient(x + 164 + 1, fillTop, x + 164 + 5, y + 16 + 1 + 52, 0xFF5BE05B, 0xFF2FA82F);
        }
    }
}
