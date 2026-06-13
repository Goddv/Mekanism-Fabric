package mekanism.fabric.recipe;

import mekanism.common.recipe.IRecipeWorldAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric {@link IRecipeWorldAccess}: builds the active {@link RecipeMap} from a {@code ServerLevel}'s vanilla recipe set
 * ({@code recipeAccess().getRecipes()} — {@code getRecipes()} is vanilla, unlike NeoForge's {@code recipeMap()}); returns
 * {@link RecipeMap#EMPTY} when no server world is available (e.g. client). NOTE: not yet exercised — the recipe-type
 * caching core that calls this is still NeoForge-resident (MekanismRecipeType); this impl lands ahead of the
 * MekanismRecipeTypeBase :common base-split. A populated client-side RecipeMap (client recipe sync) is a later item.
 */
public class FabricRecipeWorldAccess implements IRecipeWorldAccess {

    @Nullable
    @Override
    public RecipeMap activeRecipeMap(@Nullable Level world) {
        if (world instanceof ServerLevel serverLevel) {
            return RecipeMap.create(serverLevel.recipeAccess().getRecipes());
        }
        return RecipeMap.EMPTY;
    }
}
