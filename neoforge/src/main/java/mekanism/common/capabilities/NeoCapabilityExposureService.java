package mekanism.common.capabilities;

import mekanism.api.heat.IHeatHandler;
import mekanism.common.tile.base.CapabilityTileEntity;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge {@link ICapabilityExposureService}. Adjacent-heat delegates to the per-tile {@code BlockCapabilityCache} held by
 * {@link CapabilityTileEntity}; per-type registration is a no-op because NeoForge wires capabilities via static providers on
 * the BE-type builder + {@code RegisterCapabilitiesEvent} (unchanged).
 */
public class NeoCapabilityExposureService implements ICapabilityExposureService {

    @Nullable
    @Override
    public IHeatHandler getAdjacentHeat(TileEntityUpdateable sourceTile, Direction side) {
        if (sourceTile instanceof CapabilityTileEntity cap) {
            return cap.getAdjacentHeatHandler(side);
        }
        return null;
    }

    @Override
    public void registerForType(BlockEntityType<?> type) {
        //No-op: NeoForge registers tile capabilities via the BE-type builder's static providers + RegisterCapabilitiesEvent.
    }
}
