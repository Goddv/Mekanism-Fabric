package mekanism.common.fluid;

import com.mojang.serialization.Codec;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.IFluidStackProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge {@link IFluidStackProvider}: builds {@link NeoFluidStack}s + adapts NeoForge's {@code FluidStack} codecs to
 * {@link IFluidStack} via xmap. Registered via META-INF/services.
 */
public class NeoFluidStackProvider implements IFluidStackProvider {

    private static FluidStack unwrap(IFluidStack stack) {
        return ((NeoFluidStack) stack).delegate();
    }

    @Override
    public IFluidStack empty() {
        return new NeoFluidStack(FluidStack.EMPTY);
    }

    @Override
    public IFluidStack of(Holder<Fluid> fluid, long amount, DataComponentPatch components) {
        return new NeoFluidStack(new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE), components));
    }

    @Override
    public IFluidStack of(Holder<Fluid> fluid, long amount) {
        return new NeoFluidStack(new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE)));
    }

    @Override
    public boolean isSameFluidSameComponents(IFluidStack a, IFluidStack b) {
        return FluidStack.isSameFluidSameComponents(unwrap(a), unwrap(b));
    }

    @Override
    public Codec<IFluidStack> codec() {
        return FluidStack.CODEC.xmap(NeoFluidStack::new, NeoFluidStackProvider::unwrap);
    }

    @Override
    public Codec<IFluidStack> optionalCodec() {
        return FluidStack.OPTIONAL_CODEC.xmap(NeoFluidStack::new, NeoFluidStackProvider::unwrap);
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, IFluidStack> streamCodec() {
        return FluidStack.STREAM_CODEC.map(NeoFluidStack::new, NeoFluidStackProvider::unwrap);
    }

    @Override
    public Codec<IFluidStack> fixedAmountCodec(long amount) {
        return FluidStack.fixedAmountCodec((int) Math.min(amount, Integer.MAX_VALUE)).xmap(NeoFluidStack::new, NeoFluidStackProvider::unwrap);
    }
}
