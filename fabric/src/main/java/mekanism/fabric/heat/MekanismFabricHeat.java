package mekanism.fabric.heat;

import mekanism.api.heat.IHeatHandler;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric analog of NeoForge's {@code Capabilities.HEAT}: Mekanism's own block-level heat capability, exposed through
 * Fabric API's {@link BlockApiLookup}. The second core Mekanism capability brought to Fabric (after energy), proving the
 * {@code BlockApiLookup} capability pattern generalizes — the same shape fluid/chemical/item caps reuse.
 *
 * <p>The API type is the loader-neutral {@link IHeatHandler} (hoisted into {@code :common}); the context is a nullable
 * {@link Direction} (the side being queried, or {@code null} for a sideless query) — matching the NeoForge
 * {@code BlockCapability.createSided(...)} contract used for energy.
 */
public final class MekanismFabricHeat {

    private MekanismFabricHeat() {
    }

    /**
     * Mekanism's heat capability lookup. Register providers against this for blocks/block-entities that should expose an
     * {@link IHeatHandler}, and query it to find heat handlers on blocks in the world.
     */
    public static final BlockApiLookup<IHeatHandler, @Nullable Direction> SIDED = BlockApiLookup.get(
          Identifier.fromNamespaceAndPath("mekanism", "heat_handler"),
          IHeatHandler.class,
          Direction.class
    );

    /**
     * Queries the heat handler exposed by the block at {@code pos} on the given {@code side}.
     *
     * @return the handler, or {@code null} if the block exposes none.
     */
    @Nullable
    public static IHeatHandler getHeatHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return SIDED.find(level, pos, side);
    }

    /**
     * Overload that avoids redundant block/block-entity fetches when the caller already has them in hand.
     *
     * @return the handler, or {@code null} if the block exposes none.
     */
    @Nullable
    public static IHeatHandler getHeatHandler(ServerLevel level, BlockPos pos, @Nullable BlockState state,
          @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        return SIDED.find(level, pos, state, blockEntity, side);
    }
}
