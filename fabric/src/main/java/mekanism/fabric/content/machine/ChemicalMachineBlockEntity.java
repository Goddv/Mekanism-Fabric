package mekanism.fabric.content.machine;

import java.util.List;
import java.util.Optional;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.fabric.content.power.EnergyTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block-entity for the Chemical Oxidizer (item input &rarr; chemical output). The
 * chemical-output sibling of {@link MachineBlockEntity}: stores energy + a single item-input slot, and each server tick
 * looks up the {@code mekanism:oxidizing} {@link ItemStackToChemicalRecipe} for the input via the vanilla recipe
 * manager. When it matches and there is energy + tank room, it advances progress; on completion ({@link #MAX_PROGRESS}
 * ticks) it consumes one input + energy and inserts the recipe's {@link ChemicalStack} output into the internal chemical
 * tank. Exposes the energy capability (delegating to its energy container), the chemical capability (the output tank),
 * and item I/O (input slot only). The chemical output is extract-only externally (no external insert).
 */
public class ChemicalMachineBlockEntity extends BlockEntity implements WorldlyContainer, IMekanismStrictEnergyHandler,
      IChemicalHandler, MenuProvider {

    private static final int[] INPUT_SLOTS = {0};

    private static final long ENERGY_PER_TICK = 100L;
    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one operation. */
    public static final int MAX_PROGRESS = 60;
    /** Output chemical tank capacity. */
    private static final long TANK_CAPACITY = 10_000L;

    private static final Identifier OXIDIZING_ID = Identifier.fromNamespaceAndPath("mekanism", "oxidizing");

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final IChemicalTank outputTank = BasicChemicalTank.create(TANK_CAPACITY, (IContentsListener) this);
    private int progress;

    public ChemicalMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricChemicalMachines.BE_TYPE.get(), pos, state);
    }

    public void serverTick() {
        // Pull energy from adjacent cables/generators (pull-based model) before processing.
        if (level instanceof ServerLevel serverLevel) {
            EnergyTransferHelper.pull(serverLevel, worldPosition, energy, ENERGY_PULL_RATE, false);
        }
        process();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static RecipeType<ItemStackToChemicalRecipe> oxidizingType() {
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(OXIDIZING_ID);
        return (RecipeType<ItemStackToChemicalRecipe>) type;
    }

    /**
     * Looks up the {@code mekanism:oxidizing} recipe for the input slot. If it matches and there is room in the output
     * tank + enough energy, advances progress (consuming energy each tick) and, on completion ({@link #MAX_PROGRESS}
     * ticks), consumes the recipe's input count and inserts its chemical output into the tank. Progress resets if the
     * recipe/input/room is lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeType<ItemStackToChemicalRecipe> recipeType = oxidizingType();
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(0);
        if (input.isEmpty()) {
            return resetProgress();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<ItemStackToChemicalRecipe>> match = serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress();
        }
        ItemStackToChemicalRecipe recipe = match.get().value();
        int needed = recipe.getInput().count();
        ChemicalStack result = recipe.getOutput(input);
        if (result.isEmpty() || input.getCount() < needed) {
            return resetProgress();
        }
        // Room check: the tank must be able to accept the entire output stack this operation.
        ChemicalStack remainder = outputTank.insert(result, Action.SIMULATE, AutomationType.INTERNAL);
        if (!remainder.isEmpty()) {
            return resetProgress();
        }
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            outputTank.insert(result, Action.EXECUTE, AutomationType.INTERNAL);
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

    /** Energy fill as 0..1000 permille. */
    public int getEnergyStoredPermille() {
        long max = energy.getMaxEnergy();
        return max <= 0L ? 0 : (int) (energy.getEnergy() * 1000L / max);
    }

    /** Recipe progress as 0..1000 permille. */
    public int getProgressPermille() {
        return progress * 1000 / MAX_PROGRESS;
    }

    /** Direct access to the output tank for the self-test. */
    public IChemicalTank getOutputTank() {
        return outputTank;
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

    // A machine is a SINK: reject energy extraction via the capability so cables/pipes can't drain it (its own
    // processing extracts internally on the container directly, bypassing these cap methods).
    @Override
    public long extractEnergy(int container, long amount, Action action) {
        return 0L;
    }

    @Override
    public long extractEnergy(long amount, Action action) {
        return 0L;
    }

    // ---- chemical capability (output tank; extract-only externally) ----
    @Override
    public int getChemicalTanks() {
        return 1;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return tank == 0 ? outputTank.getStack() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (tank == 0) {
            outputTank.setStack(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return tank == 0 ? outputTank.getCapacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return tank == 0 && outputTank.isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        // Output tank: disallow external insertion (only this BE's processing inserts, internally on the tank).
        return stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        return stack;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return tank == 0 ? outputTank.extract(amount, action, AutomationType.EXTERNAL) : ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        return outputTank.extract(amount, action, AutomationType.EXTERNAL);
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

    // ---- sided item I/O (hoppers / pipes): insert only into the input slot (0); no item output (chemical is fluid-like) ----
    @Override
    public int[] getSlotsForFace(Direction side) {
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    // ---- menu (GUI) ----
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // No dedicated chemical GUI yet (the item-shaped MachineMenu doesn't model a chemical tank). The machine is
        // fully functional headless; a real chemical-tank screen comes with the machine-framework migration.
        return null;
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("progress", progress);
        ContainerHelper.saveAllItems(output, items);
        outputTank.serialize(output.child("outputTank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        progress = input.getInt("progress").orElse(0);
        ContainerHelper.loadAllItems(input, items);
        input.child("outputTank").ifPresent(outputTank::deserialize);
    }
}
