package mekanism.fabric.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.api.recipes.basic.BasicCombinerRecipe;
import mekanism.api.recipes.basic.BasicSawmillRecipe;
import mekanism.common.recipe.serializer.MekanismRecipeSerializerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Transitional Fabric bring-up: registers the two dual-item Mekanism {@link RecipeType}s + {@link RecipeSerializer}s
 * through the Architectury {@link DeferredRegister} into the vanilla {@code RECIPE_TYPE}/{@code RECIPE_SERIALIZER}
 * registries, under the SAME ids NeoForge uses, so identical shared datapack recipe JSON loads on both loaders.
 *
 * <p>The Combiner ({@code combining}, {@code item + item -> item}) uses
 * {@link MekanismRecipeSerializerHelper#combining} ({@code BasicCombinerRecipe}). The Precision Sawmill ({@code sawing},
 * {@code item -> item + chance secondary}) uses {@link MekanismRecipeSerializerHelper#sawing}
 * ({@code BasicSawmillRecipe}). Unlike {@link MekanismRecipeTypesRegistrar} (hard-typed to
 * {@code RecipeType<ItemStackToItemStackRecipe>}), these are distinct recipe classes, so each type is registered with its
 * own {@code RecipeType<...>} supplier. The hoisted {@code :common} recipe classes resolve these objects back by id via
 * {@code BuiltInRegistries}.
 */
public final class MekanismDualItemRecipeTypesRegistrar {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(MODID, Registries.RECIPE_SERIALIZER);

    // ---- Combiner (item + item -> item) ----
    public static final RegistrySupplier<RecipeType<CombinerRecipe>> COMBINING_TYPE = registerCombiningType("combining");
    public static final RegistrySupplier<RecipeSerializer<BasicCombinerRecipe>> COMBINING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "combining"),
                () -> MekanismRecipeSerializerHelper.combining(BasicCombinerRecipe::new));

    // ---- Precision Sawmill (item -> item + chance secondary) ----
    public static final RegistrySupplier<RecipeType<SawmillRecipe>> SAWING_TYPE = registerSawingType("sawing");
    public static final RegistrySupplier<RecipeSerializer<BasicSawmillRecipe>> SAWING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "sawing"),
                () -> MekanismRecipeSerializerHelper.sawing(BasicSawmillRecipe::new));

    private static RegistrySupplier<RecipeType<CombinerRecipe>> registerCombiningType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<CombinerRecipe>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    private static RegistrySupplier<RecipeType<SawmillRecipe>> registerSawingType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<SawmillRecipe>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    private MekanismDualItemRecipeTypesRegistrar() {
    }

    /** Finalize registrations (types before serializers; neither constructs a recipe, so order is non-critical). */
    public static void init() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
