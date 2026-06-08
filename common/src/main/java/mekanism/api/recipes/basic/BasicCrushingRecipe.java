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
 * @implNote Loader-neutral: the crushing {@link RecipeType}/{@link RecipeSerializer} are resolved by id from the vanilla
 * {@code BuiltInRegistries} (both loaders register them under {@code mekanism:crushing}), so one class works on both.
 */
@NothingNullByDefault
public class BasicCrushingRecipe extends BasicItemStackToItemStackRecipe {

    private static final Identifier CRUSHING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "crushing");
    private static final Identifier CRUSHER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "crusher");

    public BasicCrushingRecipe(ItemStackIngredient input, ItemStackTemplate output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(CRUSHING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicCrushingRecipe> getSerializer() {
        return (RecipeSerializer<BasicCrushingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(CRUSHING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(CRUSHER_ID));
    }
}
