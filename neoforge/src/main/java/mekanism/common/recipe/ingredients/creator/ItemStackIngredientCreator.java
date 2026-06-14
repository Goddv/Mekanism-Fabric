package mekanism.common.recipe.ingredients.creator;

import com.mojang.serialization.Codec;
import java.util.Objects;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IItemStackIngredientCreator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.ApiStatus;

@NothingNullByDefault
public class ItemStackIngredientCreator implements IItemStackIngredientCreator {

    public static final ItemStackIngredientCreator INSTANCE = new ItemStackIngredientCreator();

    private ItemStackIngredientCreator() {
    }

    @Override
    public Codec<ItemStackIngredient> codec() {
        return ItemStackIngredient.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ItemStackIngredient> streamCodec() {
        return ItemStackIngredient.STREAM_CODEC;
    }

    /**
     * Creates an Item Stack Ingredient from a NeoForge {@link SizedIngredient}. NeoForge-only overload (kept off the
     * loader-neutral {@link IItemStackIngredientCreator} since {@code SizedIngredient} is a NeoForge type); relocated
     * verbatim from the interface during the ingredient-API hoist to :common.
     *
     * @param ingredient Sized ingredient to match.
     *
     * @throws NullPointerException if the given instance is null.
     * @since 10.6.0
     */
    public ItemStackIngredient from(SizedIngredient ingredient) {
        Objects.requireNonNull(ingredient, "ItemStackIngredients cannot be created from a null ingredient.");
        return ItemStackIngredient.of(ingredient.ingredient(), ingredient.count());
    }
}