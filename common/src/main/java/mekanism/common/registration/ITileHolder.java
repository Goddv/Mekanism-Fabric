package mekanism.common.registration;

import java.util.function.Supplier;
import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral handle to a registered {@link BlockEntityType}, carrying the (vanilla) client/server tickers. This is the
 * {@code :common} decoupling target for the NeoForge {@code TileEntityTypeRegistryObject}: it exposes only vanilla surface
 * ({@link Supplier#get()} for the type, {@link Holder} identity, and {@link #getTicker(boolean)} which returns the vanilla
 * {@link BlockEntityTicker}). The NeoForge registry-object {@code implements} this for free (its {@code DeferredHolder} base
 * already supplies {@link Supplier}/{@link Holder} and it already has {@code getTicker}); its loader-coupled capability
 * registration stays package-private and NeoForge-only. The Fabric tile holder implements this too. Hoisted code (the
 * blocktype/attribute cluster + {@code IHasTileEntity}) references {@code ITileHolder} instead of the NeoForge RO class.
 */
@NothingNullByDefault
public interface ITileHolder<BE extends BlockEntity> extends Supplier<BlockEntityType<BE>>, Holder<BlockEntityType<?>> {

    /**
     * @return the ticker for the requested side (vanilla {@link BlockEntityTicker}), or {@code null} if none.
     */
    @Nullable
    BlockEntityTicker<BE> getTicker(boolean isClient);
}
