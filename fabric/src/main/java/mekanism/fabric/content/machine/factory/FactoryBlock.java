package mekanism.fabric.content.machine.factory;

import dev.architectury.registry.menu.MenuRegistry;
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
 * Transitional Fabric bring-up: the host block for ALL 36 Mekanism factory blocks. Each instance carries its
 * {@link FactoryType} (which base recipe type + topology) and its process count (tier: basic=3/advanced=5/elite=7/
 * ultimate=9) — the {@link FactoryBlockEntity} reads them back via {@link #factoryType()} / {@link #processes()} to
 * size its slots + run the right per-process logic. Carries the {@code facing} + {@code active} blockstate properties the
 * bundled factory models declare (all factory blockstates have facing+active variants), faces the player on placement, and
 * hosts a {@link FactoryBlockEntity} with a server ticker. A {@link BlockEntityType} supplier is passed so each
 * process-count gets its own BE type while one block class serves every factory.
 */
public class FactoryBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final FactoryType factoryType;
    private final int processes;
    private final Supplier<BlockEntityType<?>> beType;

    public FactoryBlock(Properties properties, FactoryType factoryType, int processes, Supplier<BlockEntityType<?>> beType) {
        super(properties);
        this.factoryType = factoryType;
        this.processes = processes;
        this.beType = beType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    public FactoryType factoryType() {
        return factoryType;
    }

    public int processes() {
        return processes;
    }

    /** The block-entity type used by this factory (gates the server ticker + passed to the BE constructor). */
    public BlockEntityType<?> beType() {
        return beType.get();
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
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
              && level.getBlockEntity(pos) instanceof FactoryBlockEntity factory) {
            MenuRegistry.openExtendedMenu(serverPlayer, factory.menuProvider());
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FactoryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != beType.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((FactoryBlockEntity) be).serverTick();
    }
}
