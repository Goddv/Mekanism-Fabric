package mekanism.common.tile.base;

import mekanism.api.MekanismAPIBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-specific tile machine-sound playback. A {@link mekanism.common.tile.base.TileEntityMekanism}'s looping machine
 * sound is driven CLIENT-side from {@code updateSound()}; the actual playback/stop/active-query touches client-only types
 * ({@code net.minecraft.client.Minecraft}, {@code net.minecraft.client.resources.sounds.SoundInstance}) and Mekanism's
 * {@code mekanism.client.sound.SoundHandler}, none of which {@code :common} (compiled against the vanilla common
 * classpath) may name. The tile keeps ALL of its sound BOOKKEEPING (lastSoundEvent / playSoundCooldown / the active-sound
 * token + the mute/cooldown/removed checks) and routes only the three stateless client operations through this service.
 * Resolved via {@link MekanismAPIBase#getService} (same precedent as {@link ITileSyncService}).
 *
 * <p>The "active sound" is passed around as an opaque {@link Object} token (NeoForge stores the real {@code SoundInstance};
 * the tile never needs its concrete type). NeoForge delegates verbatim to {@code SoundHandler}/{@code Minecraft}; Fabric is
 * a no-op for now — a real Fabric tile-sound impl arrives with the client/GUI stage.
 */
@Internal
public interface ITileSoundService {

    ITileSoundService INSTANCE = MekanismAPIBase.getService(ITileSoundService.class);

    /**
     * Starts (client-side) the given looping tile sound and returns an opaque token representing the active sound (or
     * {@code null} if it could not start / on loaders without tile-sound). Called only from the client sound tick.
     */
    @Nullable
    Object startTileSound(SoundEvent soundEvent, SoundSource category, float volume, RandomSource random, BlockPos pos);

    /** Stops (client-side) the tile sound at {@code pos}. */
    void stopTileSound(BlockPos pos);

    /** Whether {@code activeSound} (a token previously returned by {@link #startTileSound}) is still actively playing. */
    boolean isActiveSound(@Nullable Object activeSound);

    /** Whether machine sounds are enabled (NeoForge: the client config toggle; Fabric: false until tile-sound is ported). */
    boolean machineSoundsEnabled();
}
