package mekanism.fabric.content.transmitter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the shared host block for the five Mekanism transmitters (Universal Cable, Pressurized
 * Tube, Thermodynamic Conductor, Logistical Transporter, Mechanical Pipe). Each is a passive {@link EntityBlock} whose
 * block-entity is an <em>adjacent-relay</em> — it moves its resource (energy/chemical/heat/items/fluid) between
 * neighbouring providers and acceptors every server tick. This is the deliberately-simple transitional model: NOT the
 * real Mekanism transmitter-network graph (that is deferred), but enough to carry a resource across a line of
 * transmitters one hop at a time.
 *
 * <p><b>Connected rendering (vanilla fence-style).</b> The block carries six per-side {@link BooleanProperty}s
 * (NORTH..DOWN). A side is <em>connected</em> when the neighbour is any transmitter block-entity (transmitter-to-
 * transmitter) OR exposes this type's {@link TransmitterRenderType#lookup() capability} on the touching face
 * (transmitter-to-machine/tank). Connections are computed server-side in {@link #getStateForPlacement} and recomputed in
 * {@link #neighborChanged}/{@link #updateShape}; the blockstate syncs the six booleans to the client, where a multipart
 * model renders a center core plus an arm per connected side. The {@link VoxelShape} mirrors the model (core + arms).
 *
 * <p>Parameterized by a block-entity factory, the {@link BlockEntityType} supplier (resolved lazily, since the type
 * registers after the blocks), and the {@link TransmitterRenderType}, so all five transmitters share this one class.
 */
public class TransmitterBlock extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);

    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    // The center core is a 6x6x6 box; each connected side extends a 4x4 arm out to the block edge. Logical 0..16 space.
    private static final double CORE_MIN = 5.0D;
    private static final double CORE_MAX = 11.0D;
    private static final double ARM_MIN = 6.0D;
    private static final double ARM_MAX = 10.0D;

    private static final VoxelShape CORE_SHAPE = Block.box(CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX);
    private static final Map<Direction, VoxelShape> ARM_SHAPE = new EnumMap<>(Direction.class);

    static {
        ARM_SHAPE.put(Direction.DOWN, Block.box(ARM_MIN, 0.0D, ARM_MIN, ARM_MAX, CORE_MIN, ARM_MAX));
        ARM_SHAPE.put(Direction.UP, Block.box(ARM_MIN, CORE_MAX, ARM_MIN, ARM_MAX, 16.0D, ARM_MAX));
        ARM_SHAPE.put(Direction.NORTH, Block.box(ARM_MIN, ARM_MIN, 0.0D, ARM_MAX, ARM_MAX, CORE_MIN));
        ARM_SHAPE.put(Direction.SOUTH, Block.box(ARM_MIN, ARM_MIN, CORE_MAX, ARM_MAX, ARM_MAX, 16.0D));
        ARM_SHAPE.put(Direction.WEST, Block.box(0.0D, ARM_MIN, ARM_MIN, CORE_MIN, ARM_MAX, ARM_MAX));
        ARM_SHAPE.put(Direction.EAST, Block.box(CORE_MAX, ARM_MIN, ARM_MIN, 16.0D, ARM_MAX, ARM_MAX));
    }

    // 64 connection combinations (one bit per direction) -> cached shape (core + connected arms).
    private final VoxelShape[] shapeByConnections = new VoxelShape[64];

    private final BiFunction<BlockPos, BlockState, ? extends TransmitterBlockEntity> factory;
    private final Supplier<BlockEntityType<?>> beType;
    private final TransmitterRenderType renderType;

    public TransmitterBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends TransmitterBlockEntity> factory,
          Supplier<BlockEntityType<?>> beType, TransmitterRenderType renderType) {
        super(properties);
        this.factory = factory;
        this.beType = beType;
        this.renderType = renderType;
        registerDefaultState(stateDefinition.any()
              .setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false)
              .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
        precomputeShapes();
    }

    public TransmitterRenderType renderType() {
        return renderType;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // ---- connection computation ----

    /**
     * Decides whether {@code transmitterPos} connects to its neighbour in {@code side}. True when the neighbour is any
     * transmitter block-entity (so two transmitters always join) OR — for a non-transmitter — when it exposes this
     * type's capability on the face touching us ({@code side.getOpposite()}). Capability queries are server-side.
     */
    private boolean connectsTo(Level level, BlockPos transmitterPos, Direction side) {
        BlockPos neighborPos = transmitterPos.relative(side);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof TransmitterBlockEntity) {
            return true;
        }
        // Capability detection only works server-side (BlockApiLookup needs a Level with caps populated). On the client
        // the six booleans already arrived via the synced blockstate, so this path is server-only.
        if (level instanceof ServerLevel) {
            return renderType.lookup().find(level, neighborPos, side.getOpposite()) != null;
        }
        return false;
    }

    /** A fresh state with all six connection booleans computed from the world around {@code pos}. */
    private BlockState withConnections(BlockState state, Level level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            state = state.setValue(entry.getValue(), connectsTo(level, pos, entry.getKey()));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTick, BlockPos pos,
          Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // updateShape only sees a LevelReader (no capability access), so resolve the changed side cheaply: a same-kind
        // transmitter neighbour is detectable from its BlockState alone. Capability-based connections (to machines/
        // tanks) are handled authoritatively in neighborChanged, which has a full Level.
        boolean connected = neighborState.getBlock() instanceof TransmitterBlock
              || (level instanceof Level full && connectsTo(full, pos, direction));
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connected);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
          boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    // ---- collision/outline shape mirrors the connected model ----

    private void precomputeShapes() {
        for (int mask = 0; mask < shapeByConnections.length; mask++) {
            VoxelShape shape = CORE_SHAPE;
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.ordinal())) != 0) {
                    shape = Shapes.or(shape, ARM_SHAPE.get(dir));
                }
            }
            shapeByConnections[mask] = shape;
        }
    }

    private int connectionMask(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeByConnections[connectionMask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeByConnections[connectionMask(state)];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server-side relay only; nothing to tick client-side. Match the BE type to avoid mis-ticking other blocks.
        if (level.isClientSide() || type != beType.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((TransmitterBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
