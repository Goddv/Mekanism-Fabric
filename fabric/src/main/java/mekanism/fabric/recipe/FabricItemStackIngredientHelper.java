package mekanism.fabric.recipe;

import com.mojang.logging.LogUtils;
import java.util.List;
import mekanism.api.SerializerHelper;
import mekanism.api.recipes.ingredients.IItemStackIngredientHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.component.DataComponentPatch;
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

    @Override
    public Ingredient componentIngredient(DataComponentPatch components, Holder<Item> item) {
        //Component-sensitive item ingredients are a NeoForge-only construction path (vanilla Ingredient has no component
        //matching). No Mekanism recipe builds one at Fabric runtime - recipes load from bundled JSON via the codec - so
        //this is deferred (a fabric-api CustomIngredient impl would be needed to support it).
        throw new UnsupportedOperationException("Component-sensitive item ingredients are not yet supported on Fabric.");
    }

    @Override
    public HolderSet<Item> combineTags(List<HolderSet<Item>> tags) {
        //Multi-tag OR ingredients are constructed only at datagen time (which runs on NeoForge); no Fabric runtime caller.
        throw new UnsupportedOperationException("Multi-tag (OR) item ingredients are not yet supported on Fabric.");
    }
}
