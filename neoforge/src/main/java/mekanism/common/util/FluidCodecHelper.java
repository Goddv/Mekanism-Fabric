package mekanism.common.util;

import com.mojang.serialization.Codec;
import mekanism.api.MekanismAPI;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge-only fluid codecs split out of {@link mekanism.api.SerializerHelper} (now loader-neutral in {@code :common})
 * because they reference the NeoForge {@code FluidStack} type. Lands back in a shared location when the fluid system is
 * ported (Phase 5).
 */
public final class FluidCodecHelper {

    private FluidCodecHelper() {
    }

    /**
     * Helper codec to deserialize an optional fluid stack and fall back to the empty stack if an error is encountered in
     * deserialization.
     */
    public static final Codec<FluidStack> LENIENT_OPTIONAL_FLUID_CODEC = FluidStack.OPTIONAL_CODEC
          .promotePartial(error -> MekanismAPI.logger.error("Tried to load invalid fluid: '{}'", error))
          .orElse(FluidStack.EMPTY);
}
