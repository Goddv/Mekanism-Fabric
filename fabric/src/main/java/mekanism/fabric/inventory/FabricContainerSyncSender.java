package mekanism.fabric.inventory;

import java.util.List;
import mekanism.common.inventory.container.IContainerSyncSender;
import mekanism.common.network.to_client.container.property.PropertyData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric {@link IContainerSyncSender}: no-op for now (the Mekanism container-sync packet path isn't wired on Fabric yet;
 * basic machine GUIs sync via vanilla menu data slots). Mirrors {@code FabricTileSyncService}'s deferral; a real
 * implementation (ServerPlayNetworking + a Fabric PacketUpdateContainer) lands with the Mekanism GUI framework on Fabric.
 */
public class FabricContainerSyncSender implements IContainerSyncSender {

    @Override
    public void sendUpdate(ServerPlayer player, short containerId, List<PropertyData> dirtyData) {
        //Deferred: Mekanism container framework + its sync packet are not yet ported to Fabric.
    }
}
