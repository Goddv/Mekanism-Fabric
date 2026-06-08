package mekanism.fabric.content.machine;

import com.mojang.logging.LogUtils;
import mekanism.fabric.content.power.FabricPowerInfrastructure;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of machine/generator auto-I/O through the Fabric Transfer {@code ItemStorage} capability
 * (what hoppers + item pipes use). Proves the {@code WorldlyContainer} side rules: a machine accepts pipe insertion only
 * into its input slot and yields pipe extraction only from its output slot (input is protected); the generator accepts
 * only fuel and never yields its fuel to extraction. Grep {@code [Mekanism/Fabric][autoio-selftest] RESULT:}.
 */
public final class FabricAutoIoSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][autoio-selftest]";

    private FabricAutoIoSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        try {
            // --- machine ---
            BlockPos mp = new BlockPos(0, 64, 50);
            level.getChunk(mp.getX() >> 4, mp.getZ() >> 4);
            level.setBlock(mp, FabricRealMachines.enrichmentChamber().block().defaultBlockState(), 3);
            MachineBlockEntity machine = (MachineBlockEntity) level.getBlockEntity(mp);
            Storage<ItemVariant> mStorage = ItemStorage.SIDED.find(level, mp, Direction.UP);

            long insertedToInput = insert(mStorage, Items.DIRT, 4);
            boolean machineInputOk = insertedToInput == 4 && machine.getItem(0).is(Items.DIRT)
                  && machine.getItem(0).getCount() == 4 && machine.getItem(1).isEmpty();

            machine.setItem(1, new ItemStack(Items.DIAMOND, 3));
            long extractedOutput = extract(mStorage, Items.DIAMOND, 2);
            boolean extractOutputOk = extractedOutput == 2 && machine.getItem(1).getCount() == 1;

            long extractedInput = extract(mStorage, Items.DIRT, 1); // input must be protected from pipe extraction
            boolean inputProtectedOk = extractedInput == 0 && machine.getItem(0).getCount() == 4;
            level.removeBlock(mp, false);

            // --- generator ---
            BlockPos gp = new BlockPos(2, 64, 50);
            level.setBlock(gp, FabricPowerInfrastructure.GENERATOR.get().defaultBlockState(), 3);
            Storage<ItemVariant> gStorage = ItemStorage.SIDED.find(level, gp, Direction.UP);
            long coalIn = insert(gStorage, Items.COAL, 2);     // fuel accepted
            long dirtIn = insert(gStorage, Items.DIRT, 1);     // non-fuel rejected
            long fuelOut = extract(gStorage, Items.COAL, 1);   // fuel not extractable
            boolean generatorOk = coalIn == 2 && dirtIn == 0 && fuelOut == 0;
            level.removeBlock(gp, false);

            ok = machineInputOk && extractOutputOk && inputProtectedOk && generatorOk;
            LOGGER.info("{} {} machineInput={} extractOutput={} inputProtected={} generator={}",
                  TAG, ok ? "OK  " : "FAIL", machineInputOk, extractOutputOk, inputProtectedOk, generatorOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL autoio test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }

    private static long insert(Storage<ItemVariant> storage, Item item, int amount) {
        if (storage == null) {
            return -1;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = storage.insert(ItemVariant.of(item), amount, tx);
            tx.commit();
            return moved;
        }
    }

    private static long extract(Storage<ItemVariant> storage, Item item, int amount) {
        if (storage == null) {
            return -1;
        }
        try (Transaction tx = Transaction.openOuter()) {
            long moved = storage.extract(ItemVariant.of(item), amount, tx);
            tx.commit();
            return moved;
        }
    }
}
