package mekanism.common.block.states;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge layer over {@link IStateFluidLoggableBase}: keeps only the members whose bodies need NeoForge-only fluid
 * types — {@link #pickupBlock} (via {@code FluidType#getBucket}) and {@link #getPickupSound(BlockState)} (from
 * {@code IBucketPickupExtension}) — plus the concrete {@link #getFluidLoggedProperty()} default (kept here because its
 * body references {@code BlockStateHelper}, breaking a cross-gate cycle). All implementors keep implementing this
 * interface unchanged; every loader-neutral member is inherited from the base.
 */
public interface IStateFluidLoggable extends IStateFluidLoggableBase {

    /**
     * Gets the fluids this fluid loggable block supports. Overriding this is an easy way to change the block from supporting water and lava logging to supporting
     * specific different types of fluid, but dynamic fluid stuff cannot be done without a sizeable patch to forge/a change in vanilla so that
     * {@link BlockState#getFluidState()} has position information.
     *
     * @return BlockState property for representing fluid loggable blocks
     */
    @NotNull
    @Override
    default EnumProperty<? extends IFluidLogType> getFluidLoggedProperty() {
        return BlockStateHelper.FLUID_LOGGED;
    }

    @NotNull
    @Override
    default ItemStack pickupBlock(@Nullable LivingEntity owner, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockState state) {
        IFluidLogType fluidLogged = state.getValue(getFluidLoggedProperty());
        if (!fluidLogged.isEmpty()) {
            Fluid fluid = fluidLogged.getFluid();
            ItemStack bucket = fluid.getFluidType().getBucket(new FluidStack(fluid, FluidType.BUCKET_VOLUME));
            if (!bucket.isEmpty()) {
                world.setBlock(pos, setState(state, Fluids.EMPTY), Block.UPDATE_ALL);
                return bucket;
            }
        }
        return ItemStack.EMPTY;
    }

    @NotNull
    @Override
    default Optional<SoundEvent> getPickupSound(BlockState state) {
        return getFluid(state).getType().getPickupSound();
    }
}
