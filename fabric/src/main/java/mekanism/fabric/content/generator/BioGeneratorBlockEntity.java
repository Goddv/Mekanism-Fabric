package mekanism.fabric.content.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transitional Fabric bring-up: the Bio Generator. The NeoForge tile burns Mekanism "bio fuel" items/blocks (tag
 * {@code mekanism:fuels/bio}) into a bioethanol fluid tank, then converts fluid to energy. Those tags/fluids aren't
 * ported yet, so for bring-up we accept a transitional set of plant matter — anything compostable (vanilla
 * {@link ComposterBlock#COMPOSTABLES}) plus a hardcoded staple list (sugar cane, wheat, dried kelp) — and burn each
 * accepted item for a fixed duration at a fixed per-tick rate. 1-slot fuel inventory ({@link WorldlyContainer} via the
 * base).
 */
public class BioGeneratorBlockEntity extends AbstractFuelGeneratorBlockEntity {

    /** Energy per tick while burning. Transitional constant (NeoForge config is unavailable on Fabric). */
    private static final long BIO_GENERATION_PER_TICK = 350L;
    /** Fixed burn time (ticks) per accepted bio item. */
    private static final int BIO_BURN_TICKS = 200;

    public BioGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricGenerators.BIO_BE_TYPE.get(), pos, state);
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Compostable items are a good proxy for "plant matter" / bio fuel; add a couple of staples explicitly in case
        // composability differs across versions.
        return ComposterBlock.COMPOSTABLES.containsKey(stack.getItem())
              || stack.is(Items.SUGAR_CANE) || stack.is(Items.WHEAT) || stack.is(Items.DRIED_KELP);
    }

    @Override
    protected int burnDurationFor(ItemStack stack) {
        return BIO_BURN_TICKS;
    }

    @Override
    protected long generationPerTick() {
        return BIO_GENERATION_PER_TICK;
    }
}
