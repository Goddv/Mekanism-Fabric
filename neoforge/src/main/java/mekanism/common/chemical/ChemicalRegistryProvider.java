package mekanism.common.chemical;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalRegistryProvider;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;

/**
 * NeoForge implementation of {@link IChemicalRegistryProvider}: exposes the {@code RegistryBuilder}-created chemical
 * registry + empty holder held in {@link MekanismAPI} to the loader-neutral chemical code in {@code :common}. Registered
 * via META-INF/services.
 */
public class ChemicalRegistryProvider implements IChemicalRegistryProvider {

    @Override
    public DefaultedRegistry<Chemical> chemicalRegistry() {
        return MekanismAPI.CHEMICAL_REGISTRY;
    }

    @Override
    public Holder<Chemical> emptyChemicalHolder() {
        return MekanismAPI.EMPTY_CHEMICAL_HOLDER;
    }
}
