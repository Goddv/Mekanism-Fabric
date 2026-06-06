package mekanism.fabric.energy;

import mekanism.api.energy.IStrictEnergyHandler;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric analog of NeoForge's {@code Capabilities.STRICT_ENERGY}: Mekanism's own block-level energy capability,
 * exposed through Fabric API's {@link BlockApiLookup}. This is the loader's native capability mechanism — cross-mod
 * energy bridges (e.g. Team Reborn Energy) layer on top, mirroring how NeoForge keeps {@code STRICT_ENERGY} separate
 * from {@code ForgeEnergyCompat}.
 *
 * <p>The API type is the loader-neutral {@link IStrictEnergyHandler} (hoisted into {@code :common}); the context is a
 * nullable {@link Direction} (the side being queried, or {@code null} for a sideless query) — matching the NeoForge
 * {@code BlockCapability.createSided(...)} contract.
 */
public final class MekanismFabricEnergy {

    private MekanismFabricEnergy() {
    }

    /**
     * Mekanism's strict-energy capability lookup. Register providers against this for blocks/block-entities that should
     * expose an {@link IStrictEnergyHandler}, and query it to find energy handlers on blocks in the world.
     */
    public static final BlockApiLookup<IStrictEnergyHandler, @Nullable Direction> SIDED = BlockApiLookup.get(
          Identifier.fromNamespaceAndPath("mekanism", "strict_energy_handler"),
          IStrictEnergyHandler.class,
          Direction.class
    );

    /**
     * Queries the strict-energy handler exposed by the block at {@code pos} on the given {@code side}.
     *
     * @return the handler, or {@code null} if the block exposes none.
     */
    @Nullable
    public static IStrictEnergyHandler getStrictEnergyHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return SIDED.find(level, pos, side);
    }

    /**
     * Overload that avoids redundant block/block-entity fetches when the caller already has them in hand.
     *
     * @return the handler, or {@code null} if the block exposes none.
     */
    @Nullable
    public static IStrictEnergyHandler getStrictEnergyHandler(ServerLevel level, BlockPos pos, @Nullable BlockState state,
          @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        return SIDED.find(level, pos, state, blockEntity, side);
    }
}
