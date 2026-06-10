package mekanism.common.tile.base;

import mekanism.client.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge {@link ITileSoundService}: the exact client sound operations relocated verbatim from
 * {@code TileEntityMekanism.updateSound()} — {@code SoundHandler.startTileSound}/{@code stopTileSound} and the
 * {@code Minecraft} sound-manager active query. These run only on the client (the tile only calls them from its client
 * sound tick); the class references client-only types, which are present on the NeoForge (merged) classpath even on a
 * dedicated server, so it loads fine everywhere and behaves identically to the pre-seam inline code.
 */
public class NeoTileSoundService implements ITileSoundService {

    @Nullable
    @Override
    public Object startTileSound(SoundEvent soundEvent, SoundSource category, float volume, RandomSource random, BlockPos pos) {
        return SoundHandler.startTileSound(soundEvent, category, volume, random, pos);
    }

    @Override
    public void stopTileSound(BlockPos pos) {
        SoundHandler.stopTileSound(pos);
    }

    @Override
    public boolean isActiveSound(@Nullable Object activeSound) {
        return activeSound instanceof SoundInstance soundInstance && Minecraft.getInstance().getSoundManager().isActive(soundInstance);
    }
}
