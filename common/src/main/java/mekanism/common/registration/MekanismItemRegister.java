package mekanism.common.registration;

import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Loader-neutral item deferred-register, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.ItemDeferredRegister}. Handles the loader-neutral parts (id assignment via
 * {@code Item.Properties.setId}, the register conveniences) and hands back {@link MekanismItemHolder}s. The NeoForge
 * capability/attachment/default-component event wiring is not reproduced here — it stays in the NeoForge register until
 * the registration base pair is unified.
 */
@NothingNullByDefault
public class MekanismItemRegister extends MekanismRegister<Item> {

    private final List<MekanismItemHolder<? extends Item>> entries = new ArrayList<>();

    public MekanismItemRegister(String modid) {
        super(modid, Registries.ITEM);
    }

    /** Registers a plain {@link Item}. */
    public MekanismItemHolder<Item> registerItem(String name) {
        return registerItem(name, Item::new);
    }

    /** Registers a plain {@link Item} with the given property modifications applied. */
    public MekanismItemHolder<Item> registerSimple(String name, UnaryOperator<Item.Properties> propertyModifier) {
        return registerItem(name, properties -> new Item(propertyModifier.apply(properties)));
    }

    /** Registers an item built by {@code creator}, which receives a {@link Item.Properties} that already has its id set. */
    public <ITEM extends Item> MekanismItemHolder<ITEM> registerItem(String name, Function<Item.Properties, ITEM> creator) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(modid, name));
        RegistrySupplier<ITEM> supplier = doRegister(name, () -> creator.apply(new Item.Properties().setId(key)));
        MekanismItemHolder<ITEM> holder = new MekanismItemHolder<>(supplier);
        entries.add(holder);
        return holder;
    }

    public List<MekanismItemHolder<? extends Item>> getEntries() {
        return entries;
    }
}
