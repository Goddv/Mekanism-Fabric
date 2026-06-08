package mekanism.common.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldUtilsBase {

    WorldUtilsBase() {
    }

    /**
     * Checks if the chunk at the given position is loaded but does not validate the position is in bounds of the world.
     *
     * @param world world
     * @param pos   position
     *
     * @see #isBlockLoaded(BlockGetter, BlockPos)
     */
    @Contract("null, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, @NotNull BlockPos pos) {
        return isChunkLoaded(world, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /**
     * Checks if the chunk at the given position is loaded but does not validate the position is in bounds of the world.
     *
     * @param world world
     * @param pos   packed block position
     *
     * @see #isBlockLoaded(BlockGetter, BlockPos)
     */
    @Contract("null, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, long pos) {
        return isChunkLoaded(world, SectionPos.blockToSectionCoord(BlockPos.getX(pos)), SectionPos.blockToSectionCoord(BlockPos.getZ(pos)));
    }

    /**
     * Checks if the chunk at the given position is loaded.
     *
     * @param world    world
     * @param chunkPos Chunk position
     */
    @Contract("null, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, ChunkPos chunkPos) {
        return isChunkLoaded(world, chunkPos.x(), chunkPos.z());
    }

    /**
     * Checks if the chunk at the given position is loaded.
     *
     * @param world  world
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    @Contract("null, _, _ -> false")
    public static boolean isChunkLoaded(@Nullable LevelReader world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelAccessor accessor) {
            if (!(accessor instanceof Level level) || !level.isClientSide()) {
                return accessor.hasChunk(chunkX, chunkZ);
            }
            //Don't allow the client level to just return true for all cases, as we actually care if it is present
            // and instead use the fallback logic that we have
        }
        return world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    /**
     * Checks if a position is in bounds of the world, and is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return True if the position is loaded or the given world is of a superclass of IWorldReader that does not have a concept of being loaded.
     */
    @Contract("null, _ -> false")
    public static boolean isBlockLoaded(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelReader reader) {
            if (reader instanceof Level level && !level.isInWorldBounds(pos)) {
                return false;
            }
            //TODO: If any cases come up where things are behaving oddly due to the change from reader.hasChunkAt(pos)
            // re-evaluate this and if the specific case is being handled properly
            return isChunkLoaded(reader, pos);
        }
        return true;
    }

    private static boolean isInWorldBound(BlockGetter world, long pos) {
        return !world.isOutsideBuildHeight(BlockPos.getY(pos)) && isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(long pos) {
        int x = BlockPos.getX(pos);
        int z = BlockPos.getZ(pos);
        return x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000;
    }

    /**
     * Checks if a position is in bounds of the world, and is loaded
     *
     * @param world world
     * @param pos   packed block position
     *
     * @return True if the position is loaded or the given world is of a superclass of IWorldReader that does not have a concept of being loaded.
     *
     * @implNote the checks for world bound don't account for a mod changing the default 30,000,000 limit, but surely that's rare...
     */
    @Contract("null, _ -> false")
    public static boolean isBlockLoaded(@Nullable BlockGetter world, long pos) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelReader reader) {
            if (reader instanceof Level level && !isInWorldBound(level, pos)) {
                return false;
            }
            //TODO: If any cases come up where things are behaving oddly due to the change from reader.hasChunkAt(pos)
            // re-evaluate this and if the specific case is being handled properly
            return isChunkLoaded(reader, pos);
        }
        return true;
    }

    /**
     * Checks if a position is in bounds of the world
     *
     * @param world world
     * @param pos   position
     *
     * @return True if the position is in bounds of the world or the given world is of a superclass of IWorldReader that does not have a concept of bounds.
     */
    @Contract("null, _ -> false")
    public static boolean isBlockInBounds(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (world == null) {
            return false;
        } else if (world instanceof LevelReader reader) {
            return !(reader instanceof Level level) || level.isInWorldBounds(pos);
        }
        return true;
    }

    /**
     * Gets the chunk in a given position or {@code null} if there is no world, the position is out of bounds or the chunk isn't loaded. Tries to retrieve it from our
     * cache and if it isn't found, tries to get it from the world and adds it to our cache.
     *
     * @param world    world
     * @param chunkMap cached chunk map
     * @param pos      position
     *
     * @return The chunk in a given position or {@code null} if there is no world, the position is out of bounds or the chunk isn't loaded
     */
    @Nullable
    @Contract("null, _, _ -> null")
    static ChunkAccess getChunkForPos(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        if (!isBlockInBounds(world, pos)) {
            //Allow the world to be nullable to remove warnings when we are calling things from a place that world could be null
            // Also short circuit to check if the position is out of bounds before bothering to look up the chunk
            return null;
        }
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        long combinedChunk = ChunkPos.pack(chunkX, chunkZ);
        //We get the chunk rather than the world, so we can cache the chunk improving the overall
        // performance for retrieving a bunch of chunks in the general vicinity
        ChunkAccess chunk = chunkMap.get(combinedChunk);
        if (chunk == null) {
            //Get the chunk but don't force load it
            chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk != null) {
                chunkMap.put(combinedChunk, chunk);
            }
        }
        return chunk;
    }

    /**
     * Gets a blockstate if the location is loaded by getting the chunk from the passed in cache of chunks rather than directly using the world. We then store our chunk
     * we found back in the cache to more quickly be able to look up chunks if we are doing lots of lookups at once (For example multiblock structure validation)
     *
     * @param world    world
     * @param chunkMap cached chunk map
     * @param pos      position
     *
     * @return optional containing the blockstate if found, empty optional if not loaded
     */
    @NotNull
    public static Optional<BlockState> getBlockState(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the blockstate using the chunk we found/had cached
        return getBlockState(getChunkForPos(world, chunkMap, pos), pos);
    }

    /**
     * Gets a blockstate if the location is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return optional containing the blockstate if found, empty optional if not loaded
     */
    @NotNull
    public static Optional<BlockState> getBlockState(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return empty
            return Optional.empty();
        }
        return Optional.of(world.getBlockState(pos));
    }

    /**
     * Gets a blockstate if the location is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return the blockstate if found, null if not loaded
     */
    @Nullable
    public static BlockState getBlockStateIfLoaded(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return empty
            return null;
        }
        return world.getBlockState(pos);
    }

    /**
     * Gets a fluidstate if the location is loaded by getting the chunk from the passed in cache of chunks rather than directly using the world. We then store our chunk
     * we found back in the cache to more quickly be able to look up chunks if we are doing lots of lookups at once (For example multiblock structure validation)
     *
     * @param world    world
     * @param chunkMap cached chunk map
     * @param pos      position
     *
     * @return optional containing the fluidstate if found, empty optional if not loaded
     */
    @NotNull
    public static Optional<FluidState> getFluidState(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the fluidstate using the chunk we found/had cached
        return getFluidState(getChunkForPos(world, chunkMap, pos), pos);
    }

    /**
     * Gets a fluidstate if the location is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return optional containing the fluidstate if found, empty optional if not loaded
     */
    @NotNull
    public static Optional<FluidState> getFluidState(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return empty
            return Optional.empty();
        }
        return Optional.of(world.getFluidState(pos));
    }

    /**
     * Gets a tile entity if the location is loaded by getting the chunk from the passed in cache of chunks rather than directly using the world. We then store our chunk
     * we found back in the cache to more quickly be able to look up chunks if we are doing lots of lookups at once (For example the transporter pathfinding)
     *
     * @param world    world
     * @param chunkMap cached chunk map
     * @param pos      position
     *
     * @return tile entity if found, null if either not found or not loaded
     */
    @Nullable
    @Contract("null, _, _ -> null")
    public static BlockEntity getTileEntity(@Nullable LevelAccessor world, @NotNull Long2ObjectMap<ChunkAccess> chunkMap, @NotNull BlockPos pos) {
        //Get the tile entity using the chunk we found/had cached
        return getTileEntity(getChunkForPos(world, chunkMap, pos), pos);
    }

    /**
     * Gets a tile entity if the location is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return tile entity if found, null if either not found or not loaded
     */
    @Nullable
    @Contract("null, _ -> null")
    public static BlockEntity getTileEntity(@Nullable BlockGetter world, long pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return null
            return null;
        }
        return world.getBlockEntity(BlockPos.of(pos));
    }

    /**
     * Gets a tile entity if the location is loaded
     *
     * @param world world
     * @param pos   position
     *
     * @return tile entity if found, null if either not found or not loaded
     */
    @Nullable
    @Contract("null, _ -> null")
    public static BlockEntity getTileEntity(@Nullable BlockGetter world, @NotNull BlockPos pos) {
        if (!isBlockLoaded(world, pos)) {
            //If the world is null, or it is a world reader and the block is not loaded, return null
            return null;
        }
        return world.getBlockEntity(pos);
    }

    /**
     * Marks a chunk as dirty if it is currently loaded
     */
    public static void markChunkDirty(Level world, BlockPos pos) {
        markChunkDirty(world, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /**
     * Marks a chunk as dirty if it is currently loaded
     */
    public static void markChunkDirty(Level world, int chunkX, int chunkZ) {
        ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk != null) {
            chunk.markUnsaved();
        }
    }

    /**
     * Gets the distance to a defined positions.
     *
     * @return the distance to the defined positions
     */
    public static double distanceBetween(BlockPos start, BlockPos end) {
        return Math.sqrt(start.distSqr(end));
    }

    /**
     * Checks if all the positions are valid and the current block in them can be replaced.
     *
     * @return True if the blocks can be replaced and is within the world's bounds.
     */
    public static boolean areBlocksValidAndReplaceable(@NotNull BlockGetter world, @Nullable BlockPlaceContext baseContext, @NotNull BlockPos... positions) {
        for (BlockPos position : positions) {
            if (!isValidReplaceableBlock(world, baseContext, position)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all the positions are valid and the current block in them can be replaced.
     *
     * @return True if the blocks can be replaced and is within the world's bounds.
     */
    public static boolean areBlocksValidAndReplaceable(@NotNull BlockGetter world, @Nullable BlockPlaceContext baseContext, @NotNull Collection<BlockPos> positions) {
        //TODO: Potentially move more block placement over to these methods
        for (BlockPos position : positions) {
            if (!isValidReplaceableBlock(world, baseContext, position)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a block is valid for a position and the current block there can be replaced.
     *
     * @return True if the block can be replaced and is within the world's bounds.
     */
    public static boolean isValidReplaceableBlock(@NotNull BlockGetter world, @Nullable BlockPlaceContext baseContext, @NotNull BlockPos pos) {
        Optional<BlockState> blockState = getBlockState(world, pos);
        if (blockState.isPresent()) {
            BlockState state = blockState.get();
            if (baseContext == null) {
                return state.canBeReplaced();
            }
            return state.canBeReplaced(BlockPlaceContext.at(baseContext, pos, baseContext.getClickedFace()));
        }
        return false;
    }

    /**
     * Marks a block for a render update if loaded.
     *
     * @param world world the block is in
     * @param pos   Position of the block
     * @param state The block state at the position
     */
    public static void updateBlock(@Nullable Level world, @NotNull BlockPos pos, BlockState state) {
        if (isBlockLoaded(world, pos)) {
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * Gets the skylight factor, a percentage based on the time of day and weather. Vanilla max of 1
     * @return percentage value of float type. Normal range of 0 - 1. CAN BE HIGHER
     */
    public static float getSunBrightness(Level level, BlockPos position) {
        return Math.max(0, level.environmentAttributes().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, position));
    }

    /**
     * Checks to see if the block at the position can see the sky, and it is daytime.
     *
     * @param world World to check in.
     * @param pos   Position to check.
     *
     * @return {@code true} if it can.
     */
    @Contract("null, _ -> false")
    public static boolean canSeeSun(@Nullable Level world, BlockPos pos) {
        //Note: We manually handle the world#isDaytime check by just checking the subtracted skylight
        // as vanilla returns false if the world's time is set to a fixed value even if that time
        // would effectively be daytime
        return world != null && world.dimensionType().hasSkyLight() && world.getSkyDarken() < 4 && world.canSeeSky(pos);
    }

    /**
     * Converts a long representing a {@link ChunkPos} to a {@link BlockPos} without creating a temporary {@link ChunkPos} object.
     *
     * @param chunkPos Pos to convert.
     */
    public static BlockPos getBlockPosFromChunkPos(long chunkPos) {
        return new BlockPos((int) chunkPos, 0, (int) (chunkPos >> 32));
    }

    /**
     * Get a packed block pos on the relative side.
     * @see BlockPos#relative(Direction)
     *
     * @param pos Source position
     * @param direction direction to offset in
     * @return the packed offset position
     */
    public static long relativePos(long pos, Direction direction) {
        return BlockPos.asLong(BlockPos.getX(pos) + direction.getStepX(), BlockPos.getY(pos) + direction.getStepY(), BlockPos.getZ(pos) + direction.getStepZ());
    }
}
