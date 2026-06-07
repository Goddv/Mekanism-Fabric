package mekanism.fabric.content;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Transitional Fabric bring-up content: registers a slice of Mekanism's simple resource items (plain {@link Item}s — real
 * ids whose models/textures/lang are already bundled) plus a Mekanism creative tab, via Architectury, so real Mekanism
 * content is visible in-game on Fabric. This is a deliberate stop-gap: it will be removed once the full
 * MekanismItems / MekanismCreativeTabs framework (with its attachment/container/capability machinery) is migrated into
 * {@code :common}, at which point both loaders register this content the same way.
 */
public final class FabricBringUpContent {

    private static final String MODID = "mekanism";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MODID, Registries.CREATIVE_MODE_TAB);
    private static final List<RegistrySupplier<Item>> SIMPLE_ITEMS = new ArrayList<>();

    /** Simple resource items (plain Items, no data components/capabilities) with assets already bundled. */
    private static final String[] SIMPLE_ITEM_NAMES = {
          "enriched_iron", "salt", "sawdust", "substrate", "bio_fuel", "dye_base", "fluorite_gem",
          "hdpe_pellet", "hdpe_rod", "hdpe_sheet", "hdpe_stick", "electrolytic_core", "teleportation_core",
          "yellow_cake_uranium", "reprocessed_fissile_fragment", "pellet_antimatter", "pellet_plutonium", "pellet_polonium"
    };

    static {
        for (String name : SIMPLE_ITEM_NAMES) {
            Identifier id = Identifier.fromNamespaceAndPath(MODID, name);
            SIMPLE_ITEMS.add(ITEMS.register(id, () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)))));
        }
    }

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register(Identifier.fromNamespaceAndPath(MODID, "mekanism"), () ->
          CreativeTabRegistry.create(builder -> builder
                .title(Component.literal("Mekanism"))
                .icon(() -> new ItemStack(SIMPLE_ITEMS.getFirst().get()))
                .displayItems((params, output) -> {
                    for (RegistrySupplier<Item> item : SIMPLE_ITEMS) {
                        output.accept(item.get());
                    }
                })
          ));

    private FabricBringUpContent() {
    }

    /** Finalize the deferred registrations (call during mod init). */
    public static void init() {
        ITEMS.register();
        TABS.register();
    }
}
