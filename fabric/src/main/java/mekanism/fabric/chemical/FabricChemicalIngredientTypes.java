package mekanism.fabric.chemical;

import com.mojang.serialization.MapCodec;
import mekanism.api.MekanismAPIBase;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.CompoundChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.DifferenceChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.EmptyChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.IntersectionChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.SingleChemicalIngredient;
import mekanism.api.recipes.ingredients.chemical.TagChemicalIngredient;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

/**
 * Creates Mekanism's {@code chemical_ingredient_type} serializer registry on Fabric — the loader-specific counterpart of
 * NeoForge's {@code RegistryBuilder}-created {@code MekanismAPI.CHEMICAL_INGREDIENT_TYPES}, populated to mirror NeoForge's
 * {@code MekanismChemicalIngredientTypes} (same six ids under the {@code mekanism} namespace). This registry is what
 * backs the dispatch {@code "type"} key in the hoisted {@code mekanism.common.recipe.ingredients.ChemicalIngredientCreator}
 * (reached via {@link mekanism.api.recipes.ingredients.chemical.IChemicalIngredientTypeRegistry}).
 *
 * <p>Created with the no-default {@code FabricRegistryBuilder.create} ({@code MappedRegistry}); this registry has no
 * default entry (unlike the chemical registry). Must run during mod init (before registries freeze); call {@link #init()}
 * early from the entrypoint, right after {@link FabricChemicalRegistry#init()}.
 *
 * <p><b>Static-init order:</b> registering each {@code *.CODEC} touches that ingredient type's static initializer; e.g.
 * {@code CompoundChemicalIngredient.CODEC} ultimately drives {@code ChemicalIngredientCreator.INSTANCE} static-init,
 * whose dispatch codec calls {@code IChemicalIngredientTypeRegistry.INSTANCE.chemicalIngredientTypes().byNameCodec()}.
 * The registry is built (1) before any codec is registered (2), and {@code byNameCodec()} captures the registry lazily
 * (reading its contents only at encode/decode time), so creating-then-populating within {@link #init()} is safe.
 */
public final class FabricChemicalIngredientTypes {

    private static Registry<MapCodec<? extends ChemicalIngredient>> registry;

    private FabricChemicalIngredientTypes() {
    }

    public static void init() {
        // (1) Build + register the (empty) registry FIRST, so the seam INSTANCE resolves to it before any ingredient
        // CODEC static-init runs the dispatch codec.
        Registry<MapCodec<? extends ChemicalIngredient>> built = FabricRegistryBuilder
              .create(MekanismAPIBase.CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME)
              .buildAndRegister();
        registry = built;
        // (2) Populate with the SIX type serializers under ids identical to NeoForge's MekanismChemicalIngredientTypes.
        register(built, "compound", CompoundChemicalIngredient.CODEC);
        register(built, "difference", DifferenceChemicalIngredient.CODEC);
        register(built, "empty", EmptyChemicalIngredient.CODEC);
        register(built, "intersection", IntersectionChemicalIngredient.CODEC);
        register(built, "single", SingleChemicalIngredient.CODEC);
        register(built, "tag", TagChemicalIngredient.CODEC);
    }

    private static void register(Registry<MapCodec<? extends ChemicalIngredient>> reg, String name, MapCodec<? extends ChemicalIngredient> codec) {
        Registry.register(reg, Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, name), codec);
    }

    public static Registry<MapCodec<? extends ChemicalIngredient>> registry() {
        return registry;
    }
}
