package mekanism.fabric.content.transmitter;

import mekanism.fabric.chemical.MekanismFabricChemical;
import mekanism.fabric.energy.MekanismFabricEnergy;
import mekanism.fabric.fluid.MekanismFabricFluid;
import mekanism.fabric.heat.MekanismFabricHeat;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;

/**
 * Identifies which of the five transmitters a shared {@link TransmitterBlock} hosts, supplying the two things the
 * connected-rendering logic needs per type:
 *
 * <ol>
 *     <li>the {@link BlockApiLookup} that marks a <em>non-transmitter</em> neighbour as a connectable acceptor/provider
 *     (energy for the cable, chemical for the tube, heat for the conductor, item for the transporter, fluid for the
 *     pipe), and</li>
 *     <li>the model/texture set id (the bundled {@code basic_*} asset prefix), so each type carries its own multipart
 *     blockstate + center/arm models.</li>
 * </ol>
 *
 * <p>This is the vanilla fence-style approach: a side connects to a same-kind transmitter neighbour OR to any neighbour
 * that exposes this type's capability on the touching face. The 6 per-side {@code BooleanProperty}s on
 * {@link TransmitterBlock} are computed server-side from these checks and synced to the client via the blockstate, which
 * a multipart model turns into a center core + per-side arm.
 */
public enum TransmitterRenderType {
    UNIVERSAL_CABLE("basic_universal_cable", MekanismFabricEnergy.SIDED),
    PRESSURIZED_TUBE("basic_pressurized_tube", MekanismFabricChemical.SIDED),
    THERMODYNAMIC_CONDUCTOR("basic_thermodynamic_conductor", MekanismFabricHeat.SIDED),
    LOGISTICAL_TRANSPORTER("basic_logistical_transporter", ItemStorage.SIDED),
    MECHANICAL_PIPE("basic_mechanical_pipe", MekanismFabricFluid.SIDED);

    private final String assetName;
    private final BlockApiLookup<?, Direction> lookup;

    TransmitterRenderType(String assetName, BlockApiLookup<?, Direction> lookup) {
        this.assetName = assetName;
        this.lookup = lookup;
    }

    /** The bundled asset prefix (e.g. {@code basic_universal_cable}) shared by this type's blockstate + models. */
    public String assetName() {
        return assetName;
    }

    /**
     * The capability lookup that identifies a connectable <em>non-transmitter</em> neighbour for this type. All five
     * Mekanism capabilities map to a {@code BlockApiLookup<?, Direction>}, so a single field covers every type.
     */
    public BlockApiLookup<?, Direction> lookup() {
        return lookup;
    }
}
