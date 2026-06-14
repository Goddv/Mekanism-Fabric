package mekanism.fabric.recipe;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.basic.BasicChemicalOxidizerRecipe;
import mekanism.common.recipe.serializer.MekanismRecipeSerializerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Transitional Fabric bring-up: registers Mekanism's item&rarr;chemical {@link RecipeType} + {@link RecipeSerializer}
 * for the Chemical Oxidizer ({@code oxidizing}) through the Architectury {@link DeferredRegister} into the vanilla
 * {@code RECIPE_TYPE}/{@code RECIPE_SERIALIZER} registries, under the SAME id NeoForge uses ({@code mekanism:oxidizing}),
 * so identical shared datapack recipe JSON loads on both loaders. The serializer is built from the loader-neutral
 * {@code :common} {@link MekanismRecipeSerializerHelper#itemToChemical} factory (NeoForge-identical codec field shape),
 * and the hoisted {@code :common} {@link BasicChemicalOxidizerRecipe} resolves these objects back by id via
 * {@code BuiltInRegistries}. This is the chemical-output sibling of {@link MekanismRecipeTypesRegistrar}.
 */
public final class MekanismChemicalRecipeTypesRegistrar {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(MODID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<ItemStackToChemicalRecipe>> OXIDIZING_TYPE = registerType("oxidizing");
    public static final RegistrySupplier<RecipeSerializer<BasicChemicalOxidizerRecipe>> OXIDIZING_SERIALIZER =
          SERIALIZERS.register(Identifier.fromNamespaceAndPath(MODID, "oxidizing"),
                () -> MekanismRecipeSerializerHelper.itemToChemical(BasicChemicalOxidizerRecipe::new));

    private static RegistrySupplier<RecipeType<ItemStackToChemicalRecipe>> registerType(String name) {
        String id = MODID + ":" + name;
        return TYPES.register(Identifier.fromNamespaceAndPath(MODID, name), () -> new RecipeType<ItemStackToChemicalRecipe>() {
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
