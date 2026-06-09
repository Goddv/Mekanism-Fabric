package mekanism.api.fluid;

import mekanism.api.Action;
import mekanism.api.annotations.NothingNullByDefault;

/**
 * Loader-neutral extended fluid handler over {@link IFluidStack}, the fluid analog of {@code IChemicalHandler}. The
 * NeoForge {@code IFluidHandler} capability surface is supplied separately by a loader bridge so this stays free of
 * NeoForge types.
 */
@NothingNullByDefault
public interface IExtendedFluidHandler {

    /**
     * Returns the number of fluid storage units ("tanks") available
     *
     * @return The number of tanks available
     */
    int getTanks();

    /**
     * Returns the {@link IFluidStack} in a given tank.
     *
     * <p>
     * <strong>IMPORTANT:</strong> This {@link IFluidStack} <em>MUST NOT</em> be modified. This method is not for altering internal contents. Any implementers who are
     * able to detect modification via this method should throw an exception. It is ENTIRELY reasonable and likely that the stack returned here will be a copy.
     * </p>
     *
     * @param tank Tank to query.
     *
     * @return {@link IFluidStack} in a given tank. Empty if the tank is empty.
     */
    IFluidStack getFluidInTank(int tank);

    /**
     * Overrides the stack in the given tank. This method may throw an error if it is called unexpectedly.
     *
     * @param tank  Tank to modify
     * @param stack {@link IFluidStack} to set tank to (may be empty).
     *
     * @throws RuntimeException if the handler is called in a way that the handler was not expecting.
     **/
    void setFluidInTank(int tank, IFluidStack stack);

    /**
     * Retrieves the maximum amount of fluid that can be stored in a given tank.
     *
     * @param tank Tank to query.
     *
     * @return The maximum fluid amount held by the tank.
     */
    int getTankCapacity(int tank);

    /**
     * <p>
     * This function should be used instead of simulated insertions in cases where the contents and state of the tank are irrelevant, mainly for the purpose of automation
     * and logic.
     * </p>
     *
     * @param tank  Tank to query.
     * @param stack Stack to test with for validity
     *
     * @return true if the tank can accept the {@link IFluidStack}, not considering the current state of the tank. false if the tank can never support the given
     * {@link IFluidStack} in any situation.
     */
    boolean isFluidValid(int tank, IFluidStack stack);

    /**
     * <p>
     * Inserts a {@link IFluidStack} into a given tank and return the remainder. The {@link IFluidStack} <em>should not</em> be modified in this function!
     * </p>
     *
     * @param tank   Tank to insert to.
     * @param stack  {@link IFluidStack} to insert. This must not be modified by the tank.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return The remaining {@link IFluidStack} that was not inserted (if the entire stack is accepted, then return an empty {@link IFluidStack}). May be the same as the
     * input {@link IFluidStack} if unchanged, otherwise a new {@link IFluidStack}. The returned {@link IFluidStack} can be safely modified after
     */
    IFluidStack insertFluid(int tank, IFluidStack stack, Action action);

    /**
     * Extracts a {@link IFluidStack} from a specific tank in this handler.
     * <p>
     * The returned value must be empty if nothing is extracted, otherwise its stack size must be less than or equal to {@code amount}.
     * </p>
     *
     * @param tank   Tank to extract from.
     * @param amount Amount to extract (may be greater than the current stack's amount or the tank's capacity)
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     */
    IFluidStack extractFluid(int tank, long amount, Action action);

    /**
     * <p>
     * Inserts a {@link IFluidStack} into this handler, distribution is left <strong>entirely</strong> to this {@link IExtendedFluidHandler}. The {@link IFluidStack}
     * <em>should not</em> be modified in this function!
     * </p>
     *
     * @param stack  {@link IFluidStack} to insert. This must not be modified by the handler.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return The remaining {@link IFluidStack} that was not inserted (if the entire stack is accepted, then return an empty {@link IFluidStack}). May be the same as the
     * input {@link IFluidStack} if unchanged, otherwise a new {@link IFluidStack}. The returned {@link IFluidStack} can be safely modified after
     *
     * @implNote The default implementation of this method, attempts to insert into tanks that contain the same type of fluid as the supplied type, and if it will not all
     * fit, falls back to inserting into any empty tanks.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the insertion.
     */
    default IFluidStack insertFluid(IFluidStack stack, Action action) {
        return ExtendedFluidHandlerUtils.insert(stack, null, action, side -> getTanks(), (tank, side) -> getFluidInTank(tank),
              (tank, fluid, side, act) -> insertFluid(tank, fluid, act));
    }

    /**
     * Extracts a {@link IFluidStack} from this handler, distribution is left <strong>entirely</strong> to this {@link IExtendedFluidHandler}.
     * <p>
     * The returned value must be empty if nothing is extracted, otherwise its stack size must be less than or equal to {@code amount}.
     * </p>
     *
     * @param amount Amount to extract (may be greater than the current stack's amount or the tank's capacity)
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     *
     * @implNote The default implementation of this method, extracts across all tanks to try and reach the desired amount to extract. Once the first fluid that can be
     * extracted is found, all future extractions will make sure to also make sure they are for the same type of fluid.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the extraction.
     */
    default IFluidStack extractFluid(long amount, Action action) {
        return ExtendedFluidHandlerUtils.extract(amount, null, action, side -> getTanks(), (tank, side) -> getFluidInTank(tank),
              (tank, amt, side, act) -> extractFluid(tank, amt, act));
    }

    /**
     * Extracts a {@link IFluidStack} from this handler, distribution is left <strong>entirely</strong> to this {@link IExtendedFluidHandler}.
     * <p>
     * The returned value must be empty if nothing is extracted, otherwise its stack size must be less than or equal to {@code amount}.
     * </p>
     *
     * @param stack  {@link IFluidStack} representing the {@link net.minecraft.world.level.material.Fluid} and maximum amount to be drained.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     *
     * @implNote The default implementation of this method, extracts across all tanks that contents match the type of fluid passed into this method.
     * @apiNote It is not guaranteed that the default implementation will be how this {@link IExtendedFluidHandler} ends up distributing the extraction.
     */
    default IFluidStack extractFluid(IFluidStack stack, Action action) {
        return ExtendedFluidHandlerUtils.extract(stack, null, action, side -> getTanks(), (tank, side) -> getFluidInTank(tank),
              (tank, amount, side, act) -> extractFluid(tank, amount, act));
    }
}
