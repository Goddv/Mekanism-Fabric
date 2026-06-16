package mekanism.fabric.content.machine.factory;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.fabric.chemical.FabricChemicalRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the FACTORY blocks on Fabric. Proves that a factory runs its base recipe type across N
 * processes in parallel: it places a representative factory, fills some/all of its per-process input slots (+ the shared
 * chemical tank / shared extra slot, where applicable) + energy, ticks past completion, then asserts EACH filled process
 * produced its expected output and consumed its input — and that UNfilled processes stayed empty.
 *
 * <p>Cases:
 * <ul>
 *   <li>{@code basic_enriching_factory} (3 proc): fill all 3 inputs with dirt -&gt; assert 3 diamond outputs.</li>
 *   <li>{@code ultimate_smelting_factory} (9 proc): fill processes 0,4,8 with sand -&gt; assert glass in exactly those outputs.</li>
 *   <li>{@code basic_compressing_factory} (3 proc): fill 3 inputs (iron) + the shared chemical tank -&gt; assert 3 gold outputs.</li>
 *   <li>{@code basic_combining_factory} (3 proc): fill 3 main inputs (cobblestone) + the shared extra slot (flint) -&gt; assert 3 gravel outputs.</li>
 * </ul>
 *
 * <p>Grep {@code [Mekanism/Fabric][factory-selftest]}.
 */
