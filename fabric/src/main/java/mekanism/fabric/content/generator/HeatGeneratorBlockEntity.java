package mekanism.fabric.content.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Transitional Fabric bring-up: the Heat Generator. For now a furnace-fuel burner (the demo {@code GeneratorBlockEntity}
 * logic, lifted into {@link AbstractFuelGeneratorBlockEntity}): a 1-slot fuel inventory burns vanilla furnace fuels
 * (coal/charcoal/lava bucket/etc.) into energy. The NeoForge tile additionally consumes a lava fluid tank and applies a
 * heat/Carnot model + neighbouring-lava boost; that fluid/heat path is DEFERRED for bring-up.
 */
public class HeatGeneratorBlockEntity extends AbstractFuelGeneratorBlockEntity {

    /** Energy per tick while burning. Transitional constant (NeoForge config is unavailable on Fabric). */
    private static final long HEAT_GENERATION_PER_TICK = 200L;

    public HeatGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FabricGenerators.HEAT_BE_TYPE.get(), pos, state);
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        return level != null && level.fuelValues().isFuel(stack);
    }

    @Override
    protected int burnDurationFor(ItemStack stack) {
        return level == null ? 0 : level.fuelValues().burnDuration(stack);
    }

    @Override
    protected long generationPerTick() {
        return HEAT_GENERATION_PER_TICK;
    }
}
