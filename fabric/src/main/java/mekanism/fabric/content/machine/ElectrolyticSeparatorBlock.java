package mekanism.fabric.content.machine;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the Electrolytic Separator machine block (fluid input &rarr; two chemical outputs) — the
 * first FLUID-input machine on Fabric. Carries ONLY the {@code facing} blockstate property (no {@code active}), exactly
 * matching the real bundled {@code electrolytic_separator.json} blockstate (facing-only variants) so the real model
 * renders with no missing-model spam. Hosts an {@link ElectrolyticSeparatorBlockEntity} with a server ticker;
 * {@code useWithoutItem} opens its GUI (fluid input bar + two chemical output bars + energy bar).
 */
public class ElectrolyticSeparatorBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ElectrolyticSeparatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
              && level.getBlockEntity(pos) instanceof ElectrolyticSeparatorBlockEntity machine) {
            MenuRegistry.openExtendedMenu(serverPlayer, machine.menuProvider());
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyticSeparatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricElectrolyticSeparator.BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ElectrolyticSeparatorBlockEntity) be).serverTick();
    }
}
