package mekanism.fabric.recipe;

import com.mojang.serialization.Codec;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Fabric implementation of the hoisted {@code :common} {@link IItemStackIngredientCreator}. The factory surface is all
 * default methods built on vanilla {@code Ingredient}; only the two codec accessors are supplied here, and both delegate
 * straight to the loader-neutral {@link ItemStackIngredient} codecs (byte-identical wire format to NeoForge). The two
 * NeoForge-only construction branches (component-sensitive + multi-tag OR) route through
 * {@code IItemStackIngredientHelper} and are deferred on Fabric (see {@link FabricItemStackIngredientHelper}); no
 * Mekanism recipe builds those at Fabric runtime.
 *
 * <p>Not yet wired into a creator-access facade ({@code IMekanismAccess} split is the next stage); accessed directly via
 * {@link #INSTANCE} for now (validated by {@code FabricRecipeSelfTest}).
 */
@NothingNullByDefault
public class FabricItemStackIngredientCreator implements IItemStackIngredientCreator {

    public static final FabricItemStackIngredientCreator INSTANCE = new FabricItemStackIngredientCreator();

    private FabricItemStackIngredientCreator() {
    }

    @Override
    public Codec<ItemStackIngredient> codec() {
        return ItemStackIngredient.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemStackIngredient> streamCodec() {
        return ItemStackIngredient.STREAM_CODEC;
    }
}
