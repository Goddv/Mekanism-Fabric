package mekanism.fabric.content.energy;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.fabric.chemical.MekanismFabricChemical;
import mekanism.fabric.energy.MekanismFabricEnergy;
import mekanism.fabric.heat.MekanismFabricHeat;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
 * Transitional Fabric bring-up: registers a functional energy block (block + block-item + block-entity-type) via
 * Architectury and wires its energy capability into {@link MekanismFabricEnergy#SIDED} via
 * {@code registerForBlockEntity}. Proves the full block-entity + capability-provider + persistence chain on Fabric —
 * the foundation for real Mekanism machines. Removed once the real machine framework is migrated to {@code :common}.
 */
public final class FabricEnergyBlockDemo {

    private static final String MODID = "mekanism";
    private static final Identifier ID = Identifier.fromNamespaceAndPath(MODID, "fabric_energy_demo");

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> BLOCK = BLOCKS.register(ID, () -> new DemoEnergyBlock(
          BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ID)).strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistrySupplier<Item> ITEM = ITEMS.register(ID, () -> new BlockItem(BLOCK.get(),
          new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    public static final RegistrySupplier<BlockEntityType<DemoEnergyBlockEntity>> BE_TYPE = BE_TYPES.register(ID, () ->
          FabricBlockEntityTypeBuilder.create(DemoEnergyBlockEntity::new, BLOCK.get()).build());

    private FabricEnergyBlockDemo() {
    }

    /** Finalize registrations (blocks -> BE types -> items so cross-references resolve), then register the energy + heat providers. */
    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricHeat.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
    }
}
