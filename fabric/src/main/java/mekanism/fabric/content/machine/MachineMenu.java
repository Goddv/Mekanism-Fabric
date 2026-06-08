package mekanism.fabric.content.machine;

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
 * Transitional Fabric bring-up: a basic machine container-menu (input slot, output slot, player inventory) with live
 * energy + recipe-progress sync via a vanilla {@link ContainerData}. The server builds it over the real machine
 * block-entity (reading its energy/progress); the client builds it over a dummy container + data, which vanilla menu
 * sync fills. The real Mekanism GUI layouts/widgets come with the machine-framework migration.
 */
public class MachineMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 2;
    private final Container machine;
    private final ContainerData data;

    /** Client-side constructor (used by the registered menu type). */
    public MachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(2));
    }

    /** Server-side constructor — backed by the real machine block-entity (inventory + live energy/progress). */
    public MachineMenu(int containerId, Inventory playerInventory, MachineBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity, dataFor(blockEntity));
    }

    private MachineMenu(int containerId, Inventory playerInventory, Container machine, ContainerData data) {
        super(FabricMachineMenus.MACHINE.get(), containerId);
        checkContainerSize(machine, MACHINE_SLOTS);
        this.machine = machine;
        this.data = data;
        // Positions match Mekanism's electric machine (GuiElectricMachine / TileEntityElectricMachine).
        addSlot(new Slot(machine, 0, 64, 17));   // input
        addSlot(new Slot(machine, 1, 116, 35));  // output
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    private static ContainerData dataFor(MachineBlockEntity blockEntity) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getEnergyStoredPermille();
                    case 1 -> blockEntity.getProgressPermille();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    /** Energy fill 0..1000 permille (synced). */
    public int getEnergyPermille() {
        return data.get(0);
    }

    /** Recipe progress 0..1000 permille (synced). */
    public int getProgressPermille() {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
}
