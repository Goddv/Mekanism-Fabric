package mekanism.fabric.recipe;

import com.mojang.serialization.Codec;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.IChemicalStackIngredientCreator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Throwing placeholder for the {@code :common} {@link IChemicalStackIngredientCreator} on Fabric. Every (non-default)
 * method throws {@link UnsupportedOperationException}.
 *
 * <p><b>Why a stub:</b> identical rationale to {@link FabricStubChemicalIngredientCreator} -- no {@code :common} or
 * Fabric code builds a {@link ChemicalStackIngredient} at Fabric runtime yet (the recipes that embed it are still
 * NeoForge-resident), so this stub is never invoked. It only keeps the {@code FabricMekanismAccess} service contract
 * total now that {@link mekanism.api.IMekanismAccessBase} declares {@code chemicalStackIngredientCreator()}. The real
 * Fabric implementation lands with the chemical-ingredient dispatch increment.
 *
 * <p>Note: the convenience {@code from/fromHolder/fromHolders/from(tag)} default methods inherited from
 * {@code IChemicalStackIngredientCreator} route through {@code CommonIngredientCreatorAccess.chemical()} (the stub
 * creator), so they too transitively throw -- which is the intended behavior until the dispatch is ported.
 */
@NothingNullByDefault
public class FabricStubChemicalStackIngredientCreator implements IChemicalStackIngredientCreator {

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Chemical stack ingredient creation is not yet ported to Fabric (the dispatch codec + type registry are a later increment); this stub should never be invoked at runtime.");
    }

    @Override
    public ChemicalStackIngredient from(ChemicalIngredient ingredient, long amount) {
        throw unsupported();
    }

    @Override
    public Codec<ChemicalStackIngredient> codec() {
        throw unsupported();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChemicalStackIngredient> streamCodec() {
        throw unsupported();
    }
}
