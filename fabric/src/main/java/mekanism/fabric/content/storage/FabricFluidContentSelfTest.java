package mekanism.fabric.content.storage;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.fluid.IFluidStack;
import mekanism.fabric.content.transmitter.FabricTransmitters;
import mekanism.fabric.content.transmitter.MechanicalPipeBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of the fluid CONTENT (Fluid Tank storage + Mechanical Pipe relay), the fluid analog of the
 * transmitter self-test. Grep {@code [Mekanism/Fabric][fluid-content-selftest]}.
 *
 * <ul>
 *     <li><b>tank</b> — place a {@code basic_fluid_tank}, insert water via its {@link FluidTankBlockEntity} cap, assert the
 *     stored amount equals what was inserted (storage round-trips).</li>
 *     <li><b>pipe</b> — full tank &rarr; {@code basic_mechanical_pipe} &rarr; empty tank: tick the pipe several times, assert
 *     fluid moved out of the source tank, across the pipe buffer, into the destination tank (the relay works).</li>
 * </ul>
 */
public final class FabricFluidContentSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][fluid-content-selftest]";

    private FabricFluidContentSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean tank = validateTank(level);
        boolean pipe = validatePipe(level);
        boolean all = tank && pipe;
        LOGGER.info("{} RESULT: tank={} pipe={} => {}", TAG, tank, pipe, all ? "PASS" : "FAIL");
    }

    /** Place a basic_fluid_tank, insert water, assert stored == inserted. */
    private static boolean validateTank(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos pos = new BlockPos(20, 64, 56);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricStorage.BASIC_FLUID_TANK.get().defaultBlockState(), 3);

            long inserted = 3L * FluidConstants.BUCKET;
            long stored = 0L;
            boolean placed = level.getBlockEntity(pos) instanceof FluidTankBlockEntity;
            if (placed) {
                FluidTankBlockEntity be = (FluidTankBlockEntity) level.getBlockEntity(pos);
                IFluidStack leftover = be.insertFluid(0, IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), inserted), Action.EXECUTE);
                IFluidStack contents = be.getFluidInTank(0);
                stored = contents.getAmount();
                ok = leftover.isEmpty() && contents.getFluid() == Fluids.WATER && stored == inserted;
            }
            log(ok, "tank: basic_fluid_tank stored=" + stored + " of inserted=" + inserted);
            level.removeBlock(pos, false);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL tank test threw", TAG, t);
        }
        return ok;
    }

    /** full tank -> basic_mechanical_pipe -> empty tank; assert fluid moved across the pipe into the destination. */
    private static boolean validatePipe(ServerLevel level) {
        boolean ok = false;
        try {
            BlockPos srcPos = new BlockPos(20, 64, 60);
            BlockPos pipePos = srcPos.east();
            BlockPos sinkPos = pipePos.east();
            level.getChunk(srcPos.getX() >> 4, srcPos.getZ() >> 4);

            level.setBlock(srcPos, FabricStorage.BASIC_FLUID_TANK.get().defaultBlockState(), 3);
            level.setBlock(pipePos, FabricTransmitters.MECHANICAL_PIPE.get().defaultBlockState(), 3);
            level.setBlock(sinkPos, FabricStorage.BASIC_FLUID_TANK.get().defaultBlockState(), 3);

            boolean placed = level.getBlockEntity(srcPos) instanceof FluidTankBlockEntity
                  && level.getBlockEntity(pipePos) instanceof MechanicalPipeBlockEntity
                  && level.getBlockEntity(sinkPos) instanceof FluidTankBlockEntity;

            long startSrc = 0L;
            long endSink = 0L;
            long endPipe = 0L;
            long endSrc = 0L;
            if (placed) {
                FluidTankBlockEntity source = (FluidTankBlockEntity) level.getBlockEntity(srcPos);
                MechanicalPipeBlockEntity pipe = (MechanicalPipeBlockEntity) level.getBlockEntity(pipePos);
                FluidTankBlockEntity sink = (FluidTankBlockEntity) level.getBlockEntity(sinkPos);
                // Fill the source tank; leave the sink empty. The pipe pulls from the fuller neighbour and pushes to the
                // emptier one (downhill flow), so fluid migrates source -> pipe -> sink.
                source.getTank().setStack(IFluidStack.of(Fluids.WATER.builtInRegistryHolder(), 10L * FluidConstants.BUCKET));
                startSrc = source.getTank().getFluidAmount();
                for (int i = 0; i < 60; i++) {
                    pipe.serverTick(level);
                }
                endSrc = source.getTank().getFluidAmount();
                endSink = sink.getTank().getFluidAmount();
                endPipe = pipe.bufferedFluid().getAmount();
            }
            long movedOut = startSrc - endSrc;
            ok = placed && endSink > 0L && movedOut > 0L;
            log(ok, "pipe: tank->mechanical_pipe->tank movedFromSource=" + movedOut + " inSink=" + endSink
                  + " inPipeBuffer=" + endPipe);
            level.removeBlock(srcPos, false);
            level.removeBlock(pipePos, false);
            level.removeBlock(sinkPos, false);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL pipe test threw", TAG, t);
        }
        return ok;
    }

    private static void log(boolean ok, String msg) {
        LOGGER.info("{} {} {}", TAG, ok ? "OK  " : "FAIL", msg);
    }
}
