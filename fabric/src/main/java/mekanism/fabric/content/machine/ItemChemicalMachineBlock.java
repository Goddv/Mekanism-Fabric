package mekanism.fabric.content.machine;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
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
 * Transitional Fabric bring-up: a generic item+chemical&rarr;item machine block (item input + chemical input &rarr; item
 * output) — the two-input sibling of {@link ChemicalMachineBlock}. Each instance carries the {@link Identifier} of the
 * {@link net.minecraft.world.item.crafting.RecipeType} it processes (e.g. {@code mekanism:compressing},
 * {@code mekanism:purifying}, {@code mekanism:injecting}, {@code mekanism:metallurgic_infusing}, {@code mekanism:painting});
 * the {@link ItemChemicalToItemMachineBlockEntity} reads it back via {@link #recipeTypeId()} and resolves the type by id.
 * Carries the {@code facing} property always, plus the {@code active} property only when the bundled model's blockstate
 * declares it (osmium_compressor/purification_chamber/chemical_injection_chamber/painting_machine have facing+active
 * variants, while metallurgic_infuser is facing-only) so the real model renders without missing-model spam. Hosts an
 * {@link ItemChemicalToItemMachineBlockEntity} with a server ticker.
 */
public class ItemChemicalMachineBlock extends Block implements EntityBlock {

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
     * @param hasActive    Whether the bundled blockstate declares an {@code active} variant (false for metallurgic_infuser).
     */
    public ItemChemicalMachineBlock(Properties properties, Identifier recipeTypeId, boolean hasActive) {
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
              && level.getBlockEntity(pos) instanceof ItemChemicalToItemMachineBlockEntity machine) {
            MenuRegistry.openExtendedMenu(serverPlayer, machine.menuProvider());
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemChemicalToItemMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricChemicalMachines.ITEM_CHEMICAL_BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ItemChemicalToItemMachineBlockEntity) be).serverTick();
    }
}
