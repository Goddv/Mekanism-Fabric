package mekanism.api.fluid;

import mekanism.api.Action;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * NeoForge-specific conversions between Mekanism's loader-neutral {@link Action} and NeoForge's
 * {@link FluidAction}. Kept out of {@link Action} (which lives in :common) so the core action type
 * stays loader-neutral; only the fluid API needs these.
 */
public final class FluidActions {

    private FluidActions() {
    }

    public static Action from(FluidAction action) {
        return action == FluidAction.EXECUTE ? Action.EXECUTE : Action.SIMULATE;
    }

    public static FluidAction to(Action action) {
        return action.execute() ? FluidAction.EXECUTE : FluidAction.SIMULATE;
    }
}
