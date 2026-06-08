package mekanism.api.fluid;

import mekanism.api.Action;

/**
 * Transitional loader-neutral fluid handler used to prove the fluid capability ({@code BlockApiLookup}/NeoForge cap)
 * over {@link IFluidStack} before the full {@code mekanism.api.fluid} handler/tank API is hoisted to {@code :common}.
 * A minimal slice-1 contract (count + per-tank get/insert/extract); folds into the real {@code IExtendedFluidHandler}
 * when that is migrated.
 */
public interface ISimpleFluidHandler {

    int getFluidTanks();

    IFluidStack getFluidInTank(int tank);

    IFluidStack insertFluid(int tank, IFluidStack stack, Action action);

    IFluidStack extractFluid(int tank, long amount, Action action);
}
