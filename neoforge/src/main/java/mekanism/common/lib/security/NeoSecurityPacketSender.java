package mekanism.common.lib.security;

import java.util.UUID;
import mekanism.common.network.to_client.security.PacketSyncSecurity;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge {@link ISecurityPacketSender}: broadcasts the real {@code PacketSyncSecurity} (verbatim relocation). */
public class NeoSecurityPacketSender implements ISecurityPacketSender {

    @Override
    public void sendSecurityUpdate(UUID playerUUID) {
        PacketDistributor.sendToAllPlayers(new PacketSyncSecurity(playerUUID));
    }
}
