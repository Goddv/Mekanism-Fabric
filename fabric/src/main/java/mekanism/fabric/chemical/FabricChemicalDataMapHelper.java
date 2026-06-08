package mekanism.fabric.chemical;

import java.util.List;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalDataMapHelper;
import mekanism.api.datamaps.chemical.attribute.IChemicalAttribute;
import net.minecraft.core.Holder;

/**
 * Fabric implementation of {@link IChemicalDataMapHelper}. The NeoForge data-map system (chemical fuel/radioactivity/
 * coolant attributes) isn't ported yet, so this is a no-op — chemicals have no data-map attributes on Fabric for now.
 * Registered via META-INF/services.
 */
public class FabricChemicalDataMapHelper implements IChemicalDataMapHelper {

    @Override
    public List<IChemicalAttribute> collectAttributes(Holder<Chemical> holder) {
        return List.of();
    }

    @Override
    public double radioactivityOf(IChemicalAttribute attribute) {
        return 0;
    }

    @Override
    public boolean isRadiationEnabled() {
        return false;
    }
}
