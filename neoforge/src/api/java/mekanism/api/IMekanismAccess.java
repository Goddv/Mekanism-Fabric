package mekanism.api;

import mekanism.api.integration.emi.IMekanismEmiHelper;
import mekanism.api.integration.jei.IMekanismJEIHelper;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import mekanism.api.recipes.ingredients.creator.IFluidStackIngredientCreator;

/**
 * Provides access to a variety of different helpers that are exposed to the API.
 *
 * <p>The loader-neutral creator accessors live on the {@code :common} super-interface {@link IMekanismAccessBase}; this
 * interface adds the loader-bound accessors (JEI/EMI helpers + the NeoForge fluid-ingredient creator).
 *
 * @since 10.4.0
 */
public interface IMekanismAccess extends IMekanismAccessBase {

    /**
     * Provides access to Mekanism's internals.
     */
    IMekanismAccess INSTANCE = MekanismAPI.getService(IMekanismAccess.class);

    /**
     * Gets a helper to interact with some of Mekanism's JEI integration internals. This should only be called if JEI is loaded.
     *
     * @throws IllegalStateException if JEI is not loaded.
     */
    IMekanismJEIHelper jeiHelper();

    /**
     * Gets a helper to interact with some of Mekanism's EMI integration internals. This should only be called if EMI is loaded.
     *
     * @throws IllegalStateException if EMI is not loaded.
     * @since 10.5.10
     */
    IMekanismEmiHelper emiHelper();

    //Note: itemStackIngredientCreator() is inherited from IMekanismAccessBase (:common) - its return type is now
    // loader-neutral. The remaining accessors below stay here because their return types are loader-bound.

    /**
     * Gets the fluid stack ingredient creator.
     *
     * @apiNote Use {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#fluid()} instead.
     */
    IFluidStackIngredientCreator fluidStackIngredientCreator();

    /**
     * Gets the chemical stack ingredient creator.
     *
     * @apiNote Use {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#chemicalStack()} instead.
     * @since 10.7.0
     */
    IChemicalStackIngredientCreator chemicalStackIngredientCreator();

    /**
     * Gets the chemical ingredient creator.
     *
     * @apiNote Use {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#chemical()} instead.
     * @since 10.7.0
     */
    IChemicalIngredientCreator chemicalIngredientCreator();
}