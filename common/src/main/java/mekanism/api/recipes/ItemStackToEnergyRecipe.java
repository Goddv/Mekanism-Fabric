package mekanism.api.recipes;

import java.util.function.Predicate;
import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Input: ItemStack
 * <br>
 * Output: FloatingLong
 *
 * @apiNote Energy conversion recipes can be used in any slots in Mekanism machines that are able to convert items into energy.
 */
@NothingNullByDefault
public abstract class ItemStackToEnergyRecipe extends MekanismRecipe<SingleRecipeInput> implements Predicate<@NotNull ItemStack> {

    private static final Identifier ENERGY_CONVERSION_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "energy_conversion");
    private static final Identifier ENERGY_TABLET_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "energy_tablet");

    @Override
    public abstract boolean test(ItemStack itemStack);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        //Don't match incomplete recipes or ones that don't match
        return !isIncomplete() && test(input.item());
    }

    /**
     * Gets the input ingredient.
     */
    public abstract ItemStackIngredient getInput();

    /**
     * Gets the output based on the given input.
     *
     * @param input Specific input.
     *
     * @return Output as a constant.
     *
     * @apiNote While Mekanism does not currently make use of the input, it is important to support it and pass the proper value in case any addons define input based
     * outputs where things like NBT may be different.
     * @implNote The passed in input should <strong>NOT</strong> be modified.
     */
    @Range(from = 1, to = Long.MAX_VALUE)
    public abstract long getOutput(ItemStack input);

    /**
     * For JEI, gets the output representations to display.
     *
     * @return Representation of the output, <strong>MUST NOT</strong> be modified.
     */
    public abstract long[] getOutputDefinition();

    @Override
    public boolean isIncomplete() {
        return getInput().hasNoMatchingInstances();
    }

    @Override
    public void logMissingTags() {
        getInput().logMissingTags();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final RecipeType<ItemStackToEnergyRecipe> getType() {
        return (RecipeType<ItemStackToEnergyRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(ENERGY_CONVERSION_ID);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ENERGY_TABLET_ID));
    }
}
