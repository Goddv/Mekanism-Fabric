package mekanism.common.tile.interfaces;

/**
 * Implemented by tiles to size their machine energy buffer (a multiple of the per-tick usage) when speed/process upgrades
 * change throughput. Lets {@code MachineEnergyContainer.updateMaxEnergy()} compute the buffer without {@code instanceof}
 * downcasts to the NeoForge-only {@code TileEntityProgressMachine}/{@code TileEntityFactory} prefab classes (which would
 * otherwise drag the prefab/FactoryType closure into {@code :common}). The base tile returns {@code base} unchanged (4
 * ticks); progress machines widen it to the recipe duration; factories multiply by their parallel-process count.
 */
public interface IEnergyBufferMultiplier {

    /**
     * @param base the default buffer multiplier (ticks); 4 for a plain machine.
     *
     * @return the effective buffer multiplier for this tile. Default returns {@code base} unchanged.
     */
    default int getEnergyBufferMultiplier(int base) {
        return base;
    }
}
