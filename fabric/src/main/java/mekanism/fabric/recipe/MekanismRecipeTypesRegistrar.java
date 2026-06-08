package mekanism.fabric.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.basic.BasicEnrichingRecipe;
import mekanism.common.recipe.serializer.MekanismRecipeSerializerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Transitional Fabric bring-up: registers Mekanism's enriching {@link RecipeType} + {@link RecipeSerializer} through the
 * Architectury {@link DeferredRegister} into the vanilla {@code RECIPE_TYPE}/{@code RECIPE_SERIALIZER} registries, under
 * the SAME ids NeoForge uses ({@code mekanism:enriching}) so the identical shared datapack recipe JSON loads on both
 * loaders. The hoisted {@code :common} {@link BasicEnrichingRecipe} resolves these objects back by id via
 * {@code BuiltInRegistries}, giving one loader-neutral recipe class. Replaced once the full recipe-type framework
 * (caching {@code MekanismRecipeType} + the other 20+ types) is migrated to {@code :common}.
 */
public final class MekanismRecipeTypesRegistrar {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<ItemStackToItemStackRecipe>> ENRICHING_TYPE =
          TYPES.register(Identifier.fromNamespaceAndPath(MODID, "enriching"), () -> new RecipeType<ItemStackToItemStackRecipe>() {
              @Override
              public String toString() {
                  return "mekanism:enriching";
              }
          });

    public static final RegistrySupplier<RecipeSerializer<BasicEnrichingRecipe>> ENRICHING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "enriching"), () -> MekanismRecipeSerializerHelper.itemToItem(BasicEnrichingRecipe::new));

    private MekanismRecipeTypesRegistrar() {
    }

    /** Finalize registrations (type before serializer; neither constructs a recipe, so order is non-critical). */
    public static void init() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
