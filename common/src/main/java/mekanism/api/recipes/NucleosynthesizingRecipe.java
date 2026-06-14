package mekanism.api.recipes;

import mekanism.api.MekanismAPIBase;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Extension of {@link ItemStackChemicalToItemStackRecipe} with a defined amount of ticks needed to process. Input: ItemStack
 * <br>
 * Input: Chemical (Base value, will be multiplied by a per tick amount)
 * <br>
 * Output: ItemStack
 *
 * @apiNote Nucleosynthesizers can process this recipe type.
 */
@NothingNullByDefault
public abstract class NucleosynthesizingRecipe extends ItemStackChemicalToItemStackRecipe {

    private static final Identifier NUCLEOSYNTHESIZING_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "nucleosynthesizing");
    private static final Identifier ANTIPROTONIC_NUCLEOSYNTHESIZER_ID = Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "antiprotonic_nucleosynthesizer");

    @SuppressWarnings("unchecked")
    @Override
    public final RecipeType<NucleosynthesizingRecipe> getType() {
        return (RecipeType<NucleosynthesizingRecipe>) (RecipeType<?>) BuiltInRegistries.RECIPE_TYPE.getValue(NUCLEOSYNTHESIZING_ID);
    }

    /**
     * Gets the duration in ticks this recipe takes to complete.
     */
    public abstract int getDuration();

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(ANTIPROTONIC_NUCLEOSYNTHESIZER_ID));
    }
}
