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
public class BasicChemicalOxidizerRecipe extends BasicItemStackToChemicalRecipe {

    private static final Identifier OXIDIZING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "oxidizing");
    private static final Identifier CHEMICAL_OXIDIZER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_oxidizer");

    public BasicChemicalOxidizerRecipe(ItemStackIngredient input, ChemicalStack output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToChemicalRecipe> resolveType() {
        return (RecipeType<ItemStackToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(OXIDIZING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicChemicalOxidizerRecipe> getSerializer() {
        return (RecipeSerializer<BasicChemicalOxidizerRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(OXIDIZING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CHEMICAL_OXIDIZER_ID));
    }
}
