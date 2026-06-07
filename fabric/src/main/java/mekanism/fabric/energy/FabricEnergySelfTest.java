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
import mekanism.fabric.content.energy.DemoEnergyBlockEntity;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import mekanism.fabric.content.machine.FabricRealMachines;
import mekanism.fabric.content.machine.MachineBlock;
import mekanism.fabric.content.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            boolean lookupOk = runLookupTest(level);
            boolean blockEntityOk = runBlockEntityTest(level);
            boolean machineOk = runMachineTest(level);
            LOGGER.info("{} RESULT: api={} lookup={} blockEntity={} machine={} => {}", TAG, apiOk, lookupOk, blockEntityOk,
                  machineOk, (apiOk && lookupOk && blockEntityOk && machineOk) ? "PASS" : "FAIL");
        });
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

    /** Tier 2 — the BlockApiLookup find()->fallback-provider->handler path works end-to-end at runtime on Fabric. */
    private static boolean runLookupTest(ServerLevel level) {
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
        return lookupOk;
    }

    /** Tier 3 — a REAL functional energy block-entity exposes its capability via registerForBlockEntity, end-to-end. */
    private static boolean runBlockEntityTest(ServerLevel level) {
        boolean beOk = false;
        try {
            BlockPos pos = new BlockPos(0, 64, 4);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricEnergyBlockDemo.BLOCK.get().defaultBlockState(), 3);
            boolean placed = level.getBlockEntity(pos) instanceof DemoEnergyBlockEntity;
            IStrictEnergyHandler handler = MekanismFabricEnergy.getStrictEnergyHandler(level, pos, null);
            if (placed && handler != null) {
                long insertRemainder = handler.insertEnergy(750L, Action.EXECUTE);
                long stored = handler.getEnergy(0);
                long extracted = handler.extractEnergy(250L, Action.EXECUTE);
                long after = handler.getEnergy(0);
                beOk = insertRemainder == 0L && stored == 750L && extracted == 250L && after == 500L;
                log(beOk, "functional energy BlockEntity via registerForBlockEntity: stored=" + stored + " after=" + after
                        + " (insertRemainder=" + insertRemainder + " extracted=" + extracted + ")");
            } else {
                log(false, "demo energy block: placed=" + placed + " handlerResolved=" + (handler != null));
            }
            level.removeBlock(pos, false);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL block-entity test threw", TAG, t);
        }
        return beOk;
    }

    /** Tier 4 — a functional processing machine: ticking + energy consumption + item I/O + item capability. */
    private static boolean runMachineTest(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos pos = new BlockPos(0, 64, 8);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricRealMachines.enrichmentChamber().get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                machine.insertEnergy(2000L, Action.EXECUTE);
                machine.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
                long energyBefore = machine.getEnergy(0);
                for (int i = 0; i < 3; i++) {
                    machine.serverTick(level.getBlockState(pos));
                }
                long energyAfter = machine.getEnergy(0);
                int outCount = machine.getItem(1).getCount();
                int inLeft = machine.getItem(0).getCount();
                boolean active = level.getBlockState(pos).getValue(MachineBlock.ACTIVE);
                boolean itemCapOk = ItemStorage.SIDED.find(level, pos, null) != null;
                ok = outCount == 3 && inLeft == 2 && energyAfter == energyBefore - 600L && active && itemCapOk;
                log(ok, "real machine (enrichment_chamber) tick+process: output=" + outCount + " inputLeft=" + inLeft
                        + " energy " + energyBefore + "->" + energyAfter + " active=" + active + " itemCapability=" + itemCapOk);
            } else {
                log(false, "real machine block-entity not placed");
            }
            level.removeBlock(pos, false);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL machine test threw", TAG, t);
        }
        return ok;
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
