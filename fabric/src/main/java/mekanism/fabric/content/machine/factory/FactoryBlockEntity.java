package mekanism.fabric.content.machine.factory;

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
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.fabric.content.machine.factory.FactoryType.Topology;
import mekanism.fabric.content.power.EnergyTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the generic block-entity for ALL 36 Mekanism factory blocks — a multi-process version of a
 * base machine that runs the SAME base recipe type {@code N} times in parallel ({@code basic=3, advanced=5, elite=7,
 * ultimate=9} processes). It is parameterized at runtime by the host {@link FactoryBlock}'s {@link FactoryType} (which base
 * recipe type + which {@link Topology}) and process count, so one BE class serves every factory.
 *
 * <p><b>Shared vs per-process.</b> Energy is shared (one {@link BasicEnergyContainer}, consumed per ACTIVE process this
 * tick). Each process has its OWN input slot(s), output slot(s), and progress counter. The chemical factories
 * (compressing/purifying/injecting/infusing) share ONE chemical input tank across all processes; the combining factory
 * shares ONE extra-input slot across all processes.
 *
 * <p><b>Slot layout</b> (item {@link Container}; {@code P} = processes):
 * <ul>
 *   <li>{@code ITEM_TO_ITEM} / {@code ITEM_CHEMICAL_TO_ITEM}: {@code [in_0..in_{P-1}]} then {@code [out_0..out_{P-1}]} (size {@code 2P}).</li>
 *   <li>{@code COMBINING}: {@code [main_0..main_{P-1}]}, the shared {@code [extra]} slot, then {@code [out_0..out_{P-1}]} (size {@code 2P+1}).</li>
 *   <li>{@code SAWING}: {@code [in_0..in_{P-1}]}, {@code [mainOut_0..mainOut_{P-1}]}, {@code [secOut_0..secOut_{P-1}]} (size {@code 3P}).</li>
 * </ul>
 *
 * <p><b>serverTick.</b> Pulls energy from neighbours, then for EACH process index resolves the {@link FactoryType}'s base
 * recipe type by id and runs that process's lookup + process EXACTLY like the corresponding base-machine BE
 * (item&rarr;item / item+chemical&rarr;item over the shared tank / combining over the shared extra slot / sawing with a
 * chance secondary). Each process advances its own progress; on completion it consumes its inputs (+ shared
 * tank/extra) and emits its output(s). Energy is consumed once per active process this tick.
 *
 * <p>Capabilities: {@link WorldlyContainer} (all input slots fillable, output slots extract-only),
 * {@link IMekanismStrictEnergyHandler} (energy sink), and — for the chemical factories — {@link IChemicalHandler}
 * exposing the single shared input tank (external insert + extract allowed, so pipes / the self-test can fill it).
 */
