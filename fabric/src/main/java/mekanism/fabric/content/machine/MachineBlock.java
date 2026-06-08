package mekanism.fabric.content.machine;

import java.util.function.Supplier;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.crafting.RecipeType;
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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: a real Mekanism machine block. Carries the {@code facing}/{@code active} blockstate
 * properties the real machine models expect (so the actual enrichment_chamber/crusher/etc. assets render with correct
 * orientation + active glow), faces the player on placement, and hosts a {@link MachineBlockEntity} with a server ticker.
 */
public class MachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    @Nullable
    private final Supplier<RecipeType<ItemStackToItemStackRecipe>> recipeType;

    public MachineBlock(Properties properties, @Nullable Supplier<RecipeType<ItemStackToItemStackRecipe>> recipeType) {
        super(properties);
        this.recipeType = recipeType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    /**
     * The item&rarr;item recipe type this machine processes, or {@code null} if it has no wired recipe type yet (e.g. the
     * compressor/combiner, which need chemical/dual-item recipe types not yet ported). Resolved lazily so it can be wired
     * before the recipe registries are populated.
     */
    @Nullable
    public RecipeType<ItemStackToItemStackRecipe> recipeType() {
        return recipeType == null ? null : recipeType.get();
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
              && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            // Vanilla open path: sends the OpenScreen packet keyed to the menu type, which the client resolves via
            // the MenuScreens registration in MekanismFabricClient. (Avoids mixing Architectury's open path with
            // vanilla screen registration.)
            serverPlayer.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricRealMachines.BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((MachineBlockEntity) be).serverTick(st);
    }
}
