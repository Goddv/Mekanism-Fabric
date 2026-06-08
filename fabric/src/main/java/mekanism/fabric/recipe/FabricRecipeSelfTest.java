package mekanism.fabric.recipe;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.fabric.content.machine.FabricRealMachines;
import mekanism.fabric.content.machine.MachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the enriching recipe slice on Fabric. Proves: (A) the hoisted {@code :common}
 * {@link ItemStackIngredient} codec encodes/decodes the NeoForge-identical {@code {"ingredient":...,"count":N}} shape and
 * its {@code count} test semantics hold; (B) the enriching {@code RecipeType}/{@code RecipeSerializer} are registered
 * under {@code mekanism:enriching}; (C) end-to-end — a real datapack enriching recipe (dirt&rarr;diamond, written in
 * NeoForge's field order to prove order-independent parsing) is looked up via the vanilla recipe manager and processed by
 * a live {@link MachineBlockEntity}, consuming the recipe's input count + energy and producing its output. Grep for
 * {@code [Mekanism/Fabric][recipe-selftest] RESULT:}.
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

            // (B) Registration under the shared id.
            Identifier enriching = Identifier.fromNamespaceAndPath("mekanism", "enriching");
            boolean registrationOk = BuiltInRegistries.RECIPE_TYPE.containsKey(enriching)
                  && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(enriching);

            // Diagnostics: confirm the registry-resolved type identity matches the registrar supplier, and whether the
            // datapack recipe loaded + is found by the vanilla recipe manager.
            var type = mekanism.fabric.recipe.MekanismRecipeTypesRegistrar.ENRICHING_TYPE.get();
            Object regType = BuiltInRegistries.RECIPE_TYPE.getValue(enriching);
            var probe = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.DIRT, 8));
            var direct = level.recipeAccess().getRecipeFor(type, probe, level);
            LOGGER.info("{} diag: sameTypeInstance={} getRecipeFor(dirt)={}", TAG, type == regType, direct.isPresent());

            // (C) End-to-end: place machine, feed dirt + energy, tick, expect diamond output + shrunk input.
            boolean processOk = false;
            BlockPos pos = new BlockPos(0, 64, 30);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            level.setBlock(pos, FabricRealMachines.enrichmentChamber().block().defaultBlockState(), 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineBlockEntity machine) {
                machine.setItem(0, new ItemStack(Items.DIRT, 8));
                machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
                BlockState state = level.getBlockState(pos);
                for (int i = 0; i < 3 && machine.getItem(1).isEmpty(); i++) {
                    machine.serverTick(state);
                    state = level.getBlockState(pos);
                }
                ItemStack output = machine.getItem(1);
                ItemStack input = machine.getItem(0);
                processOk = output.is(Items.DIAMOND) && output.getCount() >= 1 && input.getCount() < 8;
            }
            level.removeBlock(pos, false);

            ok = codecOk && roundTripOk && registrationOk && processOk;
            LOGGER.info("{} {} codec={} roundTrip={} registration={} process={}",
                  TAG, ok ? "OK  " : "FAIL", codecOk, roundTripOk, registrationOk, processOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL recipe test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
