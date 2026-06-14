package mekanism.fabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.creator.IChemicalIngredientCreator;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;

/**
 * Throwing placeholder for the {@code :common} {@link IChemicalIngredientCreator} on Fabric. Every method throws
 * {@link UnsupportedOperationException}.
 *
 * <p><b>Why a stub:</b> no {@code :common} or Fabric code references the chemical-ingredient types yet -- the recipe
 * classes that embed {@link ChemicalIngredient} (and thus invoke {@code CompoundChemicalIngredient.CODEC} /
 * {@code IChemicalIngredientCreator.codec()} via the dispatch) are still NeoForge-resident. So this stub is never
 * invoked at Fabric runtime; it exists only to keep the {@code FabricMekanismAccess} service contract total now that
 * {@link mekanism.api.IMekanismAccessBase} declares {@code chemicalIngredientCreator()}.
 *
 * <p>The real Fabric chemical creator -- with the {@code CHEMICAL_INGREDIENT_TYPES} type registry + the dispatch codec
 * (NeoForge's {@code xor} / {@code dispatchMapOrElse} re-implemented in pure Mojang DFU) -- is a later increment. The
 * {@code aliasedFieldOf} wire format the dispatch relies on is already validated independently in
 * {@code FabricChemicalSelfTest} via {@link mekanism.api.recipes.codec.MekanismExtraCodecs} directly.
 */
@NothingNullByDefault
public class FabricStubChemicalIngredientCreator implements IChemicalIngredientCreator {

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Chemical ingredient creation is not yet ported to Fabric (the dispatch codec + type registry are a later increment); this stub should never be invoked at runtime.");
    }

    @Override
    public MapCodec<ChemicalIngredient> singleOrTagCodec() {
        throw unsupported();
    }

    @Override
    public MapCodec<ChemicalIngredient> mapCodecNonEmpty() {
        throw unsupported();
    }

    @Override
    public Codec<List<ChemicalIngredient>> listCodec() {
        throw unsupported();
    }

    @Override
    public Codec<List<ChemicalIngredient>> listCodecNonEmpty() {
        throw unsupported();
    }

    @Override
    public Codec<List<ChemicalIngredient>> listCodecMultipleElements() {
        throw unsupported();
    }

    @Override
    public Codec<ChemicalIngredient> codec() {
        throw unsupported();
    }

    @Override
    public Codec<ChemicalIngredient> codecNonEmpty() {
        throw unsupported();
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChemicalIngredient> streamCodec() {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient of(Holder<Chemical> holder) {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient tag(TagKey<Chemical> tag) {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient compound(List<ChemicalIngredient> children) {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient difference(ChemicalIngredient base, ChemicalIngredient subtracted) {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient intersection(ChemicalIngredient... ingredients) {
        throw unsupported();
    }

    @Override
    public ChemicalIngredient intersection(List<? extends ChemicalIngredient> ingredients) {
        throw unsupported();
    }
}
