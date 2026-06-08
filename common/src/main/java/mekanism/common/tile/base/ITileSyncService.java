package mekanism.common.tile.base;

import mekanism.api.MekanismAPIBase;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific tile client-sync. Mekanism uses its own update packet (instead of vanilla's chunk-trashing one) to push
 * a tile's render data to tracking clients; that path is NeoForge networking ({@code PacketUtils} + {@code PacketUpdateTile}),
 * which {@code :common} can't reference. The hoisted {@link TileEntityUpdateable} routes {@code sendUpdatePacket} through
 * this service: NeoForge sends the real packet to tracking players; Fabric is a no-op for now (a basic machine's
 * client-visible state — the ACTIVE blockstate — already syncs via vanilla; full tile-renderer sync arrives with the GUI
 * stage). Resolved via {@link MekanismAPIBase#getService}.
 */
@Internal
public interface ITileSyncService {

    ITileSyncService INSTANCE = MekanismAPIBase.getService(ITileSyncService.class);

    /**
     * Sends {@code tile}'s reduced update tag to all players tracking {@code tracking}. Called server-side on a loaded,
     * non-removed tile.
     */
    void sendUpdatePacket(BlockEntity tile, BlockEntity tracking);

    /** Requests a (NeoForge) model-data refresh for the tile; no-op on loaders without a model-data system. */
    void requestModelDataUpdate(BlockEntity tile);
}
