package mekanism.fabric.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the Chemical Crystallizer machine block (chemical input &rarr; item output) — the
 * REVERSED-topology sibling of {@link ChemicalMachineBlock}. Carries ONLY the {@code facing} blockstate property (no
 * {@code active}), exactly matching the shared {@code chemical_crystallizer.json} blockstate (facing-only variants) so
 * the real bundled model renders with no missing-model spam. Hosts a {@link ChemicalToItemMachineBlockEntity} with a
 * server ticker.
 */
public class ChemicalToItemMachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ChemicalToItemMachineBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalToItemMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != FabricChemicalMachines.CRYSTALLIZER_BE_TYPE.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((ChemicalToItemMachineBlockEntity) be).serverTick();
    }
}
