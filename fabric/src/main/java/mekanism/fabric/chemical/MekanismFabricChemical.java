package mekanism.fabric.chemical;

import mekanism.api.chemical.IChemicalHandler;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric analog of NeoForge's {@code Capabilities.CHEMICAL}: Mekanism's own block-level chemical capability, exposed
 * through Fabric API's {@link BlockApiLookup}. The third core Mekanism capability brought to Fabric (after energy +
 * heat), reusing the same proven pattern over the loader-neutral {@link IChemicalHandler} (hoisted into {@code :common}).
 */
public final class MekanismFabricChemical {

    private MekanismFabricChemical() {
    }

    /**
     * Mekanism's chemical capability lookup. Register providers for blocks/block-entities that expose an
     * {@link IChemicalHandler}, and query it to find chemical handlers on blocks in the world.
     */
    public static final BlockApiLookup<IChemicalHandler, @Nullable Direction> SIDED = BlockApiLookup.get(
          Identifier.fromNamespaceAndPath("mekanism", "chemical_handler"),
          IChemicalHandler.class,
          Direction.class
    );

    @Nullable
    public static IChemicalHandler getChemicalHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return SIDED.find(level, pos, side);
    }

    @Nullable
    public static IChemicalHandler getChemicalHandler(ServerLevel level, BlockPos pos, @Nullable BlockState state,
          @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        return SIDED.find(level, pos, state, blockEntity, side);
    }
}
