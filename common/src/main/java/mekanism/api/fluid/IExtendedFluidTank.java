package mekanism.api.fluid;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.SerializationConstants;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import mekanism.api.IValueIOSerializable;

/**
 * Loader-neutral fluid tank contract over {@link IFluidStack} (the fluid analog of {@code IChemicalTank}). The NeoForge
 * {@code IFluidTank}/{@code IFluidHandler} capability surface is supplied separately by a loader bridge so this stays
 * free of NeoForge types.
 */
@NothingNullByDefault
public interface IExtendedFluidTank extends IValueIOSerializable, IContentsListener {

    /**
     * Returns the {@link IFluidStack} in this tank.
     *
     * <p>
     * <strong>IMPORTANT:</strong> This {@link IFluidStack} <em>MUST NOT</em> be modified. This method is not for altering internal contents. Any implementers who are
     * able to detect modification via this method should throw an exception. It is ENTIRELY reasonable and likely that the stack returned here will be a copy.
     * </p>
     *
     * <p>
     * <strong><em>SERIOUSLY: DO NOT MODIFY THE RETURNED FLUID STACK</em></strong>
     * </p>
     *
     * @return {@link IFluidStack} in this tank. Empty instance of the {@link IFluidStack} if the tank is empty.
     */
    IFluidStack getFluid();

    /**
     * Overrides the stack in this {@link IExtendedFluidTank}.
     *
     * @param stack {@link IFluidStack} to set this tanks' contents to (may be empty).
     *
     * @throws RuntimeException if this tank is called in a way that it was not expecting.
     * @implNote If the internal stack does get updated make sure to call {@link #onContentsChanged()}
     */
    void setStack(IFluidStack stack);

    /**
     * Overrides the stack in this {@link IExtendedFluidTank}.
     *
     * @param stack {@link IFluidStack} to set this tank's contents to (may be empty).
     *
     * @apiNote Unsafe version of {@link #setStack(IFluidStack)}. This method is exposed for implementation and code deduplication reasons only and should
     * <strong>NOT</strong> be directly called outside your own {@link IExtendedFluidTank} where you already know the given {@link IFluidStack} is valid, or on the
     * client side for purposes of receiving sync data and rendering.
     * @implNote If the internal stack does get updated make sure to call {@link #onContentsChanged()}
     */
    void setStackUnchecked(IFluidStack stack);

    /**
     * <p>
     * Inserts a {@link IFluidStack} into this {@link IExtendedFluidTank} and return the remainder. The {@link IFluidStack} <em>should not</em> be modified in this
     * function!
     * </p>
     *
     * @param stack          {@link IFluidStack} to insert. This must not be modified by the tank.
     * @param action         The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param automationType The method that this tank is being interacted from.
     *
     * @return The remaining {@link IFluidStack} that was not inserted (if the entire stack is accepted, then return an empty {@link IFluidStack}). May be the same as the
     * input {@link IFluidStack} if unchanged, otherwise a new {@link IFluidStack}. The returned {@link IFluidStack} can be safely modified after
     *
     * @implNote The {@link IFluidStack} <em>should not</em> be modified in this function! If the internal stack does get updated make sure to call
     * {@link #onContentsChanged()}. It is also recommended to override this if your internal {@link IFluidStack} is mutable so that a copy does not have to be made every
     * run
     */
    default IFluidStack insert(IFluidStack stack, Action action, AutomationType automationType) {
        if (stack.isEmpty() || !isFluidValid(stack)) {
            //"Fail quick" if the given stack is empty, or we can never insert the item or currently are unable to insert it
            return stack;
        }
        long needed = getNeeded();
        if (needed <= 0) {
            //Fail if we are a full tank
            return stack;
        }
        boolean sameType = false;
        if (isEmpty() || (sameType = IFluidStack.isSameFluidSameComponents(stack, getFluid()))) {
            long toAdd = Math.min(stack.getAmount(), needed);
            if (action.execute()) {
                //If we want to actually insert the fluid, then update the current fluid
                if (sameType) {
                    //We can just grow our stack by the amount we want to increase it
                    // Note: this also will mark that the contents changed
                    growStack(toAdd, action);
                } else {
                    //If we are not the same type then we have to copy the stack and set it
                    // Note: this also will mark that the contents changed
                    setStack(stack.copyWithAmount(toAdd));
                }
            }
            return stack.copyWithAmount(stack.getAmount() - toAdd);
        }
        //If we didn't accept this fluid, then just return the given stack
        return stack;
    }

    /**
     * Extracts a {@link IFluidStack} from this {@link IExtendedFluidTank}.
     * <p>
     * The returned value must be empty if nothing is extracted, otherwise its stack size must be less than or equal to {@code amount}.
     * </p>
     *
     * @param amount         Amount to extract (may be greater than the current stack's max limit)
     * @param action         The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     * @param automationType The method that this tank is being interacted from.
     *
     * @return {@link IFluidStack} extracted from the tank, must be empty if nothing can be extracted. The returned {@link IFluidStack} can be safely modified after, so the
     * tank should return a new or copied stack.
     *
     * @implNote The returned {@link IFluidStack} can be safely modified after, so a new or copied stack should be returned. If the internal stack does get updated make
     * sure to call {@link #onContentsChanged()}. It is also recommended to override this if your internal {@link IFluidStack} is mutable so that a copy does not have to
     * be made every run
     */
    default IFluidStack extract(long amount, Action action, AutomationType automationType) {
        if (isEmpty() || amount < 1) {
            return IFluidStack.empty();
        }
        IFluidStack ret = getFluid().copyWithAmount(Math.min(getFluidAmount(), amount));
        if (!ret.isEmpty() && action.execute()) {
            // Note: this also will mark that the contents changed
            shrinkStack(ret.getAmount(), action);
        }
        return ret;
    }

