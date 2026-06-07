package mekanism.fabric.content.machine;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block-entity for real Mekanism machine blocks. Stores energy + a 2-slot inventory,
 * and each server tick processes one item (input slot 0 -> output slot 1) consuming energy, driving the block's
 * {@code active} state (so the real active model/glow shows while working). Exposes energy + item capabilities. The
 * processing here is a demo loop; the real recipe system replaces it when the machine framework is migrated to :common.
 */
public class MachineBlockEntity extends BlockEntity implements Container, IMekanismStrictEnergyHandler {

    private static final long ENERGY_PER_OP = 200L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricRealMachines.BE_TYPE.get(), pos, state);
    }

    public void serverTick(BlockState state) {
        boolean canProcess = !items.get(0).isEmpty()
              && energy.extract(ENERGY_PER_OP, Action.SIMULATE, AutomationType.INTERNAL) >= ENERGY_PER_OP
              && canOutput();
        if (canProcess) {
            energy.extract(ENERGY_PER_OP, Action.EXECUTE, AutomationType.INTERNAL);
            ItemStack input = items.get(0);
            ItemStack output = items.get(1);
            if (output.isEmpty()) {
                items.set(1, input.copyWithCount(1));
            } else {
                output.grow(1);
            }
            input.shrink(1);
            setChanged();
        }
        if (level != null && state.getValue(MachineBlock.ACTIVE) != canProcess) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.ACTIVE, canProcess), Block.UPDATE_ALL);
        }
    }

    private boolean canOutput() {
        ItemStack input = items.get(0);
        ItemStack output = items.get(1);
        return output.isEmpty() || (ItemStack.isSameItemSameComponents(output, input) && output.getCount() < output.getMaxStackSize());
    }

    // ---- energy capability ----
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    // ---- item inventory (Container) ----
    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        ContainerHelper.loadAllItems(input, items);
    }
}
