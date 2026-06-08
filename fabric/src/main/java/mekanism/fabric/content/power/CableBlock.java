package mekanism.fabric.content.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block hosting {@link CableBlockEntity}, an energy transmitter that relays power
 * between adjacent generators, cables, and machines.
 */
public class CableBlock extends Block implements EntityBlock {

    public CableBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricPowerInfrastructure.CABLE_BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((CableBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
