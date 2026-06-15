package mekanism.fabric.content.transmitter;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Logistical Transporter transmitter ({@code mekanism:basic_logistical_transporter}): an ITEM adjacent-relay. Unlike
 * the energy/chemical/heat transmitters it holds no internal buffer — each server tick it reads neighbouring inventories
 * directly through the Fabric-API item capability ({@link ItemStorage#SIDED}) and moves up to {@link #ITEMS_PER_TICK}
 * items from one adjacent inventory into a <em>different</em> adjacent inventory that accepts them. First-fit, no routing
 * graph: it walks source directions, and for each tries every other direction as a destination (skipping the source's
 * own direction so an item is never handed straight back). {@link StorageUtil#move} runs the extract+insert in one
 * transaction so a partial insert never loses items. Not the real Mekanism logistical network (no colour filters,
 * round-robin, or pathing).
 */
public class LogisticalTransporterBlockEntity extends TransmitterBlockEntity {

    private static final long ITEMS_PER_TICK = 4L;

    public LogisticalTransporterBlockEntity(BlockPos pos, BlockState state) {
        super(FabricTransmitters.LOGISTICAL_TRANSPORTER_BE_TYPE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel level) {
        long budget = ITEMS_PER_TICK;
        for (Direction from : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            Storage<ItemVariant> source = ItemStorage.SIDED.find(level, worldPosition.relative(from), from.getOpposite());
            if (source == null || !source.supportsExtraction()) {
                continue;
            }
            for (Direction to : Direction.values()) {
                if (budget <= 0L) {
                    break;
                }
                if (to == from) {
                    continue; // never hand an item straight back to where it came from
                }
                Storage<ItemVariant> dest = ItemStorage.SIDED.find(level, worldPosition.relative(to), to.getOpposite());
                if (dest == null || !dest.supportsInsertion()) {
                    continue;
                }
                // StorageUtil.move extracts from source + inserts into dest atomically, returning the amount moved.
                long moved = StorageUtil.move(source, dest, variant -> true, budget, null);
                budget -= moved;
            }
        }
    }
}
