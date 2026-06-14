package mekanism.api.recipes.ingredients.chemical;

import com.mojang.serialization.MapCodec;
import mekanism.api.MekanismAPIBase;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific access to the {@link ChemicalIngredient} type-serializer registry (keyed by
 * {@link MekanismAPIBase#CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME}). The registry itself is created differently per loader
 * (NeoForge {@code RegistryBuilder}, Fabric {@code FabricRegistryBuilder}); this service hands the loader-neutral
 * dispatch codec in {@code mekanism.common.recipe.ingredients.ChemicalIngredientCreator} the resulting {@link Registry}
 * (whose {@link Registry#byNameCodec()} backs the {@code dispatchMapOrElse} type key) without referencing
 * loader-specific registry-builder types. Resolved via {@link MekanismAPIBase#getService}.
 */
@Internal
public interface IChemicalIngredientTypeRegistry {

    IChemicalIngredientTypeRegistry INSTANCE = MekanismAPIBase.getService(IChemicalIngredientTypeRegistry.class);

    /**
     * {@return the chemical ingredient type-serializer registry} Created + populated by the loader during mod setup; the
     * dispatch codec captures it lazily via {@link Registry#byNameCodec()}, so it is safe to reference at static-init.
     */
    Registry<MapCodec<? extends ChemicalIngredient>> chemicalIngredientTypes();
}
