package mekanism.fabric.content.machine;

import java.util.List;
import java.util.Optional;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.recipes.CombinerRecipe;
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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block-entity for the Combiner ({@code item + item -> item}). Stores energy + a MAIN
 * item input slot + an EXTRA item input slot + a single item OUTPUT slot, and each server tick looks up its
 * {@link CombinerRecipe} (recipe-type id {@code mekanism:combining}) for the {@code (main, extra)} pair via the vanilla
 * recipe manager. {@link CombinerRecipe#matches(RecipeInput, net.minecraft.world.level.Level)} reads
 * {@code input.getItem(0)} (main) / {@code input.getItem(1)} (extra) and requires {@code input.size() == 2}, so this BE
 * exposes a two-item {@link RecipeInput}. When it matches, there is energy, and the output slot has room for the recipe's
 * output, it advances progress; on completion ({@link #MAX_PROGRESS} ticks) it consumes one from each input slot + energy
 * and places the assembled output {@link ItemStack} into the output slot.
 *
 * <p>Capabilities: energy (machine is an energy sink), and item I/O (both input slots fillable, the output slot
 * extract-only). {@code createMenu} returns null (no GUI yet).
 */
public class CombinerMachineBlockEntity extends BlockEntity implements WorldlyContainer, IMekanismStrictEnergyHandler, MenuProvider {

    private static final Identifier COMBINING_ID = Identifier.fromNamespaceAndPath("mekanism", "combining");

    /** Slot 0 = main item input; slot 1 = extra item input; slot 2 = item output (extract-only). */
    private static final int MAIN_INPUT_SLOT = 0;
    private static final int EXTRA_INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = {MAIN_INPUT_SLOT, EXTRA_INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};

    private static final long ENERGY_PER_TICK = 100L;
    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one operation. */
    public static final int MAX_PROGRESS = 60;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;

    public CombinerMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricRealMachines.COMBINER_BE_TYPE.get(), pos, state);
    }

    public void serverTick() {
        // Pull energy from adjacent cables/generators (pull-based model) before processing.
        if (level instanceof ServerLevel serverLevel) {
            EnergyTransferHelper.pull(serverLevel, worldPosition, energy, ENERGY_PULL_RATE, false);
        }
        boolean active = process();
        updateActiveState(active);
    }

    /** Reflects whether the machine is currently processing in the {@code active} blockstate. */
    private void updateActiveState(boolean active) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof DualItemMachineBlock && state.getValue(DualItemMachineBlock.ACTIVE) != active && level != null) {
            level.setBlock(worldPosition, state.setValue(DualItemMachineBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private RecipeType<CombinerRecipe> recipeType() {
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(COMBINING_ID);
        return (RecipeType<CombinerRecipe>) type;
    }

    /**
     * Looks up the {@code mekanism:combining} recipe type for the {@code (main, extra)} input pair. If it matches, the
     * output slot has room + there is enough energy, advances progress (consuming energy each tick) and, on completion
     * ({@link #MAX_PROGRESS} ticks), consumes one from each input slot and inserts the assembled output into the output
     * slot. Progress resets if the recipe/inputs/room is lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeType<CombinerRecipe> recipeType = recipeType();
        if (recipeType == null) {
            return false;
        }
        ItemStack main = items.get(MAIN_INPUT_SLOT);
        ItemStack extra = items.get(EXTRA_INPUT_SLOT);
        if (main.isEmpty() || extra.isEmpty()) {
            return resetProgress();
        }
        // CombinerRecipe.matches/assemble read input.getItem(0)=main, input.getItem(1)=extra, and require size()==2.
        TwoItemRecipeInput recipeInput = new TwoItemRecipeInput(main, extra);
        Optional<RecipeHolder<CombinerRecipe>> match = serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress();
        }
        CombinerRecipe recipe = match.get().value();
        ItemStack result = recipe.getOutput(main, extra).create();
        if (result.isEmpty() || main.getCount() < 1 || extra.getCount() < 1 || !canInsertOutput(result)) {
            return resetProgress();
        }
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            main.shrink(1);
            extra.shrink(1);
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

    /** Live ContainerData for the GUI: [0]=energy permille, [1]=progress permille (no chemical tank). */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getEnergyStoredPermille();
                    case 1 -> getProgressPermille();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return MachineGuiType.COMBINER.dataSize();
            }
        };
    }

    /** The extended menu provider opened from the block's use handler (main input + extra input + output + energy). */
    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), this, containerData(), MachineGuiType.COMBINER);
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

    // ---- item inventory (Container): slot 0 = main input, slot 1 = extra input, slot 2 = output ----
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

    // ---- sided item I/O: insert only into the input slots (0/1); extract only from the output slot (2) ----
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == MAIN_INPUT_SLOT || slot == EXTRA_INPUT_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return slot == MAIN_INPUT_SLOT || slot == EXTRA_INPUT_SLOT;
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
        // Main input + extra input + output + energy bar. Opened in-game via the block's use handler over the extended
        // menu path ({@link #menuProvider()}); this direct create path also backs the gui self-test.
        return menuProvider().createMenu(containerId, playerInventory, player);
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
