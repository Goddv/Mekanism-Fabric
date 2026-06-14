package mekanism.common.recipe.ingredients;

import java.util.List;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.api.SerializerHelper;
import mekanism.api.recipes.ingredients.IItemStackIngredientHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * NeoForge implementation of {@link IItemStackIngredientHelper}. Preserves the original {@code ItemStackIngredient}
 * behavior verbatim: custom (component-sensitive) ingredients are resolved by converting the typed instance — including
 * its NeoForge {@code ItemResource} form — into a full {@link ItemStack} and testing it; non-custom ingredients test by
 * holder. The missing-tag diagnostic uses NeoForge's {@code Ingredient#getValues()} tag introspection.
 */
public class NeoItemStackIngredientHelper implements IItemStackIngredientHelper {

    @Override
    public boolean testType(Ingredient ingredient, TypedInstance<Item> instance) {
        if (ingredient.isCustom()) {
            //Component data might be necessary, make it into a stack and test it
            ItemStack stack = switch (instance) {
                case ItemStack stackIn -> stackIn;
                case ItemStackTemplate template -> template.create();
                case ItemResource resource -> resource.toStack();
                //TODO: Is there a decent way to grab any potential components patch?
                default -> new ItemStack(instance.typeHolder());
            };
            return ingredient.test(stack);
        }
        //Vanilla ingredients don't need to check component data so we can just skip converting the resource to a stack
        return ingredient.acceptsItem(instance.typeHolder());
    }

    @Override
    public void logMissingTags(Ingredient ingredient) {
        //TODO - 26.1: Re-evaluate this implementation
        if (ingredient.isCustom()) {
            MekanismAPI.logger.error("Empty ItemStackIngredient: {}", SerializerHelper.stringify(Ingredient.CODEC, ingredient));
        } else {
            Optional<TagKey<Item>> tagKey = ingredient.getValues().unwrapKey();
            if (tagKey.isPresent()) {
                MekanismAPI.logger.error("Empty tag: {}", tagKey.get());
            } else {
                MekanismAPI.logger.error("Empty ItemStackIngredient: {}", SerializerHelper.stringify(Ingredient.CODEC, ingredient));
            }
        }
    }

    @Override
    public Ingredient componentIngredient(DataComponentPatch components, Holder<Item> item) {
        return DataComponentIngredient.of(false, components, item);
    }

    @Override
    public HolderSet<Item> combineTags(List<HolderSet<Item>> tags) {
        return new OrHolderSet<>(tags);
    }
}
