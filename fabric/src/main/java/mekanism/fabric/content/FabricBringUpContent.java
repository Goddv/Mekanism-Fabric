package mekanism.fabric.content;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import mekanism.common.block.basic.BlockResource;
import mekanism.common.item.block.ItemBlockMekanism;
import mekanism.common.registration.MekanismBlockHolder;
import mekanism.common.registration.MekanismBlockRegister;
import mekanism.common.registration.MekanismItemHolder;
import mekanism.common.registration.MekanismItemRegister;
import mekanism.common.resource.BlockResourceInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Transitional Fabric bring-up content: registers a slice of Mekanism's simple resource items (plain {@link Item}s) and
 * the real resource-storage blocks ({@link BlockResource} + {@link ItemBlockMekanism} block-items, now in {@code :common})
 * — real ids whose models/blockstates/textures/lang are already bundled — plus a "Mekanism" creative tab. Registered
 * through the loader-neutral {@code :common} {@link MekanismItemRegister}/{@link MekanismBlockRegister} framework (the real
 * item/block registration shapes), rather than hand-rolled Architectury DeferredRegisters. Deliberate stop-gap: the
 * remaining content (machines/multiblocks) still sidesteps the full MekanismItems/MekanismBlocks declarations (welded to
 * the attachment/container/capability + block-type/tile systems); once those move to {@code :common} this list is replaced
 * by the real registries (which would use the same framework).
 */
public final class FabricBringUpContent {

    private static final String MODID = "mekanism";
    private static final MekanismItemRegister ITEMS = new MekanismItemRegister(MODID);
    private static final MekanismBlockRegister BLOCKS = new MekanismBlockRegister(MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MODID, Registries.CREATIVE_MODE_TAB);
    /** Everything shown in the creative tab (simple items + block items), in registration order. */
    private static final List<MekanismItemHolder<? extends Item>> TAB_ENTRIES = new ArrayList<>();

    /** Simple resource items (plain Items, no data components/capabilities). */
    private static final String[] SIMPLE_ITEM_NAMES = {
          "enriched_iron", "salt", "sawdust", "substrate", "bio_fuel", "dye_base", "fluorite_gem",
          "hdpe_pellet", "hdpe_rod", "hdpe_sheet", "hdpe_stick", "electrolytic_core", "teleportation_core",
          "yellow_cake_uranium", "reprocessed_fissile_fragment", "pellet_antimatter", "pellet_plutonium", "pellet_polonium"
    };

    /**
     * Real resource-storage blocks ({@link BlockResource}) in NeoForge {@code MekanismBlocks} registration order; ids are
     * {@code block_<suffix>}. Explicit list (not {@code values()}) so future NeoForge-only constants do not auto-register
     * here and so {@code PrimaryResource} (which imports NeoForge Tags) never needs porting.
     */
    private static final BlockResourceInfo[] RESOURCE_BLOCKS = {
          //PrimaryResource-loop parity — replaces the former placeholder Blocks under IDENTICAL ids
          BlockResourceInfo.OSMIUM, BlockResourceInfo.RAW_OSMIUM,
          BlockResourceInfo.TIN, BlockResourceInfo.RAW_TIN,
          BlockResourceInfo.LEAD, BlockResourceInfo.RAW_LEAD,
          BlockResourceInfo.URANIUM, BlockResourceInfo.RAW_URANIUM,
          //MekanismBlocks named-constant parity — net-new on Fabric
          BlockResourceInfo.BRONZE, BlockResourceInfo.REFINED_OBSIDIAN, BlockResourceInfo.CHARCOAL,
          BlockResourceInfo.REFINED_GLOWSTONE, BlockResourceInfo.STEEL, BlockResourceInfo.FLUORITE
    };

    static {
        for (String name : SIMPLE_ITEM_NAMES) {
            TAB_ENTRIES.add(ITEMS.registerItem(name));
        }
        for (BlockResourceInfo resource : RESOURCE_BLOCKS) {
            //Mirrors NeoForge MekanismBlocks.registerResourceBlock: the BlockResource ctor applies
            //requiresCorrectToolForDrops + resource.modifyProperties; only non-burning resources get fire-resistant items.
            MekanismBlockHolder<BlockResource, ItemBlockMekanism<BlockResource>> block = BLOCKS.register(
                  "block_" + resource.getRegistrySuffix(),
                  properties -> new BlockResource(properties, resource),
                  (b, itemProperties) -> {
                      if (!b.getResourceInfo().burnsInFire()) {
                          itemProperties = itemProperties.fireResistant();
                      }
                      return new ItemBlockMekanism<>(b, itemProperties);
                  });
            TAB_ENTRIES.add(block.item());
        }
    }

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register(Identifier.fromNamespaceAndPath(MODID, "mekanism"), () ->
          CreativeTabRegistry.create(builder -> builder
                .title(Component.literal("Mekanism"))
                .icon(() -> new ItemStack(TAB_ENTRIES.getFirst().get()))
                .displayItems((params, output) -> {
                    for (MekanismItemHolder<? extends Item> entry : TAB_ENTRIES) {
                        output.accept(entry.get());
                    }
                })
          ));

    private FabricBringUpContent() {
    }

    /** Finalize the deferred registrations (call during mod init). Blocks first so block-items can resolve them. */
    public static void init() {
        BLOCKS.register();
        ITEMS.register();
        TABS.register();
    }
}
