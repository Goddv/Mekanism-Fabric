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
public class BasicMetallurgicInfuserRecipe extends BasicItemStackChemicalToItemStackRecipe {

    private static final Identifier METALLURGIC_INFUSING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "metallurgic_infusing");
    private static final Identifier METALLURGIC_INFUSER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "metallurgic_infuser");

    /**
     * @param itemInput     Item input.
     * @param chemicalInput Infusion input.
     * @param output        Output.
     */
    public BasicMetallurgicInfuserRecipe(ItemStackIngredient itemInput, ChemicalStackIngredient chemicalInput, ItemStackTemplate output, boolean perTickUsage) {
        super(itemInput, chemicalInput, output, perTickUsage, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackChemicalToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackChemicalToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(METALLURGIC_INFUSING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(METALLURGIC_INFUSER_ID));
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicMetallurgicInfuserRecipe> getSerializer() {
        return (RecipeSerializer<BasicMetallurgicInfuserRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(METALLURGIC_INFUSING_ID);
    }
}
