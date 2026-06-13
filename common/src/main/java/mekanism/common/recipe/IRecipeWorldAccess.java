package mekanism.common.recipe;

import mekanism.api.MekanismAPIBase;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-specific resolution of the active {@link RecipeMap} for a (possibly null) world. Mekanism's recipe-type caching
 * core needs the recipe map even in contexts without a {@code ServerLevel}; on NeoForge that goes through
 * {@code RecipeManager.recipeMap()} + {@code FMLEnvironment}/{@code MekanismClient}/{@code ServerLifecycleHooks} (none of
 * which {@code :common} may name — {@code recipeMap()} in particular is a NeoForge addition, absent on the Fabric
 * {@code RecipeManager}). The recipe-type core routes its fallback acquisition through this service so it can live in
 * {@code :common}. Resolved via {@link MekanismAPIBase#getService} (same precedent as {@code ITileSyncService}).
 */
@Internal
public interface IRecipeWorldAccess {

    IRecipeWorldAccess INSTANCE = MekanismAPIBase.getService(IRecipeWorldAccess.class);

    /**
     * Resolves the active {@link RecipeMap} for {@code world} (may be {@code null} if none is available), replicating the
     * exact branch order of the original {@code MekanismRecipeType.getRecipes(Level)}: a {@code ServerLevel}'s own recipe
     * map; else the client recipe map; else the running server's recipe map; else {@code null}.
     */
    @Nullable
    RecipeMap activeRecipeMap(@Nullable Level world);
}
