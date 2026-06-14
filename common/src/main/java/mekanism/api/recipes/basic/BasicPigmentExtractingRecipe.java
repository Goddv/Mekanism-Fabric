package mekanism.api.recipes.basic;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class BasicPigmentExtractingRecipe extends BasicItemStackToChemicalRecipe {

    private static final Identifier PIGMENT_EXTRACTING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "pigment_extracting");
    private static final Identifier PIGMENT_EXTRACTOR_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "pigment_extractor");

    /**
     * @param input  Input.
     * @param output Output.
     */
    public BasicPigmentExtractingRecipe(ItemStackIngredient input, ChemicalStack output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToChemicalRecipe> resolveType() {
        return (RecipeType<ItemStackToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(PIGMENT_EXTRACTING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(PIGMENT_EXTRACTOR_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicPigmentExtractingRecipe> getSerializer() {
        return (RecipeSerializer<BasicPigmentExtractingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(PIGMENT_EXTRACTING_ID);
    }
}
