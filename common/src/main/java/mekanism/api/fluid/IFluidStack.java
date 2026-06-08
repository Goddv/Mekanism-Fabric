package mekanism.api.fluid;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Loader-neutral fluid-stack abstraction (fluid type + amount + components) — the fluid analog of the Mekanism-owned
 * {@code ChemicalStack}. Needed because the stack type itself differs per loader: NeoForge has its own
 * {@code FluidStack} (Fluid + int mB + DataComponentPatch), while Fabric represents fluids as
 * {@code FluidVariant} (Fluid + components) + a separate {@code long} amount (droplets). Both speak vanilla
 * {@link DataComponentPatch}, so the components model is shared. Per-loader implementations (NeoForge wrapping
 * FluidStack, Fabric wrapping FluidVariant+long) are created via {@link IFluidStackProvider}.
 *
 * <p>Amounts are {@code long} to unify NeoForge's int-mB with Fabric's long-droplets; each loader keeps its own units
 * (no scaling here — unit conversion happens only at the fabric-transfer Storage bridge, a future slice).
 */
public interface IFluidStack {

    Holder<Fluid> typeHolder();

    Fluid getFluid();

    long getAmount();

    boolean isEmpty();

    DataComponentPatch getComponentsPatch();

    Component getHoverName();

    boolean is(Holder<Fluid> other);

    boolean is(TagKey<Fluid> tag);

    IFluidStack copy();

    IFluidStack copyWithAmount(long amount);

    // ---- statics routed through the per-loader provider so :common stays loader-free ----

    static IFluidStack empty() {
        return IFluidStackProvider.INSTANCE.empty();
    }

    static IFluidStack of(Holder<Fluid> fluid, long amount, DataComponentPatch components) {
        return IFluidStackProvider.INSTANCE.of(fluid, amount, components);
    }

    static IFluidStack of(Holder<Fluid> fluid, long amount) {
        return IFluidStackProvider.INSTANCE.of(fluid, amount);
    }

    static boolean isSameFluidSameComponents(IFluidStack a, IFluidStack b) {
        return IFluidStackProvider.INSTANCE.isSameFluidSameComponents(a, b);
    }
}
