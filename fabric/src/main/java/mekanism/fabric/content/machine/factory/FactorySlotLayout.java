package mekanism.fabric.content.machine.factory;

import java.util.ArrayList;
import java.util.List;
import mekanism.fabric.content.machine.factory.FactoryType.Topology;

/**
 * Transitional Fabric bring-up: the GUI slot layout for a factory, computed purely from {@code (topology, processes)} so
 * the server and the client derive an IDENTICAL layout (the client rebuilds it from the topology ordinal + process count
 * carried in the open packet — see {@link FactoryMenu}). Each entry pairs a container slot index with its GUI x/y and a
 * visual type (input/output), matching the underlying {@link FactoryBlockEntity}'s slot indexing.
 *
 * <p>Layout (HORIZONTAL, matching real Mekanism): input slots are a single ROW across the top (one per process,
 * left-to-right); output slots are a ROW directly below, each output column-aligned under its input with an arrow gap;
 * a down-arrow is drawn per process column between the input and its output. The shared chemical tank (chemical
 * factories) is a bar on the LEFT of the window (drawn by the screen, not a container slot); the shared extra-input slot
 * (combining) is a single slot to the LEFT of the input row. Sawing adds a SECOND output row below the main output row
 * (still column-aligned per process). The grid x-origin and the window width scale with the process count (ultimate=9 →
 * wide window). The positions are stable; the real Mekanism factory GuiElement layouts arrive with the machine-framework
 * migration to {@code :common}.
 */
public final class FactorySlotLayout {

    /** Visual frame for a slot background. */
    public enum SlotType { INPUT, OUTPUT }

    /** One GUI slot: its container index, position, and visual type. */
    public record SlotSpec(int containerIndex, int x, int y, SlotType type) {
    }

    /** A down-arrow between an input column and its output, at the given GUI x/y (8x10 sprite top-left). */
    public record ArrowSpec(int x, int y) {
    }

    private final List<SlotSpec> slots;
    private final List<ArrowSpec> arrows;
    private final int machineSlotCount;
    private final boolean hasTank;
    private final int processes;
    private final int outputRows;
    private final int gridLeft;
    private final int contentWidth;

    private FactorySlotLayout(List<SlotSpec> slots, List<ArrowSpec> arrows, int machineSlotCount, boolean hasTank,
          int processes, int outputRows, int gridLeft, int contentWidth) {
        this.slots = slots;
        this.arrows = arrows;
        this.machineSlotCount = machineSlotCount;
        this.hasTank = hasTank;
        this.processes = processes;
        this.outputRows = outputRows;
        this.gridLeft = gridLeft;
        this.contentWidth = contentWidth;
    }

    public List<SlotSpec> slots() {
        return slots;
    }

    /** Down-arrows to draw, one per process column, between each input and its output. */
    public List<ArrowSpec> arrows() {
        return arrows;
    }

    /** Number of machine (container) slots this layout exposes (equals the BE container size). */
    public int machineSlotCount() {
        return machineSlotCount;
    }

    public boolean hasTank() {
        return hasTank;
    }

    /** Process (column) count. */
    public int processes() {
        return processes;
    }

    /** Number of output ROWS below the input row (1 normally, 2 for sawing's secondary outputs). */
    public int outputRows() {
        return outputRows;
    }

    /** Left x of the input/output grid (the leftmost slot's x). */
    public int gridLeft() {
        return gridLeft;
    }

    /** Total content width (window imageWidth) needed to fit the side bars + the process grid. */
    public int contentWidth() {
        return contentWidth;
    }

    /** Y of the bottom-most output row (used by the screen/menu to place the player inventory below everything). */
    public int bottomRowY() {
        return OUTPUT_ROW_Y + (outputRows - 1) * ROW_SPACING;
    }

    /** ContainerData size: [0]=energy, [1]=progress, then 1 tank entry when the factory has a shared chemical tank. */
    public int dataSize() {
        return 2 + (hasTank ? 1 : 0);
    }

