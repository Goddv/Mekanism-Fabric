package mekanism.fabric.content.energy;

import java.util.List;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a minimal functional energy block-entity that stores energy in the hoisted
 * {@link BasicEnergyContainer} and exposes it as an {@link mekanism.api.energy.IStrictEnergyHandler} (via
 * {@link IMekanismStrictEnergyHandler}). Registered against {@code MekanismFabricEnergy.SIDED} so neighbours can query its
 * energy capability — the foundation pattern for real Mekanism machines on Fabric.
 */
public class DemoEnergyBlockEntity extends BlockEntity implements IMekanismStrictEnergyHandler {

    private final BasicEnergyContainer energy = BasicEnergyContainer.create(1_000_000L, this);
    private final List<IEnergyContainer> containers = List.of(energy);

    public DemoEnergyBlockEntity(BlockPos pos, BlockState state) {
        super(FabricEnergyBlockDemo.BE_TYPE.get(), pos, state);
    }

    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return containers;
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
