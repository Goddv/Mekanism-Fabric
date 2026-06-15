package mekanism.fabric.content.transmitter;

import java.util.List;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.heat.IMekanismHeatHandler;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.fabric.heat.MekanismFabricHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The Thermodynamic Conductor transmitter ({@code mekanism:basic_thermodynamic_conductor}): a HEAT adjacent-relay. Holds
 * a {@link BasicHeatCapacitor} exposed as the heat capability ({@link MekanismFabricHeat#SIDED}), and each server tick
 * equalizes heat with adjacent {@link IHeatHandler}s — heat always flows hot&rarr;cold. For each neighbour, a fraction
 * (the {@link #CONDUCTION conduction} coefficient) of the temperature difference is moved: when the neighbour is hotter
 * it loses heat to us, when it is colder we lose heat to it. Both sides' capacitors are flushed
 * ({@link BasicHeatCapacitor#update()}) so the temperature change is observable the same tick.
 *
 * <p>Transitional + simple: just demonstrates heat moving between two adjacent heat handlers through the conductor. Not
 * the real Mekanism heat-network simulation (proper conduction/insulation coefficients, ambient loss, etc.).
 */
public class ThermodynamicConductorBlockEntity extends TransmitterBlockEntity implements IMekanismHeatHandler {

    private static final double HEAT_CAPACITY = 1.0D;
    /** Fraction of each temperature difference moved per neighbour per tick. */
    private static final double CONDUCTION = 0.2D;

    private final BasicHeatCapacitor heat = BasicHeatCapacitor.create(HEAT_CAPACITY, null, this);
    private final List<IHeatCapacitor> capacitors = List.of(heat);

    public ThermodynamicConductorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricTransmitters.THERMODYNAMIC_CONDUCTOR_BE_TYPE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (Direction dir : Direction.values()) {
            IHeatHandler neighbour = MekanismFabricHeat.SIDED.find(level, worldPosition.relative(dir), dir.getOpposite());
            if (neighbour == null || neighbour.getHeatCapacitorCount() == 0) {
                continue;
            }
            double ourTemp = heat.getTemperature();
            double theirTemp = neighbour.getTotalTemperature();
            double diff = theirTemp - ourTemp;
            if (Math.abs(diff) < 1.0E-4) {
                continue;
            }
            // Move heat hot->cold. Cap the energy by the limiting side's capacity so neither side overshoots equilibrium.
            double limitingCapacity = Math.min(heat.getHeatCapacity(), neighbour.getTotalHeatCapacity());
            double transfer = diff * CONDUCTION * limitingCapacity;
            // transfer > 0 => neighbour hotter: it loses heat, we gain. transfer < 0 => we lose, neighbour gains.
            neighbour.handleHeat(-transfer);
            heat.handleHeat(transfer);
            flush(neighbour);
        }
        heat.update();
    }

    /** Flush a neighbour's buffered heat if it is a Mekanism heat handler (real ticking tiles flush on their own tick). */
    private static void flush(IHeatHandler neighbour) {
        if (neighbour instanceof IMekanismHeatHandler mek) {
            for (IHeatCapacitor capacitor : mek.getHeatCapacitors(null)) {
                if (capacitor instanceof BasicHeatCapacitor basic) {
                    basic.update();
                }
            }
        }
    }

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return capacitors;
    }

    /** Direct temperature access for the self-test. */
    public double temperature() {
        return heat.getTemperature();
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        heat.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat.deserialize(input);
    }
}
