package mekanism.common.registration;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Loader-neutral block+item deferred-register, the Architectury-based counterpart of the NeoForge
 * {@code mekanism.common.registration.impl.BlockDeferredRegister}. Registers each block together with its block-item
 * (mirroring the NeoForge double-register), assigning ids via {@code setId}. Call {@link #register()} once per loader to
 * finalize — blocks are finalized before items so each {@link BlockItem} resolves its block.
 */
@NothingNullByDefault
public class MekanismBlockRegister {

    private final String modid;
    private final DeferredRegister<Block> blocks;
    private final MekanismItemRegister items;
    private final List<MekanismBlockHolder<? extends Block, ? extends Item>> entries = new ArrayList<>();

    public MekanismBlockRegister(String modid) {
        this.modid = modid;
        this.blocks = DeferredRegister.create(modid, Registries.BLOCK);
        this.items = new MekanismItemRegister(modid);
    }

    /** Registers a plain {@link Block} (+ default {@link BlockItem}) with the given property modifications applied. */
    public MekanismBlockHolder<Block, BlockItem> registerSimple(String name, UnaryOperator<BlockBehaviour.Properties> propertyModifier) {
        return register(name, properties -> new Block(propertyModifier.apply(properties)));
    }

    /** Registers a block built by {@code blockCreator} (+ default {@link BlockItem}). */
    public <BLOCK extends Block> MekanismBlockHolder<BLOCK, BlockItem> register(String name, Function<BlockBehaviour.Properties, BLOCK> blockCreator) {
        return register(name, blockCreator, BlockItem::new);
    }

    /** Registers a block built by {@code blockCreator} together with a block-item built by {@code itemCreator}. */
    public <BLOCK extends Block, ITEM extends BlockItem> MekanismBlockHolder<BLOCK, ITEM> register(String name,
          Function<BlockBehaviour.Properties, BLOCK> blockCreator, BiFunction<BLOCK, Item.Properties, ITEM> itemCreator) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(modid, name));
        RegistrySupplier<BLOCK> blockSupplier = blocks.register(Identifier.fromNamespaceAndPath(modid, name),
              () -> blockCreator.apply(BlockBehaviour.Properties.of().setId(blockKey)));
        MekanismItemHolder<ITEM> itemHolder = items.registerItem(name,
              itemProperties -> itemCreator.apply(blockSupplier.get(), itemProperties.useBlockDescriptionPrefix()));
        MekanismBlockHolder<BLOCK, ITEM> holder = new MekanismBlockHolder<>(blockSupplier, itemHolder);
        entries.add(holder);
        return holder;
    }

    /** Finalizes blocks then items, so each block-item resolves its (already-registered) block. */
    public void register() {
        blocks.register();
        items.register();
    }

    public List<MekanismBlockHolder<? extends Block, ? extends Item>> getEntries() {
        return entries;
    }
}
