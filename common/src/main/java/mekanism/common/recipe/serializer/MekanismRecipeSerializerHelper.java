package mekanism.common.recipe.serializer;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.BiFunction;
import mekanism.api.ItemStackTemplateHelper;
import mekanism.api.SerializationConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.basic.BasicChemicalCrystallizerRecipe;
import mekanism.api.recipes.basic.BasicCombinerRecipe;
import mekanism.api.recipes.basic.BasicItemStackToChemicalRecipe;
import mekanism.api.recipes.basic.BasicItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.BasicSawmillRecipe;
import mekanism.api.recipes.basic.IBasicItemStackOutput;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

    /**
     * Loader-neutral item+item&rarr;item serializer factory for the Combiner ({@code combining}). Mirrors NeoForge's
     * {@code MekanismRecipeSerializer.combining} EXACTLY: main input via {@link ItemStackIngredient#CODEC} under
     * {@link SerializationConstants#MAIN_INPUT}, extra input via {@link ItemStackIngredient#CODEC} under
     * {@link SerializationConstants#EXTRA_INPUT}, output via {@link ItemStackTemplate#CODEC} under
     * {@link SerializationConstants#OUTPUT} (getters {@link CombinerRecipe#getMainInput()}/{@link CombinerRecipe#getExtraInput()}
     * and {@link BasicCombinerRecipe#getOutputRaw()}). Built purely from vanilla {@link RecipeSerializer} + the hoisted
     * {@link ItemStackIngredient}/{@link ItemStackTemplate} codecs, so the shared {@code combining} recipe JSON loads
     * identically on both loaders. Used by {@link BasicCombinerRecipe}.
     */
    public static RecipeSerializer<BasicCombinerRecipe> combining(Function3<ItemStackIngredient, ItemStackIngredient, ItemStackTemplate, BasicCombinerRecipe> factory) {
        return new RecipeSerializer<>(RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.MAIN_INPUT).forGetter(CombinerRecipe::getMainInput),
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.EXTRA_INPUT).forGetter(CombinerRecipe::getExtraInput),
              ItemStackTemplate.CODEC.fieldOf(SerializationConstants.OUTPUT).forGetter(BasicCombinerRecipe::getOutputRaw)
        ).apply(instance, factory)), StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, BasicCombinerRecipe::getMainInput,
              ItemStackIngredient.STREAM_CODEC, BasicCombinerRecipe::getExtraInput,
              ItemStackTemplate.STREAM_CODEC, BasicCombinerRecipe::getOutputRaw,
              factory
        ));
    }

    /**
     * Loader-neutral item&rarr;item+chance-secondary serializer factory for the Precision Sawmill ({@code sawing}). Ports
     * NeoForge's {@code SawmillRecipeSerializer.create} VERBATIM (it is pure Mojang/vanilla codec): input via
     * {@link ItemStackIngredient#CODEC} under {@link SerializationConstants#INPUT}; main output via an optional
     * {@link ItemStackTemplate#CODEC} under {@link SerializationConstants#MAIN_OUTPUT}; secondary output via an optional
     * {@link ItemStackTemplate#CODEC} under {@link SerializationConstants#SECONDARY_OUTPUT}; secondary chance via an
     * optional {@code Codec.DOUBLE} (validated {@code 0 < c <= 1}) under {@link SerializationConstants#SECONDARY_CHANCE}.
     * Uses {@link SerializerHelper#oneRequired} (at least one output) + {@link SerializerHelper#dependentOptionality} (chance
     * depends on the secondary output) and {@link ItemStackTemplateHelper#OPTIONAL_STREAM_CODEC} for the optional outputs,
     * exactly like NeoForge — so the shared {@code sawing} recipe JSON + network payload are byte-identical on both loaders.
     * Used by {@link BasicSawmillRecipe}.
     */
    public static RecipeSerializer<BasicSawmillRecipe> sawing(Function4<ItemStackIngredient, ItemStackTemplate, ItemStackTemplate, Double, BasicSawmillRecipe> factory) {
        Codec<Double> chanceCodec = Codec.DOUBLE.validate(d -> d > 0 && d <= 1 ? DataResult.success(d) : DataResult.error(() -> "Expected secondaryChance to be greater than zero, and less than or equal to one. Found " + d));
        MapCodec<Optional<Double>> secondaryChanceFieldBase = chanceCodec.optionalFieldOf(SerializationConstants.SECONDARY_CHANCE);
        MapCodec<Optional<ItemStackTemplate>> mainOutputFieldBase = ItemStackTemplate.CODEC.optionalFieldOf(SerializationConstants.MAIN_OUTPUT);
        RecordCodecBuilder<BasicSawmillRecipe, Optional<ItemStackTemplate>> secondaryOutputField = ItemStackTemplate.CODEC.optionalFieldOf(SerializationConstants.SECONDARY_OUTPUT).forGetter(BasicSawmillRecipe::getSecondaryOutputRaw);

        MapCodec<BasicSawmillRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
              ItemStackIngredient.CODEC.fieldOf(SerializationConstants.INPUT).forGetter(SawmillRecipe::getInput),
              SerializerHelper.oneRequired(secondaryOutputField, mainOutputFieldBase, BasicSawmillRecipe::getMainOutputRaw),
              secondaryOutputField,
              SerializerHelper.dependentOptionality(secondaryOutputField, secondaryChanceFieldBase, sawmillRecipe -> {
                  double secondaryChance = sawmillRecipe.getSecondaryChance();
                  return secondaryChance == 0 ? Optional.empty() : Optional.of(secondaryChance);
              })
        ).apply(instance, (input, mainOutput, secondaryOutput, secondChance) ->
              factory.apply(input, mainOutput.orElse(null), secondaryOutput.orElse(null), secondChance.orElse(0D))
        ));
        StreamCodec<RegistryFriendlyByteBuf, BasicSawmillRecipe> streamCodec = StreamCodec.composite(
              ItemStackIngredient.STREAM_CODEC, SawmillRecipe::getInput,
              ItemStackTemplateHelper.OPTIONAL_STREAM_CODEC, BasicSawmillRecipe::getMainOutputRaw,
              ItemStackTemplateHelper.OPTIONAL_STREAM_CODEC, BasicSawmillRecipe::getSecondaryOutputRaw,
              ByteBufCodecs.DOUBLE, SawmillRecipe::getSecondaryChance,
              (input, mainOutput, secondaryOutput, secondChance) ->
                    factory.apply(input, mainOutput.orElse(null), secondaryOutput.orElse(null), secondChance)
        );

        return new RecipeSerializer<>(codec, streamCodec);
    }
}
