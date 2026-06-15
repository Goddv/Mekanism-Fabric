package mekanism.fabric.content.generator;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transitional Fabric bring-up: the Wind Generator. Mirrors the NeoForge {@code TileEntityWindGenerator} shape: each
 * tick, if the sky is visible 4 blocks above (the windmill hub), it inserts a height-scaled rate into its energy
 * reservoir — higher placement produces more. No inventory.
 *
 * <p>The NeoForge tile interpolates between {@code windGenerationMin} (at {@code windGenerationMinY}) and {@code
 * windGenerationMax} (at {@code windGenerationMaxY}) by clamped height; we reproduce that linear shape with fixed
 * transitional constants ({@link MekanismGeneratorsConfig} is NeoForge-only), clamped to the world's build bounds.
 */
public class WindGeneratorBlockEntity extends AbstractGeneratorBlockEntity {

    // Transitional constants mirroring the NeoForge config defaults' shape (min..max generation over a min..max Y band).
    private static final long WIND_GENERATION_MIN = 60L;
    private static final long WIND_GENERATION_MAX = 480L;
    private static final int WIND_GENERATION_MIN_Y = 24;
    private static final int WIND_GENERATION_MAX_Y = 255;
    /** Hub offset: the NeoForge windmill checks sky/height 4 blocks above the base. */
    private static final int HUB_OFFSET = 4;

    public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricGenerators.WIND_BE_TYPE.get(), pos, state);
    }

    @Override
    protected void generate(ServerLevel level) {
        BlockPos top = getBlockPos().above(HUB_OFFSET);
        // Require open sky (and not fluid-logged) at the hub, like the NeoForge getMultiplier().
        if (!level.getFluidState(top).isEmpty() || !level.canSeeSky(top)) {
            return;
        }
        long production = getCurrentGeneration(level, top.getY());
        if (production > 0L) {
            energy.insert(production, Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    /**
     * Linear height interpolation between MIN (at MIN_Y) and MAX (at MAX_Y), clamped to the world's build bounds and to
     * the [MIN_Y, MAX_Y] band — the same shape as the NeoForge tile's getMultiplier()/getCurrentGeneration().
     */
    private long getCurrentGeneration(ServerLevel level, int hubY) {
        int minBuildHeight = level.getMinY();
        int maxLevelHeight = Math.min(level.getMaxY() + 1, minBuildHeight + level.dimensionType().logicalHeight()) - 1;
        int minY = Math.max(WIND_GENERATION_MIN_Y, minBuildHeight);
        int maxY = Math.min(WIND_GENERATION_MAX_Y, maxLevelHeight);
        if (maxY <= minY) {
            return WIND_GENERATION_MIN;
        }
        int clampedY = Math.min(maxY, Math.max(minY, hubY));
        double slope = ((double) (WIND_GENERATION_MAX - WIND_GENERATION_MIN)) / (maxY - minY);
        return Math.round(WIND_GENERATION_MIN + slope * (clampedY - minY));
    }
}
