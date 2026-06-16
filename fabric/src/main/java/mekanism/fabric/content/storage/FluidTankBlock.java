package mekanism.fabric.content.storage;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Host block for the Basic Fluid Tank ({@code mekanism:basic_fluid_tank}). A passive {@link EntityBlock} whose
 * block-entity ({@link FluidTankBlockEntity}) stores fluid and exposes it as the fluid capability. Right-clicking opens
 * the tank GUI (the generic {@link mekanism.fabric.content.machine.gui.MekanismMachineMenu} with the fluid-tank shape).
 *
 * <p>Carries the {@code active} blockstate property the bundled {@code basic_fluid_tank} blockstate keys on
 * ({@code active=false} → {@code fluid_tank}, {@code active=true} → {@code fluid_tank_active}); kept {@code false} for now
 * (active glow is cosmetic and tied to the real tank's auto-output state, deferred). No ticker — the tank is passive;
 * pipes drive fill/drain.
 */
public class FluidTankBlock extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FluidTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
              && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            MenuRegistry.openExtendedMenu(serverPlayer, tank.menuProvider());
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }
}
