package mekanism.fabric.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.basic.BasicChemicalCrystallizerRecipe;
import mekanism.api.recipes.basic.BasicChemicalOxidizerRecipe;
import mekanism.api.recipes.basic.BasicPigmentExtractingRecipe;
import mekanism.common.recipe.serializer.MekanismRecipeSerializerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Transitional Fabric bring-up: registers Mekanism's chemical-processing {@link RecipeType}s + {@link RecipeSerializer}s
 * through the Architectury {@link DeferredRegister} into the vanilla {@code RECIPE_TYPE}/{@code RECIPE_SERIALIZER}
 * registries, under the SAME ids NeoForge uses, so identical shared datapack recipe JSON loads on both loaders.
 *
 * <p>Item&rarr;chemical machines (Chemical Oxidizer {@code oxidizing}, Pigment Extractor {@code pigment_extracting})
 * all share the loader-neutral {@code :common} {@link MekanismRecipeSerializerHelper#itemToChemical} factory (their
 * {@code Basic*} recipes are all {@code BasicItemStackToChemicalRecipe} subclasses). The chemical&rarr;item Chemical
 * Crystallizer
 * ({@code crystallizing}) uses {@link MekanismRecipeSerializerHelper#crystallizing} ({@code ChemicalStackIngredient}
 * input &rarr; {@code ItemStackTemplate} output). The hoisted {@code :common} recipe classes resolve these objects back
 * by id via {@code BuiltInRegistries}. This is the chemical sibling of {@link MekanismRecipeTypesRegistrar}.
 */
public final class MekanismChemicalRecipeTypesRegistrar {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(MODID, Registries.RECIPE_SERIALIZER);

    // ---- item -> chemical machines (shared itemToChemical factory) ----
    public static final RegistrySupplier<RecipeType<ItemStackToChemicalRecipe>> OXIDIZING_TYPE = registerItemToChemicalType("oxidizing");
    public static final RegistrySupplier<RecipeSerializer<BasicChemicalOxidizerRecipe>> OXIDIZING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "oxidizing"),
                () -> MekanismRecipeSerializerHelper.itemToChemical(BasicChemicalOxidizerRecipe::new));

    public static final RegistrySupplier<RecipeType<ItemStackToChemicalRecipe>> PIGMENT_EXTRACTING_TYPE = registerItemToChemicalType("pigment_extracting");
    public static final RegistrySupplier<RecipeSerializer<BasicPigmentExtractingRecipe>> PIGMENT_EXTRACTING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "pigment_extracting"),
                () -> MekanismRecipeSerializerHelper.itemToChemical(BasicPigmentExtractingRecipe::new));

    // ---- chemical -> item machine (Chemical Crystallizer) ----
    public static final RegistrySupplier<RecipeType<ChemicalCrystallizerRecipe>> CRYSTALLIZING_TYPE = registerCrystallizingType("crystallizing");
    public static final RegistrySupplier<RecipeSerializer<BasicChemicalCrystallizerRecipe>> CRYSTALLIZING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "crystallizing"),
                () -> MekanismRecipeSerializerHelper.crystallizing(BasicChemicalCrystallizerRecipe::new));

    private static RegistrySupplier<RecipeType<ItemStackToChemicalRecipe>> registerItemToChemicalType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<ItemStackToChemicalRecipe>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    private static RegistrySupplier<RecipeType<ChemicalCrystallizerRecipe>> registerCrystallizingType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<ChemicalCrystallizerRecipe>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    private MekanismChemicalRecipeTypesRegistrar() {
    }

    /** Finalize registrations (types before serializers; neither constructs a recipe, so order is non-critical). */
    public static void init() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
