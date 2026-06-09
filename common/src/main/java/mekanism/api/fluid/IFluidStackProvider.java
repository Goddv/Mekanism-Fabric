package mekanism.api.fluid;

import com.mojang.serialization.Codec;
import mekanism.api.MekanismAPIBase;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific factory + codecs for {@link IFluidStack}. The stack type wraps a NeoForge {@code FluidStack} or a
 * Fabric {@code FluidVariant}+long, neither referenceable from {@code :common}; this service hides that. Resolved via
 * {@link MekanismAPIBase#getService}. Codecs are loader-provided (NeoForge adapts FluidStack's codecs; Fabric composes
 * {@code FluidVariant.CODEC}/{@code PACKET_CODEC} with a long amount).
 */
@Internal
public interface IFluidStackProvider {

    IFluidStackProvider INSTANCE = MekanismAPIBase.getService(IFluidStackProvider.class);

    IFluidStack empty();

    IFluidStack of(Holder<Fluid> fluid, long amount, DataComponentPatch components);

    IFluidStack of(Holder<Fluid> fluid, long amount);

    boolean isSameFluidSameComponents(IFluidStack a, IFluidStack b);

    /** Exact match incl. amount (mirrors NeoForge {@code FluidStack.matches}). */
    boolean matches(IFluidStack a, IFluidStack b);

    Codec<IFluidStack> codec();

    Codec<IFluidStack> optionalCodec();

    /** Optional codec that falls back to empty (logging) on a deserialization error — replaces FluidCodecHelper. */
    Codec<IFluidStack> lenientOptionalCodec();

    StreamCodec<RegistryFriendlyByteBuf, IFluidStack> streamCodec();

    Codec<IFluidStack> fixedAmountCodec(long amount);
}
