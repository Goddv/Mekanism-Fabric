package mekanism.common.capabilities.proxy;

import mekanism.api.Action;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.fluid.FluidActions;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.ISidedFluidHandler;
import mekanism.common.capabilities.holder.IHolder;
import mekanism.common.fluid.NeoFluidStack;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge fluid capability bridge: exposes the NeoForge {@link IFluidHandler} surface (used by {@code Capabilities.FLUID}
 * for cross-mod interop) while delegating to Mekanism's loader-neutral {@link ISidedFluidHandler}. The
 * {@link IFluidStack}&harr;{@link FluidStack} conversion happens here via {@link NeoFluidStack#wrap}/{@link NeoFluidStack#unwrap}.
 */
@NothingNullByDefault
public class ProxyFluidHandler extends ProxyHandler implements IFluidHandler {

    private final ISidedFluidHandler fluidHandler;

    public ProxyFluidHandler(ISidedFluidHandler fluidHandler, @Nullable Direction side, @Nullable IHolder holder) {
        super(side, holder);
        this.fluidHandler = fluidHandler;
    }

    public ISidedFluidHandler getInternalHandler() {
        return fluidHandler;
    }

    @Override
    public int getTanks() {
        return fluidHandler.getTanks(side);
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return NeoFluidStack.unwrap(fluidHandler.getFluidInTank(tank, side));
    }

    public void setFluidInTank(int tank, FluidStack stack) {
        if (!readOnly) {
            fluidHandler.setFluidInTank(tank, NeoFluidStack.wrap(stack), side);
        }
    }

    @Override
    public int getTankCapacity(int tank) {
        return fluidHandler.getTankCapacity(tank, side);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return !readOnly || fluidHandler.isFluidValid(tank, NeoFluidStack.wrap(stack), side);
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        if (readOnlyInsert()) {
            return 0;
        }
        IFluidStack toInsert = NeoFluidStack.wrap(stack);
        IFluidStack remainder = fluidHandler.insertFluid(toInsert, side, FluidActions.from(action));
        return (int) Math.min(toInsert.getAmount() - remainder.getAmount(), Integer.MAX_VALUE);
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        if (readOnlyExtract()) {
            return FluidStack.EMPTY;
        }
        return NeoFluidStack.unwrap(fluidHandler.extractFluid(NeoFluidStack.wrap(stack), side, FluidActions.from(action)));
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        if (readOnlyExtract()) {
            return FluidStack.EMPTY;
        }
        return NeoFluidStack.unwrap(fluidHandler.extractFluid(amount, side, FluidActions.from(action)));
    }
}
