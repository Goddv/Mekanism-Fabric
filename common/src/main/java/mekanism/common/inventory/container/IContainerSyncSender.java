package mekanism.common.inventory.container;

import java.util.List;
import mekanism.api.MekanismAPIBase;
import mekanism.common.network.to_client.container.property.PropertyData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific container-sync send. Mekanism syncs tracked GUI data to the client via its own {@code PacketUpdateContainer}
 * (NeoForge networking through {@code PacketDistributor}), which {@code :common} can't reference. The hoisted
 * {@code MekanismContainer} routes its dirty-property broadcasts through this service so the container itself stays
 * loader-neutral. Resolved via {@link MekanismAPIBase#getService} (same precedent as {@code ITileSyncService}); the
 * payload {@link PropertyData} list is already in {@code :common} (the sync framework hoist), so the seam is loader-clean.
 *
 * <p>NeoForge sends the real {@code PacketUpdateContainer} to the player; Fabric defers (no-op) for now — a basic machine's
 * GUI state syncs via vanilla menu data slots until the Mekanism container framework lands on Fabric.
 */
@Internal
public interface IContainerSyncSender {

    IContainerSyncSender INSTANCE = MekanismAPIBase.getService(IContainerSyncSender.class);

    /**
     * Sends the given dirty tracked-property data for {@code containerId} to {@code player}. Called server-side from
     * {@code MekanismContainer.broadcastChanges()}/{@code sendInitialDataToRemote()} when there is dirty data.
     */
    void sendUpdate(ServerPlayer player, short containerId, List<PropertyData> dirtyData);
}
