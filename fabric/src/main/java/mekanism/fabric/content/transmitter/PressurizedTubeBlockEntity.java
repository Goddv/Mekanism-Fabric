package mekanism.fabric.content.transmitter;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.fabric.chemical.MekanismFabricChemical;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The Pressurized Tube transmitter ({@code mekanism:basic_pressurized_tube}): a CHEMICAL adjacent-relay. Mirrors the
 * energy cable's pull/push shape over the chemical capability ({@link MekanismFabricChemical#SIDED}):
 *
 * <ol>
 *     <li><b>Pull</b> chemical from any adjacent {@link IChemicalHandler} that holds strictly more than this tube's
 *     buffer (downhill flow — keeps chemical moving one way without tube&harr;tube sloshing) into the internal
 *     {@link BasicChemicalTank}.</li>
 *     <li><b>Push</b> chemical from the buffer into adjacent acceptors that have room.</li>
 * </ol>
 *
 * The buffer is exposed as the chemical capability so upstream sources can also push into it directly. One-directional
 * flow (pull-from-higher, push-to-anyone) avoids back-and-forth. Not the real Mekanism transmitter grid.
 */
public class PressurizedTubeBlockEntity extends TransmitterBlockEntity implements IMekanismChemicalHandler {

    private static final long CAPACITY = 4_000L;
    private static final long TRANSFER_RATE = 2_000L;

    private final IChemicalTank buffer = BasicChemicalTank.create(CAPACITY, this);
    private final List<IChemicalTank> tanks = List.of(buffer);

    public PressurizedTubeBlockEntity(BlockPos pos, BlockState state) {
        super(FabricTransmitters.PRESSURIZED_TUBE_BE_TYPE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        pull(level);
        push(level);
    }

    /** Pull chemical into the buffer from neighbours holding strictly more (downhill flow). */
    private void pull(ServerLevel level) {
        long budget = Math.min(TRANSFER_RATE, buffer.getNeeded());
        if (budget <= 0L) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            IChemicalHandler source = MekanismFabricChemical.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (source == null || totalStored(source) <= buffer.getStored()) {
                continue;
            }
            // Simulate extraction first so the chemical type matches what our buffer can accept, then commit only what fits.
            ChemicalStack drained = source.extractChemical(budget, Action.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }
            ChemicalStack remainder = buffer.insert(drained, Action.SIMULATE, AutomationType.INTERNAL);
            long fits = drained.amount() - remainder.amount();
            if (fits <= 0L) {
                continue;
            }
            ChemicalStack extracted = source.extractChemical(fits, Action.EXECUTE);
            if (!extracted.isEmpty()) {
                buffer.insert(extracted, Action.EXECUTE, AutomationType.INTERNAL);
                budget -= extracted.amount();
            }
        }
    }

    /**
     * Push buffered chemical "downhill" into adjacent acceptors that hold strictly LESS than our buffer. Mirroring the
     * pull-from-higher rule keeps flow one-directional: we never push back into a fuller neighbour we just pulled from
     * (which would slosh the chemical straight back), only forward into emptier acceptors.
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
            IChemicalHandler sink = MekanismFabricChemical.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (sink == null || totalStored(sink) >= buffer.getStored()) {
                continue;
            }
            ChemicalStack offered = buffer.extract(budget, Action.SIMULATE, AutomationType.INTERNAL);
            if (offered.isEmpty()) {
                break;
            }
            ChemicalStack rejected = sink.insertChemical(offered, Action.EXECUTE);
            long accepted = offered.amount() - rejected.amount();
            if (accepted > 0L) {
                buffer.extract(accepted, Action.EXECUTE, AutomationType.INTERNAL);
                budget -= accepted;
            }
        }
    }

    private static long totalStored(IChemicalHandler handler) {
        long sum = 0L;
        for (int i = 0; i < handler.getChemicalTanks(); i++) {
            sum += handler.getChemicalInTank(i).amount();
        }
        return sum;
    }

    @Override
    public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
        return tanks;
    }

    /** Direct buffer access for the self-test. */
    public ChemicalStack bufferedChemical() {
        return buffer.getStack();
    }

    @Override
    public void onContentsChanged() {
        setChanged();
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
