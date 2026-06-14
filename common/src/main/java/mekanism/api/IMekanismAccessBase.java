package mekanism.api;

import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-neutral half of {@link mekanism.api.IMekanismAccess} (the NeoForge {@code IMekanismAccess} extends this). Holds
 * only the ingredient-creator accessors whose return types are loader-neutral and live in {@code :common}; the JEI/EMI
 * helper accessors (which import {@code mezz.jei.*} / {@code dev.emi.*}) and the fluid-ingredient-creator accessor (whose
 * return type is NeoForge-bound) stay on the loader-resident {@code IMekanismAccess}.
 *
 * <p>This is the seam that lets {@code :common} reach the ingredient creators without dragging the recipe-viewer
 * integrations into the common module. Resolved via {@link MekanismAPIBase#getService}; every loader registers an
 * implementation (NeoForge reuses its existing {@code MekanismAccess}; Fabric ships {@code FabricMekanismAccess}).
 *
 * <p>As more creator return types reach {@code :common} (the chemical ingredient family), their accessors move here too.
 * Public API consumers should use {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess} /
 * {@code CommonIngredientCreatorAccess} rather than calling this directly.
 */
@Internal
public interface IMekanismAccessBase {

    IMekanismAccessBase INSTANCE = MekanismAPIBase.getService(IMekanismAccessBase.class);

    /**
     * Gets the item stack ingredient creator.
     *
     * @apiNote Use {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#item()} instead.
     */
    IItemStackIngredientCreator itemStackIngredientCreator();
}