public class FactoryBlockEntity extends BlockEntity implements WorldlyContainer, IMekanismStrictEnergyHandler,
      IChemicalHandler, MenuProvider {

    private static final long ENERGY_PER_TICK = 100L;
    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one operation (per process). */
    public static final int MAX_PROGRESS = 60;
    private static final long TANK_CAPACITY = 10_000L;
    /** Energy capacity scales mildly with tier so wider factories can power all their processes. */
    private static final long ENERGY_PER_PROCESS_CAPACITY = 1_000_000L;

    private final FactoryType factoryType;
    private final Topology topology;
    private final int processes;

    private final BasicEnergyContainer energy;
    private final List<IEnergyContainer> energyContainers;
    private final NonNullList<ItemStack> items;
    /** Shared chemical input tank (chemical factories only; otherwise unused/empty). */
    private final IChemicalTank inputTank = BasicChemicalTank.create(TANK_CAPACITY, (IContentsListener) this);
    private final int[] progress;

    public FactoryBlockEntity(BlockPos pos, BlockState state) {
        super(beType(state), pos, state);
        FactoryBlock block = (FactoryBlock) state.getBlock();
        this.factoryType = block.factoryType();
        this.topology = factoryType.getTopology();
        this.processes = block.processes();
        this.energy = BasicEnergyContainer.create(ENERGY_PER_PROCESS_CAPACITY * processes, this);
        this.energyContainers = List.of(energy);
        this.items = NonNullList.withSize(containerSizeFor(topology, processes), ItemStack.EMPTY);
        this.progress = new int[processes];
    }

    /** Resolves the shared factory BE type from the host block (one BE type per process count). */
    private static net.minecraft.world.level.block.entity.BlockEntityType<?> beType(BlockState state) {
        return ((FactoryBlock) state.getBlock()).beType();
    }

    /** The item-container size for a topology + process count (see class doc). */
    public static int containerSizeFor(Topology topology, int processes) {
        return switch (topology) {
            case ITEM_TO_ITEM, ITEM_CHEMICAL_TO_ITEM -> 2 * processes;
            case COMBINING -> 2 * processes + 1;
            case SAWING -> 3 * processes;
        };
    }

    // ---- slot index helpers (per topology) ----

    private int inputSlot(int process) {
        return process; // inputs are always first, one per process
    }

    /** The shared extra-input slot (COMBINING only): right after the P main inputs. */
    private int extraSlot() {
        return processes;
    }

    private int outputSlot(int process) {
        return switch (topology) {
            case ITEM_TO_ITEM, ITEM_CHEMICAL_TO_ITEM -> processes + process;
            case COMBINING -> processes + 1 + process; // after P mains + 1 shared extra
            case SAWING -> processes + process;         // main outputs block
        };
    }

    /** Secondary (chance) output slot for a process (SAWING only): after the P main outputs. */
    private int secondaryOutputSlot(int process) {
        return 2 * processes + process;
    }

    // ---- tick / processing ----

    public void serverTick() {
        if (level instanceof ServerLevel serverLevel) {
            EnergyTransferHelper.pull(serverLevel, worldPosition, energy, ENERGY_PULL_RATE, false);
        }
        boolean anyActive = false;
        if (level instanceof ServerLevel serverLevel) {
            for (int p = 0; p < processes; p++) {
                anyActive |= processOne(serverLevel, p);
            }
        }
        updateActiveState(anyActive);
    }

    /** Reflects whether ANY process is currently running in the {@code active} blockstate. */
    private void updateActiveState(boolean active) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof FactoryBlock && state.getValue(FactoryBlock.ACTIVE) != active && level != null) {
            level.setBlock(worldPosition, state.setValue(FactoryBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /** Runs one process index for one tick, dispatching by topology. Returns whether this process processed this tick. */
    private boolean processOne(ServerLevel serverLevel, int p) {
        return switch (topology) {
            case ITEM_TO_ITEM -> processItemToItem(serverLevel, p);
            case ITEM_CHEMICAL_TO_ITEM -> processItemChemicalToItem(serverLevel, p);
            case COMBINING -> processCombining(serverLevel, p);
            case SAWING -> processSawing(serverLevel, p);
        };
    }

    // ---- ITEM_TO_ITEM (smelting/enriching/crushing) — mirrors MachineBlockEntity ----

    @SuppressWarnings("unchecked")
    private boolean processItemToItem(ServerLevel serverLevel, int p) {
        RecipeType<ItemStackToItemStackRecipe> recipeType =
              (RecipeType<ItemStackToItemStackRecipe>) BuiltInRegistries.RECIPE_TYPE.getValue(factoryType.getRecipeTypeId());
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(inputSlot(p));
        if (input.isEmpty()) {
            return resetProgress(p);
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<ItemStackToItemStackRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress(p);
        }
        ItemStackToItemStackRecipe recipe = match.get().value();
        int needed = recipe.getInput().count();
        ItemStack result = recipe.getOutput(input).create();
        int out = outputSlot(p);
        if (result.isEmpty() || input.getCount() < needed || !canInsert(out, result)) {
            return resetProgress(p);
        }
        if (!consumeEnergy()) {
            return false;
        }
        if (++progress[p] >= MAX_PROGRESS) {
            progress[p] = 0;
            input.shrink(needed);
            insert(out, result);
        }
        setChanged();
        return true;
    }

    // ---- ITEM_CHEMICAL_TO_ITEM (compressing/purifying/injecting/infusing) — mirrors ItemChemicalToItemMachineBlockEntity ----

    @SuppressWarnings("unchecked")
    private boolean processItemChemicalToItem(ServerLevel serverLevel, int p) {
        RecipeType<ItemStackChemicalToItemStackRecipe> recipeType =
              (RecipeType<ItemStackChemicalToItemStackRecipe>) BuiltInRegistries.RECIPE_TYPE.getValue(factoryType.getRecipeTypeId());
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(inputSlot(p));
        ChemicalStack chemical = inputTank.getStack();
        if (input.isEmpty() || chemical.isEmpty()) {
            return resetProgress(p);
        }
        SingleItemChemicalRecipeInput recipeInput = new SingleItemChemicalRecipeInput(input, chemical);
        Optional<RecipeHolder<ItemStackChemicalToItemStackRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress(p);
        }
        ItemStackChemicalToItemStackRecipe recipe = match.get().value();
        int neededItems = recipe.getItemInput().count();
        long neededChemical = recipe.getChemicalInput().amount();
        ItemStack result = recipe.getOutput(input, chemical).create();
        int out = outputSlot(p);
        if (result.isEmpty() || input.getCount() < neededItems || chemical.amount() < neededChemical || !canInsert(out, result)) {
            return resetProgress(p);
        }
        if (!consumeEnergy()) {
            return false;
        }
        if (++progress[p] >= MAX_PROGRESS) {
            progress[p] = 0;
            input.shrink(neededItems);
            // Shared tank: each completing process consumes the recipe's chemical amount from the single tank.
            inputTank.extract(neededChemical, Action.EXECUTE, AutomationType.INTERNAL);
            insert(out, result);
        }
        setChanged();
        return true;
    }

    // ---- COMBINING (combiner) — mirrors CombinerMachineBlockEntity, with a shared extra-input slot ----

    @SuppressWarnings("unchecked")
    private boolean processCombining(ServerLevel serverLevel, int p) {
        RecipeType<CombinerRecipe> recipeType =
              (RecipeType<CombinerRecipe>) BuiltInRegistries.RECIPE_TYPE.getValue(factoryType.getRecipeTypeId());
        if (recipeType == null) {
            return false;
        }
        ItemStack main = items.get(inputSlot(p));
        ItemStack extra = items.get(extraSlot());
        if (main.isEmpty() || extra.isEmpty()) {
            return resetProgress(p);
        }
        TwoItemRecipeInput recipeInput = new TwoItemRecipeInput(main, extra);
        Optional<RecipeHolder<CombinerRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress(p);
        }
        CombinerRecipe recipe = match.get().value();
        ItemStack result = recipe.getOutput(main, extra).create();
        int out = outputSlot(p);
        if (result.isEmpty() || main.getCount() < 1 || extra.getCount() < 1 || !canInsert(out, result)) {
            return resetProgress(p);
        }
        if (!consumeEnergy()) {
            return false;
        }
        if (++progress[p] >= MAX_PROGRESS) {
            progress[p] = 0;
            main.shrink(1);
            extra.shrink(1); // shared extra slot, drawn down by each completing process
            insert(out, result);
        }
        setChanged();
        return true;
    }

    // ---- SAWING (precision sawmill) — mirrors SawmillMachineBlockEntity, with per-process main+secondary outputs ----

    @SuppressWarnings("unchecked")
    private boolean processSawing(ServerLevel serverLevel, int p) {
        RecipeType<SawmillRecipe> recipeType =
              (RecipeType<SawmillRecipe>) BuiltInRegistries.RECIPE_TYPE.getValue(factoryType.getRecipeTypeId());
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(inputSlot(p));
        if (input.isEmpty()) {
            return resetProgress(p);
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SawmillRecipe>> match =
              serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress(p);
        }
        SawmillRecipe recipe = match.get().value();
        ChanceOutput output = recipe.getOutput(input);
        ItemStackTemplate mainTemplate = output.getMainOutput();
        ItemStackTemplate maxSecondaryTemplate = output.getMaxSecondaryOutput();
        ItemStack mainOutput = mainTemplate == null ? ItemStack.EMPTY : mainTemplate.create();
        ItemStack maxSecondary = maxSecondaryTemplate == null ? ItemStack.EMPTY : maxSecondaryTemplate.create();
        if (mainOutput.isEmpty() && maxSecondary.isEmpty()) {
            return resetProgress(p);
        }
        int mainOut = outputSlot(p);
        int secOut = secondaryOutputSlot(p);
        if (!mainOutput.isEmpty() && !canInsert(mainOut, mainOutput)) {
            return resetProgress(p);
        }
        if (!maxSecondary.isEmpty() && !canInsert(secOut, maxSecondary)) {
            return resetProgress(p);
        }
        if (!consumeEnergy()) {
            return false;
        }
        if (++progress[p] >= MAX_PROGRESS) {
            progress[p] = 0;
            input.shrink(1);
            if (!mainOutput.isEmpty()) {
                insert(mainOut, mainOutput);
            }
            double chance = recipe.getSecondaryChance();
            if (!maxSecondary.isEmpty() && chance > 0 && serverLevel.getRandom().nextDouble() < chance) {
                insert(secOut, maxSecondary);
            }
        }
        setChanged();
        return true;
    }

    // ---- shared helpers ----

    /** Consumes one process's worth of energy this tick. Returns false (process holds) when out of energy. */
    private boolean consumeEnergy() {
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false;
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        return true;
    }

    private boolean canInsert(int slot, ItemStack result) {
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
              && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void insert(int slot, ItemStack result) {
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            items.set(slot, result.copy());
        } else {
            current.grow(result.getCount());
        }
    }

    private boolean resetProgress(int p) {
        if (progress[p] != 0) {
            progress[p] = 0;
            setChanged();
        }
        return false;
    }

    /** A two-item {@link RecipeInput} matching what {@link CombinerRecipe} reads (slot 0 = main, slot 1 = extra). */
    private record TwoItemRecipeInput(ItemStack main, ItemStack extra) implements RecipeInput {
        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? main : extra;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return main.isEmpty() && extra.isEmpty();
        }
    }

    // ---- GUI sync helpers ----

    /** Energy fill as 0..1000 permille. */
    public int getEnergyStoredPermille() {
        long max = energy.getMaxEnergy();
        return max <= 0L ? 0 : (int) (energy.getEnergy() * 1000L / max);
    }

    /** Aggregate progress (max across processes) as 0..1000 permille — drives the single GUI arrow. */
    public int getProgressPermille() {
        int maxP = 0;
        for (int v : progress) {
            maxP = Math.max(maxP, v);
        }
        return maxP * 1000 / MAX_PROGRESS;
    }

    /** Shared input-tank fill as 0..1000 permille (chemical factories). */
    public int getTankPermille() {
        long cap = inputTank.getCapacity();
        return cap <= 0L ? 0 : (int) (inputTank.getStored() * 1000L / cap);
    }

    /** Direct access to the shared chemical input tank (self-test / chemical capability). */
    public IChemicalTank getInputTank() {
        return inputTank;
    }

    public int processes() {
        return processes;
    }

    public FactoryType factoryType() {
        return factoryType;
    }

    /** The GUI slot layout for this factory (built from topology + process count). */
    public FactorySlotLayout layout() {
        return FactorySlotLayout.build(factoryType, processes);
    }

    /** Live ContainerData for the GUI: [0]=energy permille, [1]=progress permille, [2]=tank permille (when present). */
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
                return layout().dataSize();
            }
        };
    }

    /** The extended menu provider opened from the factory block's use handler. */
    public FactoryMenuProvider menuProvider() {
        return new FactoryMenuProvider(getDisplayName(), this, containerData(), factoryType, processes);
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

    // A factory is an energy SINK: reject external extraction.
    @Override
    public long extractEnergy(int container, long amount, Action action) {
        return 0L;
    }

    @Override
    public long extractEnergy(long amount, Action action) {
        return 0L;
    }

    // ---- chemical capability (shared INPUT tank; external insert AND extract allowed; chemical factories only) ----
    private boolean hasChemicalTank() {
        return topology == Topology.ITEM_CHEMICAL_TO_ITEM;
    }

    @Override
    public int getChemicalTanks() {
        return hasChemicalTank() ? 1 : 0;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return hasChemicalTank() && tank == 0 ? inputTank.getStack() : ChemicalStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        if (hasChemicalTank() && tank == 0) {
            inputTank.setStack(stack);
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return hasChemicalTank() && tank == 0 ? inputTank.getCapacity() : 0L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return hasChemicalTank() && tank == 0 && inputTank.isValid(stack);
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        return hasChemicalTank() && tank == 0 ? inputTank.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        return hasChemicalTank() ? inputTank.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return hasChemicalTank() && tank == 0 ? inputTank.extract(amount, action, AutomationType.EXTERNAL) : ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        return hasChemicalTank() ? inputTank.extract(amount, action, AutomationType.EXTERNAL) : ChemicalStack.EMPTY;
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

    // ---- sided item I/O: insert only into input slots; extract only from output slots ----

    /** Whether a slot index is an INPUT slot (per-process input, or the shared COMBINING extra slot). */
    private boolean isInputSlot(int slot) {
        return switch (topology) {
            case ITEM_TO_ITEM, ITEM_CHEMICAL_TO_ITEM, SAWING -> slot < processes;
            case COMBINING -> slot <= processes; // P main inputs + the shared extra slot at index P
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] all = new int[items.size()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return all;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isInputSlot(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return isInputSlot(slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return !isInputSlot(slot);
    }

    // ---- menu (GUI) ----
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return menuProvider().createMenu(containerId, playerInventory, player);
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putIntArray("progress", progress);
        ContainerHelper.saveAllItems(output, items);
        if (hasChemicalTank()) {
            inputTank.serialize(output.child("inputTank"));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        int[] saved = input.getIntArray("progress").orElse(null);
        if (saved != null) {
            for (int i = 0; i < processes && i < saved.length; i++) {
                progress[i] = saved[i];
            }
        }
        ContainerHelper.loadAllItems(input, items);
        if (hasChemicalTank()) {
            input.child("inputTank").ifPresent(inputTank::deserialize);
        }
    }
}
