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
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.fabric.content.machine.gui.MachineGuiType;
import mekanism.fabric.content.machine.gui.MekanismMenuProvider;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block-entity for the {@code item + chemical -> item} machine family (Osmium
 * Compressor, Purification Chamber, Chemical Injection Chamber, Metallurgic Infuser, Painting Machine) — the TWO-INPUT
 * sibling of {@link ChemicalToItemMachineBlockEntity}. Stores energy + a single item INPUT slot + a single chemical INPUT
 * tank + a single item OUTPUT slot, and each server tick looks up its machine's {@link ItemStackChemicalToItemStackRecipe}
 * (recipe-type id read from its host {@link ItemChemicalMachineBlock}) for the {@code (input item, input chemical)} pair
 * via the vanilla recipe manager. When it matches, there is energy, and the output slot has room for the recipe's item
 * output, it advances progress; on completion ({@link #MAX_PROGRESS} ticks) it consumes one input item + the recipe's
 * chemical amount + energy and places the output {@link ItemStack} into the output slot.
 *
 * <p>Capabilities: energy (machine is an energy sink), chemical (the INPUT tank — ALLOWS external insertion + extraction
 * so pipes/the self-test can fill it, like {@link ChemicalToItemMachineBlockEntity}), and item I/O (the INPUT slot is
 * fillable, the OUTPUT slot is extract-only). {@code createMenu} returns null (no GUI yet).
 *
 * <p>Note: per the transitional bring-up, the chemical is consumed ONCE at completion (the recipe's
 * {@code chemicalInput} ingredient amount). NeoForge's full semantics consume per-tick when
 * {@link ItemStackChemicalToItemStackRecipe#perTickUsage()} is true; that per-tick model arrives with the real
 * machine-framework migration.
 */
public class ItemChemicalToItemMachineBlockEntity extends BlockEntity implements WorldlyContainer, IMekanismStrictEnergyHandler,
      IChemicalHandler, MenuProvider {

    /** Slot 0 = item input (fillable); slot 1 = item output (extract-only). */
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};

    private static final long ENERGY_PER_TICK = 100L;
    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one operation. */
    public static final int MAX_PROGRESS = 60;
    /** Input chemical tank capacity. */
    private static final long TANK_CAPACITY = 10_000L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final IChemicalTank inputTank = BasicChemicalTank.create(TANK_CAPACITY, (IContentsListener) this);
    private int progress;

    public ItemChemicalToItemMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricChemicalMachines.ITEM_CHEMICAL_BE_TYPE.get(), pos, state);
    }

    public void serverTick() {
        // Pull energy from adjacent cables/generators (pull-based model) before processing.
        if (level instanceof ServerLevel serverLevel) {
            EnergyTransferHelper.pull(serverLevel, worldPosition, energy, ENERGY_PULL_RATE, false);
        }
        boolean active = process();
        updateActiveState(active);
    }

    /** Reflects whether the machine is currently processing in the {@code active} blockstate, when the block has one. */
    private void updateActiveState(boolean active) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof ItemChemicalMachineBlock machine && machine.hasActive()
              && state.getValue(ItemChemicalMachineBlock.ACTIVE) != active && level != null) {
            level.setBlock(worldPosition, state.setValue(ItemChemicalMachineBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private RecipeType<ItemStackChemicalToItemStackRecipe> recipeType() {
        if (!(getBlockState().getBlock() instanceof ItemChemicalMachineBlock machine)) {
            return null;
        }
        Identifier id = machine.recipeTypeId();
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(id);
        return (RecipeType<ItemStackChemicalToItemStackRecipe>) type;
    }

    /**
     * Looks up this machine's recipe type (from the host block's id) for the {@code (input item, input chemical)} pair. If
     * it matches, the output slot has room + there is enough energy, advances progress (consuming energy each tick) and, on
     * completion ({@link #MAX_PROGRESS} ticks), consumes one input item + the recipe's chemical amount and inserts its item
     * output into the output slot. Progress resets if the recipe/inputs/room is lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeType<ItemStackChemicalToItemStackRecipe> recipeType = recipeType();
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(INPUT_SLOT);
        ChemicalStack chemical = inputTank.getStack();
        if (input.isEmpty() || chemical.isEmpty()) {
            return resetProgress();
        }
        SingleItemChemicalRecipeInput recipeInput = new SingleItemChemicalRecipeInput(input, chemical);
        Optional<RecipeHolder<ItemStackChemicalToItemStackRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress();
        }
        ItemStackChemicalToItemStackRecipe recipe = match.get().value();
        int neededItems = recipe.getItemInput().count();
        long neededChemical = recipe.getChemicalInput().amount();
        ItemStack result = recipe.getOutput(input, chemical).create();
        if (result.isEmpty() || input.getCount() < neededItems || chemical.amount() < neededChemical) {
            return resetProgress();
        }
        // Room check: the output slot must be able to accept the entire item output this operation.
        if (!canInsertOutput(result)) {
            return resetProgress();
        }
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            input.shrink(neededItems);
            inputTank.extract(neededChemical, Action.EXECUTE, AutomationType.INTERNAL);
            insertOutput(result);
        }
        setChanged();
        return true;
    }

    /** Whether the single output slot can fully accept the given result (empty, or same item with headroom). */
    private boolean canInsertOutput(ItemStack result) {
        ItemStack current = items.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
              && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        ItemStack current = items.get(OUTPUT_SLOT);
        if (current.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            current.grow(result.getCount());
        }
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

    /** Input-tank fill as 0..1000 permille (for the GUI tank bar). */
    public int getTankPermille() {
        long cap = inputTank.getCapacity();
        return cap <= 0L ? 0 : (int) (inputTank.getStored() * 1000L / cap);
    }

    /** Direct access to the input chemical tank for the self-test. */
    public IChemicalTank getInputTank() {
        return inputTank;
    }

    /** Live ContainerData for the GUI: [0]=energy permille, [1]=progress permille, [2]=input-tank permille. */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getEnergyStoredPermille();
                    case 1 -> getProgressPermille();
                    case 2 -> getTankPermille();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return MachineGuiType.ITEM_CHEMICAL_TO_ITEM.dataSize();
            }
        };
    }

    /** The extended menu provider opened from the block's use handler (item input + input tank + item output + energy). */
    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), this, containerData(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
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

    // ---- chemical capability (INPUT tank; external insert AND extract allowed) ----
    @Override
    public int getChemicalTanks() {
        return 1;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return tank == 0 ? inputTank.getStack() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (tank == 0) {
            inputTank.setStack(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return tank == 0 ? inputTank.getCapacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return tank == 0 && inputTank.isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        // Input tank: allow external insertion (pipes feed the chemical reagent to be processed).
        return tank == 0 ? inputTank.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        return inputTank.insert(stack, action, AutomationType.EXTERNAL);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return tank == 0 ? inputTank.extract(amount, action, AutomationType.EXTERNAL) : ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        return inputTank.extract(amount, action, AutomationType.EXTERNAL);
    }

    // ---- item inventory (Container): slot 0 = item input, slot 1 = item output ----
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

    // ---- sided item I/O (hoppers / pipes): insert only into the input slot (0); extract only from the output slot (1) ----
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }

    // ---- menu (GUI) ----
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Item input + input chemical tank + item output + energy bar. Opened in-game via the block's use handler over
        // the extended menu path ({@link #menuProvider()}); this direct create path also backs the gui self-test.
        return menuProvider().createMenu(containerId, playerInventory, player);
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("progress", progress);
        ContainerHelper.saveAllItems(output, items);
        inputTank.serialize(output.child("inputTank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        progress = input.getInt("progress").orElse(0);
        ContainerHelper.loadAllItems(input, items);
        input.child("inputTank").ifPresent(inputTank::deserialize);
    }
}
