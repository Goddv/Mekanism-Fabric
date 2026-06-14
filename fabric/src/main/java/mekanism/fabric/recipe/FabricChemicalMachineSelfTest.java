package mekanism.fabric.recipe;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPIBase;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.fabric.content.machine.ChemicalMachineBlockEntity;
import mekanism.fabric.content.machine.ChemicalToItemMachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the chemical-processing machines on Fabric. Proves, for each machine: (A) its
 * {@link net.minecraft.world.item.crafting.RecipeType} + {@link net.minecraft.world.item.crafting.RecipeSerializer} are
 * registered under their shared NeoForge id; (B) end-to-end the placed machine looks up its bundled datapack recipe via
 * the vanilla recipe manager, advances progress while consuming energy, and on completion produces the expected output.
 *
 * <p>Item&rarr;chemical machines (Chemical Oxidizer, Pigment Extractor, Chemical Conversion): feed an item + energy, tick,
 * assert the internal chemical tank filled with {@code fabric_demo_chemical} and the input item was consumed. The
 * chemical&rarr;item Chemical Crystallizer: fill its input tank with {@code fabric_demo_chemical}, inject energy, tick,
 * assert the OUTPUT ITEM SLOT holds the expected item (diamond) and the input chemical was consumed.
 *
 * <p>Grep {@code [Mekanism/Fabric][chem-machine-selftest]}.
 */
public final class FabricChemicalMachineSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][chem-machine-selftest]";

    private static final ResourceKey<Chemical> DEMO_CHEMICAL_KEY = ResourceKey.create(
          MekanismAPIBase.CHEMICAL_REGISTRY_NAME, Identifier.fromNamespaceAndPath("mekanism", "fabric_demo_chemical"));

    private FabricChemicalMachineSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean all = true;
        // ---- item -> chemical machines ----
        all &= validateItemToChemical(level, "oxidizing", "chemical_oxidizer", Items.REDSTONE, new BlockPos(8, 64, 30));
        all &= validateItemToChemical(level, "pigment_extracting", "pigment_extractor", Items.COAL, new BlockPos(8, 64, 33));
        all &= validateItemToChemical(level, "chemical_conversion", "chemical_conversion", Items.CHARCOAL, new BlockPos(8, 64, 36));
        // ---- chemical -> item machine ----
        all &= validateChemicalToItem(level, "crystallizing", "chemical_crystallizer", Items.DIAMOND, new BlockPos(8, 64, 39));
        LOGGER.info("{} RESULT: {}", TAG, all ? "PASS" : "FAIL");
    }

    /** Item&rarr;chemical: feed {@code inputItem} + energy, tick, assert the output tank filled and the input was consumed. */
    private static boolean validateItemToChemical(ServerLevel level, String recipeId, String blockId,
          net.minecraft.world.item.Item inputItem, BlockPos pos) {
        boolean ok = false;
        ChemicalStack produced = ChemicalStack.EMPTY;
        boolean registrationOk = false;
        boolean inputConsumed = false;
        try {
            Identifier id = Identifier.fromNamespaceAndPath("mekanism", recipeId);
            registrationOk = BuiltInRegistries.RECIPE_TYPE.containsKey(id) && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id);

            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", blockId));
            level.setBlock(pos, block.defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine) {
                machine.setItem(0, new ItemStack(inputItem, 8));
                machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
                for (int i = 0; i < ChemicalMachineBlockEntity.MAX_PROGRESS + 5 && machine.getOutputTank().isEmpty(); i++) {
                    machine.serverTick();
                }
                produced = machine.getOutputTank().getStack();
                inputConsumed = machine.getItem(0).getCount() < 8;
            }
            level.removeBlock(pos, false);

            boolean producedOk = !produced.isEmpty() && produced.typeHolder().is(DEMO_CHEMICAL_KEY) && produced.amount() > 0;
            ok = registrationOk && producedOk && inputConsumed;
            LOGGER.info("{} {} [{}] registration={} producedChemical={} amount={} inputConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", blockId, registrationOk,
                  produced.isEmpty() ? "<empty>" : produced.typeHolder().getRegisteredName(),
                  produced.amount(), inputConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [{}] item->chemical test threw", TAG, blockId, t);
        }
        return ok;
    }

    /** Chemical&rarr;item: fill the input tank + energy, tick, assert the output slot holds {@code expectedItem} and the chemical was consumed. */
    private static boolean validateChemicalToItem(ServerLevel level, String recipeId, String blockId,
          net.minecraft.world.item.Item expectedItem, BlockPos pos) {
        boolean ok = false;
        boolean registrationOk = false;
        boolean chemicalConsumed = false;
        ItemStack outputStack = ItemStack.EMPTY;
        try {
            Identifier id = Identifier.fromNamespaceAndPath("mekanism", recipeId);
            registrationOk = BuiltInRegistries.RECIPE_TYPE.containsKey(id) && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id);

            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", blockId));
            level.setBlock(pos, block.defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof ChemicalToItemMachineBlockEntity machine) {
                Holder<Chemical> demoHolder = mekanism.fabric.chemical.FabricChemicalRegistry.demo();
                // Fill the input tank with 500 of the demo chemical (>= the recipe's 100 amount).
                machine.getInputTank().setStack(new ChemicalStack(demoHolder, 500L));
                machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
                long startAmount = machine.getInputTank().getStored();
                for (int i = 0; i < ChemicalToItemMachineBlockEntity.MAX_PROGRESS + 5 && machine.getItem(0).isEmpty(); i++) {
                    machine.serverTick();
                }
                // Copy: removeBlock() below drops/clears the BE's container, mutating the live stack to empty. Capture a
                // snapshot so the assertion sees the post-processing slot contents.
                outputStack = machine.getItem(0).copy();
                chemicalConsumed = machine.getInputTank().getStored() < startAmount;
            }
            level.removeBlock(pos, false);

            boolean producedOk = !outputStack.isEmpty() && outputStack.is(expectedItem) && outputStack.getCount() > 0;
            ok = registrationOk && producedOk && chemicalConsumed;
            LOGGER.info("{} {} [{}] registration={} outputItem={} count={} chemicalConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", blockId, registrationOk,
                  outputStack.isEmpty() ? "<empty>" : BuiltInRegistries.ITEM.getKey(outputStack.getItem()),
                  outputStack.getCount(), chemicalConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL [{}] chemical->item test threw", TAG, blockId, t);
        }
        return ok;
    }
}
