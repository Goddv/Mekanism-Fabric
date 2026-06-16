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
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.ISimpleFluidHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.fabric.content.machine.gui.MachineGuiType;
import mekanism.fabric.content.machine.gui.MekanismMenuProvider;
import mekanism.fabric.content.power.EnergyTransferHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block-entity for the Electrolytic Separator — the first FLUID-input machine on
 * Fabric (fluid input &rarr; TWO chemical outputs). Stores energy + a fluid INPUT tank ({@link BasicFluidTank}, 10
 * buckets) + a LEFT and a RIGHT chemical OUTPUT tank ({@link BasicChemicalTank} each). Each server tick it looks up a
 * matching {@link FabricSeparatingRecipe} (a Fabric-only in-code recipe — see {@link FabricSeparatingRecipes}; the real
 * {@code BasicElectrolysisRecipe} stays on the {@code FluidStackIngredient} hoist wall) for the input tank's fluid; when
 * a recipe matches and there is enough energy, enough input fluid, and room in BOTH output tanks, it advances progress.
 * On completion ({@link #MAX_PROGRESS} ticks) it drains the recipe's fluid amount from the input tank, consumes energy,
 * and inserts the LEFT/RIGHT chemical outputs into the two output tanks.
 *
 * <p>Capabilities: the fluid INPUT tank via {@link mekanism.fabric.fluid.MekanismFabricFluid#SIDED} (external insert
 * allowed, extract blocked — it is an input); the two chemical OUTPUT tanks via
 * {@link mekanism.fabric.chemical.MekanismFabricChemical#SIDED} (external extract allowed, insert blocked — they are
 * outputs); energy via {@link mekanism.fabric.energy.MekanismFabricEnergy#SIDED} (sink). It is a {@link WorldlyContainer}
 * with no item slots. {@code useWithoutItem} opens the GUI ({@link MachineGuiType#FLUID_TO_CHEMICAL}: a fluid input bar +
 * two chemical output bars + energy bar).
 */
public class ElectrolyticSeparatorBlockEntity extends BlockEntity implements WorldlyContainer, ISimpleFluidHandler,
      IChemicalHandler, IMekanismStrictEnergyHandler, MenuProvider {

    private static final int[] NO_SLOTS = new int[0];

    private static final long ENERGY_PULL_RATE = 5_000L;
    /** Ticks to complete one electrolysis operation. */
    public static final int MAX_PROGRESS = 60;
    /** Fluid input tank capacity: 10 buckets (droplets — the Fabric fluid unit). */
    private static final int FLUID_CAPACITY = (int) (10L * FluidConstants.BUCKET);
    /** Each chemical output tank capacity. */
    private static final long CHEMICAL_CAPACITY = 10_000L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(2_000_000L, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);
    private final IExtendedFluidTank inputTank = BasicFluidTank.create(FLUID_CAPACITY, this::setChanged);
    private final IChemicalTank leftTank = BasicChemicalTank.create(CHEMICAL_CAPACITY, (IContentsListener) this);
    private final IChemicalTank rightTank = BasicChemicalTank.create(CHEMICAL_CAPACITY, (IContentsListener) this);
    private int progress;

    public ElectrolyticSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricElectrolyticSeparator.BE_TYPE.get(), pos, state);
    }

    public void serverTick() {
        // Pull energy from adjacent cables/generators (pull-based model) before processing.
        if (level instanceof ServerLevel serverLevel) {
            EnergyTransferHelper.pull(serverLevel, worldPosition, energy, ENERGY_PULL_RATE, false);
        }
        process();
    }

    /**
     * Looks up a matching {@link FabricSeparatingRecipe} for the fluid in the input tank. If it matches and there is room
     * in BOTH output tanks + enough energy, advances progress (consuming energy each tick); on completion
     * ({@link #MAX_PROGRESS} ticks) drains the recipe's fluid amount and inserts the left/right chemical outputs. Progress
     * resets if the recipe/fluid/room is lost, but is held when merely out of energy.
     */
    private boolean process() {
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        IFluidStack fluid = inputTank.getFluid();
        Optional<FabricSeparatingRecipe> match = FabricSeparatingRecipes.find(fluid);
        if (match.isEmpty()) {
            return resetProgress();
        }
        FabricSeparatingRecipe recipe = match.get();
        ChemicalStack left = recipe.leftOutput();
        ChemicalStack right = recipe.rightOutput();
        // Room check: BOTH output tanks must accept their full output this operation.
        if (!leftTank.insert(left, Action.SIMULATE, AutomationType.INTERNAL).isEmpty()
              || !rightTank.insert(right, Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return resetProgress();
        }
        long energyPerTick = recipe.energyPerTick();
        if (energy.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL) < energyPerTick) {
            return false; // out of energy: hold progress, but not active
        }
        energy.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
        progress++;
        if (progress >= MAX_PROGRESS) {
            progress = 0;
            inputTank.extract(recipe.inputAmount(), Action.EXECUTE, AutomationType.INTERNAL);
            leftTank.insert(left, Action.EXECUTE, AutomationType.INTERNAL);
            rightTank.insert(right, Action.EXECUTE, AutomationType.INTERNAL);
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

    // ---- GUI permille readers ----

    public int getEnergyStoredPermille() {
        long max = energy.getMaxEnergy();
        return max <= 0L ? 0 : (int) (energy.getEnergy() * 1000L / max);
    }

    public int getProgressPermille() {
        return progress * 1000 / MAX_PROGRESS;
    }

    private int fluidPermille() {
        int cap = inputTank.getCapacity();
        return cap <= 0 ? 0 : (int) Math.min(1000L, inputTank.getFluidAmount() * 1000L / cap);
    }

    private int chemicalPermille(IChemicalTank tank) {
        long cap = tank.getCapacity();
        return cap <= 0L ? 0 : (int) (tank.getStored() * 1000L / cap);
    }

    // ---- direct tank access for the self-test ----

    public IExtendedFluidTank getInputTank() {
        return inputTank;
    }

    public IChemicalTank getLeftTank() {
        return leftTank;
    }

    public IChemicalTank getRightTank() {
        return rightTank;
    }

    /** Live ContainerData: [0]=energy, [1]=progress, [2]=fluid input, [3]=left chemical, [4]=right chemical (permille). */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getEnergyStoredPermille();
                    case 1 -> getProgressPermille();
                    case 2 -> fluidPermille();
                    case 3 -> chemicalPermille(leftTank);
                    case 4 -> chemicalPermille(rightTank);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return MachineGuiType.FLUID_TO_CHEMICAL.dataSize();
            }
        };
    }

    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), this, containerData(), MachineGuiType.FLUID_TO_CHEMICAL);
    }

    // ---- fluid capability (input tank; insert-only externally) ----
    @Override
    public int getFluidTanks() {
        return 1;
    }

    @Override
    public IFluidStack getFluidInTank(int tank) {
        return tank == 0 ? inputTank.getFluid() : IFluidStack.empty();
    }

    @Override
    public IFluidStack insertFluid(int tank, IFluidStack stack, Action action) {
        return tank == 0 ? inputTank.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public IFluidStack extractFluid(int tank, long amount, Action action) {
        // Input tank: disallow external extraction (only this BE's processing drains it, internally).
        return IFluidStack.empty();
    }

    // ---- chemical capability (two output tanks; extract-only externally) ----
    @Override
    public int getChemicalTanks() {
        return 2;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        return switch (tank) {
            case 0 -> leftTank.getStack();
            case 1 -> rightTank.getStack();
            default -> ChemicalStack.EMPTY;
        };
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        switch (tank) {
            case 0 -> leftTank.setStack(stack);
            case 1 -> rightTank.setStack(stack);
            default -> {
            }
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return switch (tank) {
            case 0 -> leftTank.getCapacity();
            case 1 -> rightTank.getCapacity();
            default -> 0L;
        };
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        return switch (tank) {
            case 0 -> leftTank.isValid(stack);
            case 1 -> rightTank.isValid(stack);
            default -> false;
        };
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        // Output tanks: disallow external insertion (only this BE's processing inserts, internally).
        return stack;
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action) {
        return stack;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return switch (tank) {
            case 0 -> leftTank.extract(amount, action, AutomationType.EXTERNAL);
            case 1 -> rightTank.extract(amount, action, AutomationType.EXTERNAL);
            default -> ChemicalStack.EMPTY;
        };
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action) {
        // Single-arg path: drain from the left tank first, then the right (for the bulk-extract convenience method).
        ChemicalStack drained = leftTank.extract(amount, action, AutomationType.EXTERNAL);
        if (drained.isEmpty()) {
            drained = rightTank.extract(amount, action, AutomationType.EXTERNAL);
        }
        return drained;
    }

    // ---- energy capability (sink) ----
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    // A machine is a SINK: reject energy extraction via the capability so cables can't drain it (its own processing
    // extracts internally on the container directly, bypassing these cap methods).
    @Override
    public long extractEnergy(int container, long amount, Action action) {
        return 0L;
    }

    @Override
    public long extractEnergy(long amount, Action action) {
        return 0L;
    }

    // ---- item inventory: no item slots (fluid in / chemical out only) ----
    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return false;
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
        return menuProvider().createMenu(containerId, playerInventory, player);
    }

    // ---- persistence ----
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
        output.putInt("progress", progress);
        inputTank.serialize(output.child("inputTank"));
        leftTank.serialize(output.child("leftTank"));
        rightTank.serialize(output.child("rightTank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        progress = input.getInt("progress").orElse(0);
        input.child("inputTank").ifPresent(inputTank::deserialize);
        input.child("leftTank").ifPresent(leftTank::deserialize);
        input.child("rightTank").ifPresent(rightTank::deserialize);
    }
}
