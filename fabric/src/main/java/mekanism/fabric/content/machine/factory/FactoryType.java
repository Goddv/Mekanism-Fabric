package mekanism.fabric.content.machine.factory;

import net.minecraft.resources.Identifier;

/**
 * Transitional Fabric bring-up: the 9 Mekanism factory types, mirroring {@code
 * mekanism.common.content.blocktype.FactoryType} (NeoForge) but loader-neutral and self-contained — it carries only what
 * the Fabric {@link FactoryBlockEntity} needs: the registry-name component (e.g. {@code enriching}), the base recipe-type
 * {@link Identifier} each process resolves and runs, and the {@link Topology} (which per-process recipe shape to apply).
 *
 * <p>A Factory runs the SAME base recipe type N times in parallel ({@code basic=3, advanced=5, elite=7, ultimate=9}
 * processes); each process has its own input slot(s) + output slot(s) + progress, while energy and (for the chemical
 * factories) the chemical input tank / (for combining) the extra-input slot are SHARED across all processes.
 *
 * <p>NB: {@code INFUSING} resolves the {@code mekanism:metallurgic_infusing} recipe type (the base Metallurgic Infuser),
 * matching the NeoForge mapping; its registry-name component stays {@code infusing} so the block id is
 * {@code <tier>_infusing_factory}.
 */
public enum FactoryType {

    // item -> item (ItemStackToItemStackRecipe)
    SMELTING("smelting", "smelting", Topology.ITEM_TO_ITEM),
    ENRICHING("enriching", "enriching", Topology.ITEM_TO_ITEM),
    CRUSHING("crushing", "crushing", Topology.ITEM_TO_ITEM),
    // item -> item + chance secondary (SawmillRecipe)
    SAWING("sawing", "sawing", Topology.SAWING),
    // item + chemical -> item (ItemStackChemicalToItemStackRecipe; shared chemical tank)
    COMPRESSING("compressing", "compressing", Topology.ITEM_CHEMICAL_TO_ITEM),
    PURIFYING("purifying", "purifying", Topology.ITEM_CHEMICAL_TO_ITEM),
    INJECTING("injecting", "injecting", Topology.ITEM_CHEMICAL_TO_ITEM),
    INFUSING("infusing", "metallurgic_infusing", Topology.ITEM_CHEMICAL_TO_ITEM),
    // item + item -> item (CombinerRecipe; shared extra-input slot)
    COMBINING("combining", "combining", Topology.COMBINING);

    /** Per-process recipe shape, deciding slot count + lookup/process logic in {@link FactoryBlockEntity}. */
    public enum Topology {
        /** 1 input -> 1 output per process. */
        ITEM_TO_ITEM,
        /** 1 input -> main output + chance secondary output per process (2 outputs/process). */
        SAWING,
        /** 1 input + 1 shared chemical tank -> 1 output per process. */
        ITEM_CHEMICAL_TO_ITEM,
        /** 1 main input + 1 shared extra-input slot -> 1 output per process. */
        COMBINING
    }

    private final String registryNameComponent;
    private final Identifier recipeTypeId;
    private final Topology topology;

    FactoryType(String registryNameComponent, String recipeTypePath, Topology topology) {
        this.registryNameComponent = registryNameComponent;
        this.recipeTypeId = Identifier.fromNamespaceAndPath("mekanism", recipeTypePath);
        this.topology = topology;
    }

    /** The block-id component (e.g. {@code enriching}); block id is {@code <tier>_<component>_factory}. */
    public String getRegistryNameComponent() {
        return registryNameComponent;
    }

    /** The base recipe-type id each process resolves + runs (e.g. {@code mekanism:enriching}). */
    public Identifier getRecipeTypeId() {
        return recipeTypeId;
    }

    public Topology getTopology() {
        return topology;
    }
}
