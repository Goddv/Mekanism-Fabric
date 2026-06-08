package mekanism.api.chemical;

import mekanism.api.MekanismAPIBase;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific access to the chemical registry. The registry itself is created differently per loader (NeoForge
 * {@code RegistryBuilder}, Fabric {@code FabricRegistryBuilder.createDefaulted}); this service hands {@code :common}
 * chemical code ({@link Chemical}/{@link ChemicalStack}) the resulting {@link DefaultedRegistry} + the empty-chemical
 * holder without referencing loader-specific registry-builder types. Resolved via {@link MekanismAPIBase#getService}.
 */
@Internal
public interface IChemicalRegistryProvider {

    IChemicalRegistryProvider INSTANCE = MekanismAPIBase.getService(IChemicalRegistryProvider.class);

    /**
     * {@return the chemical registry} Created + registered by the loader during mod setup; safe to use at runtime
     * (chemical codecs reference it lazily).
     */
    DefaultedRegistry<Chemical> chemicalRegistry();

    /**
     * {@return the holder for the empty chemical} A lazily-bound holder for {@link MekanismAPIBase#EMPTY_CHEMICAL_KEY}.
     */
    Holder<Chemical> emptyChemicalHolder();
}
