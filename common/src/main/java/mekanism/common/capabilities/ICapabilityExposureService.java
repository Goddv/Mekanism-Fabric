package mekanism.common.capabilities;

import mekanism.api.MekanismAPIBase;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-specific capability exposure for Mekanism tiles. The capability machinery (NeoForge {@code BlockCapability} /
 * {@code BlockCapabilityCache} / {@code RegisterCapabilitiesEvent} + the {@code *HandlerManager}/resolver chain; Fabric's
 * {@code BlockApiLookup}) cannot live in {@code :common}, so the loader-neutral tile routes the few cross-loader capability
 * touch-points through this service. Resolved via {@link MekanismAPIBase#getService} (same precedent as
 * {@link mekanism.common.tile.base.ITileSyncService}); every loader MUST register an impl (getService throws otherwise).
 *
 * <p>Kept deliberately tiny: per-tile manager construction stays a direct call into the NeoForge tile layer (no virtual
 * round-trip), so this service only carries the genuinely cross-loader lookups — adjacent-heat (server-world block query)
 * and the per-type capability registration hook (a no-op on NeoForge, which uses static providers + the event).
 */
@Internal
public interface ICapabilityExposureService {

    ICapabilityExposureService INSTANCE = MekanismAPIBase.getService(ICapabilityExposureService.class);

    /**
     * Looks up the heat handler of the block adjacent to {@code sourceTile} across {@code side}. NeoForge uses a per-tile,
     * per-side {@code BlockCapabilityCache}; Fabric uses {@code MekanismFabricHeat.SIDED}. Server-side only.
     *
     * @return the adjacent {@link IHeatHandler}, or {@code null} if none / not on a server level.
     */
    @Nullable
    IHeatHandler getAdjacentHeat(TileEntityUpdateable sourceTile, Direction side);

    /**
     * Per-{@link BlockEntityType} capability registration hook, called once at registration time. NeoForge is a no-op here
     * (it registers via static providers wired on the BE-type builder + {@code RegisterCapabilitiesEvent}); Fabric registers
     * the {@code BlockApiLookup} providers wrapping the tile's loader-neutral handler getters.
     */
    void registerForType(BlockEntityType<?> type);
}
