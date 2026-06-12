package mekanism.common.capabilities.energy;

import java.util.Objects;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class MachineEnergyContainer<TILE extends TileEntityMekanism> extends BasicEnergyContainer {

    public static <TILE extends TileEntityMekanism> MachineEnergyContainer<TILE> input(TILE tile, @Nullable IContentsListener listener) {
        AttributeEnergy electricBlock = validateBlock(tile);
        return new MachineEnergyContainer<>(electricBlock.getUsage() * 4, electricBlock.getUsage(), notExternal, ConstantPredicates.alwaysTrue(), tile, listener);
    }

    public static <TILE extends TileEntityMekanism> MachineEnergyContainer<TILE> internal(TILE tile, @Nullable IContentsListener listener) {
        AttributeEnergy electricBlock = validateBlock(tile);
        return new MachineEnergyContainer<>(electricBlock.getUsage() * 4, electricBlock.getUsage(), internalOnly, internalOnly, tile, listener);
    }

    public static AttributeEnergy validateBlock(TileEntityMekanism tile) {
        Objects.requireNonNull(tile, "Tile cannot be null");
        AttributeEnergy attributeEnergy = Attribute.get(tile.getBlockHolder(), AttributeEnergy.class);
        if (attributeEnergy == null) {
            throw new IllegalArgumentException("Block provider must be an electric block");
        }
        return attributeEnergy;
    }

    protected final TILE tile;
    private final long baseEnergyPerTick;
    private long currentMaxEnergy;
    protected long currentEnergyPerTick;

    protected MachineEnergyContainer(long maxEnergy, long energyPerTick, Predicate<@NotNull AutomationType> canExtract,
          Predicate<@NotNull AutomationType> canInsert, TILE tile, @Nullable IContentsListener listener) {
        super(maxEnergy, canExtract, canInsert, listener);
        this.baseEnergyPerTick = energyPerTick;
        this.tile = tile;
        currentMaxEnergy = getBaseMaxEnergy();
        currentEnergyPerTick = baseEnergyPerTick;
    }

    @Override
    protected long clampEnergy(long energy) {
        return energy;//machines shouldn't clamp as buffer is dynamic
    }

    public boolean adjustableRates() {
        return false;
    }

    @Override
    public long getMaxEnergy() {
        return Math.max(currentMaxEnergy, getEnergy());
    }

    public long getBaseMaxEnergy() {
        return super.getMaxEnergy();
    }

    public void setMaxEnergy(long maxEnergy) {
        this.currentMaxEnergy = maxEnergy;
        //Clamp the energy
        setEnergy(getEnergy());
    }

    public long getEnergyPerTick() {
        return currentEnergyPerTick;
    }

    public long getBaseEnergyPerTick() {
        return baseEnergyPerTick;
    }

    public void setEnergyPerTick(long energyPerTick) {
        this.currentEnergyPerTick = energyPerTick;
    }

    public void updateMaxEnergy() {
        if (tile.supportsUpgrade(Upgrade.SPEED)) {
            //4 ticks by default; progress machines widen to the recipe duration, factories multiply by their process count
            //(see IEnergyBufferMultiplier overrides — replaces the former instanceof downcasts to the prefab tile classes).
            int bufferMultipler = tile.getEnergyBufferMultiplier(4);
            setMaxEnergy(getEnergyPerTick() * bufferMultipler);
        } else if (tile.supportsUpgrade(Upgrade.ENERGY)) {
            //tile-driven upgrade math (see IEnergyBufferMultiplier) — keeps the MekanismUtils/MekanismConfig/
            //TileComponentUpgrade(->ContainerType) closure off the :common energy container.
            setMaxEnergy(tile.getUpgradedMaxEnergy(getBaseMaxEnergy()));
        }
    }

    public void updateEnergyPerTick() {
        //Verbatim: getUpgradedEnergyPerTick returns base unchanged when upgrades/ENERGY/SPEED are unsupported, which equals
        //the per-tick already in effect, so the unconditional set reproduces the former conditional set exactly.
        setEnergyPerTick(tile.getUpgradedEnergyPerTick(getBaseEnergyPerTick()));
    }
}