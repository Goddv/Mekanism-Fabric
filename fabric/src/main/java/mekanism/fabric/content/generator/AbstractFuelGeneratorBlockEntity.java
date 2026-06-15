package mekanism.fabric.content.generator;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.fabric.content.machine.gui.MachineGuiType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Transitional Fabric bring-up: base for the fuel-burning generators (Heat, Bio). Adds a single-slot fuel inventory
 * exposed as a {@link WorldlyContainer} (so hoppers/pipes can feed it but never extract fuel) on top of the shared
 * {@link AbstractGeneratorBlockEntity} energy reservoir. The burn loop is the same shape as the demo {@code
 * GeneratorBlockEntity}: when idle with room for energy and a valid fuel in the slot, start a burn for {@link
 * #burnDurationFor(ItemStack)} ticks consuming one item; while burning, insert {@link #generationPerTick()} each tick.
 * Subclasses choose what counts as fuel ({@link #isFuel(ItemStack)}), how long it burns, and the per-tick rate.
 */
public abstract class AbstractFuelGeneratorBlockEntity extends AbstractGeneratorBlockEntity implements WorldlyContainer {

    private static final int[] FUEL_SLOT = {0};

    private final NonNullList<ItemStack> fuel = NonNullList.withSize(1, ItemStack.EMPTY);
    private int burnTime;
    private int burnTimeTotal;

    protected AbstractFuelGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Whether the given stack is accepted as fuel by this generator. */
    protected abstract boolean isFuel(ItemStack stack);

    /** Burn duration (ticks) for one of the given fuel item, or {@code <= 0} to reject it. */
    protected abstract int burnDurationFor(ItemStack stack);

    /** Energy inserted into the reservoir per tick while burning. */
    protected abstract long generationPerTick();

    @Override
    protected void generate(net.minecraft.server.level.ServerLevel level) {
        // Start a new burn if idle, there's room for energy, and the fuel slot holds a valid fuel.
        if (burnTime <= 0 && !fuel.get(0).isEmpty() && isFuel(fuel.get(0))) {
            int duration = burnDurationFor(fuel.get(0));
            if (duration > 0) {
                burnTime = duration;
                burnTimeTotal = duration;
                fuel.get(0).shrink(1);
                setChanged();
            }
        }
        if (burnTime > 0) {
            burnTime--;
            energy.insert(generationPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
            setChanged();
        }
    }

    /** Right-click insertion: adds one of the held fuel item to the fuel slot. Returns true if accepted. */
    public boolean addFuel(ItemStack held) {
        ItemStack slot = fuel.get(0);
        if (slot.isEmpty()) {
            fuel.set(0, held.copyWithCount(1));
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(slot, held) && slot.getCount() < slot.getMaxStackSize()) {
            slot.grow(1);
            setChanged();
            return true;
        }
        return false;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    // ---- GUI: a fuel slot + energy bar (no recipe progress) ----
    @Override
    protected MachineGuiType menuGuiType() {
        return MachineGuiType.FUEL_GENERATOR;
    }

    @Override
    protected Container menuContainer() {
        return this; // the 1-slot fuel inventory IS the menu's machine container
    }

    // ---- fuel inventory (Container) ----
    @Override
    public int getContainerSize() {
        return fuel.size();
    }

    @Override
    public boolean isEmpty() {
        return fuel.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return fuel.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(fuel, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(fuel, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        fuel.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isFuel(stack);
    }

    // ---- sided item I/O (hoppers / pipes): accept fuel into slot 0; never let automation extract fuel ----
    @Override
    public int[] getSlotsForFace(Direction side) {
        return FUEL_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        fuel.clear();
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burnTime", burnTime);
        output.putInt("burnTimeTotal", burnTimeTotal);
        ContainerHelper.saveAllItems(output, fuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getInt("burnTime").orElse(0);
        burnTimeTotal = input.getInt("burnTimeTotal").orElse(0);
        ContainerHelper.loadAllItems(input, fuel);
    }
}
