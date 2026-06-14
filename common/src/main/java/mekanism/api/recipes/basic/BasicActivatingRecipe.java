package mekanism.api.recipes.basic;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalToChemicalRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class BasicActivatingRecipe extends BasicChemicalToChemicalRecipe {

    private static final Identifier ACTIVATING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "activating");
    private static final Identifier SOLAR_NEUTRON_ACTIVATOR_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "solar_neutron_activator");

    public BasicActivatingRecipe(ChemicalStackIngredient input, ChemicalStack output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ChemicalToChemicalRecipe> resolveType() {
        return (RecipeType<ChemicalToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(ACTIVATING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicActivatingRecipe> getSerializer() {
        return (RecipeSerializer<BasicActivatingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(ACTIVATING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(SOLAR_NEUTRON_ACTIVATOR_ID));
    }
}
