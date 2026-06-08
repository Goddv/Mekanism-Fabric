package mekanism.fabric.chemical;

import mekanism.api.MekanismAPIBase;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Creates Mekanism's custom {@code chemical} registry on Fabric — the loader-specific counterpart of NeoForge's
 * {@code RegistryBuilder}-created registry. Uses fabric-api's {@link FabricRegistryBuilder#createDefaulted} (→
 * {@code DefaultedMappedRegistry}, the Fabric equivalent of NeoForge's {@code DefaultedRegistry}). Must run during mod
 * init (before registries freeze); call {@link #init()} early from the entrypoint. Registers the empty chemical (the
 * registry default) + a transitional demo chemical so the chemical pipeline can be exercised on Fabric.
 */
public final class FabricChemicalRegistry {

    private static DefaultedRegistry<Chemical> registry;
    private static Holder<Chemical> empty;
    private static Holder<Chemical> demo;

    private FabricChemicalRegistry() {
    }

    public static void init() {
        DefaultedRegistry<Chemical> built = FabricRegistryBuilder
              .createDefaulted(MekanismAPIBase.CHEMICAL_REGISTRY_NAME, MekanismAPIBase.EMPTY_CHEMICAL_KEY.identifier())
              .buildAndRegister();
        empty = Registry.registerForHolder(built, MekanismAPIBase.EMPTY_CHEMICAL_KEY,
              new Chemical(ChemicalBuilder.builder(Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "liquid/empty"))));
        // Transitional demo chemical so the registry + ChemicalStack can be validated on Fabric.
        demo = Registry.registerForHolder(built, ResourceKey.create(MekanismAPIBase.CHEMICAL_REGISTRY_NAME,
                    Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "fabric_demo_chemical")),
              new Chemical(ChemicalBuilder.builder(Identifier.fromNamespaceAndPath(MekanismAPIBase.MEKANISM_MODID, "liquid/liquid")).tint(0x55AAFF)));
        registry = built;
    }

    public static DefaultedRegistry<Chemical> registry() {
        return registry;
    }

    public static Holder<Chemical> empty() {
        return empty;
    }

    public static Holder<Chemical> demo() {
        return demo;
    }
}
