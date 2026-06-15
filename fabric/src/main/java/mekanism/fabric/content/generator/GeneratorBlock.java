package mekanism.fabric.content.generator;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Transitional Fabric bring-up: the block hosting an {@link AbstractGeneratorBlockEntity}. One configurable class backs
 * all four generators because their blockstate shapes differ (Solar: none; Wind/Bio: {@code facing}; Heat: {@code
 * facing}+{@code active}) — matching the bundled {@code mekanismgenerators} blockstate JSONs so the real models render
 * with the right orientation. Fuel-burning generators (Heat/Bio) additionally accept a right-click fuel insert into
 * their slot, like the demo generator. A server ticker drives {@link AbstractGeneratorBlockEntity#serverTick}.
 */
public class GeneratorBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * Per-construction blockstate-shape handoff. {@link #createBlockStateDefinition} runs inside the {@link Block}
     * super-constructor — before this subclass's instance fields are assigned — so the shape can't be read from a field
     * there. We stash it in a static immediately before {@code super(...)} and read it back in
     * {@code createBlockStateDefinition}. Safe because Architectury finalizes block registration single-threaded during
     * mod init (no concurrent generator construction).
     */
    private static boolean pendingHasFacing;
    private static boolean pendingHasActive;

    private final BiFunction<BlockPos, BlockState, ? extends AbstractGeneratorBlockEntity> beFactory;
    private final Supplier<BlockEntityType<?>> beType;
    private final boolean hasFacing;
    private final boolean hasActive;
    private final boolean acceptsFuel;

    public GeneratorBlock(Properties properties,
          BiFunction<BlockPos, BlockState, ? extends AbstractGeneratorBlockEntity> beFactory,
          Supplier<BlockEntityType<?>> beType, boolean hasFacing, boolean hasActive, boolean acceptsFuel) {
        super(stash(properties, hasFacing, hasActive));
        this.beFactory = beFactory;
        this.beType = beType;
        this.hasFacing = hasFacing;
        this.hasActive = hasActive;
        this.acceptsFuel = acceptsFuel;
        BlockState defaultState = stateDefinition.any();
        if (hasFacing) {
            defaultState = defaultState.setValue(FACING, Direction.NORTH);
        }
        if (hasActive) {
            defaultState = defaultState.setValue(ACTIVE, false);
        }
        registerDefaultState(defaultState);
    }

    /** Stash the shape flags for {@link #createBlockStateDefinition} (called from the super-constructor) and pass props through. */
    private static Properties stash(Properties properties, boolean hasFacing, boolean hasActive) {
        pendingHasFacing = hasFacing;
        pendingHasActive = hasActive;
        return properties;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if (pendingHasFacing) {
            builder.add(FACING);
        }
        if (pendingHasActive) {
            builder.add(ACTIVE);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        if (hasFacing) {
            state = state.setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!acceptsFuel) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AbstractFuelGeneratorBlockEntity generator) || !generator.canPlaceItem(0, stack)) {
            return InteractionResult.PASS;
        }
        // Return SUCCESS on both sides (consistent interaction); only the server mutates the fuel slot.
        if (!level.isClientSide() && generator.addFuel(stack) && !player.getAbilities().instabuild) {
            stack.shrink(1);
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
        return (lvl, pos, st, be) -> ((AbstractGeneratorBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
