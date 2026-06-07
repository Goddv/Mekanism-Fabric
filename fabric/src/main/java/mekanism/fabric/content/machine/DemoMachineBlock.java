package mekanism.fabric.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block carrying {@link DemoMachineBlockEntity}, with a server-side ticker driving the
 * process loop.
 */
public class DemoMachineBlock extends Block implements EntityBlock {

    public DemoMachineBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemoMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricMachineDemo.BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((DemoMachineBlockEntity) be).serverTick();
    }
}
