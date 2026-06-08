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
 * @implNote Loader-neutral: the enriching {@link RecipeType} and {@link RecipeSerializer} are resolved by id from the
 * vanilla {@code BuiltInRegistries} (both loaders register them under {@code mekanism:enriching} — NeoForge via its own
 * {@code MekanismRecipeType}/{@code MekanismRecipeSerializersInternal}, Fabric via the Architectury registrar), so a
 * single code path returns each loader's registered objects without referencing loader-specific holders.
 */
@NothingNullByDefault
public class BasicEnrichingRecipe extends BasicItemStackToItemStackRecipe implements IBasicItemStackOutput {

    private static final Identifier ENRICHING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "enriching");
    private static final Identifier ENRICHMENT_CHAMBER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "enrichment_chamber");

    public BasicEnrichingRecipe(ItemStackIngredient input, ItemStackTemplate output) {
        super(input, output, resolveType());
    }

    @SuppressWarnings("unchecked")
    private static RecipeType<ItemStackToItemStackRecipe> resolveType() {
        return (RecipeType<ItemStackToItemStackRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(ENRICHING_ID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeSerializer<BasicEnrichingRecipe> getSerializer() {
        return (RecipeSerializer<BasicEnrichingRecipe>) (RecipeSerializer<?>) BuiltInRegistries.RECIPE_SERIALIZER.getValue(ENRICHING_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ENRICHMENT_CHAMBER_ID));
    }
}
