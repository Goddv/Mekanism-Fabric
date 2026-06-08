package mekanism.common.registration;

import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.text.IHasTranslationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

/**
 * Loader-neutral registry handle for an item, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.ItemRegistryObject}. Carries the loader-neutral conveniences real Mekanism
 * item registration relies on ({@link #asItem()}, {@link #asStack()}, translation key) on top of the Architectury
 * {@link MekanismRegistryObject} base. The NeoForge-only attachment/capability wiring stays in the per-loader register;
 * once the registration base pair is unified, this and the NeoForge {@code ItemRegistryObject} collapse together.
 */
public class MekanismItemHolder<ITEM extends Item> extends MekanismRegistryObject<ITEM> implements ItemLike, IHasTranslationKey {

    public MekanismItemHolder(RegistrySupplier<ITEM> holder) {
        super(holder);
    }

    @NotNull
    @Override
    public ITEM asItem() {
        return value();
    }

    public ItemStack asStack() {
        return asStack(1);
    }

    public ItemStack asStack(int count) {
        return new ItemStack(value(), count);
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return value().getDescriptionId();
    }

    public Component getTextComponent() {
        return value().getName(asStack());
    }
}
