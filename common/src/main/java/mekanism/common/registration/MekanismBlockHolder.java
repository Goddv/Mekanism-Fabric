package mekanism.common.registration;

import dev.architectury.registry.registries.RegistrySupplier;
import mekanism.api.text.IHasTranslationKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Loader-neutral registry handle for a block + its block-item pair, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.BlockRegistryObject}. The primary holder is the block (so it drops into
 * {@code Holder<Block>}/{@code Supplier<Block>} call sites via {@link MekanismRegistryObject}); the paired item is
 * reachable via {@link #item()}/{@link #asItem()}.
 */
public class MekanismBlockHolder<BLOCK extends Block, ITEM extends Item> extends MekanismRegistryObject<BLOCK>
      implements ItemLike, IHasTranslationKey {

    private final MekanismItemHolder<ITEM> item;

    public MekanismBlockHolder(RegistrySupplier<BLOCK> block, MekanismItemHolder<ITEM> item) {
        super(block);
        this.item = item;
    }

    @NotNull
    public BlockState defaultState() {
        return value().defaultBlockState();
    }

    public BLOCK block() {
        return value();
    }

    public MekanismItemHolder<ITEM> item() {
        return item;
    }

    @NotNull
    @Override
    public ITEM asItem() {
        return item.get();
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return value().getDescriptionId();
    }

    public Component getTextComponent() {
        return value().getName();
    }
}
