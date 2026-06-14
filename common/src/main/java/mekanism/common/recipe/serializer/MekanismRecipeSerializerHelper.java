package mekanism.common.recipe.serializer;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.basic.BasicChemicalCrystallizerRecipe;
import mekanism.api.recipes.basic.BasicItemStackToChemicalRecipe;
import mekanism.api.recipes.basic.BasicItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.IBasicItemStackOutput;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.codec.ByteBufCodecs;
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

    /**
     * Loader-neutral chemical&rarr;item serializer factory for the Chemical Crystallizer ({@code crystallizing}). Mirrors
     * NeoForge's {@code MekanismRecipeSerializer.crystallizing} EXACTLY (input via {@link ChemicalStackIngredient#CODEC}
     * under {@link SerializationConstants#INPUT}, output via {@link ItemStackTemplate#CODEC} under
     * {@link SerializationConstants#OUTPUT}). NeoForge uses {@code IngredientCreatorAccess.chemicalStack().codec()}, which
     * returns exactly {@link ChemicalStackIngredient#CODEC} (and {@code .streamCodec()} returns
     * {@link ChemicalStackIngredient#STREAM_CODEC}), so the produced (de)serialization is byte-identical and the shared
     * {@code crystallizing} recipe JSON loads identically on both loaders. Used by {@link BasicChemicalCrystallizerRecipe}.
     */
    /**
     * Loader-neutral item+chemical&rarr;item serializer factory for the {@code item chemical to item} machine family
     * (Osmium Compressor {@code compressing}, Purification Chamber {@code purifying}, Chemical Injection Chamber
     * {@code injecting}, Metallurgic Infuser {@code metallurgic_infusing}, Painting Machine {@code painting}). Mirrors
     * NeoForge's {@code MekanismRecipeSerializer.itemChemicalToItem} EXACTLY: item input via {@link ItemStackIngredient#CODEC}
     * under {@link SerializationConstants#ITEM_INPUT}, chemical input via {@link ChemicalStackIngredient#CODEC} under
     * {@link SerializationConstants#CHEMICAL_INPUT}, output via {@link ItemStackTemplate#CODEC} under
     * {@link SerializationConstants#OUTPUT}, and a {@link SerializationConstants#PER_TICK_USAGE} boolean. NeoForge uses
     * {@code IngredientCreatorAccess.chemicalStack().codec()} for the chemical input, which returns exactly
     * {@link ChemicalStackIngredient#CODEC} (and {@code .streamCodec()} returns {@link ChemicalStackIngredient#STREAM_CODEC}),
     * so the produced (de)serialization is byte-identical and the shared recipe JSON loads identically on both loaders.
     */
    public static <RECIPE extends ItemStackChemicalToItemStackRecipe & IBasicItemStackOutput> RecipeSerializer<RECIPE> itemChemicalToItem(
          Function4<ItemStackIngredient, ChemicalStackIngredient, ItemStackTemplate, Boolean, RECIPE> factory) {
        return new RecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.ITEM_INPUT).forGetter(ItemStackChemicalToItemStackRecipe::getItemInput),
              ChemicalStackIngredient.CODEC.fieldOf(SerializationConstants.CHEMICAL_INPUT).forGetter(ItemStackChemicalToItemStackRecipe::getChemicalInput),
              ItemStackTemplate.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(IBasicItemStackOutput::getOutputRaw),
              Codec.BOOL.fieldOf(SerializationConstants.PER_TICK_USAGE).forGetter(ItemStackChemicalToItemStackRecipe::perTickUsage)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, ItemStackChemicalToItemStackRecipe::getItemInput,
              ChemicalStackIngredient.STREAM_CODEC, ItemStackChemicalToItemStackRecipe::getChemicalInput,
              ItemStackTemplate.STREAM_CODEC, IBasicItemStackOutput::getOutputRaw,
              ByteBufCodecs.BOOL, ItemStackChemicalToItemStackRecipe::perTickUsage,
              factory
        ));
    }

    public static RecipeSerializer<BasicChemicalCrystallizerRecipe> crystallizing(BiFunction<ChemicalStackIngredient, ItemStackTemplate, BasicChemicalCrystallizerRecipe> factory) {
        return new RecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ChemicalStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(BasicChemicalCrystallizerRecipe::getInput),
              ItemStackTemplate.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicChemicalCrystallizerRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ChemicalStackIngredient.STREAM_CODEC, BasicChemicalCrystallizerRecipe::getInput,
              ItemStackTemplate.STREAM_CODEC, BasicChemicalCrystallizerRecipe::getOutputRaw,
              factory
        ));
    }
}
