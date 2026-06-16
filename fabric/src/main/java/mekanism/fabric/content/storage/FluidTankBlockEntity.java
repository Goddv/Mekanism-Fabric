package mekanism.fabric.content.storage;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.ISimpleFluidHandler;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.fabric.content.machine.gui.MachineGuiType;
import mekanism.fabric.content.machine.gui.MekanismMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Basic Fluid Tank ({@code mekanism:basic_fluid_tank}): large fluid STORAGE. A single {@link BasicFluidTank} (14
 * buckets for basic tier) exposed as the fluid capability ({@link ISimpleFluidHandler} via
 * {@link mekanism.fabric.fluid.MekanismFabricFluid#SIDED}) so pipes/machines/the self-test can fill and drain it.
 *
 * <p>It opens the generic {@link mekanism.fabric.content.machine.gui.MekanismMachineMenu} with the
 * {@link MachineGuiType#FLUID_TANK} shape — one fluid-tank bar showing the fill level (no item slots, no recipe arrow).
 * As a {@link MenuProvider} it is also an (empty) {@link Container}: the transitional tank has NO item slots yet (bucket
 * fill/drain via item slots is deferred — see the report), so the container is size-zero; it fills purely via pipes or
 * direct capability insertion.
 */
public class FluidTankBlockEntity extends BlockEntity implements ISimpleFluidHandler, Container {

    /** Basic-tier capacity: 14 buckets (droplets — the Fabric fluid unit, as in {@code FabricFluidSelfTest}). */
    private static final int CAPACITY = (int) (14L * FluidConstants.BUCKET);

    private final IExtendedFluidTank tank = BasicFluidTank.create(CAPACITY, this::setChanged);

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FabricStorage.BASIC_FLUID_TANK_BE_TYPE.get(), pos, state);
    }

    // ---- ISimpleFluidHandler: expose the tank as the fluid capability ----

    @Override
    public int getFluidTanks() {
        return 1;
    }

    @Override
    public IFluidStack getFluidInTank(int tankIndex) {
        return tankIndex == 0 ? tank.getFluid() : IFluidStack.empty();
    }

    @Override
    public IFluidStack insertFluid(int tankIndex, IFluidStack stack, Action action) {
        return tankIndex == 0 ? tank.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public IFluidStack extractFluid(int tankIndex, long amount, Action action) {
        return tankIndex == 0 ? tank.extract(amount, action, AutomationType.EXTERNAL) : IFluidStack.empty();
    }

    /** Direct tank access for the self-test. */
    public IExtendedFluidTank getTank() {
        return tank;
    }

    // ---- GUI ----

    /** Live ContainerData for the GUI: [0]=energy permille (always 0), [1]=progress permille (0), [2]=tank fill permille. */
    public ContainerData containerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == 2 ? tankPermille() : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return MachineGuiType.FLUID_TANK.dataSize();
            }
        };
    }

    private int tankPermille() {
        int capacity = tank.getCapacity();
        return capacity <= 0 ? 0 : (int) Math.min(1000L, tank.getFluidAmount() * 1000L / capacity);
    }

    public MekanismMenuProvider menuProvider() {
        return new MekanismMenuProvider(getDisplayName(), this, containerData(), MachineGuiType.FLUID_TANK);
    }

    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- Container: transitional empty inventory (no item slots yet; tank fills via pipes / the cap) ----

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

    // ---- persistence ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("tank").ifPresent(tank::deserialize);
    }
}
