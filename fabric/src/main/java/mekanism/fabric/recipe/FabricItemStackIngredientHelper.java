package mekanism.fabric.recipe;

import com.mojang.logging.LogUtils;
import mekanism.api.SerializerHelper;
import mekanism.api.recipes.ingredients.IItemStackIngredientHelper;
import net.minecraft.core.TypedInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;

/**
 * Fabric {@link IItemStackIngredientHelper}. On Fabric recipe ingredients are always plain vanilla {@link Ingredient}s
 * (there is no NeoForge custom-ingredient layer and no {@code ItemResource}), so type testing is the vanilla holder
 * check {@code ingredient.acceptsItem(typeHolder())}; the {@link ItemStack}/{@link ItemStackTemplate} cases are handled
 * for completeness. Missing-tag diagnostics fall back to stringifying the empty ingredient.
 */
public class FabricItemStackIngredientHelper implements IItemStackIngredientHelper {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public boolean testType(Ingredient ingredient, TypedInstance<Item> instance) {
        return switch (instance) {
            case ItemStack stack -> ingredient.test(stack);
            case ItemStackTemplate template -> ingredient.test(template.create());
            default -> ingredient.acceptsItem(instance.typeHolder());
        };
    }

    @Override
    public void logMissingTags(Ingredient ingredient) {
        LOGGER.error("Empty ItemStackIngredient: {}", SerializerHelper.stringify(Ingredient.CODEC, ingredient));
    }
}
