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
public class BasicPaintingRecipe extends BasicItemStackChemicalToItemStackRecipe {

    private static final Identifier PAINTING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "painting");
    private static final Identifier PAINTING_MACHINE_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "painting_machine");

    /**
     * @param itemInput     Item input.
     * @param chemicalInput Chemical input.
     * @param output        Output.
     */
    public BasicPaintingRecipe(ItemStackIngredient itemInput, ChemicalStackIngredient chemicalInput, ItemStackTemplate output, boolean perTickUsage) {
        super(itemInput, chemicalInput, output, perTickUsage, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackChemicalToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackChemicalToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(PAINTING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(PAINTING_MACHINE_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicPaintingRecipe> getSerializer() {
        return (RecipeSerializer<BasicPaintingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(PAINTING_ID);
    }
}
