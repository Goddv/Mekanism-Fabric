package mekanism.fabric.content.transmitter;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.fabric.chemical.MekanismFabricChemical;
import mekanism.fabric.energy.MekanismFabricEnergy;
import mekanism.fabric.fluid.MekanismFabricFluid;
import mekanism.fabric.heat.MekanismFabricHeat;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Transitional Fabric bring-up: registers the four core Mekanism transmitters (basic tier) under their REAL ids —
 * {@code basic_universal_cable} (energy), {@code basic_pressurized_tube} (chemical),
 * {@code basic_thermodynamic_conductor} (heat), {@code basic_logistical_transporter} (items) — as functional
 * adjacent-relays. Each gets a block + block-item + block-entity type, and exposes the matching capability
 * ({@code MekanismFabricEnergy/Chemical/Heat.SIDED}; the logistical transporter reads neighbour {@code ItemStorage.SIDED}
 * caps directly, so it exposes none of its own). All four are added to the Mekanism creative tab.
 *
 * <p>Rendering note: the bundled blockstates for these ids are MULTIPART keyed on per-side connection properties (and use
 * a custom {@code mekanism:special/transmitter} model loader) that this transitional, connection-less block does not
 * carry, so they would not render on Fabric. Fabric-only SIMPLE blockstates + cube models (each using the transmitter's
 * own bundled multipart texture) override them under {@code fabric/src/main/resources/assets/mekanism}; the unparseable
 * NeoForge-datagen copies are dropped in {@code fabric/build.gradle}. Connected multipart rendering is deferred.
 */
public final class FabricTransmitters {

    private static final String MODID = "mekanism";

    private static final Identifier UNIVERSAL_CABLE_ID = id("basic_universal_cable");
    private static final Identifier PRESSURIZED_TUBE_ID = id("basic_pressurized_tube");
    private static final Identifier THERMODYNAMIC_CONDUCTOR_ID = id("basic_thermodynamic_conductor");
    private static final Identifier LOGISTICAL_TRANSPORTER_ID = id("basic_logistical_transporter");
    private static final Identifier MECHANICAL_PIPE_ID = id("basic_mechanical_pipe");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    // ---- Universal Cable (energy) ----
    public static final RegistrySupplier<Block> UNIVERSAL_CABLE = BLOCKS.register(UNIVERSAL_CABLE_ID, () -> new TransmitterBlock(
          props(UNIVERSAL_CABLE_ID), UniversalCableBlockEntity::new, FabricTransmitters::universalCableBeType,
          TransmitterRenderType.UNIVERSAL_CABLE));
    public static final RegistrySupplier<Item> UNIVERSAL_CABLE_ITEM = blockItem(UNIVERSAL_CABLE_ID, UNIVERSAL_CABLE);
    public static final RegistrySupplier<BlockEntityType<UniversalCableBlockEntity>> UNIVERSAL_CABLE_BE_TYPE =
          BE_TYPES.register(UNIVERSAL_CABLE_ID, () -> FabricBlockEntityTypeBuilder.create(
                UniversalCableBlockEntity::new, UNIVERSAL_CABLE.get()).build());

    // ---- Pressurized Tube (chemical) ----
    public static final RegistrySupplier<Block> PRESSURIZED_TUBE = BLOCKS.register(PRESSURIZED_TUBE_ID, () -> new TransmitterBlock(
          props(PRESSURIZED_TUBE_ID), PressurizedTubeBlockEntity::new, FabricTransmitters::pressurizedTubeBeType,
          TransmitterRenderType.PRESSURIZED_TUBE));
    public static final RegistrySupplier<Item> PRESSURIZED_TUBE_ITEM = blockItem(PRESSURIZED_TUBE_ID, PRESSURIZED_TUBE);
    public static final RegistrySupplier<BlockEntityType<PressurizedTubeBlockEntity>> PRESSURIZED_TUBE_BE_TYPE =
          BE_TYPES.register(PRESSURIZED_TUBE_ID, () -> FabricBlockEntityTypeBuilder.create(
                PressurizedTubeBlockEntity::new, PRESSURIZED_TUBE.get()).build());

    // ---- Thermodynamic Conductor (heat) ----
    public static final RegistrySupplier<Block> THERMODYNAMIC_CONDUCTOR = BLOCKS.register(THERMODYNAMIC_CONDUCTOR_ID, () -> new TransmitterBlock(
          props(THERMODYNAMIC_CONDUCTOR_ID), ThermodynamicConductorBlockEntity::new, FabricTransmitters::thermodynamicConductorBeType,
          TransmitterRenderType.THERMODYNAMIC_CONDUCTOR));
    public static final RegistrySupplier<Item> THERMODYNAMIC_CONDUCTOR_ITEM = blockItem(THERMODYNAMIC_CONDUCTOR_ID, THERMODYNAMIC_CONDUCTOR);
    public static final RegistrySupplier<BlockEntityType<ThermodynamicConductorBlockEntity>> THERMODYNAMIC_CONDUCTOR_BE_TYPE =
          BE_TYPES.register(THERMODYNAMIC_CONDUCTOR_ID, () -> FabricBlockEntityTypeBuilder.create(
                ThermodynamicConductorBlockEntity::new, THERMODYNAMIC_CONDUCTOR.get()).build());

