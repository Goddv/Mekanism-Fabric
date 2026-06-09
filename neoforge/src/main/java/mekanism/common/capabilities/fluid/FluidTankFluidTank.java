package mekanism.common.capabilities.fluid;

import java.util.Objects;
import java.util.function.IntSupplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.fluid.NeoFluidStack;
import mekanism.common.tier.FluidTankTier;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.util.WorldUtils;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class FluidTankFluidTank extends BasicFluidTank {

    public static FluidTankFluidTank create(TileEntityFluidTank tile, @Nullable IContentsListener listener) {
        Objects.requireNonNull(tile, "Fluid tank tile entity cannot be null");
        return new FluidTankFluidTank(tile, listener);
    }

    private final TileEntityFluidTank tile;
    private final boolean isCreative;
    private final IntSupplier rate;

    private FluidTankFluidTank(TileEntityFluidTank tile, @Nullable IContentsListener listener) {
        super(tile.tier.getStorage(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(), listener);
        this.tile = tile;
        rate = tile.tier::getOutput;
        isCreative = tile.tier == FluidTankTier.CREATIVE;
    }

    @Override
    protected int getInsertRate(@Nullable AutomationType automationType) {
        //Only limit the internal rate to change the speed at which this can be filled from an item
        return automationType == AutomationType.INTERNAL ? rate.getAsInt() : super.getInsertRate(automationType);
    }

    @Override
    protected int getExtractRate(@Nullable AutomationType automationType) {
        //Only limit the internal rate to change the speed at which this can be filled from an item
        return automationType == AutomationType.INTERNAL ? rate.getAsInt() : super.getExtractRate(automationType);
    }

    @Override
    public IFluidStack insert(IFluidStack stack, Action action, AutomationType automationType) {
        IFluidStack remainder;
        if (isCreative && isEmpty() && action.execute() && automationType != AutomationType.EXTERNAL) {
            //If a player manually inserts into a creative tank (or internally, via a FluidInventorySlot), that is empty we need to allow setting the type,
            // Note: We check that it is not external insertion because an empty creative tanks acts as a "void" for automation
            remainder = super.insert(stack, Action.SIMULATE, automationType);
            if (remainder.isEmpty()) {
                //If we are able to insert it then set perform the action of setting it to full
                setStackUnchecked(stack.copyWithAmount(getCapacity()));
            }
        } else {
            remainder = super.insert(stack, action.combine(!isCreative), automationType);
        }
        //Ensure we have the same type of fluid stored as we failed to insert, in which case we want to try to insert to the one above
        if (!remainder.isEmpty() && FluidStack.isSameFluidSameComponents(stored, NeoFluidStack.unwrap(remainder))) {
            //If we have any leftover check if we can send it to the tank that is above
            TileEntityFluidTank tileAbove = WorldUtils.getTileEntity(TileEntityFluidTank.class, this.tile.getLevel(), this.tile.getBlockPos().above());
            if (tileAbove != null) {
                //Note: We do external so that it is not limited by the internal rate limits
                remainder = tileAbove.fluidTank.insert(remainder, action, AutomationType.EXTERNAL);
            }
        }
        return remainder;
    }

    @Override
    public long growStack(long amount, Action action) {
        long grownAmount = super.growStack(amount, action);
        if (amount > 0 && grownAmount < amount) {
            //If we grew our stack less than we tried to, and we were actually growing and not shrinking it
            // try inserting into above tiles
            if (!tile.getActive()) {
                TileEntityFluidTank tileAbove = WorldUtils.getTileEntity(TileEntityFluidTank.class, this.tile.getLevel(), this.tile.getBlockPos().above());
                if (tileAbove != null) {
                    long leftOverToInsert = amount - grownAmount;
                    //Note: We do external so that it is not limited by the internal rate limits
                    IFluidStack remainder = tileAbove.fluidTank.insert(NeoFluidStack.wrap(stored.copyWithAmount((int) leftOverToInsert)), action, AutomationType.EXTERNAL);
                    grownAmount += leftOverToInsert - remainder.getAmount();
                }
            }
        }
        return grownAmount;
    }

    @Override
    public IFluidStack extract(long amount, Action action, AutomationType automationType) {
        return super.extract(amount, action.combine(!isCreative), automationType);
    }

    /**
     * {@inheritDoc}
     *
     * Note: We are only patching {@link #setStackSize(long, Action)}, as both {@link #growStack(long, Action)} and {@link #shrinkStack(long, Action)} are wrapped through
     * this method.
     */
    @Override
    public long setStackSize(long amount, Action action) {
        return super.setStackSize(amount, action.combine(!isCreative));
    }
}