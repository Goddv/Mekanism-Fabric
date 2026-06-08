package mekanism.common.chemical;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.IChemicalDataMapHelper;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.datamaps.chemical.attribute.ChemicalRadioactivity;
import mekanism.api.datamaps.chemical.attribute.IChemicalAttribute;
import mekanism.api.radiation.IRadiationManager;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * NeoForge implementation of {@link IChemicalDataMapHelper}: reads a chemical's data-map attributes (fuel /
 * radioactivity / coolants) via NeoForge {@code DataMapType}s. Keeps the NeoForge data-map + radiation coupling out of
 * the loader-neutral {@link Chemical} in {@code :common}. Registered via META-INF/services.
 */
public class ChemicalDataMapHelper implements IChemicalDataMapHelper {

    @Override
    public List<IChemicalAttribute> collectAttributes(Holder<Chemical> holder) {
        List<IChemicalAttribute> attributes = new ArrayList<>();
        track(attributes, holder, IMekanismDataMapTypes.INSTANCE.chemicalFuel());
        track(attributes, holder, IMekanismDataMapTypes.INSTANCE.chemicalRadioactivity());
        track(attributes, holder, IMekanismDataMapTypes.INSTANCE.cooledChemicalCoolant());
        track(attributes, holder, IMekanismDataMapTypes.INSTANCE.heatedChemicalCoolant());
        return attributes;
    }

    private void track(List<IChemicalAttribute> attributes, Holder<Chemical> holder, DataMapType<Chemical, ? extends IChemicalAttribute> dataMapType) {
        IChemicalAttribute attribute = holder.getData(dataMapType);
        if (attribute != null) {
            attributes.add(attribute);
        }
    }

    @Override
    public double radioactivityOf(IChemicalAttribute attribute) {
        return attribute instanceof ChemicalRadioactivity(double rads) ? rads : 0;
    }

    @Override
    public boolean isRadiationEnabled() {
        return IRadiationManager.INSTANCE.isRadiationEnabled();
    }
}
