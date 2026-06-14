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
public class BasicCentrifugingRecipe extends BasicChemicalToChemicalRecipe {

    private static final Identifier CENTRIFUGING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "centrifuging");
    private static final Identifier ISOTOPIC_CENTRIFUGE_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "isotopic_centrifuge");

    public BasicCentrifugingRecipe(ChemicalStackIngredient input, ChemicalStack output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ChemicalToChemicalRecipe> resolveType() {
        return (RecipeType<ChemicalToChemicalRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(CENTRIFUGING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicCentrifugingRecipe> getSerializer() {
        return (RecipeSerializer<BasicCentrifugingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(CENTRIFUGING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ISOTOPIC_CENTRIFUGE_ID));
    }

}
