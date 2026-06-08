package mekanism.common.tile.base;

import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.PacketUpdateTile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * NeoForge {@link ITileSyncService}: sends Mekanism's own {@code PacketUpdateTile} to tracking players (unchanged behavior).
 */
public class NeoTileSyncService implements ITileSyncService {

    @Override
    public void sendUpdatePacket(BlockEntity tile, BlockEntity tracking) {
        //Note: We use our own update packet/channel to avoid chunk trashing + a full chunk re-render when usually only a
        // TileEntityRenderer changed.
        if (PacketUtils.hasPlayersTracking((ServerLevel) tracking.getLevel(), tracking.getBlockPos())) {
            PacketUtils.sendToAllTracking(new PacketUpdateTile((TileEntityUpdateable) tile), tracking);
        }
    }

    @Override
    public void requestModelDataUpdate(BlockEntity tile) {
        tile.requestModelDataUpdate();
    }
}
