package mekanism.fabric.content.machine.factory;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Transitional Fabric bring-up: registers all 36 Mekanism FACTORY blocks — the cross product of the 4 tiers
 * ({@link FactoryTier}: basic=3/advanced=5/elite=7/ultimate=9 processes) and the 9 {@link FactoryType}s
 * (smelting/enriching/crushing/sawing/compressing/purifying/injecting/infusing/combining). Block id is
 * {@code <tier>_<factorytype>_factory} (e.g. {@code basic_enriching_factory}), using the bundled blockstate/model/item
 * assets. Each block is a {@link FactoryBlock} carrying its {@link FactoryType} + process count; one {@link
 * FactoryBlockEntity} class backs them all, with ONE shared block-entity type per process count (so the BE type can list
 * its blocks). Wires the energy / item / chemical capabilities and adds every factory to the Mekanism creative tab.
 */
public final class FabricFactories {

    private static final String MODID = "mekanism";

    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(MODID, Registries.BLOCK_ENTITY_TYPE);

    /** All 36 factory block holders, in registration order. */
    private static final List<MekanismBlockHolder<FactoryBlock, BlockItem>> FACTORIES = new ArrayList<>();
    /** Factory block holders keyed by block id (e.g. {@code basic_enriching_factory}) for the self-test. */
    private static final Map<String, MekanismBlockHolder<FactoryBlock, BlockItem>> BY_ID = new java.util.HashMap<>();
    /** Factory block holders grouped by tier, so each tier's BE type can list its 9 blocks. */
    private static final Map<FactoryTier, List<MekanismBlockHolder<FactoryBlock, BlockItem>>> BY_TIER = new EnumMap<>(FactoryTier.class);
    /** One block-entity type per tier (= per process count). */
    private static final Map<FactoryTier, RegistrySupplier<BlockEntityType<FactoryBlockEntity>>> BE_TYPE_BY_TIER = new EnumMap<>(FactoryTier.class);

    static {
        for (FactoryTier tier : FactoryTier.values()) {
            BY_TIER.put(tier, new ArrayList<>());
        }
        // Register the 36 blocks. Each block's BE-type supplier points to its tier's (lazily-resolved) BE type.
        for (FactoryTier tier : FactoryTier.values()) {
            int processes = tier.getProcesses();
            for (FactoryType type : FactoryType.values()) {
                String id = tier.getName() + "_" + type.getRegistryNameComponent() + "_factory";
                MekanismBlockHolder<FactoryBlock, BlockItem> holder = BLOCKS.register(id, properties ->
                      new FactoryBlock(properties.strength(3.5F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.METAL),
                            type, processes, () -> beTypeFor(tier)));
                FACTORIES.add(holder);
                BY_TIER.get(tier).add(holder);
                BY_ID.put(id, holder);
            }
        }
        // One BE type per tier, listing all 9 factory blocks of that tier.
        for (FactoryTier tier : FactoryTier.values()) {
            BE_TYPE_BY_TIER.put(tier, BE_TYPES.register(
                  Identifier.fromNamespaceAndPath(MODID, "factory_" + tier.getName()), () ->
                        FabricBlockEntityTypeBuilder.create(FactoryBlockEntity::new,
                              BY_TIER.get(tier).stream().map(MekanismBlockHolder::block).toArray(Block[]::new)).build()));
        }
    }

    /** Resolves a tier's BE type (used by each block's BE-type supplier + the server ticker gate). */
    private static BlockEntityType<?> beTypeFor(FactoryTier tier) {
        return BE_TYPE_BY_TIER.get(tier).get();
    }

    private FabricFactories() {
    }

    /** A representative factory block holder for the self-test (by block id, e.g. {@code basic_enriching_factory}). */
    public static MekanismBlockHolder<FactoryBlock, BlockItem> get(String blockId) {
        MekanismBlockHolder<FactoryBlock, BlockItem> holder = BY_ID.get(blockId);
        if (holder == null) {
            throw new IllegalArgumentException("Unknown factory block: " + blockId);
        }
        return holder;
    }

    public static void init() {
        BLOCKS.register();
        BE_TYPES.register();
        // Per-tier: energy sink + item I/O + (the chemical factories within the tier expose) the shared chemical tank.
        // The chemical capability is registered for every factory BE type; non-chemical factories report 0 tanks.
        for (FactoryTier tier : FactoryTier.values()) {
            BlockEntityType<FactoryBlockEntity> beType = BE_TYPE_BY_TIER.get(tier).get();
            MekanismFabricEnergy.SIDED.registerForBlockEntity((be, context) -> be, beType);
            ItemStorage.SIDED.registerForBlockEntity((be, direction) -> ContainerStorage.of(be, direction), beType);
            MekanismFabricChemical.SIDED.registerForBlockEntity((be, context) -> be, beType);
        }
        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
              Identifier.fromNamespaceAndPath(MODID, "mekanism"));
        for (MekanismBlockHolder<FactoryBlock, BlockItem> holder : FACTORIES) {
            CreativeTabRegistry.append(tab, holder.item().get());
        }
    }
}
