package mekanism.common.block;

import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateFluidLoggableBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral core of {@link mekanism.common.block.BlockMekanism}. Every override here uses a TRUE VANILLA 26.1
 * signature (javap-verified); the NeoForge layer ({@code BlockMekanism}) re-adds the {@code IBlockExtension}-shaped
 * overrides and the members whose bodies need NeoForge-only types.
 *
 * <p><b>HOIST GATE — read before extending this directly.</b> {@code BlockMekanism} (the NeoForge layer) keeps a set of
 * overrides that this base intentionally does NOT carry: {@code getPistonPushReaction(BlockState)},
 * {@code getCloneItemStack} (copyBlockData), {@code onBlockExploded} (meltdown), {@code rotate} (4-arg, LevelAccessor),
 * {@code setPlacedBy} (bounding placement + security owner), {@code hasAnalogOutputSignal}/{@code getAnalogOutputSignal},
 * {@code getDestroyProgress} (security/radiation gating), {@code animateTick} (radiation particles) and
 * {@code genericClientActivated}. A leaf that extends this base DIRECTLY (not via {@code BlockMekanism}) inherits none of
 * them and falls through to vanilla. That is only safe for a block with NO BlockEntity and NO exposed
 * owner/security/radioactive capability — see {@code BlockResource}, which was audited member-by-member to prove each
 * skipped override degenerates to its vanilla super for a tile-less block. Any future tile-bearing or multiblock leaf
 * hoisted onto this base MUST repeat that audit (or extend {@code BlockMekanism} instead) — otherwise it silently loses
 * meltdown handling, piston protection, security break-gating, etc.
 */
public abstract class BlockMekanismBase extends Block {

    protected BlockMekanismBase(BlockBehaviour.Properties properties) {
        super(BlockStateHelper.applyLightLevelAdjustments(properties));
        registerDefaultState(BlockStateHelper.getDefaultState(stateDefinition.any()));
    }

    @Override
    protected boolean canBeReplaced(@NotNull BlockState state, @NotNull Fluid fluid) {
        return false;
    }

    @Override
    protected boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
        boolean triggered = super.triggerEvent(state, level, pos, id, param);
        if (this instanceof IHasTileEntity<?> hasTileEntity) {
            return hasTileEntity.triggerBlockEntityEvent(state, level, pos, id, param);
        }
        return triggered;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        BlockStateHelper.fillBlockStateContainer(this, builder);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return BlockStateHelper.getStateForPlacement(super.getStateForPlacement(context), context);
    }

    @NotNull
    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getBlock() instanceof IStateFluidLoggableBase fluidLoggable) {
            return fluidLoggable.getFluid(state);
        }
        return super.getFluidState(state);
    }

    @NotNull
    @Override
    protected BlockState updateShape(@NotNull BlockState state, @NotNull LevelReader level, @NotNull ScheduledTickAccess scheduledTickAccess, @NotNull BlockPos currentPos,
          @NotNull Direction facing, @NotNull BlockPos facingPos, @NotNull BlockState facingState, @NotNull RandomSource random) {
        if (state.getBlock() instanceof IStateFluidLoggableBase fluidLoggable) {
            fluidLoggable.updateFluids(level, currentPos, state, scheduledTickAccess);
        }
        return super.updateShape(state, level, scheduledTickAccess, currentPos, facing, facingPos, facingState, random);
    }

    @Override
    protected void affectNeighborsAfterRemoval(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        handleBoundingBlockRemoval(state, level, pos);
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    /**
     * Hook for the NeoForge layer's {@code AttributeHasBounding} removal; runs between
     * {@code super.affectNeighborsAfterRemoval} and {@code Containers.updateNeighboursAfterDestroy} — exactly where the
     * bounding-block removal sat pre-split. Empty here (no-op for blocks without a bounding attribute, e.g. resources).
     */
    protected void handleBoundingBlockRemoval(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos) {
    }

    @NotNull
    @Override
    protected BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return AttributeStateFacing.rotate(state, rotation);
    }

    @NotNull
    @Override
    protected BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return AttributeStateFacing.mirror(state, mirror);
    }
}
