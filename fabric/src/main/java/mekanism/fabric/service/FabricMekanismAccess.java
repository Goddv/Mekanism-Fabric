package mekanism.fabric.service;

import mekanism.api.IMekanismAccessBase;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import mekanism.fabric.recipe.FabricItemStackIngredientCreator;
import mekanism.fabric.recipe.FabricStubChemicalIngredientCreator;
import mekanism.fabric.recipe.FabricStubChemicalStackIngredientCreator;

/**
 * Fabric implementation of {@link IMekanismAccessBase} (the loader-neutral creator-access seam). Mirrors NeoForge's
 * {@code MekanismAccess} for the accessors whose types live in {@code :common}; resolved via the service loader
 * (descriptor {@code META-INF/services/mekanism.api.IMekanismAccessBase}). The JEI/EMI helpers and the NeoForge
 * fluid-ingredient creator are not part of this base, so they are absent on Fabric by construction.
 *
 * <p>The chemical-ingredient creators return throwing stubs ({@link FabricStubChemicalIngredientCreator} /
 * {@link FabricStubChemicalStackIngredientCreator}): the chemical-ingredient TYPES + the two creator INTERFACES have
 * hoisted to {@code :common}, but the dispatch IMPL (type registry + {@code xor}/{@code dispatchMapOrElse} codecs) is
 * still NeoForge-only, and no {@code :common}/Fabric code builds a chemical ingredient at Fabric runtime yet. The stubs
 * keep this service total until the real Fabric chemical creator is ported.
 */
public class FabricMekanismAccess implements IMekanismAccessBase {

    private static final FabricStubChemicalIngredientCreator CHEMICAL_INGREDIENT_CREATOR = new FabricStubChemicalIngredientCreator();
    private static final FabricStubChemicalStackIngredientCreator CHEMICAL_STACK_INGREDIENT_CREATOR = new FabricStubChemicalStackIngredientCreator();

    @Override
    public IItemStackIngredientCreator itemStackIngredientCreator() {
        return FabricItemStackIngredientCreator.INSTANCE;
    }

    @Override
    public IChemicalIngredientCreator chemicalIngredientCreator() {
        return CHEMICAL_INGREDIENT_CREATOR;
    }

    @Override
    public IChemicalStackIngredientCreator chemicalStackIngredientCreator() {
        return CHEMICAL_STACK_INGREDIENT_CREATOR;
    }
}
