package mekanism.fabric.content.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block that carries {@link DemoEnergyBlockEntity}.
 */
public class DemoEnergyBlock extends Block implements EntityBlock {

    public DemoEnergyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemoEnergyBlockEntity(pos, state);
    }
}
