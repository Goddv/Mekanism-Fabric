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
 * <p>Layout: input slots are laid out in a column down the left; output slots in a column on the right. The shared
 * chemical tank (chemical factories) or shared extra-input slot (combining) is placed below the input column. Sawing draws
 * two output columns (main + secondary). The positions are coarse but stable — the real Mekanism factory GuiElement
 * layouts arrive with the machine-framework migration to {@code :common}.
 */
public final class FactorySlotLayout {

    /** Visual frame for a slot background. */
    public enum SlotType { INPUT, OUTPUT }

    /** One GUI slot: its container index, position, and visual type. */
    public record SlotSpec(int containerIndex, int x, int y, SlotType type) {
    }

    private final List<SlotSpec> slots;
    private final int machineSlotCount;
    private final boolean hasTank;
    private final int rows;

    private FactorySlotLayout(List<SlotSpec> slots, int machineSlotCount, boolean hasTank, int rows) {
        this.slots = slots;
        this.machineSlotCount = machineSlotCount;
        this.hasTank = hasTank;
        this.rows = rows;
    }

    public List<SlotSpec> slots() {
        return slots;
    }

    /** Number of machine (container) slots this layout exposes (equals the BE container size). */
    public int machineSlotCount() {
        return machineSlotCount;
    }

    public boolean hasTank() {
        return hasTank;
    }

    /** Number of slot rows in the factory grid (= process count; sawing/combining still have one row per process). */
    public int rows() {
        return rows;
    }

    /** ContainerData size: [0]=energy, [1]=progress, then 1 tank entry when the factory has a shared chemical tank. */
    public int dataSize() {
        return 2 + (hasTank ? 1 : 0);
    }

    private static final int INPUT_X = 26;
    private static final int OUTPUT_X = 122;
    private static final int SAWING_SECONDARY_X = 140;
    private static final int FIRST_Y = 17;
    private static final int ROW_SPACING = 18;

    /** Builds the layout for the given factory type + process count. */
    public static FactorySlotLayout build(FactoryType factoryType, int processes) {
        Topology topology = factoryType.getTopology();
        List<SlotSpec> specs = new ArrayList<>();
        int containerSize = FactoryBlockEntity.containerSizeFor(topology, processes);
        boolean hasTank = topology == Topology.ITEM_CHEMICAL_TO_ITEM;

        // Input column (one per process), index 0..P-1.
        for (int p = 0; p < processes; p++) {
            specs.add(new SlotSpec(p, INPUT_X, FIRST_Y + p * ROW_SPACING, SlotType.INPUT));
        }
        switch (topology) {
            case ITEM_TO_ITEM, ITEM_CHEMICAL_TO_ITEM -> {
                // Output column: index P..2P-1.
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + p, OUTPUT_X, FIRST_Y + p * ROW_SPACING, SlotType.OUTPUT));
                }
            }
            case COMBINING -> {
                // Shared extra-input slot at index P, placed below the input column.
                specs.add(new SlotSpec(processes, INPUT_X + ROW_SPACING, FIRST_Y, SlotType.INPUT));
                // Output column: index P+1..2P.
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + 1 + p, OUTPUT_X, FIRST_Y + p * ROW_SPACING, SlotType.OUTPUT));
                }
            }
            case SAWING -> {
                // Main output column: index P..2P-1; secondary output column: index 2P..3P-1.
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(processes + p, OUTPUT_X, FIRST_Y + p * ROW_SPACING, SlotType.OUTPUT));
                }
                for (int p = 0; p < processes; p++) {
                    specs.add(new SlotSpec(2 * processes + p, SAWING_SECONDARY_X, FIRST_Y + p * ROW_SPACING, SlotType.OUTPUT));
                }
            }
        }
        return new FactorySlotLayout(List.copyOf(specs), containerSize, hasTank, processes);
    }
}
