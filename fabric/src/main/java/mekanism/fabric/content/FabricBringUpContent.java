package mekanism.fabric.content;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import mekanism.common.registration.MekanismBlockHolder;
import mekanism.common.registration.MekanismBlockRegister;
import mekanism.common.registration.MekanismItemHolder;
import mekanism.common.registration.MekanismItemRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Transitional Fabric bring-up content: registers a slice of Mekanism's simple resource items (plain {@link Item}s) and
 * simple storage blocks (plain {@link Block}s + {@link BlockItem}s) — real ids whose models/blockstates/textures/lang are
 * already bundled — plus a "Mekanism" creative tab. Now registered through the loader-neutral {@code :common}
 * {@link MekanismItemRegister}/{@link MekanismBlockRegister} framework (the real item/block registration shapes), rather
 * than hand-rolled Architectury DeferredRegisters. Deliberate stop-gap: simple content sidesteps the full
 * MekanismItems/MekanismBlocks declarations (welded to the attachment/container/capability + block-type/tile systems);
 * once those move to {@code :common} this list is replaced by the real registries (which would use the same framework).
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

    /** Simple storage blocks (plain Blocks, no block-entity) with full bundled asset chains. */
    private static final String[] STORAGE_BLOCK_NAMES = {
          "block_osmium", "block_tin", "block_lead", "block_uranium",
          "block_raw_osmium", "block_raw_tin", "block_raw_lead", "block_raw_uranium"
    };

    static {
        for (String name : SIMPLE_ITEM_NAMES) {
            TAB_ENTRIES.add(ITEMS.registerItem(name));
        }
        for (String name : STORAGE_BLOCK_NAMES) {
            MekanismBlockHolder<Block, BlockItem> block = BLOCKS.registerSimple(name, properties -> properties
                  .strength(5.0F, 6.0F)
                  .requiresCorrectToolForDrops()
                  .sound(SoundType.METAL));
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
