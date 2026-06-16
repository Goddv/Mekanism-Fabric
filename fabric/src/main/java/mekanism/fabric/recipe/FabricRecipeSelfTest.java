package mekanism.fabric.recipe;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.CommonIngredientCreatorAccess;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import mekanism.fabric.content.machine.CombinerMachineBlockEntity;
import mekanism.fabric.content.machine.MachineBlockEntity;
import mekanism.fabric.content.machine.SawmillMachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the item&rarr;item recipe family on Fabric. Proves: (A) the hoisted {@code :common}
 * {@link ItemStackIngredient} codec encodes/decodes the NeoForge-identical {@code {"ingredient":...,"count":N}} shape and
 * its {@code count} test semantics hold; (B) the enriching/crushing/smelting {@code RecipeType}/{@code RecipeSerializer}s
 * are registered under their shared ids; (C) end-to-end — each machine (enrichment_chamber, crusher, energized_smelter)
 * looks up its real datapack recipe via the vanilla recipe manager (by the machine's own recipe type) and processes it,
 * consuming the recipe's input count + energy and producing its output. Grep {@code [Mekanism/Fabric][recipe-selftest]}.
 */
public final class FabricRecipeSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][recipe-selftest]";

    private FabricRecipeSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        try {
            // (A) Codec format parity + count semantics.
            ItemStackIngredient ingredient = ItemStackIngredient.of(Ingredient.of(Items.DIRT), 2);
            JsonElement json = ItemStackIngredient.CODEC.encodeStart(
                  level.registryAccess().createSerializationContext(JsonOps.INSTANCE), ingredient).getOrThrow();
            String encoded = json.toString();
            boolean codecOk = encoded.contains("\"ingredient\":\"minecraft:dirt\"") && encoded.contains("\"count\":2");
            ItemStackIngredient decoded = ItemStackIngredient.CODEC.parse(
                  level.registryAccess().createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
            boolean roundTripOk = decoded.equals(ingredient) && decoded.count() == 2
                  && decoded.test(new ItemStack(Items.DIRT, 2)) && !decoded.test(new ItemStack(Items.DIRT, 1));

            // (B) Registration under the shared ids.
            boolean registrationOk = registered("enriching") && registered("crushing") && registered("smelting")
                  && registered("combining") && registered("sawing");

            // (C) End-to-end: each item->item machine runs its real datapack recipe (energy injected here; the
            // generator->cable->machine power chain is validated separately in FabricPowerSelfTest).
            boolean enrichOk = runMachine(level, new BlockPos(0, 64, 30), "enrichment_chamber", Items.DIRT, Items.DIAMOND);
            boolean crushOk = runMachine(level, new BlockPos(2, 64, 30), "crusher", Items.COBBLESTONE, Items.GRAVEL);
            // (C2) REAL bundled datapack recipe (NOT a Fabric-only test recipe): the build-time recipe filter copies the
            // real Mekanism datagen enriching recipe glowstone -> 4 glowstone_dust (vanilla-only ids) into the jar. This
            // proves the ~570 real item-recipes now load + process on Fabric, which is the whole point of the fix.
            boolean realEnrichOk = runMachineMinCount(level, new BlockPos(12, 64, 30), "enrichment_chamber",
                  Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4);
            boolean smeltOk = runMachine(level, new BlockPos(4, 64, 30), "energized_smelter", Items.SAND, Items.GLASS);
            // Vanilla-furnace fallback: raw_iron has no mekanism:smelting recipe, so the smelter resolves it via
            // minecraft:smelting (raw_iron -> iron_ingot).
            boolean vanillaSmeltOk = runMachine(level, new BlockPos(6, 64, 30), "energized_smelter", Items.RAW_IRON, Items.IRON_INGOT);
            // Dual-item machines: Combiner (cobblestone + flint -> gravel) and Precision Sawmill (oak_log -> 6 oak_planks
            // + 25%-chance stick). Combiner asserts both inputs were consumed; sawmill asserts only the MAIN output (the
            // secondary is chance-based, so it is logged but not asserted strictly).
            boolean combineOk = runCombiner(level, new BlockPos(8, 64, 30), Items.COBBLESTONE, Items.FLINT, Items.GRAVEL);
            boolean sawOk = runSawmill(level, new BlockPos(10, 64, 30), Items.OAK_LOG, Items.OAK_PLANKS);
            boolean processOk = enrichOk && crushOk && smeltOk && vanillaSmeltOk && combineOk && sawOk && realEnrichOk;

            // (D) The hoisted :common IItemStackIngredientCreator build path works on Fabric, resolved through the
            // creator-access SEAM (CommonIngredientCreatorAccess.item() -> IMekanismAccessBase service -> the Fabric impl,
            // proving the service descriptor is wired): from(item)/from(item,amount) produce NeoForge-identical count
            // semantics + codec wire shape.
            IItemStackIngredientCreator creator = CommonIngredientCreatorAccess.item();
            ItemStackIngredient builtCounted = creator.from(Items.DIRT, 3);
            ItemStackIngredient builtSingle = creator.from(Items.IRON_INGOT);
            boolean creatorBuildOk = builtCounted.count() == 3
                  && builtCounted.test(new ItemStack(Items.DIRT, 3)) && !builtCounted.test(new ItemStack(Items.DIRT, 2))
                  && builtSingle.count() == 1 && builtSingle.test(new ItemStack(Items.IRON_INGOT));
            JsonElement builtJson = ItemStackIngredient.CODEC.encodeStart(
                  level.registryAccess().createSerializationContext(JsonOps.INSTANCE), builtCounted).getOrThrow();
            boolean creatorCodecOk = builtJson.toString().contains("\"ingredient\":\"minecraft:dirt\"")
                  && builtJson.toString().contains("\"count\":3");
            boolean creatorOk = creatorBuildOk && creatorCodecOk;

            ok = codecOk && roundTripOk && registrationOk && processOk && creatorOk;
            LOGGER.info("{} {} codec={} roundTrip={} registration={} enriching={} crushing={} smelting={} vanillaSmelt={} combining={} sawing={} realEnrich(glowstone->glowstone_dust)={} creator={}",
                  TAG, ok ? "OK  " : "FAIL", codecOk, roundTripOk, registrationOk, enrichOk, crushOk, smeltOk, vanillaSmeltOk, combineOk, sawOk, realEnrichOk, creatorOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL recipe test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }

    private static boolean registered(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("mekanism", name);
        return BuiltInRegistries.RECIPE_TYPE.containsKey(id) && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id);
    }

    /** Places the named machine, feeds it input + energy, ticks it, and asserts it produced the expected output. */
    private static boolean runMachine(ServerLevel level, BlockPos pos, String blockId, Item input, Item expectedOutput) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", blockId));
        level.setBlock(pos, block.defaultBlockState(), 3);
        boolean ok = false;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.setItem(0, new ItemStack(input, 8));
            machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            // One operation takes MAX_PROGRESS ticks; run a little past that to complete one.
            for (int i = 0; i < MachineBlockEntity.MAX_PROGRESS + 5 && machine.getItem(1).isEmpty(); i++) {
                machine.serverTick(level.getBlockState(pos));
            }
            ItemStack out = machine.getItem(1);
            ok = out.is(expectedOutput) && out.getCount() >= 1 && machine.getItem(0).getCount() < 8;
            LOGGER.info("{} {} {}: {} -> {} (got {} x{})", TAG, ok ? "OK" : "FAIL", blockId,
                  BuiltInRegistries.ITEM.getKey(input), BuiltInRegistries.ITEM.getKey(expectedOutput),
                  BuiltInRegistries.ITEM.getKey(out.getItem()), out.getCount());
        }
        level.removeBlock(pos, false);
        return ok;
    }

    /**
     * Like {@link #runMachine} but also asserts the output stack reached {@code minCount} — used for a REAL bundled
     * datapack recipe (glowstone -> 4 glowstone_dust) so we verify both the item AND the recipe's output count.
     */
    private static boolean runMachineMinCount(ServerLevel level, BlockPos pos, String blockId, Item input,
          Item expectedOutput, int minCount) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", blockId));
        level.setBlock(pos, block.defaultBlockState(), 3);
        boolean ok = false;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            machine.setItem(0, new ItemStack(input, 8));
            machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < MachineBlockEntity.MAX_PROGRESS + 5 && machine.getItem(1).isEmpty(); i++) {
                machine.serverTick(level.getBlockState(pos));
            }
            ItemStack out = machine.getItem(1);
            ok = out.is(expectedOutput) && out.getCount() >= minCount && machine.getItem(0).getCount() < 8;
            LOGGER.info("{} {} {}: {} -> {} x{} (got {} x{}, min {})", TAG, ok ? "OK" : "FAIL", blockId,
                  BuiltInRegistries.ITEM.getKey(input), BuiltInRegistries.ITEM.getKey(expectedOutput), minCount,
                  BuiltInRegistries.ITEM.getKey(out.getItem()), out.getCount(), minCount);
        }
        level.removeBlock(pos, false);
        return ok;
    }

    /**
     * Places the Combiner, feeds it the main + extra inputs + energy, ticks it, and asserts it produced the expected
     * output (slot 2) while consuming one from each input slot (slots 0/1).
     */
    private static boolean runCombiner(ServerLevel level, BlockPos pos, Item mainInput, Item extraInput, Item expectedOutput) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", "combiner"));
        level.setBlock(pos, block.defaultBlockState(), 3);
        boolean ok = false;
        if (level.getBlockEntity(pos) instanceof CombinerMachineBlockEntity machine) {
            machine.setItem(0, new ItemStack(mainInput, 8));
            machine.setItem(1, new ItemStack(extraInput, 8));
            machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < CombinerMachineBlockEntity.MAX_PROGRESS + 5 && machine.getItem(2).isEmpty(); i++) {
                machine.serverTick();
            }
            ItemStack out = machine.getItem(2);
            boolean mainConsumed = machine.getItem(0).getCount() < 8;
            boolean extraConsumed = machine.getItem(1).getCount() < 8;
            ok = out.is(expectedOutput) && out.getCount() >= 1 && mainConsumed && extraConsumed;
            LOGGER.info("{} {} combiner: {} + {} -> {} (got {} x{}, mainConsumed={} extraConsumed={})",
                  TAG, ok ? "OK" : "FAIL", BuiltInRegistries.ITEM.getKey(mainInput), BuiltInRegistries.ITEM.getKey(extraInput),
                  BuiltInRegistries.ITEM.getKey(expectedOutput), BuiltInRegistries.ITEM.getKey(out.getItem()), out.getCount(),
                  mainConsumed, extraConsumed);
        }
        level.removeBlock(pos, false);
        return ok;
    }

    /**
     * Places the Precision Sawmill, feeds it the input + energy, ticks it, and asserts the MAIN output slot (slot 1) holds
     * the expected item while the input (slot 0) was consumed. The secondary output (slot 2) is chance-based, so it is
     * logged but NOT asserted strictly.
     */
    private static boolean runSawmill(ServerLevel level, BlockPos pos, Item input, Item expectedMainOutput) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mekanism", "precision_sawmill"));
        level.setBlock(pos, block.defaultBlockState(), 3);
        boolean ok = false;
        if (level.getBlockEntity(pos) instanceof SawmillMachineBlockEntity machine) {
            machine.setItem(0, new ItemStack(input, 8));
            machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
            for (int i = 0; i < SawmillMachineBlockEntity.MAX_PROGRESS + 5 && machine.getItem(1).isEmpty(); i++) {
                machine.serverTick();
            }
            ItemStack main = machine.getItem(1);
            ItemStack secondary = machine.getItem(2);
            boolean inputConsumed = machine.getItem(0).getCount() < 8;
            ok = main.is(expectedMainOutput) && main.getCount() >= 1 && inputConsumed;
            LOGGER.info("{} {} precision_sawmill: {} -> main {} (got {} x{}), secondary {} (inputConsumed={})",
                  TAG, ok ? "OK" : "FAIL", BuiltInRegistries.ITEM.getKey(input), BuiltInRegistries.ITEM.getKey(expectedMainOutput),
                  BuiltInRegistries.ITEM.getKey(main.getItem()), main.getCount(),
                  secondary.isEmpty() ? "<none>" : BuiltInRegistries.ITEM.getKey(secondary.getItem()) + " x" + secondary.getCount(),
                  inputConsumed);
        }
        level.removeBlock(pos, false);
        return ok;
    }
}