public final class FabricFactorySelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][factory-selftest]";

    private FabricFactorySelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean all = true;
        all &= itemToItem(level, "basic_enriching_factory", new int[] {0, 1, 2}, Items.DIRT, Items.DIAMOND, new BlockPos(40, 64, 60));
        // REAL bundled datapack recipe (NOT a Fabric-only test recipe): the build-time recipe filter copies the real
        // Mekanism enriching recipe glowstone -> 4 glowstone_dust into the jar. Running it through the factory proves the
        // real recipes now drive the factory tier system on Fabric too.
        all &= itemToItem(level, "basic_enriching_factory", new int[] {0, 1, 2}, Items.GLOWSTONE, Items.GLOWSTONE_DUST, new BlockPos(43, 64, 60));
        all &= itemToItem(level, "ultimate_smelting_factory", new int[] {0, 4, 8}, Items.SAND, Items.GLASS, new BlockPos(40, 64, 63));
        all &= itemChemicalToItem(level, "basic_compressing_factory", new int[] {0, 1, 2}, Items.IRON_INGOT, Items.GOLD_INGOT, new BlockPos(40, 64, 66));
        all &= combining(level, "basic_combining_factory", new int[] {0, 1, 2}, Items.COBBLESTONE, Items.FLINT, Items.GRAVEL, new BlockPos(40, 64, 69));
        LOGGER.info("{} RESULT: {}", TAG, all ? "PASS" : "FAIL");
    }

    /** item -> item factory: fill the given process input slots, tick, assert each produced expectedItem + others empty. */
    private static boolean itemToItem(ServerLevel level, String blockId, int[] filled, Item input, Item expected, BlockPos pos) {
        boolean ok = false;
        try {
            FactoryBlockEntity factory = place(level, blockId, pos);
            int processes = factory.processes();
            for (int p : filled) {
                factory.setItem(p, new ItemStack(input, 4));
            }
            factory.getEnergyContainers(null).getFirst().insert(50_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            tick(factory);

            int produced = 0;
            boolean inputConsumed = true;
            boolean othersEmpty = true;
            for (int p = 0; p < processes; p++) {
                ItemStack out = factory.getItem(processes + p).copy();
                boolean isFilled = contains(filled, p);
                if (isFilled) {
                    if (out.is(expected) && out.getCount() > 0) {
                        produced++;
                    }
                    inputConsumed &= factory.getItem(p).getCount() < 4;
                } else {
                    othersEmpty &= out.isEmpty();
                }
            }
            level.removeBlock(pos, false);
            ok = produced == filled.length && inputConsumed && othersEmpty;
            LOGGER.info("{} {} [{}] processes={} filled={} produced={} inputConsumed={} othersEmpty={}",
                  TAG, ok ? "OK  " : "FAIL", blockId, processes, filled.length, produced, inputConsumed, othersEmpty);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [{}] item->item factory threw", TAG, blockId, t);
        }
        return ok;
    }

    /** item+chemical -> item factory: fill inputs + the shared tank, tick, assert each produced expectedItem + tank drained. */
    private static boolean itemChemicalToItem(ServerLevel level, String blockId, int[] filled, Item input, Item expected, BlockPos pos) {
        boolean ok = false;
        try {
            FactoryBlockEntity factory = place(level, blockId, pos);
            int processes = factory.processes();
            for (int p : filled) {
                factory.setItem(p, new ItemStack(input, 4));
            }
            Holder<Chemical> demo = FabricChemicalRegistry.demo();
            // Shared tank: enough for all processes (each consumes 100 per completion).
            factory.getInputTank().setStack(new ChemicalStack(demo, 5_000L));
            long startChemical = factory.getInputTank().getStored();
            factory.getEnergyContainers(null).getFirst().insert(50_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            tick(factory);

            int produced = 0;
            boolean inputConsumed = true;
            for (int p : filled) {
                ItemStack out = factory.getItem(processes + p).copy();
                if (out.is(expected) && out.getCount() > 0) {
                    produced++;
                }
                inputConsumed &= factory.getItem(p).getCount() < 4;
            }
            boolean chemicalConsumed = factory.getInputTank().getStored() < startChemical;
            level.removeBlock(pos, false);
            ok = produced == filled.length && inputConsumed && chemicalConsumed;
            LOGGER.info("{} {} [{}] processes={} filled={} produced={} inputConsumed={} chemicalConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", blockId, processes, filled.length, produced, inputConsumed, chemicalConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [{}] item+chemical->item factory threw", TAG, blockId, t);
        }
        return ok;
    }

    /** combining factory: fill main inputs + the shared extra slot, tick, assert each produced expectedItem + extra drained. */
    private static boolean combining(ServerLevel level, String blockId, int[] filled, Item main, Item extra, Item expected, BlockPos pos) {
        boolean ok = false;
        try {
            FactoryBlockEntity factory = place(level, blockId, pos);
            int processes = factory.processes();
            for (int p : filled) {
                factory.setItem(p, new ItemStack(main, 4));
            }
            // Shared extra slot is at index P; give it enough for all processes.
            factory.setItem(processes, new ItemStack(extra, 16));
            int extraStart = factory.getItem(processes).getCount();
            factory.getEnergyContainers(null).getFirst().insert(50_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            tick(factory);

            int produced = 0;
            boolean inputConsumed = true;
            for (int p : filled) {
                // Combining output slots start at index P+1.
                ItemStack out = factory.getItem(processes + 1 + p).copy();
                if (out.is(expected) && out.getCount() > 0) {
                    produced++;
                }
                inputConsumed &= factory.getItem(p).getCount() < 4;
            }
            boolean extraConsumed = factory.getItem(processes).getCount() < extraStart;
            level.removeBlock(pos, false);
            ok = produced == filled.length && inputConsumed && extraConsumed;
            LOGGER.info("{} {} [{}] processes={} filled={} produced={} inputConsumed={} extraConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", blockId, processes, filled.length, produced, inputConsumed, extraConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [{}] combining factory threw", TAG, blockId, t);
        }
        return ok;
    }

    /** Places the factory block and returns its block-entity. */
    private static FactoryBlockEntity place(ServerLevel level, String blockId, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", blockId));
        level.setBlock(pos, block.defaultBlockState(), 3);
        return (FactoryBlockEntity) level.getBlockEntity(pos);
    }

    /** Ticks the factory enough times for every process to complete one operation. */
    private static void tick(FactoryBlockEntity factory) {
        for (int i = 0; i < FactoryBlockEntity.MAX_PROGRESS + 10; i++) {
            factory.serverTick();
        }
    }

    private static boolean contains(int[] arr, int v) {
        for (int a : arr) {
            if (a == v) {
                return true;
            }
        }
        return false;
    }
}
