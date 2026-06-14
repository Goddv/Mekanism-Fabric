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
public class BasicChemicalConversionRecipe extends BasicItemStackToChemicalRecipe {

    private static final Identifier CHEMICAL_CONVERSION_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_conversion");
    private static final Identifier CREATIVE_CHEMICAL_TANK_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "creative_chemical_tank");

    public BasicChemicalConversionRecipe(ItemStackIngredient input, ChemicalStack output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToChemicalRecipe> resolveType() {
        return (RecipeType<ItemStackToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(CHEMICAL_CONVERSION_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicChemicalConversionRecipe> getSerializer() {
        return (RecipeSerializer<BasicChemicalConversionRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(CHEMICAL_CONVERSION_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CREATIVE_CHEMICAL_TANK_ID));
    }

}
