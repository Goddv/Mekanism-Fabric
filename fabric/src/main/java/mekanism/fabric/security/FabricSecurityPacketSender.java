package mekanism.fabric.security;

import java.util.UUID;
import mekanism.common.lib.security.ISecurityPacketSender;

/** Fabric {@link ISecurityPacketSender}: no-op (the Mekanism security packet isn't ported to Fabric yet). */
public class FabricSecurityPacketSender implements ISecurityPacketSender {

    @Override
    public void sendSecurityUpdate(UUID playerUUID) {
        //Deferred: Mekanism security sync packet not yet ported to Fabric.
    }
}
