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
public class BasicPigmentMixingRecipe extends BasicChemicalChemicalToChemicalRecipe {

    private static final Identifier PIGMENT_MIXING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "pigment_mixing");
    private static final Identifier PIGMENT_MIXER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "pigment_mixer");

    /**
     * @param leftInput  Left input.
     * @param rightInput Right input.
     * @param output     Output.
     *
     * @apiNote The order of the inputs does not matter.
     */
    public BasicPigmentMixingRecipe(ChemicalStackIngredient leftInput, ChemicalStackIngredient rightInput, ChemicalStack output) {
        super(leftInput, rightInput, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ChemicalChemicalToChemicalRecipe> resolveType() {
        return (RecipeType<ChemicalChemicalToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(PIGMENT_MIXING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(PIGMENT_MIXER_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicPigmentMixingRecipe> getSerializer() {
        return (RecipeSerializer<BasicPigmentMixingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(PIGMENT_MIXING_ID);
    }
}
