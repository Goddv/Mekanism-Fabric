package mekanism.fabric.service;

import mekanism.api.IMekanismAccessBase;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import mekanism.common.recipe.ingredients.ChemicalIngredientCreator;
import mekanism.common.recipe.ingredients.creator.ChemicalStackIngredientCreator;
import mekanism.fabric.recipe.FabricItemStackIngredientCreator;

/**
 * Fabric implementation of {@link IMekanismAccessBase} (the loader-neutral creator-access seam). Mirrors NeoForge's
 * {@code MekanismAccess} for the accessors whose types live in {@code :common}; resolved via the service loader
 * (descriptor {@code META-INF/services/mekanism.api.IMekanismAccessBase}). The JEI/EMI helpers and the NeoForge
 * fluid-ingredient creator are not part of this base, so they are absent on Fabric by construction.
 *
 * <p>The chemical-ingredient creators return the REAL hoisted {@code :common} impls
 * ({@link ChemicalIngredientCreator#INSTANCE} / {@link ChemicalStackIngredientCreator#INSTANCE}) — same FQN/instances
 * NeoForge uses. Their dispatch codec resolves the {@code chemical_ingredient_type} registry through
 * {@link mekanism.api.recipes.ingredients.chemical.IChemicalIngredientTypeRegistry}, whose Fabric impl wraps the registry
 * built in {@link mekanism.fabric.chemical.FabricChemicalIngredientTypes}.
 */
public class FabricMekanismAccess implements IMekanismAccessBase {

    @Override
    public IItemStackIngredientCreator itemStackIngredientCreator() {
        return FabricItemStackIngredientCreator.INSTANCE;
    }

    @Override
    public IChemicalIngredientCreator chemicalIngredientCreator() {
        return ChemicalIngredientCreator.INSTANCE;
    }

    @Override
    public IChemicalStackIngredientCreator chemicalStackIngredientCreator() {
        return ChemicalStackIngredientCreator.INSTANCE;
    }
}
