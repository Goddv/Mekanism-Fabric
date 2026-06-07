package mekanism.fabric.heat;

import com.mojang.logging.LogUtils;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.fabric.content.energy.DemoEnergyBlockEntity;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the Fabric heat slice (gated behind {@code isDevelopmentEnvironment()} by its caller).
 * Proves, on a live Fabric server, the same two things the energy slice proved — now for the second core Mekanism
 * capability:
 *
 * <ol>
 *     <li><b>The hoisted {@code :common} heat API runs under Fabric's classloader</b> — exercises
 *     {@link BasicHeatCapacitor} temperature/heat-transfer math (links {@code HeatAPI}, {@code IHeatCapacitor},
 *     {@code IContentsListener}, {@code SerializationConstants} at runtime, with no NeoForge leakage).</li>
 *     <li><b>The {@link MekanismFabricHeat#SIDED} {@code BlockApiLookup} query path works end-to-end</b> — places the
 *     demo block-entity, looks an {@link IHeatHandler} back up from a world position, transfers heat through it, and
 *     confirms the temperature changes.</li>
 * </ol>
 *
 * Results are logged with a {@code [Mekanism/Fabric][heat-selftest]} tag; grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricHeatSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][heat-selftest]";

    private FabricHeatSelfTest() {
    }

    public static void run() {
        boolean apiOk = runApiTest();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            boolean lookupOk = runBlockEntityLookupTest(level);
            LOGGER.info("{} RESULT: api={} heatCapability={} => {}", TAG, apiOk, lookupOk,
                  (apiOk && lookupOk) ? "PASS" : "FAIL");
        });
    }

    /** Tier 1 — the hoisted common heat classes load and compute correctly at runtime on Fabric. */
    private static boolean runApiTest() {
        try {
            BasicHeatCapacitor c = BasicHeatCapacitor.create(1_000.0D, null, null);
            double startTemp = c.getTemperature();              // initialized to ambient (300)
            c.handleHeat(50_000.0D);                            // +50 000 heat is buffered...
            c.update();                                         // ...applied on update
            double afterTemp = c.getTemperature();
            double expected = startTemp + 50_000.0D / c.getHeatCapacity();
            boolean ok = Math.abs(startTemp - HeatAPI.AMBIENT_TEMP) < 1.0E-3
                  && Math.abs(afterTemp - expected) < 1.0E-3 && afterTemp > startTemp;
            log(ok, "common heat API: startTemp=" + startTemp + " afterTemp=" + afterTemp + " (expected=" + expected
                    + ", capacity=" + c.getHeatCapacity() + ")");
            return ok;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL common heat API threw", TAG, t);
            return false;
        }
    }

    /** Tier 2 — the demo block-entity exposes its heat capability via registerForBlockEntity, end-to-end. */
    private static boolean runBlockEntityLookupTest(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos pos = new BlockPos(0, 64, 12);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricEnergyBlockDemo.BLOCK.get().defaultBlockState(), 3);
            boolean placed = level.getBlockEntity(pos) instanceof DemoEnergyBlockEntity;
            IHeatHandler handler = MekanismFabricHeat.getHeatHandler(level, pos, null);
            if (placed && handler != null && handler.getHeatCapacitorCount() == 1) {
                double before = handler.getTemperature(0);
                double capacity = handler.getHeatCapacity(0);
                handler.handleHeat(0, 50_000.0D);              // buffer heat through the capability...
                ((DemoEnergyBlockEntity) level.getBlockEntity(pos)).updateHeat(); // ...flush it (real tiles flush on tick)
                double after = handler.getTemperature(0);
                double expected = before + 50_000.0D / capacity;
                ok = Math.abs(before - HeatAPI.AMBIENT_TEMP) < 1.0E-3 && Math.abs(after - expected) < 1.0E-3 && after > before;
                log(ok, "heat BlockApiLookup query+transfer: temp " + before + "->" + after + " (capacity=" + capacity
                        + " expected=" + expected + ")");
            } else {
                log(false, "demo heat block: placed=" + placed + " handlerResolved=" + (handler != null));
            }
            level.removeBlock(pos, false);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL heat block-entity test threw", TAG, t);
        }
        return ok;
    }

    private static void log(boolean ok, String msg) {
        LOGGER.info("{} {} {}", TAG, ok ? "OK  " : "FAIL", msg);
    }
}
