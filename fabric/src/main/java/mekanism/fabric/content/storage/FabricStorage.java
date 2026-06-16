package mekanism.fabric.content.storage;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.fabric.fluid.MekanismFabricFluid;
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
 * Transitional Fabric bring-up: registers Mekanism's fluid STORAGE — the Basic Fluid Tank ({@code basic_fluid_tank}, its
 * REAL id, matching {@code MekanismBlocks.BASIC_FLUID_TANK}) — as a functional fluid container. It gets a block +
 * block-item + block-entity type, exposes the {@link MekanismFabricFluid#SIDED} fluid capability (so pipes / machines /
 * the self-test can fill+drain it), and is added to the Mekanism creative tab.
 *
 * <p>The bundled {@code basic_fluid_tank} blockstate ({@code active=false/true} → {@code fluid_tank}/
 * {@code fluid_tank_active}) and block models are parseable vanilla JSON, so they are used as-is (no Fabric override
 * needed). Only the datagen ITEM-model copy uses a NeoForge custom loader ({@code minecraft:special} +
 * {@code mekanism:fluid_tank}); a Fabric-only simple item-model def overrides it (see {@code fabric/build.gradle} drop).
 */
public final class FabricStorage {

    private static final String MODID = "mekanism";

    private static final Identifier BASIC_FLUID_TANK_ID = id("basic_fluid_tank");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> BASIC_FLUID_TANK = BLOCKS.register(BASIC_FLUID_TANK_ID,
          () -> new FluidTankBlock(props(BASIC_FLUID_TANK_ID)));
    public static final RegistrySupplier<Item> BASIC_FLUID_TANK_ITEM = blockItem(BASIC_FLUID_TANK_ID, BASIC_FLUID_TANK);
    public static final RegistrySupplier<BlockEntityType<FluidTankBlockEntity>> BASIC_FLUID_TANK_BE_TYPE =
          BE_TYPES.register(BASIC_FLUID_TANK_ID, () -> FabricBlockEntityTypeBuilder.create(
                FluidTankBlockEntity::new, BASIC_FLUID_TANK.get()).build());

    private FabricStorage() {
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();

        // Expose the tank's fluid storage through the Mekanism fluid capability so pipes/machines/the self-test connect.
        MekanismFabricFluid.SIDED.registerForBlockEntity((be, context) -> be, BASIC_FLUID_TANK_BE_TYPE.get());

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("mekanism"));
        CreativeTabRegistry.append(tab, BASIC_FLUID_TANK_ITEM.get());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static BlockBehaviour.Properties props(Identifier blockId) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, blockId))
              .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    private static RegistrySupplier<Item> blockItem(Identifier itemId, RegistrySupplier<Block> block) {
        return ITEMS.register(itemId, () -> new BlockItem(block.get(),
              new Item.Properties().setId(ResourceKey.create(Registries.ITEM, itemId)).useBlockDescriptionPrefix()));
    }
}
