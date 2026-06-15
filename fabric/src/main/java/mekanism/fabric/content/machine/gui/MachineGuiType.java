package mekanism.fabric.content.machine.gui;

import java.util.List;

/**
 * Transitional Fabric bring-up: the shape descriptor for the generic {@link MekanismMachineMenu} / {@link
 * MekanismMachineScreen}. Each constant captures one distinct machine/generator GUI shape: its machine slots (container
 * index + x/y + visual type), how many chemical-tank bars it draws, and whether it draws a recipe-progress arrow. The
 * shape is sent over the open packet (ordinal) by the BE's {@link mekanism.fabric.content.machine.gui.MekanismMenuProvider}
 * so the client builds the matching dummy container/data + lays out the same slots the server did.
 *
 * <p>Positions mirror the real Mekanism electric-machine family (input 56/64,17 / output 116,35) closely enough for a
 * clean transitional screen; the real GuiElement widget layouts arrive with the machine-framework migration to {@code
 * :common}.
 */
public enum MachineGuiType {

    /** Item input -&gt; chemical output (Chemical Oxidizer, Pigment Extractor): 1 item input slot, 1 output tank, arrow. */
    ITEM_TO_CHEMICAL(List.of(new SlotSpec(0, 56, 17, SlotType.INPUT)), 1, true),

    /** Chemical input -&gt; item output (Chemical Crystallizer): 1 item output slot, 1 input tank, arrow. */
    CHEMICAL_TO_ITEM(List.of(new SlotSpec(0, 116, 35, SlotType.OUTPUT)), 1, true),

    /** Item + chemical input -&gt; item output (Compressor/Purifier/Injector/Infuser/Painter): in+out item, 1 tank, arrow. */
    ITEM_CHEMICAL_TO_ITEM(List.of(
          new SlotSpec(0, 56, 17, SlotType.INPUT),
          new SlotSpec(1, 116, 35, SlotType.OUTPUT)), 1, true),

    /** Item + item -&gt; item (Combiner): main input, extra input, output, no tank, arrow. */
    COMBINER(List.of(
          new SlotSpec(0, 56, 17, SlotType.INPUT),
          new SlotSpec(1, 56, 53, SlotType.INPUT),
          new SlotSpec(2, 116, 35, SlotType.OUTPUT)), 0, true),

    /** Item -&gt; item + chance secondary (Precision Sawmill): input, main output, secondary output, no tank, arrow. */
    SAWMILL(List.of(
          new SlotSpec(0, 56, 35, SlotType.INPUT),
          new SlotSpec(1, 112, 26, SlotType.OUTPUT),
          new SlotSpec(2, 112, 44, SlotType.OUTPUT)), 0, true),

    /** Fuel-burning generator (Heat, Bio): 1 fuel input slot, no tank, no recipe arrow. */
    FUEL_GENERATOR(List.of(new SlotSpec(0, 80, 53, SlotType.INPUT)), 0, false),

    /** Passive generator (Solar, Wind): no inventory slots, no tank, no recipe arrow (energy bar only). */
    PASSIVE_GENERATOR(List.of(), 0, false);

    private final List<SlotSpec> slots;
    private final int tankCount;
    private final boolean hasProgress;

    MachineGuiType(List<SlotSpec> slots, int tankCount, boolean hasProgress) {
        this.slots = slots;
        this.tankCount = tankCount;
        this.hasProgress = hasProgress;
    }

    public List<SlotSpec> slots() {
        return slots;
    }

    /** Number of machine (container) slots this shape exposes. */
    public int machineSlotCount() {
        return slots.size();
    }

    /** Number of chemical-tank bars this shape draws (and synced tank amounts in its ContainerData). */
    public int tankCount() {
        return tankCount;
    }

    /** Whether this shape draws a recipe-progress arrow (machines yes, generators no). */
    public boolean hasProgress() {
        return hasProgress;
    }

    /**
     * The ContainerData size for this shape: index 0 = energy permille, index 1 = progress permille, then one entry per
     * chemical tank (its amount permille). Progress + tanks are always allocated (kept 0 when unused) so the size is a
     * simple, stable {@code 2 + tankCount}.
     */
    public int dataSize() {
        return 2 + tankCount;
    }

    public static MachineGuiType byOrdinal(int ordinal) {
        MachineGuiType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ITEM_TO_CHEMICAL;
    }

    /** One machine slot: its container index, GUI position, and visual frame type. */
    public record SlotSpec(int containerIndex, int x, int y, SlotType type) {
    }

    /** Visual frame for a slot background (drives which Mekanism slot texture is drawn). */
    public enum SlotType {
        INPUT, OUTPUT, NORMAL
    }
}
