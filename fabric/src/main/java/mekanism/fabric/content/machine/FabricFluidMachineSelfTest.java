package mekanism.fabric.content.machine;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.fluid.IFluidStack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the FLUID-input machine on Fabric: the Electrolytic Separator (fluid input &rarr; two
 * chemical outputs). Places an {@code electrolytic_separator}, fills its fluid INPUT tank with water ({@code >=} the
 * recipe amount), injects energy, ticks past completion, and asserts that BOTH chemical output tanks now hold chemical
 * (amount {@code > 0}) AND the input water was consumed.
 *
 * <p>Grep {@code [Mekanism/Fabric][fluid-machine-selftest]}.
 */
public final class FabricFluidMachineSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][fluid-machine-selftest]";

    private FabricFluidMachineSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean separator = validateSeparator(level);
        LOGGER.info("{} RESULT: separator={} => {}", TAG, separator, separator ? "PASS" : "FAIL");
    }

    /** Place an electrolytic_separator, fill water + energy, tick, assert both outputs filled and water was consumed. */
    private static boolean validateSeparator(ServerLevel level) {
        boolean ok = false;
        long leftOut = 0L;
        long rightOut = 0L;
        long waterConsumed = 0L;
        try {
            BlockPos pos = new BlockPos(8, 64, 60);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricElectrolyticSeparator.ELECTROLYTIC_SEPARATOR.get().defaultBlockState(), 3);

            if (level.getBlockEntity(pos) instanceof ElectrolyticSeparatorBlockEntity machine) {
                // Fill the input tank with 3 buckets of water (>= the recipe's 1-bucket amount).
                long startWater = 3L * FluidConstants.BUCKET;
                machine.getInputTank().setStack(IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), startWater));
                machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);

                for (int i = 0; i < ElectrolyticSeparatorBlockEntity.MAX_PROGRESS + 5
                      && (machine.getLeftTank().isEmpty() || machine.getRightTank().isEmpty()); i++) {
                    machine.serverTick();
                }
                leftOut = machine.getLeftTank().getStored();
                rightOut = machine.getRightTank().getStored();
                waterConsumed = startWater - machine.getInputTank().getFluidAmount();
            }
            level.removeBlock(pos, false);

            ok = leftOut > 0L && rightOut > 0L && waterConsumed > 0L;
            LOGGER.info("{} {} [electrolytic_separator] leftOut={} rightOut={} waterConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", leftOut, rightOut, waterConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [electrolytic_separator] fluid-machine test threw", TAG, t);
        }
        return ok;
    }
}
