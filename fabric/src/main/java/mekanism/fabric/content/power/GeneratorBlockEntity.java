package mekanism.fabric.content.power;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a basic fuel-burning generator. Consumes one furnace fuel item at a time (vanilla
 * {@code FuelValues}) to produce energy into a {@link BasicEnergyContainer}, and each server tick pushes that energy to
 * adjacent energy acceptors (cables/machines) via {@link EnergyPushHelper}. Exposes the strict-energy capability + a
 * single-slot fuel inventory (insertable by right-click on the block, or by a hopper via {@code ItemStorage}). Lets
 * Mekanism machines be powered in-game — the real Mekanism generators (separate {@code mekanismgenerators} module) come
 * later.
 */
public class GeneratorBlockEntity extends BlockEntity implements Container, IMekanismStrictEnergyHandler {

    private static final long CAPACITY = 400_000L;
    private static final long GENERATION_PER_TICK = 200L;
    private static final long PUSH_RATE = 5_000L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(CAPACITY, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> fuel = NonNullList.withSize(1, ItemStack.EMPTY);

    private int burnTime;
    private int burnTimeTotal;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricPowerInfrastructure.GENERATOR_BE_TYPE.get(), pos, state);
    }

    public void serverTick(ServerLevel level) {
        // Start a new burn if idle, there's room for energy, and the fuel slot holds a valid fuel.
        if (burnTime <= 0 && energy.getNeeded() > 0 && !fuel.get(0).isEmpty()) {
            int duration = level.fuelValues().burnDuration(fuel.get(0));
            if (duration > 0) {
                burnTime = duration;
                burnTimeTotal = duration;
                fuel.get(0).shrink(1);
                setChanged();
            }
        }
        if (burnTime > 0) {
            burnTime--;
            energy.insert(GENERATION_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
            setChanged();
        }
        EnergyPushHelper.pushToNeighbors(level, worldPosition, energy, PUSH_RATE);
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

    // ---- energy capability ----
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    // A generator is a SOURCE: reject energy pushed in via the capability (the burn process fills the container
    // directly). This prevents cables/neighbours from sloshing energy back into the generator. Both the per-container
    // and convenience cap insert entry points are blocked; internal generation bypasses these by inserting on the
    // container instance directly.
    @Override
    public long insertEnergy(int container, long amount, Action action) {
        return amount;
    }

    @Override
    public long insertEnergy(long amount, Action action) {
        return amount;
    }

    @Override
    public void onContentsChanged() {
        setChanged();
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
        return level != null && level.fuelValues().isFuel(stack);
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
        energy.serialize(output);
        output.putInt("burnTime", burnTime);
        output.putInt("burnTimeTotal", burnTimeTotal);
        ContainerHelper.saveAllItems(output, fuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        burnTime = input.getInt("burnTime").orElse(0);
        burnTimeTotal = input.getInt("burnTimeTotal").orElse(0);
        ContainerHelper.loadAllItems(input, fuel);
    }
}
