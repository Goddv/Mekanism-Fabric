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
 * Transitional Fabric bring-up: registers the REAL Mekanism Chemical Oxidizer machine block (real id
 * {@code mekanism:chemical_oxidizer}, whose facing blockstate + model + textures are already bundled in {@code :common}),
 * backed by a {@link ChemicalMachineBlockEntity} (item input &rarr; chemical output). The chemical-output sibling of
 * {@link FabricRealMachines}: block+item registration goes through the loader-neutral {@code :common}
 * {@link MekanismBlockRegister}; the block-entity type + capability wiring stay loader-specific. Exposes the energy,
 * chemical, and item capabilities, and adds the machine to the Mekanism creative tab.
 */
public final class FabricChemicalMachines {

    private static final String MODID = "mekanism";

    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    /** The Chemical Oxidizer block (item -> chemical). */
    public static final MekanismBlockHolder<ChemicalMachineBlock, BlockItem> CHEMICAL_OXIDIZER = BLOCKS.register(
          "chemical_oxidizer", properties -> new ChemicalMachineBlock(properties
                .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    /** Block-entity type for the chemical machine(s). */
    public static final RegistrySupplier<BlockEntityType<ChemicalMachineBlockEntity>> BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "chemical_machine"), () ->
                FabricBlockEntityTypeBuilder.create(ChemicalMachineBlockEntity::new, CHEMICAL_OXIDIZER.block()).build());

    private FabricChemicalMachines() {
    }

    public static void init() {
        BLOCKS.register();      // finalizes the chemical machine block + its block-item
        BE_TYPES.register();
        // Energy capability (machine is an energy sink) + chemical capability (output tank) + item I/O (input slot).
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BE_TYPE.get());
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        CreativeTabRegistry.append(tab, CHEMICAL_OXIDIZER.item().get());
    }
}
