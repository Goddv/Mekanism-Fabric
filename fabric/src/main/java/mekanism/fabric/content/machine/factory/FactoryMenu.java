package mekanism.fabric.content.machine.factory;

import mekanism.fabric.content.machine.factory.FactorySlotLayout.SlotSpec;
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
 * Transitional Fabric bring-up: the container-menu for ALL 36 factory blocks, driven by a {@link FactorySlotLayout}
 * computed from {@code (FactoryType, processes)}. Lays out the factory's N input + N output slots (+ the shared extra
 * slot / tank topology) from the layout, plus the standard player inventory, and syncs energy / aggregate progress /
 * shared-tank amount through a vanilla {@link ContainerData}.
 *
 * <p>The server builds it over the real {@link FactoryBlockEntity} (its {@link Container} + live {@link ContainerData}).
 * The client builds it from the open packet ({@link #fromNetwork}) — which carries the {@link FactoryType} ordinal + the
 * process count — over a dummy {@link SimpleContainer} + {@link SimpleContainerData} of the right sizes, deriving the same
 * layout the server did. Distinct from the generic {@code MekanismMachineMenu} (fixed-shape machines/generators); this one
 * is parameterized so a single menu type serves every tier/type of factory.
 */
public class FactoryMenu extends AbstractContainerMenu {

    private final Container machine;
    private final ContainerData data;
    private final FactorySlotLayout layout;
    private final int machineSlotCount;

    /** Server-side constructor — backed by the real factory block-entity. */
    public FactoryMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data,
          FactoryType factoryType, int processes) {
        super(FactoryMenus.FACTORY.get(), containerId);
        this.layout = FactorySlotLayout.build(factoryType, processes);
        this.machineSlotCount = layout.machineSlotCount();
        if (machine.getContainerSize() < machineSlotCount) {
            throw new IllegalArgumentException("Factory container too small for layout: "
                  + machine.getContainerSize() + " < " + machineSlotCount);
        }
        this.machine = machine;
        this.data = data;
        addMachineSlots();
        addPlayerInventory(playerInventory);
        addDataSlots(data);
    }

    /** Client-side factory from the open packet: reads the factory-type ordinal + process count, rebuilds the layout. */
    public static FactoryMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        FactoryType factoryType = FactoryType.values()[buf.readVarInt()];
        int processes = buf.readVarInt();
        FactorySlotLayout layout = FactorySlotLayout.build(factoryType, processes);
        Container dummy = new SimpleContainer(Math.max(1, layout.machineSlotCount()));
        ContainerData data = new SimpleContainerData(layout.dataSize());
        return new FactoryMenu(containerId, playerInventory, dummy, data, factoryType, processes);
    }

    private void addMachineSlots() {
        for (SlotSpec spec : layout.slots()) {
            addSlot(new Slot(machine, spec.containerIndex(), spec.x(), spec.y()));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Place the player inventory below the (tier-dependent) factory slot grid, so wider tiers don't overlap it.
        int invTop = playerInventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, invTop + 58));
        }
    }

    /** Y of the first player-inventory row: below the factory grid (a row per process). Shared by the screen for sizing. */
    public int playerInventoryTop() {
        return GRID_TOP + layout.rows() * ROW_SPACING + 14;
    }

    /** First factory-slot-grid row Y (matches {@link FactorySlotLayout}). */
    public static final int GRID_TOP = 17;
    private static final int ROW_SPACING = 18;

    public FactorySlotLayout layout() {
        return layout;
    }

    /** Energy fill 0..1000 permille (synced; index 0). */
    public int getEnergyPermille() {
        return data.get(0);
    }

    /** Aggregate recipe progress 0..1000 permille (synced; index 1). */
    public int getProgressPermille() {
        return data.get(1);
    }

    /** Shared chemical-tank fill 0..1000 permille (synced; index 2; 0 for non-chemical factories). */
    public int getTankPermille() {
        return data.getCount() > 2 ? data.get(2) : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /**
     * Quick-move between factory slots and the player inventory. From a factory slot, push into the player inventory.
     * From the player inventory, pull into the FIRST factory INPUT slot that can accept the stack (outputs are skipped).
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
                if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveIntoInputs(stack)) {
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

    /** Move the stack into the factory's INPUT slots only (by spec type), leaving outputs untouched. */
    private boolean moveIntoInputs(ItemStack stack) {
        boolean moved = false;
        var specs = layout.slots();
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).type() == FactorySlotLayout.SlotType.INPUT) {
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
