package mekanism.fabric.content.machine;

import dev.architectury.registry.menu.MenuRegistry;
import java.util.function.BiFunction;
import java.util.function.Supplier;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a generic item+item&rarr;item / item&rarr;dual-item machine block — the host for the
 * Combiner ({@code item + item -> item}, backed by {@link CombinerMachineBlockEntity}) and the Precision Sawmill
 * ({@code item -> item + chance secondary}, backed by {@link SawmillMachineBlockEntity}). Both bundled blockstates declare
 * {@code facing} + {@code active} variants, so this block always carries both properties and faces the player on
 * placement. Parameterized by a block-entity factory + its {@link BlockEntityType} supplier so one class serves both
 * machines while spawning + ticking the correct (distinct) block-entity. Hosts a server ticker that dispatches to the
 * concrete block-entity's {@code serverTick}.
 */
public class DualItemMachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> beFactory;
    private final Supplier<BlockEntityType<?>> beType;

    /**
     * @param properties Block properties.
     * @param beFactory  Creates the concrete block-entity for this machine.
     * @param beType     Supplies this machine's block-entity type (used to gate the server ticker).
     */
    public DualItemMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends BlockEntity> beFactory,
          Supplier<BlockEntityType<?>> beType) {
        super(properties);
        this.beFactory = beFactory;
        this.beType = beType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CombinerMachineBlockEntity combiner) {
                MenuRegistry.openExtendedMenu(serverPlayer, combiner.menuProvider());
            } else if (be instanceof SawmillMachineBlockEntity sawmill) {
                MenuRegistry.openExtendedMenu(serverPlayer, sawmill.menuProvider());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return beFactory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != beType.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof CombinerMachineBlockEntity combiner) {
                combiner.serverTick();
            } else if (be instanceof SawmillMachineBlockEntity sawmill) {
                sawmill.serverTick();
            }
        };
    }
}
