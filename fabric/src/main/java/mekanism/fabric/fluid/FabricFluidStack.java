package mekanism.fabric.fluid;

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
 */
public record FabricFluidStack(FluidVariant variant, long amount) implements IFluidStack {

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
}
