package mekanism.common.tile.interfaces;

/**
 * Tile-driven hooks that let {@code MachineEnergyContainer} size + upgrade-adjust its energy buffer without naming the
 * NeoForge-only prefab/upgrade closure ({@code TileEntityProgressMachine}/{@code TileEntityFactory} for the buffer
 * multiplier; {@code TileComponentUpgrade} + {@code MekanismUtils.getMaxEnergy}/{@code getEnergyPerTick}, which read
 * {@code MekanismConfig} and {@code TileComponentUpgrade}→{@code ContainerType}, for the upgrade math). Keeping these
 * behind the tile lets {@code MachineEnergyContainer} live in {@code :common}. All defaults are the loader-neutral
 * no-upgrade identity (return {@code base}); NeoForge tiles override them with the real prefab/upgrade math.
 */
public interface IEnergyBufferMultiplier {

    /**
     * Effective energy-buffer multiplier (ticks) for this tile. Default {@code base} (4); progress machines widen it to
     * the recipe duration; factories multiply by their parallel-process count.
     *
     * @param base the default buffer multiplier (ticks); 4 for a plain machine.
     */
    default int getEnergyBufferMultiplier(int base) {
        return base;
    }

    /**
     * The upgrade-adjusted maximum energy for this tile, given its base maximum. Default returns {@code base} unchanged
     * (no upgrade scaling); NeoForge tiles apply {@code MekanismUtils.getMaxEnergy} when the ENERGY upgrade is supported.
     */
    default long getUpgradedMaxEnergy(long base) {
        return base;
    }

    /**
     * The upgrade-adjusted energy-per-tick for this tile, given its base per-tick usage. Default returns {@code base}
     * unchanged; NeoForge tiles apply {@code MekanismUtils.getEnergyPerTick} when an ENERGY/SPEED upgrade is supported.
     */
    default long getUpgradedEnergyPerTick(long base) {
        return base;
    }
}
