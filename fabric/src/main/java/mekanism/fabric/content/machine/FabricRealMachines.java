package mekanism.fabric.content.machine;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
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
 * Transitional Fabric bring-up: registers REAL Mekanism machine blocks (enrichment_chamber, crusher, energized_smelter,
 * osmium_compressor, combiner) on Fabric — real ids whose facing/active blockstates + models + textures are already
 * bundled — sharing one {@link MachineBlock} class + {@link MachineBlockEntity} (energy + items + processing). Wires both
 * capabilities and adds them to the Mekanism creative tab. The processing loop is a demo; the real recipe/GUI systems
 * replace it once the machine framework is migrated to :common.
 */
public final class FabricRealMachines {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    private static final List<RegistrySupplier<Block>> MACHINE_BLOCKS = new ArrayList<>();
    private static final List<RegistrySupplier<Item>> MACHINE_ITEMS = new ArrayList<>();

    private static final String[] MACHINE_NAMES = {
          "enrichment_chamber", "crusher", "energized_smelter", "osmium_compressor", "combiner"
    };

    static {
        for (String name : MACHINE_NAMES) {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
            RegistrySupplier<Block> block = BLOCKS.register(id, () -> new MachineBlock(BlockBehaviour.Properties.of()
                  .setId(ResourceKey.create(Registries.BLOCK, id)).strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
            MACHINE_BLOCKS.add(block);
            MACHINE_ITEMS.add(ITEMS.register(id, () -> new BlockItem(block.get(),
                  new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix())));
        }
    }

    /** Single block-entity type shared by all machine blocks. */
    public static final RegistrySupplier<BlockEntityType<MachineBlockEntity>> BE_TYPE = BE_TYPES.register(
          Identifier.fromNamespaceAndPath(MODID, "machine"), () ->
                FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new,
                      MACHINE_BLOCKS.stream().map(RegistrySupplier::get).toArray(Block[]::new)).build());

    private FabricRealMachines() {
    }

    /** A representative machine block (enrichment_chamber) for the dev self-test. */
    public static RegistrySupplier<Block> enrichmentChamber() {
        return MACHINE_BLOCKS.getFirst();
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        ITEMS.register();
        MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, BE_TYPE.get());
        ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), BE_TYPE.get());
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        for (RegistrySupplier<Item> machineItem : MACHINE_ITEMS) {
            CreativeTabRegistry.append(tab, machineItem);
        }
    }
}
