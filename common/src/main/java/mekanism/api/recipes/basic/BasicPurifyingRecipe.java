package mekanism.api.recipes.basic;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class BasicPurifyingRecipe extends BasicItemStackChemicalToItemStackRecipe {

    private static final Identifier PURIFYING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "purifying");
    private static final Identifier PURIFICATION_CHAMBER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "purification_chamber");

    public BasicPurifyingRecipe(ItemStackIngredient itemInput, ChemicalStackIngredient chemicalInput, ItemStackTemplate output, boolean perTickUsage) {
        super(itemInput, chemicalInput, output, perTickUsage, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackChemicalToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackChemicalToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(PURIFYING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicPurifyingRecipe> getSerializer() {
        return (RecipeSerializer<BasicPurifyingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(PURIFYING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(PURIFICATION_CHAMBER_ID));
    }
}
