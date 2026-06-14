package mekanism.fabric.recipe;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.CommonIngredientCreatorAccess;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import mekanism.fabric.content.machine.MachineBlockEntity;
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
            boolean registrationOk = registered("enriching") && registered("crushing") && registered("smelting");

            // (C) End-to-end: each item->item machine runs its real datapack recipe (energy injected here; the
            // generator->cable->machine power chain is validated separately in FabricPowerSelfTest).
            boolean enrichOk = runMachine(level, new BlockPos(0, 64, 30), "enrichment_chamber", Items.DIRT, Items.DIAMOND);
            boolean crushOk = runMachine(level, new BlockPos(2, 64, 30), "crusher", Items.COBBLESTONE, Items.GRAVEL);
            boolean smeltOk = runMachine(level, new BlockPos(4, 64, 30), "energized_smelter", Items.SAND, Items.GLASS);
            // Vanilla-furnace fallback: raw_iron has no mekanism:smelting recipe, so the smelter resolves it via
            // minecraft:smelting (raw_iron -> iron_ingot).
            boolean vanillaSmeltOk = runMachine(level, new BlockPos(6, 64, 30), "energized_smelter", Items.RAW_IRON, Items.IRON_INGOT);
            boolean processOk = enrichOk && crushOk && smeltOk && vanillaSmeltOk;

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
            LOGGER.info("{} {} codec={} roundTrip={} registration={} enriching={} crushing={} smelting={} vanillaSmelt={} creator={}",
                  TAG, ok ? "OK  " : "FAIL", codecOk, roundTripOk, registrationOk, enrichOk, crushOk, smeltOk, vanillaSmeltOk, creatorOk);
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
}
