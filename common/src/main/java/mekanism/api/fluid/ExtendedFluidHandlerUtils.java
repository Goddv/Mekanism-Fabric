package mekanism.api.fluid;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.container.ContainerInteraction;
import mekanism.api.container.InContainerGetter;
import mekanism.api.container.LongContainerInteraction;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ExtendedFluidHandlerUtils {

    private ExtendedFluidHandlerUtils() {
    }

    /**
     * Util method for a generic insert implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack insert(IFluidStack stack, @Nullable Direction side, Action action, ToIntFunction<@Nullable Direction> tankCount,
          InContainerGetter<IFluidStack> inTankGetter, ContainerInteraction<IFluidStack> insertFluid) {
        if (stack.isEmpty()) {
            //Short circuit if nothing is actually being inserted
            return IFluidStack.empty();
        }
        int tanks = tankCount.applyAsInt(side);
        if (tanks == 0) {
            return stack;
        } else if (tanks == 1) {
            return insertFluid.interact(0, stack, side, action);
        }
        IFluidStack toInsert = stack;
        //Start by trying to insert into the tanks that have the same type
        IntList emptyTanks = new IntArrayList();
        for (int tank = 0; tank < tanks; tank++) {
            IFluidStack inTank = inTankGetter.getStored(tank, side);
            if (inTank.isEmpty()) {
                emptyTanks.add(tank);
            } else if (IFluidStack.isSameFluidSameComponents(inTank, stack)) {
                IFluidStack remainder = insertFluid.interact(tank, toInsert, side, action);
                if (remainder.isEmpty()) {
                    //If we have no remaining fluid, return that we fit it all
                    return IFluidStack.empty();
                }
                //Update what we have left to insert, to be the amount we were unable to insert
                toInsert = remainder;
            }
        }
        for (int tank : emptyTanks) {
            IFluidStack remainder = insertFluid.interact(tank, toInsert, side, action);
            if (remainder.isEmpty()) {
                //If we have no remaining fluid, return that we fit it all
                return IFluidStack.empty();
            }
            //Update what we have left to insert, to be the amount we were unable to insert
            toInsert = remainder;
        }
        return toInsert;
    }

    /**
     * Util method for a generic insert implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack insert(IFluidStack stack, @Nullable Direction side, Function<@Nullable Direction, List<IExtendedFluidTank>> fluidTankSupplier,
          Action action, AutomationType automationType) {
        if (stack.isEmpty()) {
            //Short circuit if nothing is actually being inserted
            return IFluidStack.empty();
        }
        List<IExtendedFluidTank> fluidTanks = fluidTankSupplier.apply(side);
        return insert(stack, action, automationType, fluidTanks.size(), fluidTanks);
    }

    /**
     * Util method for a generic insert implementation for various handlers. Mainly for internal use only
     *
     * @since 10.6.0
     */
    public static IFluidStack insert(IFluidStack stack, Action action, AutomationType automationType, int size, List<IExtendedFluidTank> fluidTanks) {
        if (stack.isEmpty()) {
            //Short circuit if nothing is actually being inserted
            return IFluidStack.empty();
        } else if (size == 0) {
            return stack;
        } else if (size == 1) {
            //noinspection SequencedCollectionMethodCanBeUsed: we know size
            return fluidTanks.get(0).insert(stack, action, automationType);
        }
        IFluidStack toInsert = stack;
        //Start by trying to insert into the tanks that have the same type
        List<IExtendedFluidTank> emptyTanks = new ArrayList<>();
        for (IExtendedFluidTank tank : fluidTanks) {
            if (tank.isEmpty()) {
                emptyTanks.add(tank);
            } else if (tank.isFluidEqual(stack)) {
                IFluidStack remainder = tank.insert(toInsert, action, automationType);
                if (remainder.isEmpty()) {
                    //If we have no remaining fluid, return that we fit it all
                    return IFluidStack.empty();
                }
                //Update what we have left to insert, to be the amount we were unable to insert
                toInsert = remainder;
            }
        }
        for (IExtendedFluidTank tank : emptyTanks) {
            IFluidStack remainder = tank.insert(toInsert, action, automationType);
            if (remainder.isEmpty()) {
                //If we have no remaining fluid, return that we fit it all
                return IFluidStack.empty();
            }
            //Update what we have left to insert, to be the amount we were unable to insert
            toInsert = remainder;
        }
        return toInsert;
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack extract(long amount, @Nullable Direction side, Action action, ToIntFunction<@Nullable Direction> tankCount,
          InContainerGetter<IFluidStack> inTankGetter, LongContainerInteraction<IFluidStack> extractFluid) {
        if (amount == 0) {
            return IFluidStack.empty();
        }
        int tanks = tankCount.applyAsInt(side);
        if (tanks == 0) {
            return IFluidStack.empty();
        } else if (tanks == 1) {
            return extractFluid.interact(0, amount, side, action);
        }
        IFluidStack extracted = IFluidStack.empty();
        long toDrain = amount;
        for (int tank = 0; tank < tanks; tank++) {
            if (extracted.isEmpty() || IFluidStack.isSameFluidSameComponents(extracted, inTankGetter.getStored(tank, side))) {
                //If there is fluid in the tank that matches the type we have started draining, or we haven't found a type yet
                IFluidStack drained = extractFluid.interact(tank, toDrain, side, action);
                if (!drained.isEmpty()) {
                    //If we were able to drain something, set it as the type we have extracted/increase how much we have extracted
                    if (extracted.isEmpty()) {
                        extracted = drained;
                    } else {
                        extracted.grow(drained.getAmount());
                    }
                    toDrain -= drained.getAmount();
                    if (toDrain == 0) {
                        //If we are done draining break and return the amount extracted
                        break;
                    }
                    //Otherwise, keep looking and attempt to drain more from the handler, making sure that it is of
                    // the same type as we have found
                }
            }
        }
        return extracted;
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack extract(long amount, @Nullable Direction side, Function<@Nullable Direction, List<IExtendedFluidTank>> fluidTankSupplier,
          Action action, AutomationType automationType) {
        if (amount == 0) {
            return IFluidStack.empty();
        }
        List<IExtendedFluidTank> fluidTanks = fluidTankSupplier.apply(side);
        return extract(amount, action, automationType, fluidTanks.size(), fluidTanks);
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.6.0
     */
    public static IFluidStack extract(long amount, Action action, AutomationType automationType, int size, List<IExtendedFluidTank> fluidTanks) {
        if (amount == 0 || size == 0) {
            return IFluidStack.empty();
        } else if (size == 1) {
            //noinspection SequencedCollectionMethodCanBeUsed: we know size
            return fluidTanks.get(0).extract(amount, action, automationType);
        }
        IFluidStack extracted = IFluidStack.empty();
        long toDrain = amount;
        for (IExtendedFluidTank fluidTank : fluidTanks) {
            if (extracted.isEmpty() || fluidTank.isFluidEqual(extracted)) {
                //If there is fluid in the tank that matches the type we have started draining, or we haven't found a type yet
                IFluidStack drained = fluidTank.extract(toDrain, action, automationType);
                if (!drained.isEmpty()) {
                    //If we were able to drain something, set it as the type we have extracted/increase how much we have extracted
                    if (extracted.isEmpty()) {
                        extracted = drained;
                    } else {
                        extracted.grow(drained.getAmount());
                    }
                    toDrain -= drained.getAmount();
                    if (toDrain == 0) {
                        //If we are done draining break and return the amount extracted
                        break;
                    }
                    //Otherwise, keep looking and attempt to drain more from the handler, making sure that it is of
                    // the same type as we have found
                }
            }
        }
        return extracted;
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack extract(IFluidStack stack, @Nullable Direction side, Action action, ToIntFunction<@Nullable Direction> tankCount,
          InContainerGetter<IFluidStack> inTankGetter, LongContainerInteraction<IFluidStack> extractFluid) {
        if (stack.isEmpty()) {
            return IFluidStack.empty();
        }
        int tanks = tankCount.applyAsInt(side);
        if (tanks == 0) {
            return IFluidStack.empty();
        } else if (tanks == 1) {
            IFluidStack inTank = inTankGetter.getStored(0, side);
            if (inTank.isEmpty() || !IFluidStack.isSameFluidSameComponents(inTank, stack)) {
                return IFluidStack.empty();
            }
            return extractFluid.interact(0, stack.getAmount(), side, action);
        }
        IFluidStack extracted = IFluidStack.empty();
        long toDrain = stack.getAmount();
        for (int tank = 0; tank < tanks; tank++) {
            if (IFluidStack.isSameFluidSameComponents(stack, inTankGetter.getStored(tank, side))) {
                //If there is fluid in the tank that matches the type we are trying to drain, try to drain from it
                IFluidStack drained = extractFluid.interact(tank, toDrain, side, action);
                if (!drained.isEmpty()) {
                    //If we were able to drain something, set it as the type we have extracted/increase how much we have extracted
                    if (extracted.isEmpty()) {
                        extracted = drained;
                    } else {
                        extracted.grow(drained.getAmount());
                    }
                    toDrain -= drained.getAmount();
                    if (toDrain == 0) {
                        //If we are done draining break and return the amount extracted
                        break;
                    }
                    //Otherwise, keep looking and attempt to drain more from the handler
                }
            }
        }
        return extracted;
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.5.13
     */
    public static IFluidStack extract(IFluidStack stack, @Nullable Direction side, Function<@Nullable Direction, List<IExtendedFluidTank>> fluidTankSupplier,
          Action action, AutomationType automationType) {
        if (stack.isEmpty()) {
            return IFluidStack.empty();
        }
        List<IExtendedFluidTank> fluidTanks = fluidTankSupplier.apply(side);
        return extract(stack, action, automationType, fluidTanks.size(), fluidTanks);
    }

    /**
     * Util method for a generic extraction implementation for various handlers. Mainly for internal use only
     *
     * @since 10.6.0
     */
    public static IFluidStack extract(IFluidStack stack, Action action, AutomationType automationType, int size, Iterable<IExtendedFluidTank> fluidTanks) {
        if (stack.isEmpty() || size == 0) {
            return IFluidStack.empty();
        } else if (size == 1) {
            IExtendedFluidTank tank = fluidTanks.iterator().next();
            if (tank.isEmpty() || !tank.isFluidEqual(stack)) {
                return IFluidStack.empty();
            }
            return tank.extract(stack.getAmount(), action, automationType);
        }
        IFluidStack extracted = IFluidStack.empty();
        long toDrain = stack.getAmount();
        for (IExtendedFluidTank fluidTank : fluidTanks) {
            if (fluidTank.isFluidEqual(stack)) {
                //If there is fluid in the tank that matches the type we are trying to drain, try to drain from it
                IFluidStack drained = fluidTank.extract(toDrain, action, automationType);
                if (!drained.isEmpty()) {
                    //If we were able to drain something, set it as the type we have extracted/increase how much we have extracted
                    if (extracted.isEmpty()) {
                        extracted = drained;
                    } else {
                        extracted.grow(drained.getAmount());
                    }
                    toDrain -= drained.getAmount();
                    if (toDrain == 0) {
                        //If we are done draining break and return the amount extracted
                        break;
                    }
                    //Otherwise, keep looking and attempt to drain more from the handler
                }
            }
        }
        return extracted;
    }
}
