package mekanism.api.recipes.cache;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ChemicalToChemicalRecipe;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.ElectrolysisRecipe.ElectrolysisRecipeOutput;
import mekanism.api.recipes.FluidToFluidRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ItemStackToFluidOptionalItemRecipe;
import mekanism.api.recipes.ItemStackToFluidOptionalItemRecipe.FluidOptionalItemOutput;
import mekanism.api.recipes.ItemStackToFluidRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.SawmillRecipe.ChanceOutput;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.outputs.IOutputHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jetbrains.annotations.NotNull;

/**
 * Loader-specific factories for {@link OneInputCachedRecipe} whose recipe types or output containers couple to NeoForge
 * (fluids, chemicals, electrolysis, sawmill, crystallizer). These live alongside {@link OneInputCachedRecipe} in the same
 * package so they can invoke its protected constructor; the loader-neutral {@link OneInputCachedRecipe#itemToItem} factory
 * remains on {@link OneInputCachedRecipe} itself.
 */
@NothingNullByDefault
public final class OneInputCachedRecipeFactory {

    private OneInputCachedRecipeFactory() {
    }

    private static final Predicate<ElectrolysisRecipeOutput> SEPARATOR_OUTPUT_EMPTY = output -> output.left().isEmpty() || output.right().isEmpty();
    //TODO - 26.1: Re-evaluate this as this is never null? So should this always just return false?
    private static final Predicate<FluidOptionalItemOutput> FLUID_OPTIONAL_ITEM_OUTPUT_EMPTY = output -> output.fluid() == null;

    /**
     * Base implementation for handling Crystallizing Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler, handles both the left and right outputs.
     *
     * @since 10.7.0
     */
    public static OneInputCachedRecipe<Chemical, @NotNull ChemicalStack, @NotNull ItemStackTemplate, ChemicalCrystallizerRecipe> crystallizing(ChemicalCrystallizerRecipe recipe,
          BooleanSupplier recheckAllErrors, IInputHandler<Chemical, @NotNull ChemicalStack> inputHandler, IOutputHandler<@NotNull ItemStackTemplate> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.CHEMICAL_EMPTY,
              ConstantPredicates.INVALID_ITEM_TEMPLATE);
    }

    /**
     * Base implementation for handling Electrolytic Separating Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler, handles both the left and right outputs.
     */
    public static OneInputCachedRecipe<Fluid, @NotNull FluidStack, @NotNull ElectrolysisRecipeOutput, ElectrolysisRecipe> separating(ElectrolysisRecipe recipe,
          BooleanSupplier recheckAllErrors, IInputHandler<Fluid, @NotNull FluidStack> inputHandler, IOutputHandler<@NotNull ElectrolysisRecipeOutput> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.FLUID_EMPTY,
              SEPARATOR_OUTPUT_EMPTY);
    }

    /**
     * Base implementation for handling Fluid to Fluid Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     */
    public static OneInputCachedRecipe<Fluid, @NotNull FluidStack, @NotNull FluidStackTemplate, FluidToFluidRecipe> fluidToFluid(FluidToFluidRecipe recipe,
          BooleanSupplier recheckAllErrors, IInputHandler<Fluid, @NotNull FluidStack> inputHandler, IOutputHandler<@NotNull FluidStackTemplate> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.FLUID_EMPTY,
              ConstantPredicates.INVALID_FLUID_TEMPLATE);
    }

    /**
     * Base implementation for handling ItemStack to Fluid Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     */
    public static OneInputCachedRecipe<Item, @NotNull ItemStack, @NotNull FluidStack, ItemStackToFluidRecipe> itemToFluid(ItemStackToFluidRecipe recipe,
          BooleanSupplier recheckAllErrors, IInputHandler<Item, @NotNull ItemStack> inputHandler, IOutputHandler<@NotNull FluidStack> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.ITEM_EMPTY,
              ConstantPredicates.FLUID_EMPTY);
    }

    /**
     * Base implementation for handling ItemStack to Fluid with optional Item Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     *
     * @since 10.6.3
     */
    public static <RECIPE extends ItemStackToFluidOptionalItemRecipe> OneInputCachedRecipe<Item, @NotNull ItemStack, @NotNull FluidOptionalItemOutput, RECIPE> itemToFluidOptionalItem(
          RECIPE recipe, BooleanSupplier recheckAllErrors, IInputHandler<Item, @NotNull ItemStack> inputHandler, IOutputHandler<@NotNull FluidOptionalItemOutput> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.ITEM_EMPTY,
              FLUID_OPTIONAL_ITEM_OUTPUT_EMPTY);
    }

    /**
     * Base implementation for handling ItemStack to Chemical Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     */
    public static <RECIPE extends ItemStackToChemicalRecipe>
    OneInputCachedRecipe<Item, @NotNull ItemStack, @NotNull ChemicalStack, RECIPE> itemToChemical(RECIPE recipe, BooleanSupplier recheckAllErrors,
          IInputHandler<Item, @NotNull ItemStack> inputHandler, IOutputHandler<@NotNull ChemicalStack> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.ITEM_EMPTY,
              ConstantPredicates.CHEMICAL_EMPTY);
    }

    /**
     * Base implementation for handling Chemical to Chemical Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     */
    public static <RECIPE extends ChemicalToChemicalRecipe> OneInputCachedRecipe<Chemical, @NotNull ChemicalStack, @NotNull ChemicalStack, RECIPE> chemicalToChemical(
          RECIPE recipe, BooleanSupplier recheckAllErrors, IInputHandler<Chemical, @NotNull ChemicalStack> inputHandler, IOutputHandler<@NotNull ChemicalStack> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.CHEMICAL_EMPTY,
              ConstantPredicates.CHEMICAL_EMPTY);
    }

    /**
     * Base implementation for handling Sawing Recipes.
     *
     * @param recipe           Recipe.
     * @param recheckAllErrors Returns {@code true} if processing should be continued even if an error is hit in order to gather all the errors. It is recommended to not
     *                         do this every tick or if there is no one viewing recipes.
     * @param inputHandler     Input handler.
     * @param outputHandler    Output handler.
     */
    public static OneInputCachedRecipe<Item, @NotNull ItemStack, @NotNull ChanceOutput, SawmillRecipe> sawing(SawmillRecipe recipe, BooleanSupplier recheckAllErrors,
          IInputHandler<Item, @NotNull ItemStack> inputHandler, IOutputHandler<@NotNull ChanceOutput> outputHandler) {
        return new OneInputCachedRecipe<>(recipe, recheckAllErrors, inputHandler, outputHandler, recipe::getInput, recipe::getOutput, ConstantPredicates.ITEM_EMPTY,
              ConstantPredicates.alwaysFalse());
    }
}
