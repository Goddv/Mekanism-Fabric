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
 * Transitional Fabric bring-up: a minimal <strong>pull-based</strong> energy-distribution helper used by the cable +
 * machine block-entities. Each tick a block pulls energy out of adjacent {@link IStrictEnergyHandler}s (via
 * {@link MekanismFabricEnergy#SIDED}) into its own container.
 *
 * <p>Pull (not push) is used deliberately: a push model comparing fill <em>ratios</em> stalls when capacities differ
 * wildly (a 400k-capacity generator holding 200 FE has a far lower ratio than an 8k cable holding 40 FE, so it stops
 * pushing). Pulling avoids that — generators simply accumulate and are drained; consumers (machines) and relays (cables)
 * pull what they can hold. Direction is controlled by {@code onlyFromHigher}: cables pull only from higher-energy
 * neighbours so energy flows "downhill" generator&rarr;cable&rarr;…&rarr;machine without cable&harr;cable sloshing,
 * while machines (always the lowest, since they consume) pull from anyone. Sources reject insertion + sinks reject
 * extraction (handled on the block-entities), so energy never flows backward. The real Mekanism transmitter-network grid
 * replaces this once that subsystem is hoisted.
 */
public final class EnergyTransferHelper {

    private EnergyTransferHelper() {
    }

    /**
     * Pulls up to {@code maxPerTick} energy into {@code own} from adjacent handlers.
     *
     * @param onlyFromHigher if true, only pull from neighbours holding strictly more energy than {@code own} (downhill
     *                       flow for cables); if false, pull from any neighbour (sinks like machines).
     */
    public static void pull(ServerLevel level, BlockPos pos, IEnergyContainer own, long maxPerTick, boolean onlyFromHigher) {
        long budget = Math.min(maxPerTick, own.getNeeded());
        if (budget <= 0L) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            IStrictEnergyHandler source = MekanismFabricEnergy.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (source == null) {
                continue;
            }
            if (onlyFromHigher && totalEnergy(source) <= own.getEnergy()) {
                continue;
            }
            long want = Math.min(budget, own.getNeeded());
            if (want <= 0L) {
                break;
            }
            // extractEnergy returns the amount actually extracted; insert it into our own container (room was checked).
            long extracted = source.extractEnergy(want, Action.EXECUTE);
            if (extracted > 0L) {
                own.insert(extracted, Action.EXECUTE, AutomationType.INTERNAL);
                budget -= extracted;
            }
        }
    }

    private static long totalEnergy(IStrictEnergyHandler handler) {
        long sum = 0L;
        for (int i = 0; i < handler.getEnergyContainerCount(); i++) {
            sum += handler.getEnergy(i);
        }
        return sum;
    }
}
