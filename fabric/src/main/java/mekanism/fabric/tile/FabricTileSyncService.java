package mekanism.fabric.tile;

import mekanism.common.tile.base.ITileSyncService;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fabric {@link ITileSyncService}: no-op for now. A basic machine's client-visible state (the ACTIVE blockstate) syncs
 * via vanilla; the full tile-renderer client sync (Mekanism's PacketUpdateTile) is wired when the GUI/network stage ports.
 */
public class FabricTileSyncService implements ITileSyncService {

    @Override
    public void sendUpdatePacket(BlockEntity tile, BlockEntity tracking) {
        //Deferred: vanilla block updates cover the basic machine's client-visible state.
    }

    @Override
    public void requestModelDataUpdate(BlockEntity tile) {
        //No model-data system on Fabric for this bring-up; vanilla sendBlockUpdated (in TileEntityUpdateable) suffices.
    }
}
