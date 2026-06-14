package mekanism.common.recipe.ingredients;

import com.mojang.serialization.MapCodec;
import mekanism.api.MekanismAPI;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.IChemicalIngredientTypeRegistry;
import net.minecraft.core.Registry;

/**
 * NeoForge implementation of {@link IChemicalIngredientTypeRegistry}: exposes the {@code RegistryBuilder}-created
 * {@link MekanismAPI#CHEMICAL_INGREDIENT_TYPES} to the hoisted {@code :common} dispatch codec in
 * {@link ChemicalIngredientCreator}. Registered via META-INF/services. This is the same registry instance the dispatch
 * referenced directly before the hoist, so the NeoForge wire format is byte-identical.
 */
public class ChemicalIngredientTypeRegistry implements IChemicalIngredientTypeRegistry {

    @Override
    public Registry<MapCodec<? extends ChemicalIngredient>> chemicalIngredientTypes() {
        return MekanismAPI.CHEMICAL_INGREDIENT_TYPES;
    }
}
