package mekanism.fabric.content.machine;

import java.util.List;
import java.util.Optional;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
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
import net.minecraft.world.item.ItemStackTemplate;
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
 * Transitional Fabric bring-up: the block-entity for the Precision Sawmill ({@code item -> item + chance secondary}).
 * Stores energy + a single item INPUT slot + a MAIN item output slot + a SECONDARY (chance based) item output slot, and
 * each server tick looks up its {@link SawmillRecipe} (recipe-type id {@code mekanism:sawing}) for the input via the
 * vanilla recipe manager. When it matches, there is energy, and the output slots have room, it advances progress; on
 * completion ({@link #MAX_PROGRESS} ticks) it consumes one input + energy, ALWAYS emits the recipe's main output (when
 * present) into the main output slot, and rolls the recipe's secondary chance — emitting the secondary output into the
 * secondary slot when the roll succeeds.
 *
 * <p>Capabilities: energy (machine is an energy sink), and item I/O (the input slot is fillable, both output slots
 * extract-only). {@code createMenu} returns null (no GUI yet).
 *
 * <p>Note: the secondary chance is rolled here via {@code level.getRandom().nextDouble() < recipe.getSecondaryChance()}
 * rather than via {@link ChanceOutput#getSecondaryOutput()} (whose own RNG is keyed to a precomputed roll), keeping the
 * transitional bring-up self-contained. The full chance-output semantics arrive with the machine-framework migration.
 */
public class SawmillMachineBlockEntity extends BlockEntity implements WorldlyContainer, IMekanismStrictEnergyHandler, MenuProvider {

    private static final Identifier SAWING_ID = Identifier.fromNamespaceAndPath("mekanism", "sawing");

    /** Slot 0 = item input (fillable); slot 1 = main output (extract-only); slot 2 = secondary output (extract-only). */
    private static final int INPUT_SLOT = 0;
    private static final int MAIN_OUTPUT_SLOT = 1;
    private static final int SECONDARY_OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {MAIN_OUTPUT_SLOT, SECONDARY_OUTPUT_SLOT};

    private static final long ENERGY_PER_TICK = 100L;
    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one operation. */
    public static final int MAX_PROGRESS = 60;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;

    public SawmillMachineBlockEntity(BlockPos pos, BlockState state) {
        super(FabricRealMachines.SAWMILL_BE_TYPE.get(), pos, state);
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
    private RecipeType<SawmillRecipe> recipeType() {
        RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(SAWING_ID);
        return (RecipeType<SawmillRecipe>) type;
    }

    /**
     * Looks up the {@code mekanism:sawing} recipe type for the input. If it matches, the output slots have room + there is
     * enough energy, advances progress (consuming energy each tick) and, on completion ({@link #MAX_PROGRESS} ticks),
     * consumes one input, emits the main output (when present) and rolls the secondary chance. Progress resets if the
     * recipe/input/room is lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        RecipeType<SawmillRecipe> recipeType = recipeType();
        if (recipeType == null) {
            return false;
        }
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return resetProgress();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<SawmillRecipe>> match = serverLevel.recipeAccess().getRecipeFor(recipeType, recipeInput, serverLevel);
        if (match.isEmpty()) {
            return resetProgress();
        }
        SawmillRecipe recipe = match.get().value();
        ChanceOutput output = recipe.getOutput(input);
        ItemStackTemplate mainTemplate = output.getMainOutput();
        ItemStackTemplate maxSecondaryTemplate = output.getMaxSecondaryOutput();
        ItemStack mainOutput = mainTemplate == null ? ItemStack.EMPTY : mainTemplate.create();
        ItemStack maxSecondary = maxSecondaryTemplate == null ? ItemStack.EMPTY : maxSecondaryTemplate.create();
        if (mainOutput.isEmpty() && maxSecondary.isEmpty()) {
            return resetProgress();
        }
        // Room check: the main slot must fit the main output, and the secondary slot must fit the MAXIMUM secondary
        // output (so a successful chance roll never overflows / drops items).
        if (!mainOutput.isEmpty() && !canInsert(MAIN_OUTPUT_SLOT, mainOutput)) {
            return resetProgress();
        }
        if (!maxSecondary.isEmpty() && !canInsert(SECONDARY_OUTPUT_SLOT, maxSecondary)) {
            return resetProgress();
        }
        if (energy.extract(ENERGY_PER_TICK, Action.SIMULATE, AutomationType.INTERNAL) < ENERGY_PER_TICK) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(ENERGY_PER_TICK, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            input.shrink(1);
            if (!mainOutput.isEmpty()) {
                insert(MAIN_OUTPUT_SLOT, mainOutput);
            }
            // Roll the secondary chance: emit the secondary output when the roll succeeds.
            double chance = recipe.getSecondaryChance();
            if (!maxSecondary.isEmpty() && chance > 0 && serverLevel.getRandom().nextDouble() < chance) {
                insert(SECONDARY_OUTPUT_SLOT, maxSecondary);
            }
        }
        setChanged();
        return true;
    }

    /** Whether the given output slot can fully accept the result (empty, or same item with headroom). */
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
                return MachineGuiType.SAWMILL.dataSize();
            }
        };
    }

    /** The extended menu provider opened from the block's use handler (input + main output + secondary output + energy). */
    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), this, containerData(), MachineGuiType.SAWMILL);
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

    // ---- item inventory (Container): slot 0 = input, slot 1 = main output, slot 2 = secondary output ----
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

    // ---- sided item I/O: insert only into the input slot (0); extract only from the output slots (1/2) ----
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
        return slot == MAIN_OUTPUT_SLOT || slot == SECONDARY_OUTPUT_SLOT;
    }

    // ---- menu (GUI) ----
    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Input + main output + secondary output + energy bar. Opened in-game via the block's use handler over the
        // extended menu path ({@link #menuProvider()}); this direct create path also backs the gui self-test.
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
