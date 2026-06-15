package mekanism.fabric.content.transmitter;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: the shared host block for the four core Mekanism transmitters (Universal Cable,
 * Pressurized Tube, Thermodynamic Conductor, Logistical Transporter). Each is a passive {@link EntityBlock} whose
 * block-entity is an <em>adjacent-relay</em> — it moves its resource (energy/chemical/heat/items) between neighbouring
 * providers and acceptors every server tick. This is the deliberately-simple transitional model: NOT the real Mekanism
 * transmitter-network graph (that + connected multipart rendering are deferred), but enough to carry a resource across a
 * line of transmitters one hop at a time.
 *
 * <p>Parameterized by a block-entity factory + the {@link BlockEntityType} supplier (resolved lazily, since the type
 * registers after the blocks), so all four transmitters share this one class — mirroring {@code CableBlock} but generic.
 */
public class TransmitterBlock extends Block implements EntityBlock {

    private final BiFunction<BlockPos, BlockState, ? extends TransmitterBlockEntity> factory;
    private final Supplier<BlockEntityType<?>> beType;

    public TransmitterBlock(Properties properties, BiFunction<BlockPos, BlockState, ? extends TransmitterBlockEntity> factory,
          Supplier<BlockEntityType<?>> beType) {
        super(properties);
        this.factory = factory;
        this.beType = beType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server-side relay only; nothing to tick client-side. Match the BE type to avoid mis-ticking other blocks.
        if (level.isClientSide() || type != beType.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((TransmitterBlockEntity) be).serverTick((ServerLevel) lvl);
    }
}
