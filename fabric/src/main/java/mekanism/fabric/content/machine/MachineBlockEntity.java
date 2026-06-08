package mekanism.fabric.content.machine;

import java.util.List;
import java.util.Optional;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
public class MachineBlockEntity extends BlockEntity implements Container, IMekanismStrictEnergyHandler, MenuProvider {

    private static final long ENERGY_PER_TICK = 100L;
    /** Ticks to complete one operation (drives the progress arrow animation). */
    public static final int MAX_PROGRESS = 60;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricRealMachines.BE_TYPE.get(), pos, state);
    }

    public void serverTick(BlockState state) {
        boolean canProcess = process();
        if (level != null && state.getValue(MachineBlock.ACTIVE) != canProcess) {
            level.setBlock(worldPosition, state.setValue(MachineBlock.ACTIVE, canProcess), Block.UPDATE_ALL);
        }
    }

    /**
     * Looks up the {@link ItemStackToItemStackRecipe} for this machine's recipe type + input slot via the vanilla recipe
     * manager. If it matches and there is room + energy, advances progress (consuming energy each tick) and, on
     * completion ({@link #MAX_PROGRESS} ticks), consumes the recipe's input count and produces its output. Returns
     * whether processing happened this tick (drives the ACTIVE blockstate). Progress resets if the recipe/input/room is
     * lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeType<ItemStackToItemStackRecipe> recipeType = getBlockState().getBlock() instanceof MachineBlock machine ? machine.recipeType() : null;
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(0);
        if (input.isEmpty()) {
            return resetProgress();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<ItemStackToItemStackRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress();
        }
        ItemStackToItemStackRecipe recipe = match.get().value();
        int needed = recipe.getInput().count();
        ItemStack result = recipe.getOutput(input).create();
        if (input.getCount() < needed || !canFit(result)) {
            return resetProgress();
        }
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            ItemStack output = items.get(1);
            if (output.isEmpty()) {
                items.set(1, result);
            } else {
                output.grow(result.getCount());
            }
            input.shrink(needed);
        }
        setChanged();
        return true;
    }

    private boolean resetProgress() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
        return false;
    }

    /** Energy fill as 0..1000 permille (for GUI sync; raw energy exceeds int range). */
    public int getEnergyStoredPermille() {
        long max = energy.getMaxEnergy();
        return max <= 0L ? 0 : (int) (energy.getEnergy() * 1000L / max);
    }

    /** Recipe progress as 0..1000 permille (for the progress-arrow fill). */
    public int getProgressPermille() {
        return progress * 1000 / MAX_PROGRESS;
    }

    private boolean canFit(ItemStack result) {
        ItemStack output = items.get(1);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
              && output.getCount() + result.getCount() <= output.getMaxStackSize();
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

    // ---- menu (GUI) ----
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineMenu(containerId, playerInventory, this);
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("progress", progress);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        progress = input.getInt("progress").orElse(0);
        ContainerHelper.loadAllItems(input, items);
    }
}
