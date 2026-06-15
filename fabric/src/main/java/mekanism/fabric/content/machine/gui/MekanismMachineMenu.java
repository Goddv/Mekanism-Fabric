package mekanism.fabric.content.machine.gui;

import mekanism.fabric.content.machine.gui.MachineGuiType.SlotSpec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Transitional Fabric bring-up: ONE generic machine/generator container-menu, driven by a {@link MachineGuiType} shape.
 * Lays out that shape's machine slots (from {@link MachineGuiType#slots()}) plus the standard player inventory, and syncs
 * live values (energy permille, recipe-progress permille, per-tank amount permille) through a vanilla {@link ContainerData}.
 *
 * <p>The server builds it over the real machine block-entity (its {@link Container} inventory + a {@link ContainerData}
 * reading the BE's live energy/progress/tank). The client builds it from the open packet ({@link #fromNetwork}) over a
 * dummy {@link SimpleContainer} + {@link SimpleContainerData} of the right sizes, which vanilla menu-sync fills. The shape
 * ordinal travels in the open packet so the client lays out identical slots. Replaces the six {@code createMenu == null}
 * stubs (chemical / dual-item machines + the four generators); the real Mekanism GuiElement layouts arrive with the
 * machine-framework migration to {@code :common}.
 */
public class MekanismMachineMenu extends AbstractContainerMenu {

    private final Container machine;
    private final ContainerData data;
    private final MachineGuiType guiType;
    private final int machineSlotCount;

    /** Server-side constructor — backed by the real block-entity (its inventory + a live ContainerData). */
    public MekanismMachineMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data,
          MachineGuiType guiType) {
        super(MekanismMachineMenus.MEKANISM_MACHINE.get(), containerId);
        this.guiType = guiType;
        this.machineSlotCount = guiType.machineSlotCount();
        if (machine.getContainerSize() < machineSlotCount) {
            throw new IllegalArgumentException("Machine container too small for GUI " + guiType + ": "
                  + machine.getContainerSize() + " < " + machineSlotCount);
        }
        this.machine = machine;
        this.data = data;
        addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** Client-side factory from the open packet: reads the shape ordinal, builds matching dummy container + data. */
    public static MekanismMachineMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        MachineGuiType guiType = MachineGuiType.byOrdinal(buf.readVarInt());
        Container dummy = new SimpleContainer(Math.max(1, guiType.machineSlotCount()));
        ContainerData data = new SimpleContainerData(guiType.dataSize());
        return new MekanismMachineMenu(containerId, playerInventory, dummy, data, guiType);
    }

    private void addMachineSlots() {
        for (SlotSpec spec : guiType.slots()) {
            addSlot(new Slot(machine, spec.containerIndex(), spec.x(), spec.y()));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public MachineGuiType guiType() {
        return guiType;
    }

    /** Energy fill 0..1000 permille (synced; index 0). */
    public int getEnergyPermille() {
        return data.get(0);
    }

    /** Recipe progress 0..1000 permille (synced; index 1). 0 for generators (no progress). */
    public int getProgressPermille() {
        return data.get(1);
    }

    /** Chemical-tank fill 0..1000 permille (synced; index {@code 2 + tank}). */
    public int getTankPermille(int tank) {
        int index = 2 + tank;
        return index < data.getCount() ? data.get(index) : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /**
     * Quick-move between the machine slots and the player inventory. From a machine slot, push into the player inventory.
     * From the player inventory, pull into the FIRST machine input slot that can accept the stack (so shift-click feeds
     * inputs; output slots are not accepted as targets since vanilla {@code moveItemStackTo} respects {@link
     * Slot#mayPlace}, but the dummy/SimpleContainer slots allow placement — so we restrict targets to input specs).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = machineSlotCount;
            int playerEnd = slots.size();
            if (index < machineSlotCount) {
                // Machine slot -> player inventory.
                if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveIntoInputs(stack)) {
                // Player inventory -> machine input slots (skip output slots).
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** Move the stack into the machine's INPUT slots only (by spec type), leaving outputs untouched. */
    private boolean moveIntoInputs(ItemStack stack) {
        boolean moved = false;
        var specs = guiType.slots();
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).type() == MachineGuiType.SlotType.INPUT) {
                if (moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
        return moved;
    }
}
