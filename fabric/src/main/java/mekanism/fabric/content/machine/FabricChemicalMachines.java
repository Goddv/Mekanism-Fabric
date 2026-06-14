package mekanism.fabric.content.machine;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.common.registration.MekanismBlockHolder;
import mekanism.common.registration.MekanismBlockRegister;
import mekanism.fabric.chemical.MekanismFabricChemical;
import mekanism.fabric.energy.MekanismFabricEnergy;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Transitional Fabric bring-up: registers Mekanism's chemical-processing machine blocks. Block+item registration goes
 * through the loader-neutral {@code :common} {@link MekanismBlockRegister}; the block-entity type + capability wiring
 * stay loader-specific.
 *
 * <p>Item&rarr;chemical machines (all backed by the generic {@link ChemicalMachineBlockEntity}, parameterized by their
 * recipe-type id): Chemical Oxidizer ({@code oxidizing}), Pigment Extractor ({@code pigment_extracting}). The
 * chemical&rarr;item Chemical Crystallizer ({@code crystallizing}) is backed by the reversed-topology
 * {@link ChemicalToItemMachineBlockEntity}. Exposes the energy, chemical, and item capabilities, and adds each machine
 * to the Mekanism creative tab.
 */
public final class FabricChemicalMachines {

    private static final String MODID = "mekanism";

    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    private static final Identifier OXIDIZING_ID = Identifier.fromNamespaceAndPath(MODID, "oxidizing");
    private static final Identifier PIGMENT_EXTRACTING_ID = Identifier.fromNamespaceAndPath(MODID, "pigment_extracting");

    // ---- item -> chemical machines (generic ChemicalMachineBlock + ChemicalMachineBlockEntity) ----

    /** The Chemical Oxidizer block (item -> chemical). Facing-only blockstate. */
    public static final MekanismBlockHolder<ChemicalMachineBlock, BlockItem> CHEMICAL_OXIDIZER = BLOCKS.register(
          "chemical_oxidizer", properties -> new ChemicalMachineBlock(properties
                .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL), OXIDIZING_ID, false));

    /** The Pigment Extractor block (item -> chemical). Its bundled blockstate declares facing+active variants. */
    public static final MekanismBlockHolder<ChemicalMachineBlock, BlockItem> PIGMENT_EXTRACTOR = BLOCKS.register(
          "pigment_extractor", properties -> new ChemicalMachineBlock(properties
                .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL), PIGMENT_EXTRACTING_ID, true));

    /** Block-entity type for the item->chemical machines (shared across oxidizer/pigment-extractor). */
    public static final RegistrySupplier<BlockEntityType<ChemicalMachineBlockEntity>> BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "chemical_machine"), () ->
                FabricBlockEntityTypeBuilder.create(ChemicalMachineBlockEntity::new,
                      CHEMICAL_OXIDIZER.block(), PIGMENT_EXTRACTOR.block()).build());

    // ---- chemical -> item machine (Chemical Crystallizer) ----

    /** The Chemical Crystallizer block (chemical -> item). Facing-only blockstate. */
    public static final MekanismBlockHolder<ChemicalToItemMachineBlock, BlockItem> CHEMICAL_CRYSTALLIZER = BLOCKS.register(
          "chemical_crystallizer", properties -> new ChemicalToItemMachineBlock(properties
                .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    /** Block-entity type for the chemical->item crystallizer. */
    public static final RegistrySupplier<BlockEntityType<ChemicalToItemMachineBlockEntity>> CRYSTALLIZER_BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "chemical_crystallizer"), () ->
                FabricBlockEntityTypeBuilder.create(ChemicalToItemMachineBlockEntity::new, CHEMICAL_CRYSTALLIZER.block()).build());

    private FabricChemicalMachines() {
    }

    public static void init() {
        BLOCKS.register();      // finalizes the chemical machine blocks + their block-items
        BE_TYPES.register();

        // ---- item -> chemical machines: energy sink + chemical output tank + item input slot ----
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BE_TYPE.get());

        // ---- chemical -> item crystallizer: energy sink + chemical input tank + item output slot ----
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, CRYSTALLIZER_BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, CRYSTALLIZER_BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), CRYSTALLIZER_BE_TYPE.get());

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        CreativeTabRegistry.append(tab, CHEMICAL_OXIDIZER.item().get());
        CreativeTabRegistry.append(tab, PIGMENT_EXTRACTOR.item().get());
        CreativeTabRegistry.append(tab, CHEMICAL_CRYSTALLIZER.item().get());
    }
}
