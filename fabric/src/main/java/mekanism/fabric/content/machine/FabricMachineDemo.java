package mekanism.fabric.content.machine;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Transitional Fabric bring-up: registers a functional processing machine (block + item + block-entity-type) via
 * Architectury and wires BOTH capabilities — energy via {@link MekanismFabricEnergy#SIDED} and the item inventory via
 * fabric-transfer-api's {@link ItemStorage#SIDED} (backed by {@link InventoryStorage}). Proves the full machine archetype
 * on Fabric: ticking + energy + item I/O + persistence + dual capability exposure. Removed once real machines land in
 * {@code :common}.
 */
public final class FabricMachineDemo {

    private static final String MODID = "mekanism";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(MODID, "fabric_machine_demo");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> BLOCK = BLOCKS.register(ID, () -> new DemoMachineBlock(
          BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ID)).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistrySupplier<Item> ITEM = ITEMS.register(ID, () -> new BlockItem(BLOCK.get(),
          new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    public static final RegistrySupplier<BlockEntityType<DemoMachineBlockEntity>> BE_TYPE = BE_TYPES.register(ID, () ->
          FabricBlockEntityTypeBuilder.create(DemoMachineBlockEntity::new, BLOCK.get()).build());

    private FabricMachineDemo() {
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();
        // Expose energy + item inventory as capabilities so neighbours can interact.
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BE_TYPE.get());
    }
}
