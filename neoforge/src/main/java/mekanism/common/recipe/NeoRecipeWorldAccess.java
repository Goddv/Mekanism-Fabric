package mekanism.common.recipe;

import mekanism.client.MekanismClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge {@link IRecipeWorldAccess}: the exact fallback-RecipeMap acquisition relocated verbatim from
 * {@code MekanismRecipeType.getRecipes(Level)} — a ServerLevel's own {@code recipeAccess().recipeMap()}, else the client
 * map ({@code MekanismClient.clientRecipes()}), else the running server's {@code getRecipeManager().recipeMap()}, else null.
 */
public class NeoRecipeWorldAccess implements IRecipeWorldAccess {

    @Nullable
    @Override
    public RecipeMap activeRecipeMap(@Nullable Level world) {
        RecipeMap recipeMap = null;
        if (!(world instanceof ServerLevel serverLevel)) {
            //Try to get a fallback world if we are in a context that may not have one
            //If we are on the client get the client's world, if we are on the server get the current server's world
            if (FMLEnvironment.getDist().isClient()) {
                recipeMap = MekanismClient.clientRecipes();
            } else {
                MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
                if (currentServer != null) {
                    recipeMap = currentServer.getRecipeManager().recipeMap();
                }
            }
        } else {
            recipeMap = serverLevel.recipeAccess().recipeMap();
        }
        return recipeMap;
    }
}
