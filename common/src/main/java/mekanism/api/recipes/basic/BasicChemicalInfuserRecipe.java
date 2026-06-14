package mekanism.api.recipes.basic;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class BasicChemicalInfuserRecipe extends BasicChemicalChemicalToChemicalRecipe {

    private static final Identifier CHEMICAL_INFUSING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_infusing");
    private static final Identifier CHEMICAL_INFUSER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "chemical_infuser");

    /**
     * @param leftInput  Left input.
     * @param rightInput Right input.
     * @param output     Output.
     *
     * @apiNote The order of the inputs does not matter.
     */
    public BasicChemicalInfuserRecipe(ChemicalStackIngredient leftInput, ChemicalStackIngredient rightInput, ChemicalStack output) {
        super(leftInput, rightInput, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ChemicalChemicalToChemicalRecipe> resolveType() {
        return (RecipeType<ChemicalChemicalToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(CHEMICAL_INFUSING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CHEMICAL_INFUSER_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicChemicalInfuserRecipe> getSerializer() {
        return (RecipeSerializer<BasicChemicalInfuserRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(CHEMICAL_INFUSING_ID);
    }
}
