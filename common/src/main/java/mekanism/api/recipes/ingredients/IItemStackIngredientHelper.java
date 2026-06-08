package mekanism.api.recipes.ingredients;

import mekanism.api.MekanismAPIBase;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific helper for the parts of {@link ItemStackIngredient} that touch loader-extended {@code Ingredient}
 * surface. On NeoForge {@code Ingredient} is patched with {@code isCustom()}/{@code getValues()} and the transfer layer
 * uses {@code ItemResource}; none of that exists in {@code :common} (vanilla) or on Fabric. This service hides it.
 *
 * <p>{@link #testType(Ingredient, TypedInstance)} mirrors the original ItemStackIngredient logic: NeoForge resolves
 * custom (component-sensitive) ingredients by converting the instance — including its {@code ItemResource} form — to a
 * full stack and testing it; the vanilla/Fabric path is simply {@code ingredient.acceptsItem(instance.typeHolder())}.
 *
 * <p>Resolved via {@link MekanismAPIBase#getService}; every loader registers an implementation.
 */
@Internal
public interface IItemStackIngredientHelper {

    IItemStackIngredientHelper INSTANCE = MekanismAPIBase.getService(IItemStackIngredientHelper.class);

    /**
     * Type-only test (ignores size) for the given vanilla ingredient against an arbitrary typed instance.
     */
    boolean testType(Ingredient ingredient, TypedInstance<Item> instance);

    /**
     * Logs a diagnostic for an empty/incomplete ingredient (e.g. a missing tag). Only invoked for incomplete recipes.
     */
    void logMissingTags(Ingredient ingredient);
}
