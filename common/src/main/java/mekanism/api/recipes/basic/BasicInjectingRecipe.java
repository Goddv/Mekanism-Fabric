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
public class BasicInjectingRecipe extends BasicItemStackChemicalToItemStackRecipe {

    private static final Identifier INJECTING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "injecting");
    private static final Identifier CHEMICAL_INJECTION_CHAMBER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_injection_chamber");

    public BasicInjectingRecipe(ItemStackIngredient itemInput, ChemicalStackIngredient chemicalInput, ItemStackTemplate output, boolean perTickUsage) {
        super(itemInput, chemicalInput, output, perTickUsage, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackChemicalToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackChemicalToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(INJECTING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicInjectingRecipe> getSerializer() {
        return (RecipeSerializer<BasicInjectingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(INJECTING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CHEMICAL_INJECTION_CHAMBER_ID));
    }
}
