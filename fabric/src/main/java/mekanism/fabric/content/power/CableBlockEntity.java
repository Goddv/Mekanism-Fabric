package mekanism.fabric.content.power;

import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: an energy cable. Holds a small buffer (exposed as the strict-energy capability so
 * upstream blocks push into it) and each server tick redistributes that buffer to lower-fill neighbours via
 * {@link EnergyPushHelper} — a "bucket-brigade" that carries energy generator&rarr;cable&rarr;…&rarr;machine. Not the
 * real Mekanism transmitter-network grid (that subsystem is hoisted later), but enough to wire power across distance.
 */
public class CableBlockEntity extends BlockEntity implements IMekanismStrictEnergyHandler {

    private static final long CAPACITY = 8_000L;
    private static final long TRANSFER_RATE = 5_000L;

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(CAPACITY, this);
    private final List<IEnergyContainer> energyContainers = List.of(energy);

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(FabricPowerInfrastructure.CABLE_BE_TYPE.get(), pos, state);
    }

    public void serverTick(ServerLevel level) {
        EnergyPushHelper.pushToNeighbors(level, worldPosition, energy, TRANSFER_RATE);
    }

    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return energyContainers;
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
