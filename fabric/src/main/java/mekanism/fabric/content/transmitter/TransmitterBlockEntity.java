package mekanism.fabric.content.transmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transitional Fabric bring-up: the common base for the four transmitter block-entities. Each concrete transmitter holds
 * a small internal buffer (an energy container / chemical tank / heat capacitor) and, every server tick, relays its
 * resource between adjacent providers and acceptors via the matching {@code BlockApiLookup} capability — the proven
 * adjacent-relay shape first established by {@code CableBlockEntity}. No routing graph, no multipart connection state:
 * the real Mekanism transmitter network replaces this once that subsystem is hoisted.
 */
public abstract class TransmitterBlockEntity extends BlockEntity {

    protected TransmitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Relay one tick: move this transmitter's resource between adjacent providers and acceptors. */
    public abstract void serverTick(ServerLevel level);
}
