package mekanism.fabric.content.generator;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.fabric.energy.MekanismFabricEnergy;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
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
 * Transitional Fabric bring-up: registers the four core Mekanism generators — {@code solar_generator},
 * {@code wind_generator}, {@code heat_generator}, {@code bio_generator} — under the separate {@code mekanismgenerators}
 * namespace (their own module), each as a {@link GeneratorBlock} + {@link AbstractGeneratorBlockEntity}. Reuses the
 * existing Fabric energy capability ({@link MekanismFabricEnergy#SIDED}) so the demo cable relays their output to
 * machines; the fuel-burning generators (Heat/Bio) also expose their fuel slot via {@code ItemStorage} for hoppers.
 * Block-items are added to the shared {@code mekanism} creative tab. Bundled {@code mekanismgenerators} blockstates +
 * models render them (the Bio Generator's composite model is flattened client-side).
 */
public final class FabricGenerators {

    private static final String MODID = "mekanismgenerators";

    private static final Identifier SOLAR_ID = Identifier.fromNamespaceAndPath(MODID, "solar_generator");
    private static final Identifier WIND_ID = Identifier.fromNamespaceAndPath(MODID, "wind_generator");
    private static final Identifier HEAT_ID = Identifier.fromNamespaceAndPath(MODID, "heat_generator");
    private static final Identifier BIO_ID = Identifier.fromNamespaceAndPath(MODID, "bio_generator");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    // ---- blocks (blockstate shape matches the bundled mekanismgenerators JSONs) ----
    // The block-entity types are declared AFTER the blocks; the BE-type access in each block goes through a static
    // method (e.g. FabricGenerators::solarBeType) so it isn't an illegal forward reference to a later-declared field.
    // solar_generator: empty-variant blockstate (no properties), no inventory.
    public static final RegistrySupplier<Block> SOLAR_GENERATOR = BLOCKS.register(SOLAR_ID, () -> new GeneratorBlock(
          props(SOLAR_ID), SolarGeneratorBlockEntity::new, FabricGenerators::solarBeType, false, false, false));
    // wind_generator: facing-only blockstate, no inventory.
    public static final RegistrySupplier<Block> WIND_GENERATOR = BLOCKS.register(WIND_ID, () -> new GeneratorBlock(
          props(WIND_ID), WindGeneratorBlockEntity::new, FabricGenerators::windBeType, true, false, false));
    // heat_generator: facing + active blockstate, 1-slot fuel inventory (right-click / hopper feed).
    public static final RegistrySupplier<Block> HEAT_GENERATOR = BLOCKS.register(HEAT_ID, () -> new GeneratorBlock(
          props(HEAT_ID), HeatGeneratorBlockEntity::new, FabricGenerators::heatBeType, true, true, true));
    // bio_generator: facing-only blockstate, 1-slot fuel inventory (right-click / hopper feed).
    public static final RegistrySupplier<Block> BIO_GENERATOR = BLOCKS.register(BIO_ID, () -> new GeneratorBlock(
          props(BIO_ID), BioGeneratorBlockEntity::new, FabricGenerators::bioBeType, true, false, true));

    // ---- block-entity types ----
    public static final RegistrySupplier<BlockEntityType<SolarGeneratorBlockEntity>> SOLAR_BE_TYPE = BE_TYPES.register(SOLAR_ID, () ->
          FabricBlockEntityTypeBuilder.create(SolarGeneratorBlockEntity::new, SOLAR_GENERATOR.get()).build());
    public static final RegistrySupplier<BlockEntityType<WindGeneratorBlockEntity>> WIND_BE_TYPE = BE_TYPES.register(WIND_ID, () ->
          FabricBlockEntityTypeBuilder.create(WindGeneratorBlockEntity::new, WIND_GENERATOR.get()).build());
    public static final RegistrySupplier<BlockEntityType<HeatGeneratorBlockEntity>> HEAT_BE_TYPE = BE_TYPES.register(HEAT_ID, () ->
          FabricBlockEntityTypeBuilder.create(HeatGeneratorBlockEntity::new, HEAT_GENERATOR.get()).build());
    public static final RegistrySupplier<BlockEntityType<BioGeneratorBlockEntity>> BIO_BE_TYPE = BE_TYPES.register(BIO_ID, () ->
          FabricBlockEntityTypeBuilder.create(BioGeneratorBlockEntity::new, BIO_GENERATOR.get()).build());

    public static final RegistrySupplier<Item> SOLAR_ITEM = ITEMS.register(SOLAR_ID, () -> blockItem(SOLAR_GENERATOR, SOLAR_ID));
    public static final RegistrySupplier<Item> WIND_ITEM = ITEMS.register(WIND_ID, () -> blockItem(WIND_GENERATOR, WIND_ID));
    public static final RegistrySupplier<Item> HEAT_ITEM = ITEMS.register(HEAT_ID, () -> blockItem(HEAT_GENERATOR, HEAT_ID));
    public static final RegistrySupplier<Item> BIO_ITEM = ITEMS.register(BIO_ID, () -> blockItem(BIO_GENERATOR, BIO_ID));

    private FabricGenerators() {
    }

    // Indirection so each block's BE-type supplier isn't an illegal forward reference to a later-declared field.
    private static BlockEntityType<?> solarBeType() {
        return SOLAR_BE_TYPE.get();
    }

    private static BlockEntityType<?> windBeType() {
        return WIND_BE_TYPE.get();
    }

    private static BlockEntityType<?> heatBeType() {
        return HEAT_BE_TYPE.get();
    }

    private static BlockEntityType<?> bioBeType() {
        return BIO_BE_TYPE.get();
    }

    private static BlockBehaviour.Properties props(Identifier id) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))
              .strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static BlockItem blockItem(RegistrySupplier<Block> block, Identifier id) {
        return new BlockItem(block.get(), new Item.Properties()
              .setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix());
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();

        // Energy capability for all four (cables/machines pull their output). Fuel generators also expose their fuel
        // slot via ItemStorage so hoppers can feed them.
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, SOLAR_BE_TYPE.get());
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, WIND_BE_TYPE.get());
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, HEAT_BE_TYPE.get());
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BIO_BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), HEAT_BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BIO_BE_TYPE.get());

        // Add the generators to the existing Mekanism creative tab so they're reachable in-game.
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
              Identifier.fromNamespaceAndPath("mekanism", "mekanism"));
        CreativeTabRegistry.append(tab, SOLAR_ITEM.get());
        CreativeTabRegistry.append(tab, WIND_ITEM.get());
        CreativeTabRegistry.append(tab, HEAT_ITEM.get());
        CreativeTabRegistry.append(tab, BIO_ITEM.get());
    }
}
