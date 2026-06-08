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

                // Diagnostics: confirm the neighbour caps are findable + watch the first tick's energy movement.
                boolean cableCapFound = mekanism.fabric.energy.MekanismFabricEnergy.getStrictEnergyHandler(level, cablePos, net.minecraft.core.Direction.WEST) != null;
                boolean machineCapFound = mekanism.fabric.energy.MekanismFabricEnergy.getStrictEnergyHandler(level, machinePos, net.minecraft.core.Direction.WEST) != null;
                generator.serverTick(level);
                LOGGER.info("{} diag: cableCapFound={} machineCapFound={} afterGenTick genEnergy={} cableEnergy={}",
                      TAG, cableCapFound, machineCapFound, generator.getEnergy(0), cable.getEnergy(0));

                long cablePeakEnergy = 0L;
                for (int i = 0; i < 60; i++) {
                    generator.serverTick(level);
                    // Measure the cable's buffer right after the generator pushes, before the cable forwards it onward.
                    cablePeakEnergy = Math.max(cablePeakEnergy, cable.getEnergy(0));
                    cable.serverTick(level);
                    BlockState machineState = level.getBlockState(machinePos);
                    machine.serverTick(machineState);
                }

                ItemStack output = machine.getItem(1);
                diamonds = output.is(Items.DIAMOND) ? output.getCount() : 0;
                fuelBurned = generator.getItem(0).getCount() < 4 || generator.isBurning();
                processOk = diamonds >= 1 && fuelBurned && cablePeakEnergy > 0L;
                LOGGER.info("{} {} chain: diamonds={} fuelLeft={} generatorBurning={} cablePeakEnergy={}",
                      TAG, processOk ? "OK  " : "FAIL", diamonds, generator.getItem(0).getCount(), generator.isBurning(), cablePeakEnergy);
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
