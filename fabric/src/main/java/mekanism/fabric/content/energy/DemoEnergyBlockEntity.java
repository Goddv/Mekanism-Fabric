package mekanism.fabric.content.energy;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.ISimpleFluidHandler;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IMekanismHeatHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a minimal functional block-entity that stores energy AND heat in the hoisted
 * {@code :common} {@link BasicEnergyContainer}/{@link BasicHeatCapacitor} and exposes both as Mekanism capabilities
 * (energy via {@link IMekanismStrictEnergyHandler}, heat via {@link IMekanismHeatHandler}). Registered against
 * {@code MekanismFabricEnergy.SIDED} and {@code MekanismFabricHeat.SIDED} so neighbours can query both — the foundation
 * pattern for real Mekanism machines on Fabric, and proof the BlockApiLookup capability shape generalizes beyond energy.
 */
public class DemoEnergyBlockEntity extends BlockEntity implements IMekanismStrictEnergyHandler, IMekanismHeatHandler, IMekanismChemicalHandler, ISimpleFluidHandler {

    private static final double HEAT_CAPACITY = 1_000.0D;
    private static final long CHEMICAL_CAPACITY = 64_000L;
    private static final long FLUID_CAPACITY = 810_000L; // 10 buckets in droplets

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(1_000_000L, this);
    private final List<IEnergyContainer> containers = List.of(energy);
    private final BasicHeatCapacitor heat = BasicHeatCapacitor.create(HEAT_CAPACITY, null, this);
    private final List<IHeatCapacitor> capacitors = List.of(heat);
    private final IChemicalTank chemicalTank = BasicChemicalTank.create(CHEMICAL_CAPACITY, this);
    private final List<IChemicalTank> chemicalTanks = List.of(chemicalTank);
    private IFluidStack fluidStored = IFluidStack.empty();

    public DemoEnergyBlockEntity(BlockPos pos, BlockState state) {
        super(FabricEnergyBlockDemo.BE_TYPE.get(), pos, state);
    }

    // ---- energy capability ----
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return containers;
    }

    // ---- heat capability ----
    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return capacitors;
    }

    // ---- chemical capability ----
    @Override
    public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
        return chemicalTanks;
    }

    // ---- fluid capability (transitional ISimpleFluidHandler over IFluidStack) ----
    @Override
    public int getFluidTanks() {
        return 1;
    }

    @Override
    public IFluidStack getFluidInTank(int tank) {
        return fluidStored;
    }

    @Override
    public IFluidStack insertFluid(int tank, IFluidStack stack, Action action) {
        if (stack.isEmpty()) {
            return IFluidStack.empty();
        }
        if (!fluidStored.isEmpty() && !IFluidStack.isSameFluidSameComponents(fluidStored, stack)) {
            return stack; // can't mix different fluids
        }
        long stored = fluidStored.isEmpty() ? 0L : fluidStored.getAmount();
        long accepted = Math.min(stack.getAmount(), FLUID_CAPACITY - stored);
        if (accepted <= 0L) {
            return stack;
        }
        if (action.execute()) {
            fluidStored = stack.copyWithAmount(stored + accepted);
            setChanged();
        }
        return accepted >= stack.getAmount() ? IFluidStack.empty() : stack.copyWithAmount(stack.getAmount() - accepted);
    }

    @Override
    public IFluidStack extractFluid(int tank, long amount, Action action) {
        if (fluidStored.isEmpty() || amount <= 0L) {
            return IFluidStack.empty();
        }
        long given = Math.min(amount, fluidStored.getAmount());
        IFluidStack extracted = fluidStored.copyWithAmount(given);
        if (action.execute()) {
            fluidStored = fluidStored.copyWithAmount(fluidStored.getAmount() - given);
            setChanged();
        }
        return extracted;
    }

    /**
     * Applies any buffered heat to stored heat. {@link BasicHeatCapacitor#handleHeat(double)} only buffers; real tiles
     * flush it from their tick. Exposed so the dev self-test can flush without a ticking block.
     */
    public void updateHeat() {
        heat.update();
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Flat serialization is safe here: energy writes "stored", BasicHeatCapacitor writes only "heatCapacity".
        energy.serialize(output);
        heat.serialize(output);
        chemicalTank.serialize(output.child("chemical"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
        heat.deserialize(input);
        input.child("chemical").ifPresent(chemicalTank::deserialize);
    }
}
