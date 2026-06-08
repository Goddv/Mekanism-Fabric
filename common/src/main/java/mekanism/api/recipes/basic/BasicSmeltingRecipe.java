package mekanism.api.recipes.basic;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * @implNote Loader-neutral: the smelting {@link RecipeType}/{@link RecipeSerializer} are resolved by id from the vanilla
 * {@code BuiltInRegistries} (both loaders register them under {@code mekanism:smelting}), so one class works on both.
 */
@NothingNullByDefault
public class BasicSmeltingRecipe extends BasicItemStackToItemStackRecipe implements IBasicItemStackOutput {

    private static final Identifier SMELTING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "smelting");
    private static final Identifier ENERGIZED_SMELTER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "energized_smelter");

    public BasicSmeltingRecipe(ItemStackIngredient input, ItemStackTemplate output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(SMELTING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicSmeltingRecipe> getSerializer() {
        return (RecipeSerializer<BasicSmeltingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(SMELTING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ENERGIZED_SMELTER_ID));
    }
}
