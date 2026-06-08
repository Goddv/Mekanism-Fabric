package mekanism.api.recipes.ingredients;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import mekanism.api.SerializationConstants;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.TypedInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory.ForStacks;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation for how Mekanism handle's ItemStack Ingredients.
 * <p>
 * Create instances of this using {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#item()}.
 *
 * @implNote Loader-neutral: holds a vanilla {@link Ingredient} plus an int {@code count}. The serialized form
 * ({@code {"ingredient": <Ingredient>, "count": N}}, {@code count} defaulting to 1) is byte-identical to NeoForge's
 * {@code SizedIngredient.NESTED_CODEC}, so the same recipe JSON loads on both loaders. Loader-extended {@code Ingredient}
 * surface (custom-ingredient component matching, the transfer {@code ItemResource} form, tag introspection) is routed
 * through {@link IItemStackIngredientHelper}.
 */
@NothingNullByDefault
public final class ItemStackIngredient implements InputIngredient<Item, @NotNull ItemStack> {

    /**
     * A codec which can (de)encode item stack ingredients.
     *
     * @since 10.6.0
     */
    public static final Codec<ItemStackIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Ingredient.CODEC.fieldOf(SerializationConstants.INGREDIENT).forGetter(ItemStackIngredient::ingredient),
          ExtraCodecs.POSITIVE_INT.optionalFieldOf(SerializationConstants.COUNT).xmap(count -> count.orElse(1), Optional::of).forGetter(ItemStackIngredient::count)
    ).apply(instance, ItemStackIngredient::new));
    /**
     * A stream codec which can be used to encode and decode item stack ingredients over the network.
     *
     * @since 10.6.0
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackIngredient> STREAM_CODEC = StreamCodec.composite(
          Ingredient.CONTENTS_STREAM_CODEC, ItemStackIngredient::ingredient,
          ByteBufCodecs.VAR_INT, ItemStackIngredient::count,
          ItemStackIngredient::new
    );

    /**
     * Creates an Item Stack Ingredient that matches a given ingredient and amount. Prefer calling via
     * {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#item()}.
     *
     * @param ingredient Ingredient to match.
     * @param amount     Amount needed.
     *
     * @throws NullPointerException     if the given ingredient is null.
     * @throws IllegalArgumentException if the given amount is smaller than one.
     * @since 10.6.0
     */
    public static ItemStackIngredient of(Ingredient ingredient, int amount) {
        Objects.requireNonNull(ingredient, "ItemStackIngredients cannot be created from a null ingredient.");
        return new ItemStackIngredient(ingredient, amount);
    }

    private final Ingredient ingredient;
    private final int count;
    @Nullable
    private List<ItemStack> representations;

    private ItemStackIngredient(Ingredient ingredient, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.ingredient = ingredient;
        this.count = count;
    }

    @Override
    public boolean test(ItemStack stack) {
        Objects.requireNonNull(stack);
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public boolean testType(TypedInstance<Item> instance) {
        Objects.requireNonNull(instance);
        return IItemStackIngredientHelper.INSTANCE.testType(ingredient, instance);
    }

    @Override
    public ItemStack getMatchingInstance(ItemStack stack) {
        return test(stack) ? stack.copyWithCount(count) : ItemStack.EMPTY;
    }

    @Override
    public long getNeededAmount(TypedInstance<Item> instance) {
        return testType(instance) ? count : 0;
    }

    @Override
    public boolean hasNoMatchingInstances() {
        return ingredient.isEmpty();
    }

    @Override
    public void logMissingTags() {
        if (hasNoMatchingInstances()) {
            IItemStackIngredientHelper.INSTANCE.logMissingTags(ingredient);
        }
    }

    @Override
    public List<@NotNull ItemStack> getRepresentations(ContextMap context) {
        if (this.representations == null) {
            this.representations = ingredient.display().resolve(context, (ForStacks<ItemStack>) stack -> stack.copyWithCount(count)).toList();
        }
        return representations;
    }

    /**
     * For use in recipe input caching. Gets the internal vanilla ingredient.
     *
     * @since 10.6.0
     */
    @Internal
    public Ingredient ingredient() {
        return ingredient;
    }

    /**
     * Gets the amount of the input needed.
     *
     * @since 10.6.0
     */
    @Internal
    public int count() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemStackIngredient other = (ItemStackIngredient) o;
        return count == other.count && ingredient.equals(other.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient, count);
    }

    @Override
    public String toString() {
        return count + "x " + ingredient;
    }
}
