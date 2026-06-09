package mekanism.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * API-source-set bridge between NeoForge's {@link FluidStack} and the loader-neutral {@link IFluidStack}, for the recipe
 * API layer (which is NeoForge-only and FluidStack-typed but now talks to {@link IFluidStack}-typed tank contracts). The
 * main source set has its own richer {@code NeoFluidStack.wrap/unwrap}; this exists only because {@code src/api} cannot
 * see {@code src/main}.
 */
public final class NeoFluidStackBridge {

    private NeoFluidStackBridge() {
    }

    /** Wraps a NeoForge {@link FluidStack} as a loader-neutral {@link IFluidStack}. */
    public static IFluidStack wrap(FluidStack stack) {
        if (stack.isEmpty()) {
            return IFluidStack.empty();
        }
        return IFluidStack.of(stack.typeHolder(), stack.amount(), stack.getComponentsPatch());
    }

    /** Unwraps a loader-neutral {@link IFluidStack} to a NeoForge {@link FluidStack}. */
    public static FluidStack unwrap(IFluidStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(stack.typeHolder(), (int) Math.min(stack.getAmount(), Integer.MAX_VALUE), stack.getComponentsPatch());
    }
}
