package mekanism.common.recipe.lookup.cache;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeTypeBase;
import mekanism.common.recipe.lookup.cache.DoubleInputRecipeCache.DoubleSameInputRecipeCache;
import mekanism.common.recipe.lookup.cache.type.ChemicalInputCache;
import mekanism.common.recipe.lookup.cache.type.FluidInputCache;
import mekanism.common.recipe.lookup.cache.type.ItemInputCache;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.fluids.FluidStack;

public class InputRecipeCache {

    public static class SingleItem<RECIPE extends MekanismRecipe<?> & Predicate<ItemStack>>
          extends SingleInputRecipeCache<Item, ItemStack, ItemStackIngredient, RECIPE, ItemInputCache<RECIPE>> {

        public SingleItem(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, ItemStackIngredient> inputExtractor) {
            super(recipeType, inputExtractor, new ItemInputCache<>());
        }
    }

    public static class SingleFluid<RECIPE extends MekanismRecipe<?> & Predicate<FluidStack>>
          extends SingleInputRecipeCache<Fluid, FluidStack, FluidStackIngredient, RECIPE, FluidInputCache<RECIPE>> {

        public SingleFluid(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, FluidStackIngredient> inputExtractor) {
            super(recipeType, inputExtractor, new FluidInputCache<>());
        }
    }

    public static class SingleChemical<RECIPE extends MekanismRecipe<?> & Predicate<ChemicalStack>>
          extends SingleInputRecipeCache<Chemical, ChemicalStack, ChemicalStackIngredient, RECIPE, ChemicalInputCache<RECIPE>> {

        public SingleChemical(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, ChemicalStackIngredient> inputExtractor) {
            super(recipeType, inputExtractor, new ChemicalInputCache<>());
        }
    }

    public static class DoubleItem<RECIPE extends MekanismRecipe<?> & BiPredicate<ItemStack, ItemStack>>
          extends DoubleSameInputRecipeCache<Item, ItemStack, ItemStackIngredient, RECIPE, ItemInputCache<RECIPE>> {

        public DoubleItem(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, ItemStackIngredient> inputAExtractor,
              Function<RECIPE, ItemStackIngredient> inputBExtractor) {
            super(recipeType, inputAExtractor, inputBExtractor, ItemInputCache::new);
        }
    }

    public static class ItemChemical<RECIPE extends MekanismRecipe<?> & BiPredicate<ItemStack, ChemicalStack>> extends
          DoubleInputRecipeCache<Item, ItemStack, ItemStackIngredient, Chemical, ChemicalStack, ChemicalStackIngredient, RECIPE, ItemInputCache<RECIPE>, ChemicalInputCache<RECIPE>> {

        public ItemChemical(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, ItemStackIngredient> inputAExtractor,
              Function<RECIPE, ChemicalStackIngredient> inputBExtractor) {
            super(recipeType, inputAExtractor, new ItemInputCache<>(), inputBExtractor, new ChemicalInputCache<>());
        }
    }

    public static class FluidChemical<RECIPE extends MekanismRecipe<?> & BiPredicate<FluidStack, ChemicalStack>> extends
          DoubleInputRecipeCache<Fluid, FluidStack, FluidStackIngredient, Chemical, ChemicalStack, ChemicalStackIngredient, RECIPE, FluidInputCache<RECIPE>, ChemicalInputCache<RECIPE>> {

        public FluidChemical(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, FluidStackIngredient> inputAExtractor,
              Function<RECIPE, ChemicalStackIngredient> inputBExtractor) {
            super(recipeType, inputAExtractor, new FluidInputCache<>(), inputBExtractor, new ChemicalInputCache<>());
        }
    }

    public static class EitherSideChemical<RECIPE extends ChemicalChemicalToChemicalRecipe>
          extends EitherSideInputRecipeCache<Chemical, ChemicalStack, ChemicalStackIngredient, RECIPE, ChemicalInputCache<RECIPE>> {

        public EitherSideChemical(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType) {
            super(recipeType, ChemicalChemicalToChemicalRecipe::getLeftInput, ChemicalChemicalToChemicalRecipe::getRightInput, new ChemicalInputCache<>());
        }
    }

    public static class ItemFluidChemical<RECIPE extends MekanismRecipe<?> & TriPredicate<ItemStack, FluidStack, ChemicalStack>> extends
          TripleInputRecipeCache<Item, ItemStack, ItemStackIngredient, Fluid, FluidStack, FluidStackIngredient, Chemical, ChemicalStack, ChemicalStackIngredient, RECIPE, ItemInputCache<RECIPE>,
                FluidInputCache<RECIPE>, ChemicalInputCache<RECIPE>> {

        public ItemFluidChemical(MekanismRecipeTypeBase<?, RECIPE, ?> recipeType, Function<RECIPE, ItemStackIngredient> inputAExtractor,
              Function<RECIPE, FluidStackIngredient> inputBExtractor, Function<RECIPE, ChemicalStackIngredient> inputCExtractor) {
            super(recipeType, inputAExtractor, new ItemInputCache<>(), inputBExtractor, new FluidInputCache<>(), inputCExtractor, new ChemicalInputCache<>());
        }
    }
}