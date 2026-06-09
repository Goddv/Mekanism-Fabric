package mekanism.fabric.fluid;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import mekanism.api.Action;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.IFluidStackProvider;
import mekanism.api.fluid.ISimpleFluidHandler;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the fluid abstraction + capability on Fabric. Proves: (A) the {@code :common}
 * {@link IFluidStack} backed by {@code FluidVariant}+long works (construction, amount/empty/copyWithAmount,
 * isSameFluidSameComponents) and BOTH codecs round-trip (the load-bearing proof that {@code FluidVariant.CODEC}/
 * {@code PACKET_CODEC} compose into an IFluidStack codec); (B) the {@link MekanismFabricFluid#SIDED}
 * {@code BlockApiLookup<ISimpleFluidHandler>} resolves a real fluid handler from the world and stores an IFluidStack.
 * Grep for {@code [Mekanism/Fabric][fluid-selftest] RESULT:}.
 */
public final class FabricFluidSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][fluid-selftest]";

    private FabricFluidSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        try {
            // Tier A: IFluidStack basics over FluidVariant+long.
            IFluidStack water = IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), FluidConstants.BUCKET);
            boolean basicsOk = !water.isEmpty()
                  && water.getAmount() == FluidConstants.BUCKET
                  && water.getFluid() == Fluids.WATER
                  && water.copyWithAmount(0).isEmpty()
                  && IFluidStack.isSameFluidSameComponents(water, water.copy())
                  && IFluidStack.empty().isEmpty();

            // Tier A: M1 in-place mutators + matches/hashFluidAndComponents (the new surface the fluid migration needs).
            IFluidStack mut = IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), FluidConstants.BUCKET);
            mut.grow(FluidConstants.BUCKET);          // 2 buckets
            mut.shrink(FluidConstants.BUCKET / 2);    // 1.5 buckets
            mut.setAmount(FluidConstants.BUCKET);     // back to 1 bucket
            boolean mutOk = mut.getAmount() == FluidConstants.BUCKET
                  && IFluidStack.matches(mut, water)
                  && mut.hashFluidAndComponents() == water.hashFluidAndComponents()
                  && !IFluidStack.matches(water, water.copyWithAmount(FluidConstants.BUCKET * 2));

            // Tier A: codec round-trips (the hard part — FluidVariant.CODEC / PACKET_CODEC composed with a long).
            Codec<IFluidStack> codec = IFluidStackProvider.INSTANCE.codec();
            Tag encoded = codec.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), water).getOrThrow();
            IFluidStack decoded = codec.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), encoded).getOrThrow();
            boolean codecOk = decoded.getFluid() == Fluids.WATER && decoded.getAmount() == FluidConstants.BUCKET;

            StreamCodec<RegistryFriendlyByteBuf, IFluidStack> streamCodec = IFluidStackProvider.INSTANCE.streamCodec();
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
            streamCodec.encode(buf, water);
            IFluidStack streamDecoded = streamCodec.decode(buf);
            boolean streamOk = streamDecoded.getFluid() == Fluids.WATER && streamDecoded.getAmount() == FluidConstants.BUCKET;

            // Tier B: fluid capability end-to-end via the BlockApiLookup.
            boolean capabilityOk = false;
            BlockPos pos = new BlockPos(0, 64, 20);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricEnergyBlockDemo.BLOCK.get().defaultBlockState(), 3);
            ISimpleFluidHandler handler = MekanismFabricFluid.getFluidHandler(level, pos, null);
            if (handler != null && handler.getFluidTanks() > 0) {
                IFluidStack leftover = handler.insertFluid(0, IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), 3 * FluidConstants.BUCKET), Action.EXECUTE);
                IFluidStack stored = handler.getFluidInTank(0);
                capabilityOk = leftover.isEmpty() && stored.getFluid() == Fluids.WATER && stored.getAmount() == 3 * FluidConstants.BUCKET;
            }
            level.removeBlock(pos, false);

            ok = basicsOk && mutOk && codecOk && streamOk && capabilityOk;
            LOGGER.info("{} {} fluid abstraction+capability: basics={} mutators={} codec={} streamCodec={} capability={}",
                  TAG, ok ? "OK  " : "FAIL", basicsOk, mutOk, codecOk, streamOk, capabilityOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL fluid test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
