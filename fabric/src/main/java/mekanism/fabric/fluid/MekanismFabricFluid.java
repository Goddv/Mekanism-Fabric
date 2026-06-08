package mekanism.fabric.fluid;

import mekanism.api.fluid.ISimpleFluidHandler;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric analog of NeoForge's fluid capability: Mekanism's block-level fluid handler exposed through Fabric API's
 * {@link BlockApiLookup}, over the loader-neutral {@link ISimpleFluidHandler} / {@link mekanism.api.fluid.IFluidStack}.
 * The 4th core capability brought to Fabric (after energy/heat/chemical), reusing the same pattern. (Transitional: uses
 * ISimpleFluidHandler until the full mekanism.api.fluid handler API is hoisted.)
 */
public final class MekanismFabricFluid {

    private MekanismFabricFluid() {
    }

    public static final BlockApiLookup<ISimpleFluidHandler, @Nullable Direction> SIDED = BlockApiLookup.get(
          Identifier.fromNamespaceAndPath("mekanism", "fluid_handler"),
          ISimpleFluidHandler.class,
          Direction.class
    );

    @Nullable
    public static ISimpleFluidHandler getFluidHandler(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return SIDED.find(level, pos, side);
    }
}
