package mekanism.fabric.chemical;

import com.mojang.serialization.MapCodec;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.IChemicalIngredientTypeRegistry;
import net.minecraft.core.Registry;

/**
 * Fabric implementation of {@link IChemicalIngredientTypeRegistry} — delegates to the {@link FabricChemicalIngredientTypes}
 * registry created during mod init. Registered via META-INF/services. This is what backs the {@code dispatchMapOrElse}
 * type key in the hoisted {@code :common} {@code ChemicalIngredientCreator} on Fabric.
 */
public class FabricChemicalIngredientTypeRegistry implements IChemicalIngredientTypeRegistry {

    @Override
    public Registry<MapCodec<? extends ChemicalIngredient>> chemicalIngredientTypes() {
        return FabricChemicalIngredientTypes.registry();
    }
}
