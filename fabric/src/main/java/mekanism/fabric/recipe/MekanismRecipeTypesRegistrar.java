package mekanism.fabric.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.BiFunction;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.BasicCrushingRecipe;
import mekanism.api.recipes.basic.BasicEnrichingRecipe;
import mekanism.api.recipes.basic.BasicItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.BasicSmeltingRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.serializer.MekanismRecipeSerializerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Transitional Fabric bring-up: registers Mekanism's item&rarr;item {@link RecipeType}s + {@link RecipeSerializer}s
 * (enriching, crushing, smelting) through the Architectury {@link DeferredRegister} into the vanilla
 * {@code RECIPE_TYPE}/{@code RECIPE_SERIALIZER} registries, under the SAME ids NeoForge uses, so the identical shared
 * datapack recipe JSON loads on both loaders. The hoisted {@code :common} recipe classes resolve these objects back by
 * id via {@code BuiltInRegistries}, giving one loader-neutral recipe class per type. Replaced once the full recipe-type
 * framework (caching {@code MekanismRecipeType} + the chemical/fluid types) is migrated to {@code :common}.
 */
public final class MekanismRecipeTypesRegistrar {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<ItemStackToItemStackRecipe>> ENRICHING_TYPE = registerType("enriching");
    public static final RegistrySupplier<RecipeSerializer<BasicEnrichingRecipe>> ENRICHING_SERIALIZER = registerSerializer("enriching", BasicEnrichingRecipe::new);

    public static final RegistrySupplier<RecipeType<ItemStackToItemStackRecipe>> CRUSHING_TYPE = registerType("crushing");
    public static final RegistrySupplier<RecipeSerializer<BasicCrushingRecipe>> CRUSHING_SERIALIZER = registerSerializer("crushing", BasicCrushingRecipe::new);

    public static final RegistrySupplier<RecipeType<ItemStackToItemStackRecipe>> SMELTING_TYPE = registerType("smelting");
    public static final RegistrySupplier<RecipeSerializer<BasicSmeltingRecipe>> SMELTING_SERIALIZER = registerSerializer("smelting", BasicSmeltingRecipe::new);

    private static RegistrySupplier<RecipeType<ItemStackToItemStackRecipe>> registerType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<ItemStackToItemStackRecipe>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    private static <RECIPE extends BasicItemStackToItemStackRecipe> RegistrySupplier<RecipeSerializer<RECIPE>> registerSerializer(
          String name, BiFunction<ItemStackIngredient, ItemStackTemplate, RECIPE> factory) {
        return SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, name), () -> MekanismRecipeSerializerHelper.itemToItem(factory));
    }

    private MekanismRecipeTypesRegistrar() {
    }

    /** Finalize registrations (types before serializers; neither constructs a recipe, so order is non-critical). */
    public static void init() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
