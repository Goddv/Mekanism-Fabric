package mekanism.common.inventory.container;

import java.util.List;
import mekanism.common.network.to_client.container.PacketUpdateContainer;
import mekanism.common.network.to_client.container.property.PropertyData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoForge {@link IContainerSyncSender}: sends Mekanism's {@code PacketUpdateContainer} to the player via
 * {@code PacketDistributor} (the exact call relocated verbatim from {@code MekanismContainer}).
 */
public class NeoContainerSyncSender implements IContainerSyncSender {

    @Override
    public void sendUpdate(ServerPlayer player, short containerId, List<PropertyData> dirtyData) {
        PacketDistributor.sendToPlayer(player, new PacketUpdateContainer(containerId, dirtyData));
    }
}
