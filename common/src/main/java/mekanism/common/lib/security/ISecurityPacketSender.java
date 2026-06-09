package mekanism.common.lib.security;

import java.util.UUID;
import mekanism.api.MekanismAPIBase;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific broadcast of a security-owner update. When a Mekanism block is first placed and takes the placer as its
 * owner, NeoForge pushes a {@code PacketSyncSecurity} to all players (via {@code PacketDistributor}); {@code :common} can't
 * reference that networking. The hoisted {@code BlockMekanism} routes through this service. Resolved via
 * {@link MekanismAPIBase#getService} (same precedent as {@code ITileSyncService}/{@code IContainerSyncSender}); Fabric
 * defers (no-op) until the Mekanism security packet is ported.
 */
@Internal
public interface ISecurityPacketSender {

    ISecurityPacketSender INSTANCE = MekanismAPIBase.getService(ISecurityPacketSender.class);

    /** Broadcasts that the block at the just-placed position is now owned by {@code playerUUID}. Server-side. */
    void sendSecurityUpdate(UUID playerUUID);
}
