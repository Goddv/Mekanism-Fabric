package mekanism.api.chemical;

import java.util.List;
import mekanism.api.MekanismAPIBase;
import mekanism.api.datamaps.chemical.attribute.IChemicalAttribute;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-specific access to a chemical's data-map-driven attributes. The data-map system is NeoForge-only
 * ({@code DataMapType}/{@code IMekanismDataMapTypes}), so {@link Chemical#updateFromDataMap} delegates here instead of
 * referencing those types directly. The NeoForge impl reads the chemical fuel/radioactivity/coolant data maps; the
 * Fabric impl returns an empty result (no data maps yet). Resolved via {@link MekanismAPIBase#getService}.
 */
@Internal
public interface IChemicalDataMapHelper {

    IChemicalDataMapHelper INSTANCE = MekanismAPIBase.getService(IChemicalDataMapHelper.class);

    /**
     * Collects the data-map attributes attached to the given chemical holder.
     *
     * @param holder the chemical's reference holder.
     *
     * @return the attached attributes (empty if none / no data-map system on this loader).
     */
    List<IChemicalAttribute> collectAttributes(Holder<Chemical> holder);

    /**
     * {@return the radioactivity value of the given attribute if it is a radioactivity attribute, else 0} Keeps the
     * loader-specific {@code ChemicalRadioactivity} type out of {@code :common} {@link Chemical}.
     */
    double radioactivityOf(IChemicalAttribute attribute);

    /**
     * {@return whether radiation validation is currently enabled} NeoForge wires this to the radiation manager; Fabric
     * returns {@code false} until the radiation system is ported.
     */
    boolean isRadiationEnabled();
}