    /**
     * Retrieves the maximum stack size allowed to exist in this {@link IExtendedFluidTank}.
     *
     * @return The maximum stack size allowed in this {@link IExtendedFluidTank}.
     */
    int getCapacity();

    /**
     * <p>
     * This function should be used instead of simulated insertions in cases where the contents and state of the tank are irrelevant, mainly for the purpose of automation
     * and logic.
     * </p>
     *
     * @param stack Stack to test with for validity
     *
     * @return true if this {@link IExtendedFluidTank} can accept the {@link IFluidStack}, not considering the current state of the tank. false if this
     * {@link IExtendedFluidTank} can never insert the {@link IFluidStack} in any situation.
     */
    boolean isFluidValid(IFluidStack stack);

    /**
     * Convenience method for modifying the size of the stored stack.
     * <p>
     * If there is a stack stored in this tank, set the size of it to the given amount. Capping at this fluid tank's limit. If the amount is less than or equal to zero,
     * then this instead sets the stack to the empty stack.
     *
     * @param amount The desired size to set the stack to.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return Actual size the stack was set to.
     *
     * @implNote It is recommended to override this if your internal {@link IFluidStack} is mutable so that a copy does not have to be made every run. If the internal
     * stack does get updated make sure to call {@link #onContentsChanged()}
     */
    default long setStackSize(long amount, Action action) {
        if (isEmpty()) {
            return 0;
        } else if (amount <= 0) {
            if (action.execute()) {
                setEmpty();
            }
            return 0;
        }
        long maxStackSize = getCapacity();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        if (getFluidAmount() == amount || action.simulate()) {
            //If our size is not changing, or we are only simulating the change, don't do anything
            return amount;
        }
        setStack(getFluid().copyWithAmount(amount));
        return amount;
    }

    /**
     * Convenience method for growing the size of the stored stack.
     * <p>
     * If there is a stack stored in this tank, increase its size by the given amount. Capping at this fluid tank's limit. If the stack shrinks to an amount of less than
     * or equal to zero, then this instead sets the stack to the empty stack.
     *
     * @param amount The desired amount to grow the stack by.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return Actual amount the stack grew.
     *
     * @apiNote Negative values for amount are valid, and will instead cause the stack to shrink.
     * @implNote If the internal stack does get updated make sure to call {@link #onContentsChanged()}
     */
    default long growStack(long amount, Action action) {
        long current = getFluidAmount();
        if (current == 0) {
            //"Fail quick" if our stack is empty, so we can't grow it
            return 0;
        } else if (amount > 0) {
            //Cap adding amount at how much we need, so that we don't risk overflow
            amount = Math.min(amount, getNeeded());
        }
        long newSize = setStackSize(current + amount, action);
        return newSize - current;
    }

    /**
     * Convenience method for shrinking the size of the stored stack.
     * <p>
     * If there is a stack stored in this tank, shrink its size by the given amount. If this causes its size to become less than or equal to zero, then the stack is set
     * to the empty stack. If this method is used to grow the stack the size gets capped at this fluid tank's limit.
     *
     * @param amount The desired amount to shrink the stack by.
     * @param action The action to perform, either {@link Action#EXECUTE} or {@link Action#SIMULATE}
     *
     * @return Actual amount the stack shrunk.
     *
     * @apiNote Negative values for amount are valid, and will instead cause the stack to grow.
     * @implNote If the internal stack does get updated make sure to call {@link #onContentsChanged()}
     */
    default long shrinkStack(long amount, Action action) {
        return -growStack(-amount, action);
    }

    /**
     * Convenience method for checking if this tank is empty.
     *
     * @return True if the tank is empty, false otherwise.
     *
     * @implNote If your implementation of {@link #getFluid()} returns a copy, this should be overridden to directly check against the internal stack.
     */
    default boolean isEmpty() {
        return getFluid().isEmpty();
    }

    /**
     * Convenience method for emptying this {@link IExtendedFluidTank}.
     */
    default void setEmpty() {
        setStack(IFluidStack.empty());
    }

    /**
     * Convenience method for checking if this tank's contents are of an equal type to a given fluid stack's.
     *
     * @param other The stack to compare to.
     *
     * @return True if the tank's contents are equal, false otherwise.
     *
     * @implNote If your implementation of {@link #getFluid()} returns a copy, this should be overridden to directly check against the internal stack.
     */
    default boolean isFluidEqual(IFluidStack other) {
        return IFluidStack.isSameFluidSameComponents(getFluid(), other);
    }

    /**
     * Gets the amount of fluid needed by this {@link IExtendedFluidTank} to reach a filled state.
     *
     * @return Amount of fluid needed
     */
    default long getNeeded() {
        return Math.max(0, getCapacity() - getFluidAmount());
    }

    /**
     * Convenience method for checking the amount of fluid in this tank.
     *
     * @return The size of the stored stack, or zero if the stack is empty.
     *
     * @implNote If your implementation of {@link #getFluid()} returns a copy, this should be overridden to directly check against the internal stack.
     */
    default long getFluidAmount() {
        return getFluid().getAmount();
    }

    @Override
    default void serialize(ValueOutput output) {
        if (!isEmpty()) {
            output.store(SerializationConstants.STORED, IFluidStackProvider.INSTANCE.codec(), getFluid());
        }
    }

    @Override
    default void deserialize(ValueInput input) {
        setStackUnchecked(input.read(SerializationConstants.STORED, IFluidStackProvider.INSTANCE.codec()).orElse(IFluidStack.empty()));
    }
}
