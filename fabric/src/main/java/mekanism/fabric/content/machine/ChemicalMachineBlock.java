package mekanism.fabric.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a generic item&rarr;chemical machine block (item input &rarr; chemical output), the
 * chemical-output sibling of {@link MachineBlock}. Each instance carries the {@link Identifier} of the
 * {@link net.minecraft.world.item.crafting.RecipeType} it processes (e.g. {@code mekanism:oxidizing},
 * {@code mekanism:pigment_extracting}, {@code mekanism:chemical_conversion}); the {@link ChemicalMachineBlockEntity}
 * reads it back via {@link #recipeTypeId()} and resolves the type by id. Carries the {@code facing} property always, plus
 * the {@code active} property only when the bundled model's blockstate declares it (e.g. {@code pigment_extractor.json}
 * has facing+active variants, while {@code chemical_oxidizer.json} is facing-only) so the real model renders without
 * missing-model spam. Hosts a {@link ChemicalMachineBlockEntity} with a server ticker.
 */
public class ChemicalMachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    // createBlockStateDefinition runs inside super(properties) — BEFORE the subclass `hasActive` field is assigned — so
    // the flag is conveyed via this thread-local, set just before super() runs and consumed during the super call.
    private static final ThreadLocal<Boolean> HAS_ACTIVE_HOLDER = new ThreadLocal<>();

    private final Identifier recipeTypeId;
    private final boolean hasActive;

    /**
     * @param properties   Block properties.
     * @param recipeTypeId The {@code RecipeType} id this machine processes.
     * @param hasActive    Whether the bundled blockstate declares an {@code active} variant (true for pigment_extractor).
     */
    public ChemicalMachineBlock(Properties properties, Identifier recipeTypeId, boolean hasActive) {
        super(withActiveFlag(properties, hasActive));
        HAS_ACTIVE_HOLDER.remove();
        this.recipeTypeId = recipeTypeId;
        this.hasActive = hasActive;
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (hasActive) {
            state = state.setValue(ACTIVE, false);
        }
        registerDefaultState(state);
    }

    /** Stashes {@code hasActive} so {@link #createBlockStateDefinition} (called from {@code super}) can read it. */
    private static Properties withActiveFlag(Properties properties, boolean hasActive) {
        HAS_ACTIVE_HOLDER.set(hasActive);
        return properties;
    }

    /** The id of the {@code RecipeType} this machine resolves and processes each tick. */
    public Identifier recipeTypeId() {
        return recipeTypeId;
    }

    /** Whether this block carries the {@code active} blockstate property (and the BE should toggle it while processing). */
    public boolean hasActive() {
        return hasActive;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        // Reads the thread-local because this runs from super(), before `hasActive` is assigned. Defaults to false for
        // any path that constructs without going through our ctor.
        Boolean flag = HAS_ACTIVE_HOLDER.get();
        if (Boolean.TRUE.equals(flag)) {
            builder.add(ACTIVE);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricChemicalMachines.BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ChemicalMachineBlockEntity) be).serverTick();
    }
}
