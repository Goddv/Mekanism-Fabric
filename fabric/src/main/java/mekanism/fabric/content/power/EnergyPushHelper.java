package mekanism.fabric.content.power;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.fabric.energy.MekanismFabricEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/**
 * Transitional Fabric bring-up: a minimal energy-distribution helper used by the generator + cable block-entities to move
 * energy to adjacent {@link IStrictEnergyHandler}s through {@link MekanismFabricEnergy#SIDED}.
 *
 * <p>Uses a <strong>fill-ratio gradient</strong>: a block only pushes into a neighbour whose stored/capacity ratio is
 * lower than its own, so energy flows "downhill" toward the consuming machine (which is always emptiest) instead of
 * sloshing back into the source. This is robust without relying on per-side insert/extract gating — the current
 * BlockApiLookup registration returns the block-entity directly, so cap-level transfers are treated as INTERNAL and the
 * {@code input}/{@code output} container variants can't gate direction. The real Mekanism transmitter-network grid
 * replaces this once that subsystem is hoisted.
 */
public final class EnergyPushHelper {

    private EnergyPushHelper() {
    }

    /**
     * Pushes up to {@code maxPerTick} energy from {@code source} into lower-fill adjacent handlers.
     */
    public static void pushToNeighbors(ServerLevel level, BlockPos pos, IEnergyContainer source, long maxPerTick) {
        long max = source.getMaxEnergy();
        if (max <= 0 || source.getEnergy() <= 0) {
            return;
        }
        long budget = Math.min(maxPerTick, source.getEnergy());
        for (Direction dir : Direction.values()) {
            if (budget <= 0 || source.getEnergy() <= 0) {
                break;
            }
            IStrictEnergyHandler sink = MekanismFabricEnergy.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (sink == null) {
                continue;
            }
            int containers = sink.getEnergyContainerCount();
            if (containers == 0) {
                continue;
            }
            long sinkEnergy = 0L;
            long sinkMax = 0L;
            for (int c = 0; c < containers; c++) {
                sinkEnergy += sink.getEnergy(c);
                sinkMax += sink.getMaxEnergy(c);
            }
            if (sinkMax <= 0L) {
                continue;
            }
            // Only flow downhill (toward emptier neighbours) to avoid back-and-forth sloshing.
            double sourceRatio = source.getEnergy() / (double) max;
            double sinkRatio = sinkEnergy / (double) sinkMax;
            if (sinkRatio >= sourceRatio) {
                continue;
            }
            long toSend = Math.min(budget, source.getEnergy());
            // Mekanism's insertEnergy returns the REMAINDER (amount NOT accepted), so acceptable = toSend - remainder.
            long acceptable = toSend - sink.insertEnergy(toSend, Action.SIMULATE);
            if (acceptable > 0L) {
                // extract() returns the amount actually extracted; feed exactly that into the sink.
                long extracted = source.extract(acceptable, Action.EXECUTE, AutomationType.INTERNAL);
                if (extracted > 0L) {
                    sink.insertEnergy(extracted, Action.EXECUTE);
                    budget -= extracted;
                }
            }
        }
    }
}
