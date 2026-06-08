package mekanism.fabric.content.power;

import com.mojang.logging.LogUtils;
import mekanism.fabric.content.machine.FabricRealMachines;
import mekanism.fabric.content.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of the power infrastructure: places generator &rarr; cable &rarr; enrichment_chamber in a
 * line, drops coal in the generator + dirt in the machine, ticks them, and asserts the machine produced diamond — which
 * can only happen if energy was generated, relayed across the cable, and delivered to the machine to run the real
 * enriching recipe. Grep for {@code [Mekanism/Fabric][power-selftest] RESULT:}.
 */
public final class FabricPowerSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][power-selftest]";

    private FabricPowerSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos genPos = new BlockPos(0, 64, 40);
            BlockPos cablePos = new BlockPos(1, 64, 40);
            BlockPos machinePos = new BlockPos(2, 64, 40);
            level.getChunk(genPos.getX() >> 4, genPos.getZ() >> 4);

            level.setBlock(genPos, FabricPowerInfrastructure.GENERATOR.get().defaultBlockState(), 3);
            level.setBlock(cablePos, FabricPowerInfrastructure.CABLE.get().defaultBlockState(), 3);
            level.setBlock(machinePos, FabricRealMachines.enrichmentChamber().block().defaultBlockState(), 3);

            boolean placed = level.getBlockEntity(genPos) instanceof GeneratorBlockEntity
                  && level.getBlockEntity(cablePos) instanceof CableBlockEntity
                  && level.getBlockEntity(machinePos) instanceof MachineBlockEntity;

            boolean processOk = false;
            int diamonds = 0;
            boolean fuelBurned = false;
            if (placed) {
                GeneratorBlockEntity generator = (GeneratorBlockEntity) level.getBlockEntity(genPos);
                CableBlockEntity cable = (CableBlockEntity) level.getBlockEntity(cablePos);
                MachineBlockEntity machine = (MachineBlockEntity) level.getBlockEntity(machinePos);

                generator.setItem(0, new ItemStack(Items.COAL, 4));
                machine.setItem(0, new ItemStack(Items.DIRT, 16));

                // Tick in REVERSE order (machine, then cable, then generator) each game tick to prove the pull-based
                // model is tick-order-independent — the bug the old fixed-order push test masked. Energy must still flow
                // generator -> cable -> machine over time. Run long enough to complete several operations.
                long cablePeakEnergy = 0L;
                long machinePeakEnergy = 0L;
                for (int i = 0; i < 200; i++) {
                    BlockState machineState = level.getBlockState(machinePos);
                    machine.serverTick(machineState);
                    cable.serverTick(level);
                    generator.serverTick(level);
                    cablePeakEnergy = Math.max(cablePeakEnergy, cable.getEnergy(0));
                    machinePeakEnergy = Math.max(machinePeakEnergy, machine.getEnergy(0));
                }

                ItemStack output = machine.getItem(1);
                diamonds = output.is(Items.DIAMOND) ? output.getCount() : 0;
                fuelBurned = generator.getItem(0).getCount() < 4 || generator.isBurning();
                processOk = diamonds >= 1 && fuelBurned && cablePeakEnergy > 0L && machinePeakEnergy > 0L;
                LOGGER.info("{} {} chain (reverse-tick): diamonds={} fuelLeft={} generatorBurning={} cablePeak={} machinePeak={}",
                      TAG, processOk ? "OK  " : "FAIL", diamonds, generator.getItem(0).getCount(), generator.isBurning(), cablePeakEnergy, machinePeakEnergy);
            } else {
                LOGGER.info("{} FAIL blocks/block-entities not placed", TAG);
            }

            level.removeBlock(genPos, false);
            level.removeBlock(cablePos, false);
            level.removeBlock(machinePos, false);

            ok = placed && processOk;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL power test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
