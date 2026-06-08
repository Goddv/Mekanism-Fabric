package mekanism.fabric.content.power;

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
 * Transitional Fabric bring-up: registers a basic fuel-burning {@link GeneratorBlock} and an energy {@link CableBlock},
 * so Mekanism machines can be powered in-game (generator &rarr; cable &rarr; machine) for manual testing — instead of
 * the self-test injecting energy programmatically. Reuses bundled {@code mekanism} block models for rendering; wires the
 * strict-energy capability (both) + the generator's fuel inventory ({@code ItemStorage}, for hoppers). Adds both to the
 * Mekanism creative tab. Replaced when the real generators ({@code mekanismgenerators} module) + transmitter network are
 * ported.
 */
public final class FabricPowerInfrastructure {

    private static final String MODID = "mekanism";
    private static final Identifier GENERATOR_ID = Identifier.fromNamespaceAndPath(MODID, "fabric_generator");
    private static final Identifier CABLE_ID = Identifier.fromNamespaceAndPath(MODID, "fabric_cable");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> GENERATOR = BLOCKS.register(GENERATOR_ID, () -> new GeneratorBlock(
          BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, GENERATOR_ID))
                .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistrySupplier<Item> GENERATOR_ITEM = ITEMS.register(GENERATOR_ID, () -> new BlockItem(GENERATOR.get(),
          new Item.Properties().setId(ResourceKey.create(Registries.ITEM, GENERATOR_ID)).useBlockDescriptionPrefix()));
    public static final RegistrySupplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR_BE_TYPE = BE_TYPES.register(GENERATOR_ID, () ->
          FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, GENERATOR.get()).build());

    public static final RegistrySupplier<Block> CABLE = BLOCKS.register(CABLE_ID, () -> new CableBlock(
          BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, CABLE_ID))
                .strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistrySupplier<Item> CABLE_ITEM = ITEMS.register(CABLE_ID, () -> new BlockItem(CABLE.get(),
          new Item.Properties().setId(ResourceKey.create(Registries.ITEM, CABLE_ID)).useBlockDescriptionPrefix()));
    public static final RegistrySupplier<BlockEntityType<CableBlockEntity>> CABLE_BE_TYPE = BE_TYPES.register(CABLE_ID, () ->
          FabricBlockEntityTypeBuilder.create(CableBlockEntity::new, CABLE.get()).build());

    private FabricPowerInfrastructure() {
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();
        // Energy capability for both; generator also exposes its fuel inventory so hoppers can feed it.
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, GENERATOR_BE_TYPE.get());
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, CABLE_BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), GENERATOR_BE_TYPE.get());
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        CreativeTabRegistry.append(tab, GENERATOR_ITEM.get());
        CreativeTabRegistry.append(tab, CABLE_ITEM.get());
    }
}
