package mekanism.fabric.content.transmitter;

import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import mekanism.fabric.content.power.EnergyTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * The Universal Cable transmitter ({@code mekanism:basic_universal_cable}): an ENERGY adjacent-relay. Reuses the exact
 * proven {@code CableBlockEntity} logic — a small {@link BasicEnergyContainer} buffer exposed as the strict-energy
 * capability (so upstream blocks can push into it), and each server tick {@link EnergyTransferHelper#pull pulls} energy
 * "downhill" from higher-energy neighbours (generators / fuller cables) into the buffer; machines then pull from us.
 * Pulling only from higher neighbours keeps energy flowing one way without cable&harr;cable sloshing.
 */
public class UniversalCableBlockEntity extends TransmitterBlockEntity implements IMekanismStrictEnergyHandler {

    private static final long CAPACITY = 8_000L;
    private static final long TRANSFER_RATE = 5_000L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(CAPACITY, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);

    public UniversalCableBlockEntity(BlockPos pos, BlockState state) {
        super(FabricTransmitters.UNIVERSAL_CABLE_BE_TYPE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        EnergyTransferHelper.pull(level, worldPosition, energy, TRANSFER_RATE, true);
    }

    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
    }

    /** Direct buffer access for the self-test. */
    public long bufferedEnergy() {
        return energy.getEnergy();
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.deserialize(input);
    }
}
