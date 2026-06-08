package mekanism.fabric.capabilities;

import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.ICapabilityExposureService;
import mekanism.common.tile.base.TileEntityUpdateable;
import mekanism.fabric.heat.MekanismFabricHeat;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric {@link ICapabilityExposureService}. Adjacent-heat uses the {@code BlockApiLookup}-backed
 * {@link MekanismFabricHeat#SIDED} (itself cache-backed, so no per-tile cache is needed). Per-type registration wires the
 * Fabric SIDED providers wrapping the tile's loader-neutral handler getters (added in a later slice as the tile gains the
 * loader-neutral handler views; left minimal for now).
 */
public class FabricCapabilityExposureService implements ICapabilityExposureService {

    @Nullable
    @Override
    public IHeatHandler getAdjacentHeat(TileEntityUpdateable sourceTile, Direction side) {
        Level level = sourceTile.getLevel();
        if (level instanceof ServerLevel server) {
            return MekanismFabricHeat.getHeatHandler(server, sourceTile.getBlockPos().relative(side), side.getOpposite());
        }
        return null;
    }

    @Override
    public void registerForType(BlockEntityType<?> type) {
        //BlockApiLookup provider registration for hoisted Mekanism tiles is wired in the tile-hoist slice once the
        //loader-neutral handler views are available; intentionally minimal here.
    }
}