    // --- Horizontal grid geometry (GUI px). ---
    private static final int SLOT_SPACING = 18;
    private static final int ROW_SPACING = 18;
    /** Input row Y, output row(s) one arrow-gap below. */
    private static final int INPUT_ROW_Y = 17;
    private static final int OUTPUT_ROW_Y = INPUT_ROW_Y + 2 * ROW_SPACING; // 53 — leaves a row for the down-arrow
    /** Left margin reserved for the energy/tank/extra-input side gutter before the process grid begins. */
    private static final int LEFT_GUTTER = 26;
    /** Right margin after the last grid column (room for the energy bar). */
    private static final int RIGHT_GUTTER = 26;
    /** Minimum window width — never narrower than the standard 176px chest-style window. */
    private static final int MIN_WIDTH = 176;
    /** Combining's shared extra-input slot sits in the left gutter, aligned to the input row. */
    private static final int EXTRA_INPUT_X = 8;
    /** Down-arrow sprite is 8px wide; center it in the 18px slot column. */
    private static final int ARROW_X_INSET = (SLOT_SPACING - 8) / 2;
    /** Down-arrow sits between the input row and the output row. */
    private static final int ARROW_Y = INPUT_ROW_Y + ROW_SPACING + 4;

    /** Builds the layout for the given factory type + process count. */
    public static FactorySlotLayout build(FactoryType factoryType, int processes) {
        Topology topology = factoryType.getTopology();
        List<SlotSpec> specs = new ArrayList<>();
        List<ArrowSpec> arrowSpecs = new ArrayList<>();
        int containerSize = FactoryBlockEntity.containerSizeFor(topology, processes);
        boolean hasTank = topology == Topology.ITEM_CHEMICAL_TO_ITEM;

        int gridWidth = processes * SLOT_SPACING;
        int contentWidth = Math.max(MIN_WIDTH, LEFT_GUTTER + gridWidth + RIGHT_GUTTER);
        // Center the process grid within the gutters.
        int gridLeft = (contentWidth - gridWidth) / 2;

        // Input row (one per process), index 0..P-1.
        for (int p = 0; p < processes; p++) {
            specs.add(new SlotSpec(p, gridLeft + p * SLOT_SPACING, INPUT_ROW_Y, SlotType.INPUT));
        }
        // One down-arrow per process column, centered in the column, between input and output rows.
        for (int p = 0; p < processes; p++) {
            arrowSpecs.add(new ArrowSpec(gridLeft + p * SLOT_SPACING + ARROW_X_INSET, ARROW_Y));
        }

        int outputRows = 1;
        switch (topology) {
            case ITEM_TO_ITEM, ITEM_CHEMICAL_TO_ITEM -> {
                // Output row directly below each input: index P..2P-1.
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + p, gridLeft + p * SLOT_SPACING, OUTPUT_ROW_Y, SlotType.OUTPUT));
                }
            }
            case COMBINING -> {
                // Shared extra-input slot at index P, in the left gutter on the input row.
                specs.add(new SlotSpec(processes, EXTRA_INPUT_X, INPUT_ROW_Y, SlotType.INPUT));
                // Output row directly below each input: index P+1..2P.
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + 1 + p, gridLeft + p * SLOT_SPACING, OUTPUT_ROW_Y, SlotType.OUTPUT));
                }
            }
            case SAWING -> {
                // Main output row: index P..2P-1; secondary output row directly below it: index 2P..3P-1.
                outputRows = 2;
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + p, gridLeft + p * SLOT_SPACING, OUTPUT_ROW_Y, SlotType.OUTPUT));
                }
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(2 * processes + p, gridLeft + p * SLOT_SPACING,
                          OUTPUT_ROW_Y + ROW_SPACING, SlotType.OUTPUT));
                }
            }
        }
        return new FactorySlotLayout(List.copyOf(specs), List.copyOf(arrowSpecs), containerSize, hasTank,
              processes, outputRows, gridLeft, contentWidth);
    }
}
