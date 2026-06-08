package mekanism.fabric.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.IFluidStackProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/**
 * Fabric {@link IFluidStackProvider}: builds {@link FabricFluidStack}s (FluidVariant + long) + composes codecs from
 * {@code FluidVariant.CODEC}/{@code PACKET_CODEC} with a long amount. Registered via META-INF/services.
 */
public class FabricFluidStackProvider implements IFluidStackProvider {

    private static FluidVariant variantOf(IFluidStack stack) {
        return ((FabricFluidStack) stack).variant();
    }

    private static final Codec<IFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          FluidVariant.CODEC.fieldOf("fluid").forGetter(FabricFluidStackProvider::variantOf),
          Codec.LONG.fieldOf("amount").forGetter(IFluidStack::getAmount)
    ).apply(instance, FabricFluidStack::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, IFluidStack> STREAM_CODEC = StreamCodec.composite(
          FluidVariant.PACKET_CODEC, FabricFluidStackProvider::variantOf,
          ByteBufCodecs.VAR_LONG, IFluidStack::getAmount,
          FabricFluidStack::new
    );

    @Override
    public IFluidStack empty() {
        return new FabricFluidStack(FluidVariant.blank(), 0L);
    }

    @Override
    public IFluidStack of(Holder<Fluid> fluid, long amount, DataComponentPatch components) {
        return new FabricFluidStack(FluidVariant.of(fluid.value(), components), amount);
    }

    @Override
    public IFluidStack of(Holder<Fluid> fluid, long amount) {
        return new FabricFluidStack(FluidVariant.of(fluid.value()), amount);
    }

    @Override
    public boolean isSameFluidSameComponents(IFluidStack a, IFluidStack b) {
        return a.getFluid() == b.getFluid() && a.getComponentsPatch().equals(b.getComponentsPatch());
    }

    @Override
    public Codec<IFluidStack> codec() {
        return CODEC;
    }

    @Override
    public Codec<IFluidStack> optionalCodec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, IFluidStack> streamCodec() {
        return STREAM_CODEC;
    }

    @Override
    public Codec<IFluidStack> fixedAmountCodec(long amount) {
        return FluidVariant.CODEC.xmap(variant -> new FabricFluidStack(variant, amount), FabricFluidStackProvider::variantOf);
    }
}
