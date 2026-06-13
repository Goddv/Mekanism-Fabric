package mekanism.common.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.api.recipes.ChemicalToChemicalRecipe;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.FluidChemicalToChemicalRecipe;
import mekanism.api.recipes.FluidToFluidRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.ItemStackToEnergyRecipe;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.NucleosynthesizingRecipe;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.vanilla_input.BiChemicalRecipeInput;
import mekanism.api.recipes.vanilla_input.ReactionRecipeInput;
import mekanism.api.recipes.vanilla_input.RotaryRecipeInput;
import mekanism.api.recipes.vanilla_input.SingleChemicalRecipeInput;
import mekanism.api.recipes.vanilla_input.SingleFluidChemicalRecipeInput;
import mekanism.api.recipes.vanilla_input.SingleFluidRecipeInput;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.common.Mekanism;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.DoubleItem;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.EitherSideChemical;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.FluidChemical;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemChemical;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.ItemFluidChemical;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleChemical;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleFluid;
import mekanism.common.recipe.lookup.cache.InputRecipeCache.SingleItem;
import mekanism.common.recipe.lookup.cache.RotaryInputRecipeCache;
import mekanism.common.registration.impl.RecipeTypeDeferredRegister;
import mekanism.common.registration.impl.RecipeTypeRegistryObject;
import mekanism.common.util.RegistryUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MekanismRecipeType<VANILLA_INPUT extends RecipeInput, RECIPE extends MekanismRecipe<VANILLA_INPUT>, INPUT_CACHE extends IInputRecipeCache>
      extends MekanismRecipeTypeBase<VANILLA_INPUT, RECIPE, INPUT_CACHE> {

    public static final RecipeTypeDeferredRegister RECIPE_TYPES = new RecipeTypeDeferredRegister(Mekanism.MODID);

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> CRUSHING = register(MekanismRecipeTypes.NAME_CRUSHING, recipeType -> new SingleItem<>(recipeType, ItemStackToItemStackRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> ENRICHING = register(MekanismRecipeTypes.NAME_ENRICHING, recipeType -> new SingleItem<>(recipeType, ItemStackToItemStackRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToItemStackRecipe, SingleItem<ItemStackToItemStackRecipe>> SMELTING = register(MekanismRecipeTypes.NAME_SMELTING, recipeType -> new SingleItem<>(recipeType, ItemStackToItemStackRecipe::getInput));

    public static final RecipeTypeRegistryObject<BiChemicalRecipeInput, ChemicalChemicalToChemicalRecipe, EitherSideChemical<ChemicalChemicalToChemicalRecipe>> CHEMICAL_INFUSING = register(MekanismRecipeTypes.NAME_CHEMICAL_INFUSING, EitherSideChemical::new);

    public static final RecipeTypeRegistryObject<RecipeInput, CombinerRecipe, DoubleItem<CombinerRecipe>> COMBINING = register(MekanismRecipeTypes.NAME_COMBINING, recipeType -> new DoubleItem<>(recipeType, CombinerRecipe::getMainInput, CombinerRecipe::getExtraInput));

    public static final RecipeTypeRegistryObject<SingleFluidRecipeInput, ElectrolysisRecipe, SingleFluid<ElectrolysisRecipe>> SEPARATING = register(MekanismRecipeTypes.NAME_SEPARATING, recipeType -> new SingleFluid<>(recipeType, ElectrolysisRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleFluidChemicalRecipeInput, FluidChemicalToChemicalRecipe, FluidChemical<FluidChemicalToChemicalRecipe>> WASHING = register(MekanismRecipeTypes.NAME_WASHING, recipeType -> new FluidChemical<>(recipeType, FluidChemicalToChemicalRecipe::getFluidInput, FluidChemicalToChemicalRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleFluidRecipeInput, FluidToFluidRecipe, SingleFluid<FluidToFluidRecipe>> EVAPORATING = register(MekanismRecipeTypes.NAME_EVAPORATING, recipeType -> new SingleFluid<>(recipeType, FluidToFluidRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleChemicalRecipeInput, ChemicalToChemicalRecipe, SingleChemical<ChemicalToChemicalRecipe>> ACTIVATING = register(MekanismRecipeTypes.NAME_ACTIVATING, recipeType -> new SingleChemical<>(recipeType, ChemicalToChemicalRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleChemicalRecipeInput, ChemicalToChemicalRecipe, SingleChemical<ChemicalToChemicalRecipe>> CENTRIFUGING = register(MekanismRecipeTypes.NAME_CENTRIFUGING, recipeType -> new SingleChemical<>(recipeType, ChemicalToChemicalRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleChemicalRecipeInput, ChemicalCrystallizerRecipe, SingleChemical<ChemicalCrystallizerRecipe>> CRYSTALLIZING = register(MekanismRecipeTypes.NAME_CRYSTALLIZING, recipeType -> new SingleChemical<>(recipeType, ChemicalCrystallizerRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ChemicalDissolutionRecipe, ItemChemical<ChemicalDissolutionRecipe>> DISSOLUTION = register(MekanismRecipeTypes.NAME_DISSOLUTION, recipeType -> new ItemChemical<>(recipeType, ChemicalDissolutionRecipe::getItemInput, ChemicalDissolutionRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> COMPRESSING = register(MekanismRecipeTypes.NAME_COMPRESSING, recipeType -> new ItemChemical<>(recipeType, ItemStackChemicalToItemStackRecipe::getItemInput, ItemStackChemicalToItemStackRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> PURIFYING = register(MekanismRecipeTypes.NAME_PURIFYING, recipeType -> new ItemChemical<>(recipeType, ItemStackChemicalToItemStackRecipe::getItemInput, ItemStackChemicalToItemStackRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> INJECTING = register(MekanismRecipeTypes.NAME_INJECTING, recipeType -> new ItemChemical<>(recipeType, ItemStackChemicalToItemStackRecipe::getItemInput, ItemStackChemicalToItemStackRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, NucleosynthesizingRecipe, ItemChemical<NucleosynthesizingRecipe>> NUCLEOSYNTHESIZING = register(MekanismRecipeTypes.NAME_NUCLEOSYNTHESIZING, recipeType -> new ItemChemical<>(recipeType, NucleosynthesizingRecipe::getItemInput, NucleosynthesizingRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToEnergyRecipe, SingleItem<ItemStackToEnergyRecipe>> ENERGY_CONVERSION = register(MekanismRecipeTypes.NAME_ENERGY_CONVERSION, recipeType -> new SingleItem<>(recipeType, ItemStackToEnergyRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToChemicalRecipe, SingleItem<ItemStackToChemicalRecipe>> CHEMICAL_CONVERSION = register(MekanismRecipeTypes.NAME_CHEMICAL_CONVERSION, recipeType -> new SingleItem<>(recipeType, ItemStackToChemicalRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToChemicalRecipe, SingleItem<ItemStackToChemicalRecipe>> OXIDIZING = register(MekanismRecipeTypes.NAME_OXIDIZING, recipeType -> new SingleItem<>(recipeType, ItemStackToChemicalRecipe::getInput));

    public static final RecipeTypeRegistryObject<SingleRecipeInput, ItemStackToChemicalRecipe, SingleItem<ItemStackToChemicalRecipe>> PIGMENT_EXTRACTING = register(MekanismRecipeTypes.NAME_PIGMENT_EXTRACTING, recipeType -> new SingleItem<>(recipeType, ItemStackToChemicalRecipe::getInput));

    public static final RecipeTypeRegistryObject<BiChemicalRecipeInput, ChemicalChemicalToChemicalRecipe, EitherSideChemical<ChemicalChemicalToChemicalRecipe>> PIGMENT_MIXING = register(MekanismRecipeTypes.NAME_PIGMENT_MIXING, EitherSideChemical::new);

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> METALLURGIC_INFUSING = register(MekanismRecipeTypes.NAME_METALLURGIC_INFUSING, recipeType -> new ItemChemical<>(recipeType, ItemStackChemicalToItemStackRecipe::getItemInput, ItemStackChemicalToItemStackRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<SingleItemChemicalRecipeInput, ItemStackChemicalToItemStackRecipe, ItemChemical<ItemStackChemicalToItemStackRecipe>> PAINTING = register(MekanismRecipeTypes.NAME_PAINTING, recipeType -> new ItemChemical<>(recipeType, ItemStackChemicalToItemStackRecipe::getItemInput, ItemStackChemicalToItemStackRecipe::getChemicalInput));

    public static final RecipeTypeRegistryObject<ReactionRecipeInput, PressurizedReactionRecipe, ItemFluidChemical<PressurizedReactionRecipe>> REACTION = register(MekanismRecipeTypes.NAME_REACTION, recipeType -> new ItemFluidChemical<>(recipeType, PressurizedReactionRecipe::getInputSolid, PressurizedReactionRecipe::getInputFluid, PressurizedReactionRecipe::getInputChemical));

    public static final RecipeTypeRegistryObject<RotaryRecipeInput, RotaryRecipe, RotaryInputRecipeCache> ROTARY = register(MekanismRecipeTypes.NAME_ROTARY, RotaryInputRecipeCache::new);

    public static final RecipeTypeRegistryObject<SingleRecipeInput, SawmillRecipe, SingleItem<SawmillRecipe>> SAWING = register(MekanismRecipeTypes.NAME_SAWING, recipeType -> new SingleItem<>(recipeType, SawmillRecipe::getInput));

    private static <VANILLA_INPUT extends RecipeInput, RECIPE extends MekanismRecipe<VANILLA_INPUT>, INPUT_CACHE extends IInputRecipeCache>
    RecipeTypeRegistryObject<VANILLA_INPUT, RECIPE, INPUT_CACHE> register(
          Identifier name,
          Function<MekanismRecipeTypeBase<VANILLA_INPUT, RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator
    ) {
        if (!Mekanism.MODID.equals(name.getNamespace())) {
            throw new IllegalStateException("Name must be in " + Mekanism.MODID + " namespace");
        }
        return RECIPE_TYPES.registerMek(name.getPath(), registryName -> new MekanismRecipeType<>(registryName, inputCacheCreator));
    }

    public static void clearCache() {
        for (Holder<RecipeType<?>> entry : RECIPE_TYPES.getEntries()) {
            //Note: We expect all entries to be a MekanismRecipeType, but we validate it just to be sure
            if (entry.value() instanceof MekanismRecipeType<?, ?, ?> recipeType) {
                recipeType.clearCaches();
            }
        }
    }

    public static boolean checkIncompleteRecipes(MinecraftServer server) {
        return checkIncompleteRecipes(server.getRecipeManager(), server.registryAccess());
    }

    public static boolean checkIncompleteRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        boolean foundIncompleteRecipes = false;
        for (DeferredHolder<RecipeType<?>, ? extends RecipeType<?>> holder : RECIPE_TYPES.getEntries()) {
            MekanismRecipeType<?, ?, ?> recipeType = (MekanismRecipeType<?, ?, ?>) holder.value();
            foundIncompleteRecipes |= recipeType.checkMyIncompleteRecipes(recipeManager.recipeMap());
        }
        return foundIncompleteRecipes;
    }

    private MekanismRecipeType(Identifier name, Function<MekanismRecipeTypeBase<VANILLA_INPUT, RECIPE, INPUT_CACHE>, INPUT_CACHE> inputCacheCreator) {
        super(name, inputCacheCreator);
    }

    @Override
    protected Collection<RecipeHolder<RECIPE>> mergeGeneratedRecipes(RecipeMap recipeMap, Collection<RecipeHolder<RECIPE>> recipes) {
        if (this == SMELTING.get()) {
            //Ensure the recipes can be modified
            recipes = new ArrayList<>(recipes);
            for (RecipeHolder<SmeltingRecipe> recipeHolder : recipeMap.byType(RecipeType.SMELTING)) {
                SmeltingRecipe smeltingRecipe = recipeHolder.value();
                if (smeltingRecipe.input().isEmpty()) {
                    continue;
                }
                ItemStackToItemStackRecipe possiblyUnwrapped = WrappedSmelterRecipe.tryUnwrap(smeltingRecipe);
                ResourceKey<Recipe<?>> generateId = RegistryUtils.syntheticKey(recipeHolder.id(), "mekanism_generated");
                recipes.add(new RecipeHolder<>(generateId, castRecipe(possiblyUnwrapped)));

            }
        }
        return recipes;
    }

    /**
     * Helper for getting a recipe from a world's recipe manager.
     */
    public static <I extends RecipeInput, RECIPE_TYPE extends Recipe<I>> Optional<RecipeHolder<RECIPE_TYPE>> getRecipeFor(RecipeType<RECIPE_TYPE> recipeType, I input,
          Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            //TODO - 26.1: Re-evaluate callers and see what we can do for client side that might want to know that recipes exist
            return Optional.empty();
        }
        //Only allow looking up complete recipes or special recipes as we only use this method for vanilla recipe types
        // and special recipes return that they are not complete
        return serverLevel.recipeAccess().getRecipeFor(recipeType, input, level)
              /*.filter(recipe -> recipe.value().isSpecial() || !recipe.value().isIncomplete())*/;
    }

    /**
     * Helper for getting a recipe from a world's recipe manager.
     */
    public static Optional<RecipeHolder<?>> byKey(Level level, ResourceKey<Recipe<?>> id) {
        if (!(level instanceof ServerLevel serverLevel)) {
            //TODO - 26.1: Re-evaluate callers and see what we can do for client side that might want to know that recipes exist
            return Optional.empty();
        }
        //Only allow looking up complete recipes or special recipes as we only use this method for vanilla recipe types
        // and special recipes return that they are not complete
        return serverLevel.recipeAccess().byKey(id)
              /*.filter(recipe -> recipe.value().isSpecial() || !recipe.value().isIncomplete())*/;
    }
}