package mekanism.common.recipe;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import mekanism.api.MekanismAPIBase;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MekanismRecipeTypeBase<VANILLA_INPUT extends RecipeInput, RECIPE extends MekanismRecipe<VANILLA_INPUT>, INPUT_CACHE extends IInputRecipeCache>
      implements RecipeType<RECIPE>, IMekanismRecipeTypeProvider<VANILLA_INPUT, RECIPE, INPUT_CACHE> {

    private List<RecipeHolder<RECIPE>> cachedRecipes = Collections.emptyList();
    private final Identifier registryName;
    private final INPUT_CACHE inputCache;

    protected MekanismRecipeTypeBase(Identifier name, Function<MekanismRecipeTypeBase<VANILLA_INPUT, RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator) {
        this.registryName = name;
        this.inputCache = inputCacheCreator.apply(this);
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    @Override
    public Identifier getRegistryName() {
        return registryName;
    }

    @Override
    public MekanismRecipeTypeBase<VANILLA_INPUT, RECIPE, INPUT_CACHE> getRecipeType() {
        return this;
    }

    protected void clearCaches() {
        cachedRecipes = Collections.emptyList();
        inputCache.clear();
    }

    @Override
    public INPUT_CACHE getInputCache() {
        return inputCache;
    }

    @NotNull
    @Override
    public List<RecipeHolder<RECIPE>> getRecipes(@Nullable Level world) {
        //Loader-neutral fallback-RecipeMap acquisition (the FMLEnvironment/MekanismClient/ServerLifecycleHooks +
        //ServerLevel.recipeAccess().recipeMap() logic relocated verbatim into NeoRecipeWorldAccess); recipeMap() is a
        //NeoForge addition absent on Fabric, so it lives behind the seam.
        RecipeMap recipeMap = IRecipeWorldAccess.INSTANCE.activeRecipeMap(world);
        if (recipeMap == null) {
            //If we failed, then return no recipes
            return Collections.emptyList();
        }
        return getRecipes(recipeMap);
    }

    @NotNull
    @Override
    public List<RecipeHolder<RECIPE>> getRecipes(RecipeMap recipeMap) {
        if (cachedRecipes.isEmpty()) {
            //Note: This is a fresh immutable list that gets returned
            Collection<RecipeHolder<RECIPE>> recipes = getRecipesUncached(recipeMap);
            //Make the list of cached recipes immutable and filter out any incomplete recipes
            // as there is no reason to potentially look the partial complete piece up if
            // the other portion of the recipe is incomplete
            cachedRecipes = recipes.stream()
                  .filter(recipe -> !recipe.value().isIncomplete())
                  .toList();
        }
        return cachedRecipes;
    }

    /**
     * Get a list of recipes directly from the manager
     *
     * @param recipeMap The recipes map
     */
    @NotNull
    protected Collection<RecipeHolder<RECIPE>> getRecipesUncached(RecipeMap recipeMap) {
        Collection<RecipeHolder<RECIPE>> recipes = recipeMap.byType(this);
        return mergeGeneratedRecipes(recipeMap, recipes);
    }

    /**
     * Hook for loaders/recipe-types that need to merge synthetically generated recipes (e.g. wrapped vanilla smelting) into the
     * recipe list. The base implementation performs no merging and returns the recipes unchanged.
     *
     * @param recipeMap The recipes map
     * @param recipes   The recipes resolved directly from the map for this type.
     */
    protected Collection<RecipeHolder<RECIPE>> mergeGeneratedRecipes(RecipeMap recipeMap, Collection<RecipeHolder<RECIPE>> recipes) {
        return recipes;
    }

    @SuppressWarnings("unchecked")
    protected RECIPE castRecipe(MekanismRecipe<?> o) {
        if (o.getType() != this) {
            throw new IllegalArgumentException("Wrong recipe type");
        }
        return (RECIPE) o;
    }

    protected boolean checkMyIncompleteRecipes(RecipeMap recipeMap) {
        boolean incomplete = false;
        for (RecipeHolder<RECIPE> holder : getRecipesUncached(recipeMap)) {
            if (!holder.value().isIncomplete()) {
                continue;
            }
            MekanismAPIBase.logger.error("Incomplete recipe detected: {}", holder.id());
            incomplete = true;
            holder.value().logMissingTags();
        }
        return incomplete;
    }
}
