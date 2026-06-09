package mekanism.common.util;

import mekanism.common.content.blocktype.FactoryType;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.resource.ore.OreType;
import mekanism.common.tier.BinTier;
import mekanism.common.tier.CableTier;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.tier.ConductorTier;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tier.FluidTankTier;
import mekanism.common.tier.InductionCellTier;
import mekanism.common.tier.InductionProviderTier;
import mekanism.common.tier.PipeTier;
import mekanism.common.tier.TransporterTier;
import mekanism.common.tier.TubeTier;
import mekanism.common.tile.qio.TileEntityQIODriveArray.DriveStatus;
import mekanism.common.util.UnitDisplayUtils.MeasurementUnit;

public class EnumUtils extends EnumUtilsBase {

    private EnumUtils() {
    }

    /**
     * Cached value of {@link MeasurementUnit#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final MeasurementUnit[] MEASUREMENT_UNITS = MeasurementUnit.values();

    /**
     * Cached value of {@link CableTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final CableTier[] CABLE_TIERS = CableTier.values();

    /**
     * Cached value of {@link TransporterTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final TransporterTier[] TRANSPORTER_TIERS = TransporterTier.values();

    /**
     * Cached value of {@link ConductorTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final ConductorTier[] CONDUCTOR_TIERS = ConductorTier.values();

    /**
     * Cached value of {@link TubeTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final TubeTier[] TUBE_TIERS = TubeTier.values();

    /**
     * Cached value of {@link PipeTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final PipeTier[] PIPE_TIERS = PipeTier.values();

    /**
     * Cached value of {@link ChemicalTankTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final ChemicalTankTier[] CHEMICAL_TANK_TIERS = ChemicalTankTier.values();

    /**
     * Cached value of {@link FluidTankTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final FluidTankTier[] FLUID_TANK_TIERS = FluidTankTier.values();

    /**
     * Cached value of {@link BinTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final BinTier[] BIN_TIERS = BinTier.values();

    /**
     * Cached value of {@link EnergyCubeTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final EnergyCubeTier[] ENERGY_CUBE_TIERS = EnergyCubeTier.values();

    /**
     * Cached value of {@link InductionCellTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final InductionCellTier[] INDUCTION_CELL_TIERS = InductionCellTier.values();

    /**
     * Cached value of {@link InductionProviderTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final InductionProviderTier[] INDUCTION_PROVIDER_TIERS = InductionProviderTier.values();

    /**
     * Cached value of {@link FactoryType#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final FactoryType[] FACTORY_TYPES = FactoryType.values();

    /**
     * Cached value of {@link OreType#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final OreType[] ORE_TYPES = OreType.values();

    /**
     * Cached value of {@link PrimaryResource#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final PrimaryResource[] PRIMARY_RESOURCES = PrimaryResource.values();

    /**
     * Cached value of {@link ResourceType#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final ResourceType[] RESOURCE_TYPES = ResourceType.values();

    /**
     * Cached value of {@link DriveStatus#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final DriveStatus[] DRIVE_STATUSES = DriveStatus.values();
}
