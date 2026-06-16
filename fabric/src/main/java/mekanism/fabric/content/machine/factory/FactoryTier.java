package mekanism.fabric.content.machine.factory;

/**
 * Transitional Fabric bring-up: the four factory tiers and their parallel-process counts, mirroring the NeoForge
 * {@code mekanism.common.tier.FactoryTier} (basic=3, advanced=5, elite=7, ultimate=9). The tier name is the first
 * component of the block id ({@code <tier>_<factorytype>_factory}); the process count is how many copies of the base
 * recipe the {@link FactoryBlockEntity} runs in parallel.
 */
public enum FactoryTier {
    BASIC(3),
    ADVANCED(5),
    ELITE(7),
    ULTIMATE(9);

    private final int processes;

    FactoryTier(int processes) {
        this.processes = processes;
    }

    /** Number of parallel processes (= input slots, = main output slots) for this tier. */
    public int getProcesses() {
        return processes;
    }

    /** The block-id component (e.g. {@code basic}); lower-cased enum name. */
    public String getName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
