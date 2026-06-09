package mekanism.common.capabilities.fluid;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import mekanism.api.Action;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.common.capabilities.DynamicHandler;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class DynamicFluidHandler extends DynamicHandler<IExtendedFluidTank> implements IMekanismFluidHandler {

    public DynamicFluidHandler(Function<Direction, List<IExtendedFluidTank>> tankSupplier, Predicate<@Nullable Direction> canExtract,
          Predicate<@Nullable Direction> canInsert, @Nullable IContentsListener listener) {
        super(tankSupplier, canExtract, canInsert, listener);
    }

    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return containerSupplier.apply(side);
    }

    @Override
    public IFluidStack insertFluid(int tank, IFluidStack stack, @Nullable Direction side, Action action) {
        //If we can insert into the specific side, try to. Otherwise exit
        return canInsert.test(side) ? IMekanismFluidHandler.super.insertFluid(tank, stack, side, action) : stack;
    }

    @Override
    public IFluidStack extractFluid(int tank, long amount, @Nullable Direction side, Action action) {
        //If we can extract from a specific side, try to. Otherwise exit
        return canExtract.test(side) ? IMekanismFluidHandler.super.extractFluid(tank, amount, side, action) : IFluidStack.empty();
    }

    @Override
    public IFluidStack insertFluid(IFluidStack stack, @Nullable Direction side, Action action) {
        //If we can insert into the specific side, try to. Otherwise exit
        return canInsert.test(side) ? IMekanismFluidHandler.super.insertFluid(stack, side, action) : stack;
    }

    @Override
    public IFluidStack extractFluid(long amount, @Nullable Direction side, Action action) {
        //If we can extract from a specific side, try to. Otherwise exit
        return canExtract.test(side) ? IMekanismFluidHandler.super.extractFluid(amount, side, action) : IFluidStack.empty();
    }

    @Override
    public IFluidStack extractFluid(IFluidStack stack, @Nullable Direction side, Action action) {
        //If we can extract from a specific side, try to. Otherwise exit
        return canExtract.test(side) ? IMekanismFluidHandler.super.extractFluid(stack, side, action) : IFluidStack.empty();
    }
}
