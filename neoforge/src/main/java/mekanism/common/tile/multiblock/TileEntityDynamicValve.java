package mekanism.common.tile.multiblock;

import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import mekanism.api.fluid.IFluidStack;
import org.jetbrains.annotations.NotNull;

public class TileEntityDynamicValve extends TileEntityDynamicTank {

    public TileEntityDynamicValve(BlockPos pos, BlockState state) {
        super(MekanismBlocks.DYNAMIC_VALVE, pos, state);
    }

    @NotNull
    @Override
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        return side -> getMultiblock().getFluidTanks(side);
    }

    @NotNull
    @Override
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return side -> getMultiblock().getChemicalTanks(side);
    }
    
    @Override
    public boolean persists(ContainerType<?, ?, ?> type) {
        //Do not handle fluid when it comes to syncing it/saving this tile to disk
        if (type == ContainerType.FLUID || type == ContainerType.CHEMICAL) {
            return false;
        }
        return super.persists(type);
    }

    @NotNull
    @Override
    public IFluidStack insertFluid(int tank, @NotNull IFluidStack stack, Direction side, @NotNull Action action) {
        return handleValves(stack, action, super.insertFluid(tank, stack, side, action));
    }

    @NotNull
    @Override
    public IFluidStack insertFluid(@NotNull IFluidStack stack, Direction side, @NotNull Action action) {
        return handleValves(stack, action, super.insertFluid(stack, side, action));
    }

    private IFluidStack handleValves(@NotNull IFluidStack stack, @NotNull Action action, @NotNull IFluidStack remainder) {
        if (action.execute() && remainder.getAmount() < stack.getAmount()) {
            getMultiblock().triggerValveTransfer(this);
        }
        return remainder;
    }

    @Override
    public int getRedstoneLevel() {
        return getMultiblock().getCurrentRedstoneLevel();
    }
}