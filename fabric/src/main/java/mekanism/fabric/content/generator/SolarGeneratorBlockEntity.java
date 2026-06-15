package mekanism.fabric.content.generator;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.common.util.WorldUtilsBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transitional Fabric bring-up: the Solar Generator. Mirrors the NeoForge {@code TileEntitySolarGenerator} shape: each
 * tick, if the block above can see the sun, it inserts a sun-brightness-scaled rate into its energy reservoir. No
 * inventory.
 *
 * <p>"Can see sun" reuses the loader-neutral {@link WorldUtilsBase#canSeeSun(net.minecraft.world.level.Level, BlockPos)}
 * (skylight dim &lt; 4, has sky access) — the same predicate the NeoForge tile's {@code SolarCheck} ultimately calls —
 * and brightness reuses {@link WorldUtilsBase#getSunBrightness}. The per-tick rate is a fixed transitional constant
 * ({@link MekanismGeneratorsConfig} is NeoForge-only) chosen to fill visibly within a few ticks.
 */
public class SolarGeneratorBlockEntity extends AbstractGeneratorBlockEntity {

    /** Peak per-tick production at full sun. Transitional constant (NeoForge config is unavailable on Fabric). */
    private static final long SOLAR_RATE = 50L;

    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricGenerators.SOLAR_BE_TYPE.get(), pos, state);
    }

    @Override
    protected void generate(ServerLevel level) {
        BlockPos above = getBlockPos().above();
        if (WorldUtilsBase.canSeeSun(level, above)) {
            // Production = peak rate * current sun brightness (so it tapers at dawn/dusk and under rain), mirroring the
            // NeoForge tile's getProduction() = configuredMax * brightness * generationMultiplier shape.
            float brightness = WorldUtilsBase.getSunBrightness(level, getBlockPos());
            long production = (long) (SOLAR_RATE * brightness);
            if (production > 0L) {
                energy.insert(production, Action.EXECUTE, AutomationType.INTERNAL);
            }
        }
    }
}
