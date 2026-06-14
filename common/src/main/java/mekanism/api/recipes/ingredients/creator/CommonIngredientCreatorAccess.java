package mekanism.api.recipes.ingredients.creator;

import mekanism.api.IMekanismAccessBase;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-neutral counterpart of {@link IngredientCreatorAccess} reachable from {@code :common}, routed through
 * {@link IMekanismAccessBase#INSTANCE}. Exposes only the creators whose types live in {@code :common}; the NeoForge
 * {@code IngredientCreatorAccess} (with its {@code fluid()} accessor) stays at its FQN for the NeoForge call sites.
 *
 * <p>As the chemical ingredient family reaches {@code :common}, its {@code chemical()} / {@code chemicalStack()}
 * accessors will be added here (and to {@link IMekanismAccessBase}).
 */
@Internal
public final class CommonIngredientCreatorAccess {

    private CommonIngredientCreatorAccess() {
    }

    /**
     * Gets the item stack ingredient creator.
     */
    public static IItemStackIngredientCreator item() {
        return IMekanismAccessBase.INSTANCE.itemStackIngredientCreator();
    }
}
