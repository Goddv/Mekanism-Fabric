package mekanism.api.fluid;

import mekanism.api.Action;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A sided variant of {@link IExtendedFluidHandler}
 */
@NothingNullByDefault
public interface ISidedFluidHandler extends IExtendedFluidHandler {

    /**
     * The side this {@link ISidedFluidHandler} is for. This defaults to null, which is for internal use.
     *
     * @return The default side to use for the normal {@link IExtendedFluidHandler} methods when wrapping them into {@link ISidedFluidHandler} methods.
     */
    @Nullable
    default Direction getFluidSideFor() {
        return null;
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#getTanks()}, docs copied for convenience.
     * <p>
     * Returns the number of fluid storage units ("tanks") available
     *
     * @param side The side we are interacting with the handler from (null for internal).
     *
     * @return The number of tanks available
     */
    int getTanks(@Nullable Direction side);

    @Override
    default int getTanks() {
        return getTanks(getFluidSideFor());
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#getFluidInTank(int)}, docs copied for convenience.
     * <p>
     * Returns the {@link IFluidStack} in a given tank.
     *
     * @param tank Tank to query.
     * @param side The side we are interacting with the handler from (null for internal).
     *
     * @return {@link IFluidStack} in a given tank. Empty if the tank is empty.
     */
    IFluidStack getFluidInTank(int tank, @Nullable Direction side);

    @Override
    default IFluidStack getFluidInTank(int tank) {
        return getFluidInTank(tank, getFluidSideFor());
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#setFluidInTank(int, IFluidStack)}, docs copied for convenience.
     * <p>
     * Overrides the stack in the given tank. This method may throw an error if it is called unexpectedly.
     *
     * @param tank  Tank to modify
     * @param stack {@link IFluidStack} to set tank to (may be empty).
     * @param side  The side we are interacting with the handler from (null for internal).
     *
     * @throws RuntimeException if the handler is called in a way that the handler was not expecting.
     **/
    void setFluidInTank(int tank, IFluidStack stack, @Nullable Direction side);

    @Override
    default void setFluidInTank(int tank, IFluidStack stack) {
        setFluidInTank(tank, stack, getFluidSideFor());
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#getTankCapacity(int)}, docs copied for convenience.
     * <p>
     * Retrieves the maximum amount of fluid that can be stored in a given tank.
     *
     * @param tank Tank to query.
     * @param side The side we are interacting with the handler from (null for internal).
     *
     * @return The maximum fluid amount held by the tank.
     */
    int getTankCapacity(int tank, @Nullable Direction side);

    @Override
    default int getTankCapacity(int tank) {
        return getTankCapacity(tank, getFluidSideFor());
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#isFluidValid(int, IFluidStack)}, docs copied for convenience.
     *
     * @param tank  Tank to query.
     * @param stack Stack to test with for validity
     * @param side  The side we are interacting with the handler from (null for internal).
     *
     * @return true if the tank can accept the {@link IFluidStack}, not considering the current state of the tank. false if the tank can never support the given
     * {@link IFluidStack} in any situation.
     */
    boolean isFluidValid(int tank, IFluidStack stack, @Nullable Direction side);

    @Override
    default boolean isFluidValid(int tank, IFluidStack stack) {
        return isFluidValid(tank, stack, getFluidSideFor());
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#insertFluid(int, IFluidStack, Action)}, docs copied for convenience.
     *
     * @param tank   Tank to insert to.
     * @param stack  {@link IFluidStack} to insert. This must not be modified by the tank.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param side   The side we are interacting with the handler from (null for internal).
     *
     * @return The remaining {@link IFluidStack} that was not inserted (if the entire stack is accepted, then return an empty {@link IFluidStack}). May be the same as the
     * input {@link IFluidStack} if unchanged, otherwise a new {@link IFluidStack}. The returned {@link IFluidStack} can be safely modified after
     */
    IFluidStack insertFluid(int tank, IFluidStack stack, @Nullable Direction side, Action action);

    @Override
    default IFluidStack insertFluid(int tank, IFluidStack stack, Action action) {
        return insertFluid(tank, stack, getFluidSideFor(), action);
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#extractFluid(int, long, Action)}, docs copied for convenience.
     *
     * @param tank   Tank to extract from.
     * @param amount Amount to extract (may be greater than the current stack's amount or the tank's capacity)
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param side   The side we are interacting with the handler from (null for internal).
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     */
    IFluidStack extractFluid(int tank, long amount, @Nullable Direction side, Action action);

    @Override
    default IFluidStack extractFluid(int tank, long amount, Action action) {
        return extractFluid(tank, amount, getFluidSideFor(), action);
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#insertFluid(IFluidStack, Action)}, docs copied for convenience.
     *
     * @param stack  {@link IFluidStack} to insert. This must not be modified by the handler.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param side   The side we are interacting with the handler from (null for internal).
     *
     * @return The remaining {@link IFluidStack} that was not inserted (if the entire stack is accepted, then return an empty {@link IFluidStack}). May be the same as the
     * input {@link IFluidStack} if unchanged, otherwise a new {@link IFluidStack}. The returned {@link IFluidStack} can be safely modified after
     *
     * @implNote The default implementation of this method, attempts to insert into tanks that contain the same type of fluid as the supplied type, and if it will not all
     * fit, falls back to inserting into any empty tanks.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the insertion.
     */
    default IFluidStack insertFluid(IFluidStack stack, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.insert(stack, side, action, this::getTanks, this::getFluidInTank, this::insertFluid);
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#extractFluid(long, Action)}, docs copied for convenience.
     *
     * @param amount Amount to extract (may be greater than the current stack's amount or the tank's capacity)
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param side   The side we are interacting with the handler from (null for internal).
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     *
     * @implNote The default implementation of this method, extracts across all tanks to try and reach the desired amount to extract. Once the first fluid that can be
     * extracted is found, all future extractions will make sure to also make sure they are for the same type of fluid.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the extraction.
     */
    default IFluidStack extractFluid(long amount, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.extract(amount, side, action, this::getTanks, this::getFluidInTank, this::extractFluid);
    }

    /**
     * A sided variant of {@link IExtendedFluidHandler#extractFluid(IFluidStack, Action)}, docs copied for convenience.
     *
     * @param stack  {@link IFluidStack} representing the {@link net.minecraft.world.level.material.Fluid} and maximum amount to be drained.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param side   The side we are interacting with the handler from (null for internal).
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     *
     * @implNote The default implementation of this method, extracts across all tanks that contents match the type of fluid passed into this method.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the extraction.
     */
    default IFluidStack extractFluid(IFluidStack stack, @Nullable Direction side, Action action) {
        return ExtendedFluidHandlerUtils.extract(stack, side, action, this::getTanks, this::getFluidInTank, this::extractFluid);
    }
}
