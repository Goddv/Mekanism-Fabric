package mekanism.fabric.tile;

import mekanism.common.tile.base.ITileSoundService;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric {@link ITileSoundService}: no-op for now (mirrors {@code FabricTileSyncService}'s deferral). Mekanism's looping
 * machine-sound playback relies on NeoForge's {@code SoundHandler} muffling/event machinery, which has no Fabric
 * equivalent yet; a real Fabric tile-sound implementation lands with the client/GUI stage. Until then machines simply
 * play no looping sound on Fabric (the ACTIVE blockstate still animates normally).
 */
public class FabricTileSoundService implements ITileSoundService {

    @Nullable
    @Override
    public Object startTileSound(SoundEvent soundEvent, SoundSource category, float volume, RandomSource random, BlockPos pos) {
        return null;
    }

    @Override
    public void stopTileSound(BlockPos pos) {
        //Deferred: Fabric tile-sound playback is not yet ported.
    }

    @Override
    public boolean isActiveSound(@Nullable Object activeSound) {
        return false;
    }
}
