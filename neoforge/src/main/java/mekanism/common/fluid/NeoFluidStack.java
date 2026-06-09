package mekanism.common.fluid;

import mekanism.api.fluid.IFluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge implementation of the loader-neutral {@link IFluidStack}, wrapping NeoForge's {@code FluidStack}. Amounts stay
 * in mB (the {@code long} is just the carrier type; no droplet scaling). copyWithAmount clamps the long to int.
 */
public record NeoFluidStack(FluidStack delegate) implements IFluidStack {

    @Override
    public Holder<Fluid> typeHolder() {
        return delegate.typeHolder();
    }

    @Override
    public Fluid getFluid() {
        return delegate.getFluid();
    }

    @Override
    public long getAmount() {
        return delegate.getAmount();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public DataComponentPatch getComponentsPatch() {
        return delegate.getComponentsPatch();
    }

    @Override
    public Component getHoverName() {
        return delegate.getHoverName();
    }

    @Override
    public boolean is(Holder<Fluid> other) {
        return delegate.getFluid() == other.value();
    }

    @Override
    public boolean is(TagKey<Fluid> tag) {
        return delegate.getFluid().builtInRegistryHolder().is(tag);
    }

    @Override
    public IFluidStack copy() {
        return new NeoFluidStack(delegate.copy());
    }

    @Override
    public IFluidStack copyWithAmount(long amount) {
        return new NeoFluidStack(delegate.copyWithAmount((int) Math.min(amount, Integer.MAX_VALUE)));
    }

    @Override
    public void grow(long amount) {
        delegate.grow((int) Math.min(amount, Integer.MAX_VALUE));
    }

    @Override
    public void shrink(long amount) {
        delegate.shrink((int) Math.min(amount, Integer.MAX_VALUE));
    }

    @Override
    public void setAmount(long amount) {
        delegate.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
    }

    @Override
    public int hashFluidAndComponents() {
        return FluidStack.hashFluidAndComponents(delegate);
    }

    // ---- IFluidStack <-> FluidStack bridge (used at every NeoForge capability/AttachedFluids/render boundary) ----

    /** Unwraps a loader-neutral {@link IFluidStack} to the NeoForge {@code FluidStack} it wraps. */
    public static FluidStack unwrap(IFluidStack stack) {
        return stack instanceof NeoFluidStack neo ? neo.delegate : FluidStack.EMPTY;
    }

    /** Wraps a NeoForge {@code FluidStack} as a loader-neutral {@link IFluidStack}. */
    public static IFluidStack wrap(FluidStack stack) {
        return new NeoFluidStack(stack);
    }
}
