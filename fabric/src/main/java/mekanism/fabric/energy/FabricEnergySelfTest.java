package mekanism.fabric.energy;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the Fabric energy slice. Gated behind {@code isDevelopmentEnvironment()} by its
 * caller, so it never runs for end users. It proves, on a live Fabric server, two things the energy port depends on:
 *
 * <ol>
 *     <li><b>The hoisted {@code :common} energy API functions under Fabric's classloader</b> — exercises
 *     {@link BasicEnergyContainer} insert/extract/needed accounting (which links {@code Action}, {@code AutomationType},
 *     the {@code IEnergyContainer}/{@code IStrictEnergyHandler} defaults and {@code LongTransferUtils} at runtime, with
 *     no NeoForge class leakage).</li>
 *     <li><b>The {@link MekanismFabricEnergy#SIDED} {@code BlockApiLookup} query path works end-to-end</b> — registers a
 *     provider, then looks an {@link IStrictEnergyHandler} back up from a world position and moves energy through it.</li>
 * </ol>
 *
 * Results are logged with a {@code [Mekanism/Fabric][energy-selftest]} tag; grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricEnergySelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][energy-selftest]";

    /** Gate so the registered fallback only answers for the single position we probe during the lookup test. */
    private static final AtomicReference<BlockPos> PROBE_POS = new AtomicReference<>();
    private static final IStrictEnergyHandler PROBE_HANDLER = handlerOf(BasicEnergyContainer.create(10_000L, null));

    private FabricEnergySelfTest() {
    }

    public static void run() {
        boolean apiOk = runApiTest();
        registerLookupProbe();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runLookupTest(server.overworld(), apiOk));
    }

    /** Tier 1 — the hoisted common energy classes load and compute correctly at runtime on Fabric. */
    private static boolean runApiTest() {
        try {
            BasicEnergyContainer c = BasicEnergyContainer.create(1_000L, null);
            long insertRemainder = c.insert(400L, Action.EXECUTE, AutomationType.EXTERNAL);
            long extracted = c.extract(150L, Action.EXECUTE, AutomationType.EXTERNAL);
            boolean ok = insertRemainder == 0L && extracted == 150L && c.getEnergy() == 250L && c.getNeeded() == 750L && !c.isEmpty();
            log(ok, "common energy API: stored=" + c.getEnergy() + " needed=" + c.getNeeded()
                    + " (insertRemainder=" + insertRemainder + " extracted=" + extracted + ")");
            return ok;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL common energy API threw", TAG, t);
            return false;
        }
    }

    private static void registerLookupProbe() {
        MekanismFabricEnergy.SIDED.registerFallback((level, pos, state, blockEntity, context) ->
              pos.equals(PROBE_POS.get()) ? PROBE_HANDLER : null);
    }

    /** Tier 2 — the BlockApiLookup find()->provider->handler path works end-to-end at runtime on Fabric. */
    private static void runLookupTest(ServerLevel level, boolean apiOk) {
        boolean lookupOk = false;
        try {
            BlockPos pos = new BlockPos(0, 64, 0);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4); // force-load so find() can read state/BE
            PROBE_POS.set(pos);
            IStrictEnergyHandler found = MekanismFabricEnergy.getStrictEnergyHandler(level, pos, null);
            PROBE_POS.set(null);
            if (found == PROBE_HANDLER) {
                long before = found.getEnergy(0);
                long insertRemainder = found.insertEnergy(500L, Action.EXECUTE);
                long extracted = found.extractEnergy(200L, Action.EXECUTE);
                long after = found.getEnergy(0);
                lookupOk = insertRemainder == 0L && extracted == 200L && after == before + 300L;
                log(lookupOk, "BlockApiLookup query+transfer: before=" + before + " after=" + after
                        + " (insertRemainder=" + insertRemainder + " extracted=" + extracted + ")");
            } else {
                log(false, "BlockApiLookup.find did not return the registered handler (got " + found + ")");
            }
        } catch (Throwable t) {
            PROBE_POS.set(null);
            LOGGER.error("{} FAIL lookup test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: api={} lookup={} => {}", TAG, apiOk, lookupOk, (apiOk && lookupOk) ? "PASS" : "FAIL");
    }

    private static IStrictEnergyHandler handlerOf(IEnergyContainer container) {
        List<IEnergyContainer> containers = List.of(container);
        return new IMekanismStrictEnergyHandler() {
            @Override
            public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
                return containers;
            }

            @Override
            public void onContentsChanged() {
            }
        };
    }

    private static void log(boolean ok, String msg) {
        LOGGER.info("{} {} {}", TAG, ok ? "OK  " : "FAIL", msg);
    }
}