    // ---- Logistical Transporter (items) ----
    public static final RegistrySupplier<Block> LOGISTICAL_TRANSPORTER = BLOCKS.register(LOGISTICAL_TRANSPORTER_ID, () -> new TransmitterBlock(
          props(LOGISTICAL_TRANSPORTER_ID), LogisticalTransporterBlockEntity::new, FabricTransmitters::logisticalTransporterBeType,
          TransmitterRenderType.LOGISTICAL_TRANSPORTER));
    public static final RegistrySupplier<Item> LOGISTICAL_TRANSPORTER_ITEM = blockItem(LOGISTICAL_TRANSPORTER_ID, LOGISTICAL_TRANSPORTER);
    public static final RegistrySupplier<BlockEntityType<LogisticalTransporterBlockEntity>> LOGISTICAL_TRANSPORTER_BE_TYPE =
          BE_TYPES.register(LOGISTICAL_TRANSPORTER_ID, () -> FabricBlockEntityTypeBuilder.create(
                LogisticalTransporterBlockEntity::new, LOGISTICAL_TRANSPORTER.get()).build());

    // ---- Mechanical Pipe (fluid) ----
    public static final RegistrySupplier<Block> MECHANICAL_PIPE = BLOCKS.register(MECHANICAL_PIPE_ID, () -> new TransmitterBlock(
          props(MECHANICAL_PIPE_ID), MechanicalPipeBlockEntity::new, FabricTransmitters::mechanicalPipeBeType,
          TransmitterRenderType.MECHANICAL_PIPE));
    public static final RegistrySupplier<Item> MECHANICAL_PIPE_ITEM = blockItem(MECHANICAL_PIPE_ID, MECHANICAL_PIPE);
    public static final RegistrySupplier<BlockEntityType<MechanicalPipeBlockEntity>> MECHANICAL_PIPE_BE_TYPE =
          BE_TYPES.register(MECHANICAL_PIPE_ID, () -> FabricBlockEntityTypeBuilder.create(
                MechanicalPipeBlockEntity::new, MECHANICAL_PIPE.get()).build());

    private FabricTransmitters() {
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();

        // Expose each transmitter's relay buffer through the matching Mekanism capability so neighbours (and the
        // self-test) can query it. The logistical transporter exposes no capability of its own — it operates purely by
        // reading neighbour ItemStorage.SIDED caps.
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, UNIVERSAL_CABLE_BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, PRESSURIZED_TUBE_BE_TYPE.get());
        MekanismFabricHeat.SIDED.registerForBlockEntity((be, context) -> be, THERMODYNAMIC_CONDUCTOR_BE_TYPE.get());
        MekanismFabricFluid.SIDED.registerForBlockEntity((be, context) -> be, MECHANICAL_PIPE_BE_TYPE.get());

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("mekanism"));
        CreativeTabRegistry.append(tab, UNIVERSAL_CABLE_ITEM.get());
        CreativeTabRegistry.append(tab, PRESSURIZED_TUBE_ITEM.get());
        CreativeTabRegistry.append(tab, THERMODYNAMIC_CONDUCTOR_ITEM.get());
        CreativeTabRegistry.append(tab, LOGISTICAL_TRANSPORTER_ITEM.get());
        CreativeTabRegistry.append(tab, MECHANICAL_PIPE_ITEM.get());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static BlockBehaviour.Properties props(Identifier blockId) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, blockId))
              .strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static RegistrySupplier<Item> blockItem(Identifier itemId, RegistrySupplier<Block> block) {
        return ITEMS.register(itemId, () -> new BlockItem(block.get(),
              new Item.Properties().setId(ResourceKey.create(Registries.ITEM, itemId)).useBlockDescriptionPrefix()));
    }

    // BE-type supplier indirection so each block's ticker can resolve its type lazily (types register after blocks).
    private static BlockEntityType<?> universalCableBeType() {
        return UNIVERSAL_CABLE_BE_TYPE.get();
    }

    private static BlockEntityType<?> pressurizedTubeBeType() {
        return PRESSURIZED_TUBE_BE_TYPE.get();
    }

    private static BlockEntityType<?> thermodynamicConductorBeType() {
        return THERMODYNAMIC_CONDUCTOR_BE_TYPE.get();
    }

    private static BlockEntityType<?> logisticalTransporterBeType() {
        return LOGISTICAL_TRANSPORTER_BE_TYPE.get();
    }

    private static BlockEntityType<?> mechanicalPipeBeType() {
        return MECHANICAL_PIPE_BE_TYPE.get();
    }
}
