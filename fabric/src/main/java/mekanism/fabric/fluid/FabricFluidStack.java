package mekanism.fabric.fluid;

import java.util.Objects;
import mekanism.api.fluid.IFluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.material.Fluid;

/**
 * Fabric implementation of {@link IFluidStack}, wrapping a fabric-transfer {@code FluidVariant} (fluid + components) +
 * a {@code long} amount in droplets (the amount lives outside the immutable variant, mirroring how
 * {@code ChemicalStack} stores its long amount alongside a Holder). copyWithAmount allocates only a new wrapper (the
 * variant is shared/immutable).
 *
 * <p>This is a MUTABLE class (was a record): the {@code amount} is non-final so the loader-neutral tank/util algorithms
 * can do NeoForge's zero-allocation in-place {@code grow}/{@code shrink}/{@code setAmount} (the {@link FluidVariant}
 * stays immutable). The 2-arg constructor + {@link #variant()}/{@link #amount()} accessors are preserved verbatim
 * because {@link FabricFluidStackProvider}'s codecs reference {@code FabricFluidStack::new} + {@code variant()}.
 */
public final class FabricFluidStack implements IFluidStack {

    private final FluidVariant variant;
    private long amount;

    public FabricFluidStack(FluidVariant variant, long amount) {
        this.variant = variant;
        this.amount = amount;
    }

    public FluidVariant variant() {
        return variant;
    }

    /** Raw stored amount (not empty-clamped — use {@link #getAmount()} for the sentinel-aware value). */
    public long amount() {
        return amount;
    }

    @Override
    public Holder<Fluid> typeHolder() {
        return variant.typeHolder();
    }

    @Override
    public Fluid getFluid() {
        return variant.getFluid();
    }

    @Override
    public long getAmount() {
        return isEmpty() ? 0L : amount;
    }

    @Override
    public boolean isEmpty() {
        return variant.isBlank() || amount <= 0L;
    }

    @Override
    public DataComponentPatch getComponentsPatch() {
        return variant.getComponentsPatch();
    }

    @Override
    public Component getHoverName() {
        // Server-safe plain name (no client-only FluidVariantRendering): the fluid's translation key.
        return Component.translatable(Util.makeDescriptionId("fluid", BuiltInRegistries.FLUID.getKey(variant.getFluid())));
    }

    @Override
    public boolean is(Holder<Fluid> other) {
        return variant.getFluid() == other.value();
    }

    @Override
    public boolean is(TagKey<Fluid> tag) {
        return variant.getFluid().builtInRegistryHolder().is(tag);
    }

    @Override
    public IFluidStack copy() {
        return new FabricFluidStack(variant, amount);
    }

    @Override
    public IFluidStack copyWithAmount(long newAmount) {
        return newAmount <= 0L ? IFluidStack.empty() : new FabricFluidStack(variant, newAmount);
    }

    // ---- in-place mutators (the variant stays immutable; only the long amount changes) ----

    @Override
    public void grow(long delta) {
        this.amount += delta;
    }

    @Override
    public void shrink(long delta) {
        this.amount -= delta;
    }

    @Override
    public void setAmount(long newAmount) {
        this.amount = newAmount;
    }

    @Override
    public int hashFluidAndComponents() {
        //FluidVariant hashes fluid + components (NOT amount), mirroring NeoForge FluidStack.hashFluidAndComponents.
        return variant.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof FabricFluidStack other && this.amount == other.amount && this.variant.equals(other.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, amount);
    }

    @Override
    public String toString() {
        return "FabricFluidStack[variant=" + variant + ", amount=" + amount + ']';
    }
}
