package mekanism.fabric.content.transmitter;

import com.mojang.logging.LogUtils;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.heat.HeatAPI;
import mekanism.fabric.chemical.FabricChemicalRegistry;
import mekanism.fabric.content.energy.DemoEnergyBlockEntity;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import mekanism.fabric.content.machine.ChemicalToItemMachineBlockEntity;
import mekanism.fabric.content.machine.FabricChemicalMachines;
import mekanism.fabric.content.machine.FabricRealMachines;
import mekanism.fabric.content.machine.MachineBlockEntity;
import mekanism.fabric.content.power.FabricPowerInfrastructure;
import mekanism.fabric.content.power.GeneratorBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of the four core transmitters: each is placed between a matching source and sink, ticked,
 * and asserted to have RELAYED its resource end-to-end. Grep {@code [Mekanism/Fabric][transmitter-selftest]}.
 *
 * <ul>
 *     <li><b>energy</b> — generator &rarr; basic_universal_cable &rarr; enrichment_chamber: drop coal + dirt, tick, assert
 *     the machine produced diamond (only possible if energy crossed the cable) and the cable buffered energy.</li>
 *     <li><b>chemical</b> — a filled crystallizer (its input tank allows external extract) &rarr; basic_pressurized_tube
 *     &rarr; an empty crystallizer: tick, assert the demo chemical moved across the tube into the sink tank.</li>
 *     <li><b>heat</b> — a hot demo heat-handler &rarr; basic_thermodynamic_conductor &rarr; a cold (ambient) demo
 *     heat-handler: tick, assert the cold side warmed up (heat flowed hot&rarr;cold through the conductor).</li>
 *     <li><b>items</b> — a chest holding cobblestone &rarr; basic_logistical_transporter &rarr; an empty chest: tick,
 *     assert the items moved across the transporter into the destination chest.</li>
 * </ul>
 */
