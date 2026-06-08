package mekanism.fabric.content.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the block hosting {@link GeneratorBlockEntity}. Right-clicking with a furnace fuel item
 * (coal, charcoal, blocks of coal, etc.) feeds one into the generator's fuel slot; it then produces energy and pushes it
 * to adjacent cables/machines.
 */
public class GeneratorBlock extends Block implements EntityBlock {

    public GeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.fuelValues().isFuel(stack)) {
            return InteractionResult.PASS;
        }
        // Return SUCCESS on both sides (consistent interaction); only the server mutates the fuel slot.
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator && generator.addFuel(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricPowerInfrastructure.GENERATOR_BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((GeneratorBlockEntity) be).serverTick((net.minecraft.server.level.ServerLevel) lvl);
    }
}
