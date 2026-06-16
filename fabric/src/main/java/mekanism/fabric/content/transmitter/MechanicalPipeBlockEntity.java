package mekanism.fabric.content.transmitter;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.ISimpleFluidHandler;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.fabric.fluid.MekanismFabricFluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Mechanical Pipe transmitter ({@code mekanism:basic_mechanical_pipe}): a FLUID adjacent-relay. The fluid analog of
 * the {@link PressurizedTubeBlockEntity} chemical tube — same pull/push shape, just over {@link IFluidStack} /
 * {@link ISimpleFluidHandler} (the fluid capability {@link MekanismFabricFluid#SIDED}) instead of chemical:
 *
 * <ol>
 *     <li><b>Pull</b> fluid from any adjacent {@link ISimpleFluidHandler} that holds strictly more than this pipe's
 *     buffer (downhill flow — keeps fluid moving one way without pipe&harr;pipe sloshing) into the internal
 *     {@link BasicFluidTank}.</li>
 *     <li><b>Push</b> fluid from the buffer into adjacent acceptors that hold strictly LESS than our buffer (mirroring the
 *     pull-from-higher rule keeps flow one-directional — we never push back into a fuller neighbour we just pulled from,
 *     only forward into emptier acceptors).</li>
 * </ol>
 *
 * The buffer is exposed as the fluid capability ({@link ISimpleFluidHandler}) so pipes chain, upstream sources can push
 * into it directly, and machines/tanks connect. Not the real Mekanism transmitter grid (no routing graph, no multipart
 * connection state) — the deliberately-simple transitional adjacent-relay.
 */
public class MechanicalPipeBlockEntity extends TransmitterBlockEntity implements ISimpleFluidHandler {

    /** 10 buckets of internal buffer (droplets — the Fabric fluid unit, as in {@code FabricFluidSelfTest}). */
    private static final int CAPACITY = (int) (10L * FluidConstants.BUCKET);
    /** Per-tick transfer budget (2 buckets), mirroring the tube's rate-limited relay. */
    private static final long TRANSFER_RATE = 2L * FluidConstants.BUCKET;

    private final IExtendedFluidTank buffer = BasicFluidTank.create(CAPACITY, this::setChanged);

    public MechanicalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(FabricTransmitters.MECHANICAL_PIPE_BE_TYPE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        pull(level);
        push(level);
    }

    /** Pull fluid into the buffer from neighbours holding strictly more (downhill flow). */
    private void pull(ServerLevel level) {
        long budget = Math.min(TRANSFER_RATE, buffer.getNeeded());
        if (budget <= 0L) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            ISimpleFluidHandler source = MekanismFabricFluid.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (source == null || totalStored(source) <= buffer.getFluidAmount()) {
                continue;
            }
            // Walk the source's tanks; extract from each (simulate to learn the fluid type our buffer can accept, then
            // commit only what fits) until our per-tick budget runs out.
            for (int tank = 0; tank < source.getFluidTanks() && budget > 0L; tank++) {
                IFluidStack drained = source.extractFluid(tank, budget, Action.SIMULATE);
                if (drained.isEmpty()) {
                    continue;
                }
                IFluidStack remainder = buffer.insert(drained, Action.SIMULATE, AutomationType.INTERNAL);
                long fits = drained.getAmount() - remainder.getAmount();
                if (fits <= 0L) {
                    continue;
                }
                IFluidStack extracted = source.extractFluid(tank, fits, Action.EXECUTE);
                if (!extracted.isEmpty()) {
                    buffer.insert(extracted, Action.EXECUTE, AutomationType.INTERNAL);
                    budget -= extracted.getAmount();
                }
            }
        }
    }

    /**
     * Push buffered fluid "downhill" into adjacent acceptors that hold strictly LESS than our buffer. Mirroring the
     * pull-from-higher rule keeps flow one-directional: we never push back into a fuller neighbour we just pulled from
     * (which would slosh the fluid straight back), only forward into emptier acceptors.
     */
    private void push(ServerLevel level) {
        if (buffer.isEmpty()) {
            return;
        }
        long budget = TRANSFER_RATE;
        for (Direction dir : Direction.values()) {
            if (budget <= 0L || buffer.isEmpty()) {
                break;
            }
            ISimpleFluidHandler sink = MekanismFabricFluid.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (sink == null || totalStored(sink) >= buffer.getFluidAmount()) {
                continue;
            }
            IFluidStack offered = buffer.extract(budget, Action.SIMULATE, AutomationType.INTERNAL);
            if (offered.isEmpty()) {
                break;
            }
            // Offer the buffered fluid to each of the sink's tanks; commit whatever each accepts.
            for (int tank = 0; tank < sink.getFluidTanks() && !offered.isEmpty(); tank++) {
                IFluidStack rejected = sink.insertFluid(tank, offered, Action.EXECUTE);
                long accepted = offered.getAmount() - rejected.getAmount();
                if (accepted > 0L) {
                    buffer.extract(accepted, Action.EXECUTE, AutomationType.INTERNAL);
                    budget -= accepted;
                    offered = buffer.extract(budget, Action.SIMULATE, AutomationType.INTERNAL);
                }
            }
        }
    }

    private static long totalStored(ISimpleFluidHandler handler) {
        long sum = 0L;
        for (int i = 0; i < handler.getFluidTanks(); i++) {
            sum += handler.getFluidInTank(i).getAmount();
        }
        return sum;
    }

    // ---- ISimpleFluidHandler: expose the single buffer as the fluid capability ----

    @Override
    public int getFluidTanks() {
        return 1;
    }

    @Override
    public IFluidStack getFluidInTank(int tank) {
        return tank == 0 ? buffer.getFluid() : IFluidStack.empty();
    }

    @Override
    public IFluidStack insertFluid(int tank, IFluidStack stack, Action action) {
        return tank == 0 ? buffer.insert(stack, action, AutomationType.EXTERNAL) : stack;
    }

    @Override
    public IFluidStack extractFluid(int tank, long amount, Action action) {
        return tank == 0 ? buffer.extract(amount, action, AutomationType.EXTERNAL) : IFluidStack.empty();
    }

    /** Direct buffer access for the self-test. */
    public IFluidStack bufferedFluid() {
        return buffer.getFluid();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output.child("buffer"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("buffer").ifPresent(buffer::deserialize);
    }
}