public final class FabricTransmitterSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][transmitter-selftest]";

    private FabricTransmitterSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean energy = validateEnergy(level);
        boolean chemical = validateChemical(level);
        boolean heat = validateHeat(level);
        boolean items = validateItems(level);
        boolean all = energy && chemical && heat && items;
        LOGGER.info("{} RESULT: energy={} chemical={} heat={} items={} => {}", TAG, energy, chemical, heat, items,
              all ? "PASS" : "FAIL");
    }

    /** generator -> basic_universal_cable -> enrichment_chamber; assert energy crossed the cable (machine made diamond). */
    private static boolean validateEnergy(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos genPos = new BlockPos(20, 64, 40);
            BlockPos cablePos = genPos.east();
            BlockPos machinePos = cablePos.east();
            level.getChunk(genPos.getX() >> 4, genPos.getZ() >> 4);

            level.setBlock(genPos, FabricPowerInfrastructure.GENERATOR.get().defaultBlockState(), 3);
            level.setBlock(cablePos, FabricTransmitters.UNIVERSAL_CABLE.get().defaultBlockState(), 3);
            level.setBlock(machinePos, FabricRealMachines.enrichmentChamber().block().defaultBlockState(), 3);

            boolean placed = level.getBlockEntity(genPos) instanceof GeneratorBlockEntity
                  && level.getBlockEntity(cablePos) instanceof UniversalCableBlockEntity
                  && level.getBlockEntity(machinePos) instanceof MachineBlockEntity;

            int diamonds = 0;
            long cablePeak = 0L;
            if (placed) {
                GeneratorBlockEntity generator = (GeneratorBlockEntity) level.getBlockEntity(genPos);
                UniversalCableBlockEntity cable = (UniversalCableBlockEntity) level.getBlockEntity(cablePos);
                MachineBlockEntity machine = (MachineBlockEntity) level.getBlockEntity(machinePos);
                generator.setItem(0, new ItemStack(Items.COAL, 4));
                machine.setItem(0, new ItemStack(Items.DIRT, 16));
                for (int i = 0; i < 200; i++) {
                    BlockState machineState = level.getBlockState(machinePos);
                    machine.serverTick(machineState);
                    cable.serverTick(level);
                    generator.serverTick(level);
                    cablePeak = Math.max(cablePeak, cable.bufferedEnergy());
                }
                ItemStack output = machine.getItem(1);
                diamonds = output.is(Items.DIAMOND) ? output.getCount() : 0;
            }
            ok = placed && diamonds >= 1 && cablePeak > 0L;
            log(ok, "energy: generator->cable->enrichment_chamber diamonds=" + diamonds + " cableBufferPeak=" + cablePeak);
            removeAll(level, genPos, cablePos, machinePos);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL energy transmitter test threw", TAG, t);
        }
        return ok;
    }

    /** filled crystallizer -> basic_pressurized_tube -> empty crystallizer; assert chemical moved across the tube. */
    private static boolean validateChemical(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos srcPos = new BlockPos(20, 64, 44);
            BlockPos tubePos = srcPos.east();
            BlockPos sinkPos = tubePos.east();
            level.getChunk(srcPos.getX() >> 4, srcPos.getZ() >> 4);

            BlockState crystallizer = FabricChemicalMachines.CHEMICAL_CRYSTALLIZER.block().defaultBlockState();
            level.setBlock(srcPos, crystallizer, 3);
            level.setBlock(tubePos, FabricTransmitters.PRESSURIZED_TUBE.get().defaultBlockState(), 3);
            level.setBlock(sinkPos, crystallizer, 3);

            boolean placed = level.getBlockEntity(srcPos) instanceof ChemicalToItemMachineBlockEntity
                  && level.getBlockEntity(tubePos) instanceof PressurizedTubeBlockEntity
                  && level.getBlockEntity(sinkPos) instanceof ChemicalToItemMachineBlockEntity;

            long startSrc = 0L;
            long endSink = 0L;
            long endTube = 0L;
            if (placed) {
                ChemicalToItemMachineBlockEntity source = (ChemicalToItemMachineBlockEntity) level.getBlockEntity(srcPos);
                PressurizedTubeBlockEntity tube = (PressurizedTubeBlockEntity) level.getBlockEntity(tubePos);
                ChemicalToItemMachineBlockEntity sink = (ChemicalToItemMachineBlockEntity) level.getBlockEntity(sinkPos);
                // Fill the source's input tank with demo chemical; leave the sink empty. The crystallizer input tank
                // allows external extract+insert, so the tube can pull from the source and push into the sink.
                source.getInputTank().setStack(new ChemicalStack(FabricChemicalRegistry.demo(), 5_000L));
                startSrc = source.getInputTank().getStored();
                for (int i = 0; i < 60; i++) {
                    tube.serverTick(level);
                }
                endSink = sink.getInputTank().getStored();
                endTube = tube.bufferedChemical().amount();
            }
            // The chemical the source lost equals what reached the tube buffer + the sink (no loss).
            long movedOut = startSrc - (placed ? ((ChemicalToItemMachineBlockEntity) level.getBlockEntity(srcPos)).getInputTank().getStored() : startSrc);
            ok = placed && endSink > 0L && movedOut > 0L;
            log(ok, "chemical: crystallizer->tube->crystallizer movedFromSource=" + movedOut + " inSink=" + endSink
                  + " inTubeBuffer=" + endTube);
            removeAll(level, srcPos, tubePos, sinkPos);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL chemical transmitter test threw", TAG, t);
        }
        return ok;
    }

    /** hot demo heat-handler -> basic_thermodynamic_conductor -> cold demo heat-handler; assert the cold side warmed. */
    private static boolean validateHeat(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos hotPos = new BlockPos(20, 64, 48);
            BlockPos condPos = hotPos.east();
            BlockPos coldPos = condPos.east();
            level.getChunk(hotPos.getX() >> 4, hotPos.getZ() >> 4);

            BlockState demo = FabricEnergyBlockDemo.BLOCK.get().defaultBlockState();
            level.setBlock(hotPos, demo, 3);
            level.setBlock(condPos, FabricTransmitters.THERMODYNAMIC_CONDUCTOR.get().defaultBlockState(), 3);
            level.setBlock(coldPos, demo, 3);

            boolean placed = level.getBlockEntity(hotPos) instanceof DemoEnergyBlockEntity
                  && level.getBlockEntity(condPos) instanceof ThermodynamicConductorBlockEntity
                  && level.getBlockEntity(coldPos) instanceof DemoEnergyBlockEntity;

            double coldBefore = HeatAPI.AMBIENT_TEMP;
            double coldAfter = HeatAPI.AMBIENT_TEMP;
            double hotBefore = HeatAPI.AMBIENT_TEMP;
            if (placed) {
                DemoEnergyBlockEntity hot = (DemoEnergyBlockEntity) level.getBlockEntity(hotPos);
                ThermodynamicConductorBlockEntity conductor = (ThermodynamicConductorBlockEntity) level.getBlockEntity(condPos);
                DemoEnergyBlockEntity cold = (DemoEnergyBlockEntity) level.getBlockEntity(coldPos);
                // Heat the hot side well above ambient and bake it in; the cold side stays at ambient.
                hot.getHeatCapacitors(null).getFirst().handleHeat(500_000.0D);
                hot.updateHeat();
                hotBefore = hot.getHeatCapacitors(null).getFirst().getTemperature();
                coldBefore = cold.getHeatCapacitors(null).getFirst().getTemperature();
                for (int i = 0; i < 100; i++) {
                    conductor.serverTick(level);
                }
                coldAfter = cold.getHeatCapacitors(null).getFirst().getTemperature();
            }
            ok = placed && hotBefore > HeatAPI.AMBIENT_TEMP + 1.0D && coldAfter > coldBefore + 1.0D;
            log(ok, "heat: hot(" + fmt(hotBefore) + ")->conductor->cold cold " + fmt(coldBefore) + "->" + fmt(coldAfter)
                  + " conductorTemp=" + fmt(placed ? ((ThermodynamicConductorBlockEntity) level.getBlockEntity(condPos)).temperature() : 0));
            removeAll(level, hotPos, condPos, coldPos);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL heat transmitter test threw", TAG, t);
        }
        return ok;
    }

    /** chest(cobblestone) -> basic_logistical_transporter -> empty chest; assert the items moved across. */
    private static boolean validateItems(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos srcPos = new BlockPos(20, 64, 52);
            BlockPos transPos = srcPos.east();
            BlockPos destPos = transPos.east();
            level.getChunk(srcPos.getX() >> 4, srcPos.getZ() >> 4);

            level.setBlock(srcPos, Blocks.CHEST.defaultBlockState(), 3);
            level.setBlock(transPos, FabricTransmitters.LOGISTICAL_TRANSPORTER.get().defaultBlockState(), 3);
            level.setBlock(destPos, Blocks.CHEST.defaultBlockState(), 3);

            boolean placed = level.getBlockEntity(srcPos) instanceof ChestBlockEntity
                  && level.getBlockEntity(transPos) instanceof LogisticalTransporterBlockEntity
                  && level.getBlockEntity(destPos) instanceof ChestBlockEntity;

            int startCount = 0;
            int destCount = 0;
            if (placed) {
                Container source = (Container) level.getBlockEntity(srcPos);
                LogisticalTransporterBlockEntity transporter = (LogisticalTransporterBlockEntity) level.getBlockEntity(transPos);
                Container dest = (Container) level.getBlockEntity(destPos);
                source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
                startCount = 32;
                for (int i = 0; i < 40; i++) {
                    transporter.serverTick(level);
                }
                destCount = countItem(dest, Items.COBBLESTONE);
            }
            ok = placed && destCount > 0;
            int srcLeft = placed ? countItem((Container) level.getBlockEntity(srcPos), Items.COBBLESTONE) : startCount;
            log(ok, "items: chest->transporter->chest moved=" + destCount + " of " + startCount + " (srcLeft=" + srcLeft + ")");
            removeAll(level, srcPos, transPos, destPos);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL item transmitter test threw", TAG, t);
        }
        return ok;
    }

    private static int countItem(Container container, net.minecraft.world.item.Item item) {
        int sum = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(item)) {
                sum += stack.getCount();
            }
        }
        return sum;
    }

    private static void removeAll(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            level.removeBlock(pos, false);
        }
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static void log(boolean ok, String msg) {
        LOGGER.info("{} {} {}", TAG, ok ? "OK  " : "FAIL", msg);
    }
}
