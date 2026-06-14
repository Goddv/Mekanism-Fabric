package mekanism.common.recipe.serializer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.basic.BasicItemStackToChemicalRecipe;
import mekanism.api.recipes.basic.BasicItemStackToItemStackRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Loader-neutral subset of Mekanism's recipe-serializer factories — just the helpers needed for the item&rarr;item
 * (enriching/crushing/smelting) path. Built purely from vanilla {@link RecipeSerializer} + the hoisted
 * {@link ItemStackIngredient}/{@link ItemStackTemplate} codecs, so it compiles and behaves identically on both loaders.
 * The full {@code MekanismRecipeSerializer} (chemical/fluid factories) remains NeoForge-side until those stacks hoist.
 */
public final class MekanismRecipeSerializerHelper {

    private MekanismRecipeSerializerHelper() {
    }

    public static <RECIPE extends Recipe<?>> RecipeSerializer<RECIPE> singleton(RECIPE instance) {
        return new RecipeSerializer<>(MapCodec.unit(instance), StreamCodec.unit(instance));
    }

    public static <RECIPE extends BasicItemStackToItemStackRecipe> RecipeSerializer<RECIPE> itemToItem(BiFunction<ItemStackIngredient, ItemStackTemplate, RECIPE> factory) {
        return new RecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(BasicItemStackToItemStackRecipe::getInput),
              ItemStackTemplate.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicItemStackToItemStackRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicItemStackToItemStackRecipe::getInput,
              ItemStackTemplate.STREAM_CODEC, BasicItemStackToItemStackRecipe::getOutputRaw,
              factory
        ));
    }

    /**
     * Loader-neutral item&rarr;chemical serializer factory (mirrors {@link #itemToItem(BiFunction)} but with a
     * {@link ChemicalStack} output). Built from the same field names + codec structure as NeoForge's
     * {@code MekanismRecipeSerializer.itemToChemical} (input via {@link ItemStackIngredient#CODEC} under
     * {@link SerializationConstants#INPUT}, output via {@link ChemicalStack#MAP_CODEC} under
     * {@link SerializationConstants#OUTPUT}), so the shared {@code oxidizing} recipe JSON loads identically on both
     * loaders. Used by the Chemical Oxidizer ({@code BasicChemicalOxidizerRecipe}).
     */
    public static <RECIPE extends BasicItemStackToChemicalRecipe> RecipeSerializer<RECIPE> itemToChemical(BiFunction<ItemStackIngredient, ChemicalStack, RECIPE> factory) {
        return new RecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(BasicItemStackToChemicalRecipe::getInput),
              ChemicalStack.MAP_CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicItemStackToChemicalRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicItemStackToChemicalRecipe::getInput,
              ChemicalStack.STREAM_CODEC, BasicItemStackToChemicalRecipe::getOutputRaw,
              factory
        ));
    }
}
