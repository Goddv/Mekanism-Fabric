package mekanism.fabric.service;

import mekanism.api.IMekanismAccessBase;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import mekanism.fabric.recipe.FabricItemStackIngredientCreator;

/**
 * Fabric implementation of {@link IMekanismAccessBase} (the loader-neutral creator-access seam). Mirrors NeoForge's
 * {@code MekanismAccess} for the accessors whose types live in {@code :common}; resolved via the service loader
 * (descriptor {@code META-INF/services/mekanism.api.IMekanismAccessBase}). The JEI/EMI helpers and the NeoForge
 * fluid-ingredient creator are not part of this base, so they are absent on Fabric by construction.
 */
public class FabricMekanismAccess implements IMekanismAccessBase {

    @Override
    public IItemStackIngredientCreator itemStackIngredientCreator() {
        return FabricItemStackIngredientCreator.INSTANCE;
    }
}
