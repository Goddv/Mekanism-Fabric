package mekanism.fabric.chemical;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalRegistryProvider;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;

/**
 * Fabric implementation of {@link IChemicalRegistryProvider} — delegates to the {@link FabricChemicalRegistry} created
 * during mod init. Registered via META-INF/services.
 */
public class FabricChemicalRegistryProvider implements IChemicalRegistryProvider {

    @Override
    public DefaultedRegistry<Chemical> chemicalRegistry() {
        return FabricChemicalRegistry.registry();
    }

    @Override
    public Holder<Chemical> emptyChemicalHolder() {
        return FabricChemicalRegistry.empty();
    }
}
