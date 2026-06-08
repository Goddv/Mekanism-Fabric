package mekanism.common.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import mekanism.common.Mekanism;
import mekanism.common.lib.multiblock.IInternalMultiblock;
import mekanism.common.lib.multiblock.IMultiblock;
import mekanism.common.lib.multiblock.IStructuralMultiblock;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.lib.multiblock.Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldUtils extends WorldUtilsBase {

    private WorldUtils() {
    }

    /**
     * Gets the capability of a block at a given location if it is loaded
     *
     * @param level   Level
     * @param cap     Capability to look up
     * @param pos     position
     * @param context Capability context
     *
     * @return capability if present, null if either not found or not loaded
     */
    @Nullable
    @Contract("null, _, _, _ -> null")
    public static <CAP, CONTEXT> CAP getCapability(@Nullable Level level, BlockCapability<CAP, CONTEXT> cap, BlockPos pos, CONTEXT context) {
        return getCapability(level, cap, pos, null, null, context);
    }

    /**
     * Gets the capability of a block at a given location if it is loaded
     *
     * @param level   Level
     * @param cap     Capability to look up
     * @param pos     position
     * @param state   the block state, if known, or {@code null} if unknown
     * @param tile    the block entity, if known, or {@code null} if unknown
     * @param context Capability context
     *
     * @return capability if present, null if either not found or not loaded
     */
    @Nullable
    @Contract("null, _, _, _, _, _ -> null")
    public static <CAP, CONTEXT> CAP getCapability(@Nullable Level level, BlockCapability<CAP, CONTEXT> cap, BlockPos pos, @Nullable BlockState state,
          @Nullable BlockEntity tile, CONTEXT context) {
        if (!isBlockLoaded(level, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return null
            return null;
        }
        return level.getCapability(cap, pos, state, tile, context);
    }

    /**
     * Gets a tile entity if the location is loaded by getting the chunk from the passed in cache of chunks rather than directly using the world. We then store our chunk
     * we found back in the cache to more quickly be able to look up chunks if we are doing lots of lookups at once (For example the transporter pathfinding)
     *
     * @param clazz    Class type of the TileEntity we expect to be in the position
     * @param world    world
     * @param chunkMap cached chunk map
     * @param pos      position
     *
     * @return tile entity if found, null if either not found, not loaded, or of the wrong type
     */
    @Nullable
    @Contract("_, null, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        return getTileEntity(clazz, world, chunkMap, pos, false);
    }

    /**
     * Gets a tile entity if the location is loaded by getting the chunk from the passed in cache of chunks rather than directly using the world. We then store our chunk
     * we found back in the cache to more quickly be able to look up chunks if we are doing lots of lookups at once (For example the transporter pathfinding)
     *
     * @param clazz        Class type of the TileEntity we expect to be in the position
     * @param world        world
     * @param chunkMap     cached chunk map
     * @param pos          position
     * @param logWrongType Whether an error should be logged if a tile of a different type is found at the position
     *
     * @return tile entity if found, null if either not found, not loaded, or of the wrong type
     */
    @Nullable
    @Contract("_, null, _, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos,
          boolean logWrongType) {
        //Get the tile entity using the chunk we found/had cached
        return getTileEntity(clazz, getChunkForPos(world, chunkMap, pos), pos, logWrongType);
    }

    /**
     * Gets a tile entity if the location is loaded
     *
     * @param clazz Class type of the TileEntity we expect to be in the position
     * @param world world
     * @param pos   position
     *
     * @return tile entity if found, null if either not found, not loaded, or of the wrong type
     */
    @Nullable
    @Contract("_, null, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable BlockGetter world, @NotNull BlockPos pos) {
        return getTileEntity(clazz, world, pos, false);
    }

    /**
     * Gets a tile entity if the location is loaded
     *
     * @param clazz        Class type of the TileEntity we expect to be in the position
     * @param world        world
     * @param pos          position
     * @param logWrongType Whether an error should be logged if a tile of a different type is found at the position
     *
     * @return tile entity if found, null if either not found or not loaded, or of the wrong type
     */
    @Nullable
    @Contract("_, null, _, _ -> null")
    public static <T extends BlockEntity> T getTileEntity(@NotNull Class<T> clazz, @Nullable BlockGetter world, @NotNull BlockPos pos, boolean logWrongType) {
        BlockEntity tile = getTileEntity(world, pos);
        if (tile == null) {
            return null;
        }
        if (clazz.isInstance(tile)) {
            return clazz.cast(tile);
        } else if (logWrongType) {
            Mekanism.logger.warn("Unexpected BlockEntity class at {}, expected {}, but found: {}", pos, clazz, tile.getClass());
        }
        return null;
    }

    /**
     * Dismantles a block, adding to player inventory (or dropping it) and removing it from the world.
     */
    public static void dismantleBlock(BlockState state, Level world, BlockPos pos, @Nullable Entity entity, ItemStack tool) {
        dismantleBlock(state, world, pos, getTileEntity(world, pos), entity, tool);
    }

    /**
     * Dismantles a block, adding to player inventory (or dropping it) and removing it from the world.
     *
     * @implNote This method ignores {@link GameRules#BLOCK_DROPS}, and does not drop experience.
     */
    public static void dismantleBlock(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity tile, @Nullable Entity entity, ItemStack tool) {
        if (world instanceof ServerLevel level) {
            if (entity instanceof Player player) {
                for (ItemStack dropStack : getDrops(state, level, pos, tile, entity, tool)) {
                    if (player.addItem(dropStack)) {
                        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 1.4F + 2.0F);
                    } else {
                        player.drop(dropStack, false);
                    }
                }
            } else {
                for (ItemEntity drop : getDrops(state, level, pos, tile, entity, tool, true)) {
                    if (!drop.getItem().isEmpty()) {
                        level.addFreshEntity(drop);
                    }
                }
            }
            //Note: This will have no effect for any mek blocks currently
            state.spawnAfterBreak(level, pos, tool, false);
        }
        world.removeBlock(pos, false);
    }

    /**
     * Gets the drops from breaking the block at a given spot, including any drops added via the BlockDropsEvent
     */
    public static List<ItemEntity> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity tile, @Nullable Entity entity, ItemStack tool,
          boolean applyMomentum) {
        List<ItemStack> rawDrops = Block.getDrops(state, level, pos, tile, entity, tool);
        List<ItemEntity> initialDrops = new ArrayList<>(rawDrops.size());
        if (!rawDrops.isEmpty()) {
            double itemHeight = EntityType.ITEM.getHeight() / 2.0;
            for (ItemStack rawDrop : rawDrops) {
                if (!rawDrop.isEmpty()) {//Probably won't have empty stacks, but just in case
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 0.5;
                    double z = pos.getZ() + 0.5;
                    if (applyMomentum) {
                        //Apply momentum similar to Block#popResource
                        x += Mth.nextDouble(level.getRandom(), -0.25, 0.25);
                        y += Mth.nextDouble(level.getRandom(), -0.25, 0.25) - itemHeight;
                        z += Mth.nextDouble(level.getRandom(), -0.25, 0.25);
                    }
                    ItemEntity item = new ItemEntity(level, x, y, z, rawDrop);
                    item.setDefaultPickUpDelay();
                    initialDrops.add(item);
                }
            }
        }
        BlockDropsEvent event = new BlockDropsEvent(level, pos, state, tile, initialDrops, entity, tool);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return Collections.emptyList();
        }
        return event.getDrops();
    }

    /**
     * Gets the drops from breaking the block at a given spot, including any drops added via the BlockDropsEvent
     */
    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity tile, @Nullable Entity entity, ItemStack tool) {
        List<ItemEntity> drops = getDrops(state, level, pos, tile, entity, tool, false);
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * A method used to find the Direction represented by the distance of the defined Coord4D. Most likely won't have many applicable uses.
     *
     * @return Direction representing the side the defined relative Coord4D is on to this
     */
    public static Direction sideDifference(BlockPos pos, BlockPos other) {
        int xDiff = pos.getX() - other.getX();
        int yDiff = pos.getY() - other.getY();
        int zDiff = pos.getZ() - other.getZ();
        return getDirection(xDiff, yDiff, zDiff);
    }

    /**
     * A method used to find the Direction represented by the distance of the defined packed BlockPos. Most likely won't have many applicable uses.
     *
     * @return Direction representing the side the defined relative packed BlockPos is on to this
     */
    public static Direction sideDifference(long pos, long other) {
        int xDiff = BlockPos.getX(pos) - BlockPos.getX(other);
        int yDiff = BlockPos.getY(pos) - BlockPos.getY(other);
        int zDiff = BlockPos.getZ(pos) - BlockPos.getZ(other);
        return getDirection(xDiff, yDiff, zDiff);
    }

    private static @Nullable Direction getDirection(int xDiff, int yDiff, int zDiff) {
        for (Direction side : EnumUtils.DIRECTIONS) {
            if (side.getStepX() == xDiff && side.getStepY() == yDiff && side.getStepZ() == zDiff) {
                return side;
            }
        }
        return null;
    }

    //TODO: Come up with a better method name, this is used for checking if a mob can spawn inside or if it is dangerous for mobs to say teleport inside of
    public static boolean isInsideFormedMultiblock(BlockGetter reader, BlockPos pos, @Nullable Mob mob) {
        BlockEntity tile = getTileEntity(reader, pos);
        if (tile instanceof IMultiblock<?> multiblockTile) {
            if (reader instanceof LevelReader levelReader && levelReader.isClientSide() || mob != null && mob.level().isClientSide()) {
                //If we are on the client just check if we are formed as we don't sync structure information
                // to the client. This way the client is at least relatively accurate with what values
                // it returns for if mobs can spawn
                return multiblockTile.getMultiblock().isFormed();
            }
            //If the multiblock is formed and the position above this block is inside the bounds of the multiblock
            // don't allow spawning on it.
            return multiblockTile.getMultiblock().isPositionInsideBounds(multiblockTile.getStructure(), pos.above());
        } else if (tile instanceof IStructuralMultiblock structuralMultiblock && structuralMultiblock.hasFormedMultiblock()) {
            //Note: This isn't actually used as all our structural multiblocks are transparent and vanilla tends to not let
            // mobs spawn on glass or stuff
            if (reader instanceof LevelReader levelReader && levelReader.isClientSide() || mob != null && mob.level().isClientSide()) {
                //If we are on the client return we can't spawn if it is formed. This way the client is at least relatively
                // accurate with what values it returns for if mobs can spawn
                return true;
            } else {
                BlockPos above = pos.above();
                for (Structure structure : structuralMultiblock.getStructureMap().values()) {
                    //Manually handle the getMultiblockData logic to avoid extra lookups
                    MultiblockData data = structure.getMultiblockData();
                    if (data != null && data.isFormed() && data.isPositionInsideBounds(structure, above)) {
                        //If the multiblock is formed and the position above this block is inside the bounds of the multiblock
                        // don't allow spawning on it.
                        return true;
                    }
                }
            }
            return false;
        }
        //If it is an internal multiblock don't allow spawning mobs on it if it is formed
        return tile instanceof IInternalMultiblock internalMultiblock && internalMultiblock.hasFormedMultiblock();
    }

    /**
     * Whether the provided chunk is being vibrated by a Seismic Vibrator.
     *
     * @param chunk chunk to check
     *
     * @return if the chunk is being vibrated
     */
    public static boolean isChunkVibrated(ChunkPos chunk, Level world) {
        for (GlobalPos coord : Mekanism.activeVibrators) {
            if (coord.dimension() == world.dimension() && SectionPos.blockToSectionCoord(coord.pos().getX()) == chunk.x() &&
                SectionPos.blockToSectionCoord(coord.pos().getZ()) == chunk.z()) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryPlaceContainedLiquid(@Nullable Player player, Level world, BlockPos pos, @NotNull FluidStack fluidStack, @Nullable Direction side) {
        Fluid fluid = fluidStack.getFluid();
        FluidType fluidType = fluid.getFluidType();
        if (!fluidType.canBePlacedInLevel(world, pos, fluidStack)) {
            //If there is no fluid, or it cannot be placed in the world just
            return false;
        }
        BlockState state = world.getBlockState(pos);
        boolean isReplaceable = state.canBeReplaced(fluid);
        boolean canContainFluid = state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(player, world, pos, state, fluid);
        if (state.isAir() || isReplaceable || canContainFluid) {
            if (fluidType.isVaporizedOnPlacement(world, pos, fluidStack)) {
                fluidType.onVaporize(player, world, pos, fluidStack);
            } else if (canContainFluid) {
                if (!((LiquidBlockContainer) state.getBlock()).placeLiquid(world, pos, state, fluidType.getStateForPlacement(world, pos, fluidStack))) {
                    //If something went wrong return that we couldn't actually place it
                    return false;
                }
                playEmptySound(player, world, pos, fluidType);
            } else {
                if (!world.isClientSide() && isReplaceable && !state.liquid()) {
                    world.destroyBlock(pos, true);
                }
                playEmptySound(player, world, pos, fluidType);
                world.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL_IMMEDIATE);
            }
            return true;
        }
        return side != null && tryPlaceContainedLiquid(player, world, pos.relative(side), fluidStack, null);
    }

    private static void playEmptySound(@Nullable Player player, LevelAccessor world, BlockPos pos, FluidType fluidType) {
        SoundEvent soundevent = fluidType.getSound(player, world, pos, SoundActions.BUCKET_EMPTY);
        if (soundevent != null) {
            world.playSound(player, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void playFillSound(@Nullable Player player, LevelAccessor world, BlockPos pos, @NotNull FluidStack fluidStack, @Nullable SoundEvent soundEvent) {
        if (soundEvent == null) {
            Fluid fluid = fluidStack.getFluid();
            Optional<SoundEvent> pickupSound = fluid.getPickupSound();
            //noinspection OptionalIsPresent - Capturing lambdas
            if (pickupSound.isPresent()) {
                soundEvent = pickupSound.get();
            } else {
                soundEvent = fluid.getFluidType().getSound(player, world, pos, SoundActions.BUCKET_FILL);
            }
        }
        if (soundEvent != null) {
            world.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Better version of the World.getRedstonePowerFromNeighbors() method that doesn't load chunks.
     *
     * @param world the world to perform the check in
     * @param pos   the position of the block performing the check
     *
     * @return if the block is indirectly getting powered by LOADED chunks
     */
    public static boolean isGettingPowered(Level world, BlockPos pos) {
        if (isBlockLoaded(world, pos)) {
            BlockPos.MutableBlockPos offset = new MutableBlockPos();
            for (Direction side : EnumUtils.DIRECTIONS) {
                offset.setWithOffset(pos, side);
                if (isBlockLoaded(world, offset)) {
                    BlockState blockState = world.getBlockState(offset);
                    boolean weakPower = blockState.getBlock().shouldCheckWeakPower(blockState, world, pos, side);
                    if (weakPower && isDirectlyGettingPowered(world, offset) || !weakPower && blockState.getSignal(world, offset, side) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a block is directly getting powered by any of its neighbors without loading any chunks.
     *
     * @param world the world to perform the check in
     * @param pos   the BlockPos of the block to check
     *
     * @return if the block is directly getting powered
     */
    public static boolean isDirectlyGettingPowered(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos offset = new MutableBlockPos();
        for (Direction side : EnumUtils.DIRECTIONS) {
            offset.setWithOffset(pos, side);
            if (isBlockLoaded(world, offset) && world.getSignal(pos, side) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calls BOTH neighbour changed functions because nobody can decide on which one to implement, assuming that the neighboring position is loaded.
     *
     * @param world   world the change exists in
     * @param pos     neighbor to notify
     * @param fromPos pos of our block that updated
     */
    public static void notifyNeighborOfChange(@Nullable Level world, BlockPos pos, BlockPos fromPos) {
        Optional<BlockState> blockState = getBlockState(world, pos);
        if (blockState.isPresent() && world != null) {//World can't be null here but double check it
            BlockState state = blockState.get();
            state.onNeighborChange(world, pos, fromPos);
            state.handleNeighborChanged(world, pos, world.getBlockState(fromPos).getBlock(), null, false);
        }
    }

    /**
     * Notifies neighboring blocks of a TileEntity change without loading chunks.
     *
     * @param world world to perform the operation in
     * @param pos   BlockPos to perform the operation on
     */
    public static void notifyLoadedNeighborsOfTileChange(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockPos.MutableBlockPos offset = new MutableBlockPos();
        for (Direction dir : EnumUtils.DIRECTIONS) {
            offset.setWithOffset(pos, dir);
            if (isBlockLoaded(world, offset)) {
                BlockState offsetState = world.getBlockState(offset);
                offsetState.onNeighborChange(world, offset, pos);
                offsetState.handleNeighborChanged(world, offset, state.getBlock(), null, false);
                if (offsetState.isRedstoneConductor(world, offset)) {
                    //If redstone can be conducted through it, forward the change along an extra spot
                    offset.move(dir);
                    if (isBlockLoaded(world, offset)) {
                        offsetState = world.getBlockState(offset);
                        if (offsetState.getWeakChanges(world, offset)) {
                            offsetState.onNeighborChange(world, offset, pos);
                        }
                    }
                }
            }
        }
    }
}
