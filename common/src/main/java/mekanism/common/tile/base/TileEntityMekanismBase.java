package mekanism.common.tile.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.MekanismAPIBase;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.attachments.containers.chemical.AttachedChemicals;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.attachments.containers.item.AttachedItems;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeStateActive;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.capabilities.ICapabilityExposureService;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.capabilities.heat.ITileHeatHandler;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.heat.IHeatCapacitorHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.LastEnergyTracker;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.interfaces.IComparatorSupport;
import mekanism.common.tile.interfaces.IEnergyBufferMultiplier;
import mekanism.common.tile.interfaces.ITierUpgradable;
import mekanism.common.tile.interfaces.ITileActive;
import mekanism.common.tile.interfaces.ITileDirectional;
import mekanism.common.tile.interfaces.ITileRedstone;
import mekanism.common.tile.interfaces.ITileSound;
import mekanism.common.upgrade.IUpgradeData;
import mekanism.common.util.MekanismUtilsBase;
import mekanism.common.util.NBTUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral core of {@link TileEntityMekanism}. Holds the tile logic that names no {@code net.neoforged}/
 * {@code net.fabricmc} and no {@code net.minecraft.client} types, so it can live in {@code :common}.
 *
 * <p>Hierarchy: {@code TileEntityUpdateable} &lt;- {@code TileEntityMekanismBase} (this) &lt;- {@code CapabilityTileEntity}
 * (NeoForge) &lt;- {@code TileEntityMekanism} (NeoForge leaf, FQN preserved). The 5 capability handler managers live on
 * {@code CapabilityTileEntity} and are reached here only through the 10 seam methods ({@code hasInventory}/{@code canHandle*}
 * + the {@code getXContainers} getters), which default to false/empty here and are overridden on {@code CapabilityTileEntity}.
 * The {@code ContainerType.TYPES} save/load iteration, the dynamic container sync, the computer integration, the
 * frequency/security/upgrade components, the wrench/gui cluster and the client sound playback all stay on the NeoForge leaf;
 * this base reaches the leaf's pieces only through protected no-op hooks ({@code saveAdditionalContainers},
 * {@code loadAdditionalContainers}, {@code applyImplicitComponentsLeaf}, {@code collectImplicitComponentsLeaf},
 * {@code addRemapEntriesLeaf}, {@code onComponentAdded}, {@code isFullyMuffled}) so NBT/data-component byte ordering is
 * preserved exactly.
 */
//TODO: We need to move the "supports" methods into the source interfaces so that we make sure they get checked before being used
public abstract class TileEntityMekanismBase extends TileEntityUpdateable implements ITileDirectional, IConfigCardAccess, ITileActive, ITileSound,
      ITileRedstone, ITierUpgradable, IComparatorSupport, IMekanismFluidHandler, IMekanismStrictEnergyHandler, ITileHeatHandler, IMekanismChemicalHandler,
      IEnergyBufferMultiplier, Nameable {

    /**
     * The players currently using this block.
     */
    public final Set<Player> playersUsing = new HashSet<>();

    /**
     * A timer used to send packets to clients.
     */
    public int ticker;
    protected final List<ITileComponent> components = new ArrayList<>();

    protected boolean supportsComparator;
    protected boolean supportsComputers;
    protected boolean supportsUpgrades;
    protected boolean supportsRedstone;
    protected boolean canBeUpgraded;
    protected boolean isDirectional;
    protected boolean isActivatable;
    protected AttributeStateActive activeAttribute;
    protected boolean hasBounding;
    protected boolean hasSecurity;
    protected boolean hasSound;
    protected boolean hasGui;
    protected boolean hasChunkloader;
    protected boolean nameable;

    @Nullable
    protected Component customName;

    protected boolean syncMasterToBounding;

    //Methods for implementing ITileDirectional
    @Nullable
    protected Direction cachedDirection;

    //TODO: Re-evaluate if we should have this be null when we are not a directional tile?
    public final Supplier<Direction> facingSupplier = this::getDirection;
    //End variables ITileRedstone

    //Variables for handling ITileRedstone
    //TODO: Move these to private variables?
    protected boolean redstone = false;
    protected boolean redstoneLastTick = false;
    /**
     * This machine's current RedstoneControl type.
     */
    protected RedstoneControl controlType = RedstoneControl.DISABLED;
    //End variables ITileRedstone

    //Variables for handling IComparatorSupport
    protected int currentRedstoneLevel;
    protected boolean updateComparators;
    //End variables IComparatorSupport

    //Note: the 5 capability handler managers live on the NeoForge CapabilityTileEntity superclass (protected) and are
    //reached here only through the 10 seam methods (canHandle*/getXContainers), which default to false/emptyList here.

    //Variables for handling IMekanismChemicalHandler
    protected float radiationScale;
    //End variables IMekanismChemicalHandler

    //Variables for handling IMekanismStrictEnergyHandler
    protected final LastEnergyTracker lastEnergyTracker = new LastEnergyTracker();
    //End variables IMekanismStrictEnergyHandler

    //Variables for handling IMekanismHeatHandler
    //Assigned in the NeoForge leaf ctor (named after CachedAmbientTemperature is built); non-final for the base/leaf split.
    @Nullable
    protected CachedAmbientTemperature ambientTemperature;
    //End variables for IMekanismHeatHandler

    //Variables for handling ITileActive
    protected boolean currentActive;
    protected int updateDelay;
    //Loader-neutral default; the NeoForge leaf ctor reassigns this to MekanismConfig.general.blockDeactivationDelay.
    protected IntSupplier delaySupplier = () -> 0;
    //End variables ITileActive

    //Variables for handling ITileSound
    //Assigned in the NeoForge leaf ctor (named after AttributeSound is resolved); non-final for the base/leaf split.
    @Nullable
    protected Supplier<SoundEvent> soundEvent;
    @Nullable
    protected SoundEvent lastSoundEvent;

    /**
     * Only used on the client
     */
    /** Opaque token for the active machine sound (a {@code SoundInstance} on NeoForge); driven via {@link ITileSoundService}. */
    private Object activeSound;
    private int playSoundCooldown = 0;
    //End variables ITileSound

    protected TileEntityMekanismBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * @return the block this tile was registered for. Implemented on the NeoForge leaf (it holds the {@code blockProvider}).
     */
    public abstract Holder<Block> getBlockHolder();

    /**
     * Sets variables up, called immediately after the supported-types setup but before any things start being created.
     *
     * @implNote This method should be used for setting any variables that would normally be set directly, except that gets run too late to set things up properly in our
     * constructor.
     */
    protected void presetVariables() {
    }

    //Note: not @Override here — supportsUpgrades() is declared by the NeoForge-only IUpgradeTile; the leaf (which
    //implements it) inherits this. Same reasoning applies to getInventorySlots() below (IMekanismInventory).
    public final boolean supportsUpgrades() {
        return supportsUpgrades;
    }

    @Override
    public final boolean supportsComparator() {
        return supportsComparator;
    }

    @Override
    public final boolean canBeUpgraded() {
        return canBeUpgraded;
    }

    @Override
    public final boolean isDirectional() {
        return isDirectional;
    }

    @Override
    public final boolean supportsRedstone() {
        return supportsRedstone;
    }

    @Override
    public final boolean hasSound() {
        return hasSound;
    }

    public final boolean hasGui() {
        return hasGui;
    }

    @Override
    public final boolean isActivatable() {
        return isActivatable;
    }

    //Capability seam: default false/empty here; CapabilityTileEntity overrides these to read its handler managers.
    public boolean hasInventory() {
        return false;
    }

    public boolean canHandleChemicals() {
        return false;
    }

    public boolean canHandleFluid() {
        return false;
    }

    public boolean canHandleEnergy() {
        return false;
    }

    public boolean canHandleHeat() {
        return false;
    }

    public void addComponent(ITileComponent component) {
        components.add(component);
        onComponentAdded(component);
    }

    /**
     * Hook invoked when a component is added. The NeoForge {@code CapabilityTileEntity} overrides this to register a
     * {@code TileComponentConfig} as a capability config component (the only loader-coupled branch of the former
     * {@code addComponent}).
     */
    protected void onComponentAdded(ITileComponent component) {
    }

    public List<ITileComponent> getComponents() {
        return components;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return isNameable() ? customName : null;
    }

    public void setCustomName(@Nullable Component name) {
        if (isNameable()) {
            this.customName = name;
        }
    }

    /**
     * This should return false if naming it would be pointless, in order to save on NBT data on both the tile entity and the block item.
     *
     * @return if the tile entity can be named
     */
    public boolean isNameable() {
        return nameable;
    }

    @NotNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Component getName() {
        //Base-level name (block name / custom name). The NeoForge machine leaf overrides this (and getDisplayName) with the
        //container-description variant; non-machine subclasses (e.g. TileEntityTransmitter) use this block-name default.
        return hasCustomName() ? getCustomName() : TextComponentUtil.build(getBlockHolder());
    }

    @Override
    public void markDirtyComparator() {
        //Only mark our comparators as needing update if we support comparators
        if (supportsComparator()) {
            updateComparators = true;
        }
    }

    protected void notifyComparatorChange() {
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public void encodeExtraContainerData(RegistryFriendlyByteBuf buffer) {
    }

    public void open(Player player) {
        playersUsing.add(player);
    }

    public void close(Player player) {
        playersUsing.remove(player);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (ITileComponent component : components) {
            component.invalidate();
        }
        if (isRemote() && hasSound()) {
            updateSound();
        }
    }

    @Override
    public void preRemoveSideEffects(@NotNull BlockPos pos, @NotNull BlockState state) {
        super.preRemoveSideEffects(pos, state);
        for (ITileComponent component : components) {
            component.removed();
        }
        if (!isRemote() && IRadiationManager.INSTANCE.isRadiationEnabled() && shouldDumpRadiation()) {
            //If we are on a server and radiation is enabled dump all gas tanks with radioactive materials
            // Note: we handle clearing radioactive contents later in drop calculation due to when things are written to NBT
            IRadiationManager.INSTANCE.dumpRadiation(getWorldNN(), worldPosition, getChemicalTanks(null), false);
        }
    }

    /**
     * Update call for machines. Use instead of updateEntity -- it's called every tick on the client side.
     */
    protected void onUpdateClient() {
    }

    /**
     * Update call for machines. Use instead of updateEntity -- it's called every tick on the server side.
     *
     * @return {@code true} if an update packet needs to be sent to the client.
     */
    protected boolean onUpdateServer() {
        return false;
    }

    public void resyncMasterToBounding() {
        if (hasBounding) {
            syncMasterToBounding = true;
        }
    }

    @Override
    @Deprecated
    public void setBlockState(@NotNull BlockState newState) {
        super.setBlockState(newState);
        if (isDirectional()) {
            //Note: We get the new cached direction from the state as hopefully the state is not changing super often
            // and that way we can properly clear things that only should happen when the direction actually changes and not when we go from active to inactive
            Direction newDirection = Attribute.getFacing(newState);
            if (cachedDirection != newDirection) {
                invalidateDirectionCaches(newDirection);
            }
        }
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        redstone = input.getBooleanOr(SerializationConstants.REDSTONE, redstone);
        for (ITileComponent component : components) {
            component.read(input);
        }
        if (supportsUpgrades()) {
            recalculateUpgrades(Upgrade.SPEED);//force buffer to update
        }
        readSustainedData(input);
        loadAdditionalContainers(input);
        if (isActivatable()) {
            currentActive = input.getBooleanOr(SerializationConstants.ACTIVE_STATE, currentActive);
            updateDelay = input.getIntOr(SerializationConstants.UPDATE_DELAY, updateDelay);
        }
        if (supportsComparator()) {
            currentRedstoneLevel = input.getIntOr(SerializationConstants.CURRENT_REDSTONE, currentRedstoneLevel);
        }
        if (isNameable()) {
            customName = parseCustomNameSafe(input, SerializationConstants.CUSTOM_NAME);
        }
    }

    /**
     * Hook for the {@code ContainerType.TYPES} read loop (kept on the NeoForge leaf). Inserted at the exact position the
     * loop occupied in the former monolithic {@code loadAdditional} so NBT read order is unchanged.
     */
    protected void loadAdditionalContainers(@NotNull ValueInput input) {
    }

    /**
     * Runs ONLY {@link TileEntityUpdateable#loadAdditional} (the lightweight load), NOT this full tile load. The NeoForge
     * client update-tag receive path ({@code handleUpdateTag} on the leaf) historically reached the grand-super load via
     * {@code super.loadAdditional} back when this logic sat directly on the leaf; now that the full load lives here, the
     * leaf routes through this helper so its behavior is byte-identical (it must not re-run the full machine load).
     */
    protected final void loadAdditionalFromUpdateTag(@NotNull ValueInput input) {
        super.loadAdditional(input);
    }

    @Override
    public void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(SerializationConstants.REDSTONE, redstone);
        for (ITileComponent component : components) {
            component.write(output);
        }
        writeSustainedData(output);

        saveAdditionalContainers(output);

        if (isActivatable()) {
            output.putBoolean(SerializationConstants.ACTIVE_STATE, currentActive);
            output.putInt(SerializationConstants.UPDATE_DELAY, updateDelay);
        }
        if (supportsComparator()) {
            output.putInt(SerializationConstants.CURRENT_REDSTONE, currentRedstoneLevel);
        }

        // Save the custom name, if the tile can be named. storeNullable will handle ensuring it doesn't write it when there is no name
        if (isNameable()) {
            output.storeNullable(SerializationConstants.CUSTOM_NAME, ComponentSerialization.CODEC, this.customName);
        }
    }

    /**
     * Hook for the {@code ContainerType.TYPES} save loop (kept on the NeoForge leaf). Inserted at the exact position the
     * loop occupied in the former monolithic {@code saveAdditional} so NBT write order is unchanged.
     */
    protected void saveAdditionalContainers(@NotNull ValueOutput output) {
    }

    public void writeSustainedData(@NotNull ValueOutput output) {
        if (supportsRedstone()) {
            NBTUtils.writeEnum(output, SerializationConstants.CONTROL_TYPE, controlType);
        }
    }

    public void readSustainedData(@NotNull ValueInput input) {
        if (supportsRedstone()) {
            NBTUtils.setEnumIfPresent(input, SerializationConstants.CONTROL_TYPE, RedstoneControl.BY_ID, type -> controlType = supportedOrNextType(type));
        }
    }

    //TODO: Re-evaluate the entirety of this method and see what parts potentially should not be getting called at all when on the client side.
    // We previously had issues in readSustainedData regarding frequencies when on the client side so that is why the frequency data has this check
    // but there is a good chance a lot of this stuff has no real reason to need to be set on the client side at all
    @Override
    protected void applyImplicitComponents(@NotNull DataComponentGetter input) {
        super.applyImplicitComponents(input);
        // Check if the stack has a custom name, and if the tile supports naming, name it
        if (isNameable()) {
            setCustomName(input.get(DataComponents.CUSTOM_NAME));
        }

        for (ITileComponent component : components) {
            component.applyImplicitComponents(input);
        }
        if (supportsUpgrades()) {
            //Recalculate upgrades before setting types so that we don't clamp the stored energy
            for (Upgrade upgrade : getSupportedUpgrade()) {
                recalculateUpgrades(upgrade);
            }
        }

        applyImplicitComponentsLeaf(input);
    }

    /**
     * Hook for the {@code ContainerType.TYPES} copy-to-tile + filter + redstone-control application (kept on the NeoForge
     * leaf). Invoked at the exact tail position of the former monolithic {@code applyImplicitComponents}.
     */
    protected void applyImplicitComponentsLeaf(@NotNull DataComponentGetter input) {
    }

    @Override
    public List<DataComponentType<?>> getRemapEntries() {
        List<DataComponentType<?>> remapEntries = super.getRemapEntries();
        for (ITileComponent component : components) {
            component.addRemapEntries(remapEntries);
        }
        addRemapEntriesLeaf(remapEntries);
        return remapEntries;
    }

    /**
     * Hook for the {@code ContainerType.TYPES} + filter remap entries (kept on the NeoForge leaf). Mutates the SAME list
     * the base built, at the exact append position of the former monolithic {@code getRemapEntries}.
     */
    protected void addRemapEntriesLeaf(List<DataComponentType<?>> remapEntries) {
    }

    @Override
    @Deprecated
    public void removeComponentsFromTag(@NotNull ValueOutput output) {
        super.removeComponentsFromTag(output);
        for (ITileComponent component : components) {
            output.discard(component.getComponentKey());
        }
        output.discard(SerializationConstants.REDSTONE);
        if (supportsComparator()) {
            output.discard(SerializationConstants.CURRENT_REDSTONE);
        }
        if (isActivatable()) {
            output.discard(SerializationConstants.ACTIVE_STATE);
            output.discard(SerializationConstants.UPDATE_DELAY);
        }
        if (supportsRedstone()) {
            output.discard(SerializationConstants.CONTROL_TYPE);
        }
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        //TODO: Some of the data doesn't get properly "picked", because there are cases such as before opening the GUI where
        // the server doesn't bother syncing the data to the client. For example with what frequencies there are
        for (ITileComponent component : components) {
            component.collectImplicitComponents(builder);
        }
        collectImplicitComponentsLeaf(builder);
    }

    /**
     * Hook for the {@code ContainerType.TYPES} copy-from-tile + filter + redstone-control collection (kept on the NeoForge
     * leaf). Invoked at the exact tail position of the former monolithic {@code collectImplicitComponents}.
     */
    protected void collectImplicitComponentsLeaf(@NotNull DataComponentMap.Builder builder) {
    }

    @Override
    public void writeReducedUpdatedTag(@NotNull ValueOutput output) {
        super.writeReducedUpdatedTag(output);
        for (ITileComponent component : components) {
            //TODO - 26.1: Do we want to be passing a child?
            component.addToUpdateTag(output);
        }
        output.putFloat(SerializationConstants.RADIATION, radiationScale);
    }

    public void onNeighborChange(BlockPos neighborPos) {
        if (!isRemote()) {
            updatePower();
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        updatePower();
        if (getClientActive()) {
            currentActive = true;
        }
    }

    //Methods pertaining to IUpgradeableTile
    public void parseUpgradeData(@NotNull IUpgradeData data, Provider provider) {
        MekanismAPIBase.logger.warn("Unhandled upgrade data.", new Throwable());
    }
    //End methods IUpgradeableTile

    protected void invalidateDirectionCaches(Direction newDirection) {
        cachedDirection = newDirection;
    }

    //ITileDirectional seam: base-level (non-directional) defaults so non-machine subclasses (TileEntityTransmitter) are
    //concrete; the NeoForge machine leaf overrides both with the real directional logic (getDirection is @ComputerMethod-pinned).
    @NotNull
    @Override
    public Direction getDirection() {
        return Direction.NORTH;
    }

    @Override
    public void setFacing(@NotNull Direction direction) {
    }

    //Methods for implementing ITileRedstone
    //getControlType reads the base controlType field; the machine leaf overrides it solely to add the @ComputerMethod binding.
    @Override
    public RedstoneControl getControlType() {
        return controlType;
    }

    @Override
    public void setControlType(@NotNull RedstoneControl type) {
        if (supportsRedstone()) {
            type = supportedOrNextType(type);
            if (type != controlType) {
                controlType = type;
                markForSave();
            }
        }
    }

    private RedstoneControl supportedOrNextType(@NotNull RedstoneControl type) {
        Objects.requireNonNull(type);
        if (!supportsMode(type)) {
            //Validate we support the mode that is being set
            type = type.getNext(this::supportsMode);
        }
        return type;
    }

    @Override
    public boolean isPowered() {
        return supportsRedstone() && redstone;
    }

    @Override
    public final boolean wasPowered() {
        return supportsRedstone() && redstoneLastTick;
    }

    public final void updatePower() {
        if (supportsRedstone()) {
            boolean power = level.hasNeighborSignal(getBlockPos());
            if (redstone != power) {
                redstone = power;
                onPowerChange();
            }
        }
    }

    public final boolean isRedstoneActivated() {
        return !supportsRedstone() ||
               switch (controlType) {
                   case DISABLED -> true;
                   case HIGH -> isPowered();
                   case LOW -> !isPowered();
                   case PULSE -> isPowered() && !redstoneLastTick;
               };
    }

    public boolean canFunction() {
        return isRedstoneActivated();
    }
    //End methods ITileRedstone

    //Methods for implementing IComparatorSupport
    @Override
    public int getRedstoneLevel() {
        if (supportsComparator()) {
            if (hasInventory()) {
                return MekanismUtilsBase.redstoneLevelFromContents(getInventorySlots(null));
            }
            //TODO: Do we want some other defaults as well?
        }
        return Redstone.SIGNAL_NONE;
    }

    //getCurrentRedstoneLevel reads the base field; the machine leaf overrides it solely to add the @ComputerMethod binding.
    @Override
    public int getCurrentRedstoneLevel() {
        return currentRedstoneLevel;
    }
    //End methods IComparatorSupport

    //Methods for implementing ITileUpgradable
    @NotNull
    public Set<Upgrade> getSupportedUpgrade() {
        if (supportsUpgrades()) {
            return Attribute.getOrThrow(getBlockHolder(), AttributeUpgradeSupport.class).supportedUpgrades();
        }
        return Collections.emptySet();
    }

    /**
     * Whether the given upgrade is supported. Default {@code false} here; the NeoForge leaf overrides this with the real
     * {@code supportsUpgrades() && getComponent().supports(upgradeType)} body (the former {@code IUpgradeTile} default).
     */
    public boolean supportsUpgrade(Upgrade upgradeType) {
        return false;
    }

    public void recalculateUpgrades(Upgrade upgrade) {
        if (upgrade == Upgrade.SPEED) {
            for (IEnergyContainer energyContainer : getEnergyContainers(null)) {
                if (energyContainer instanceof MachineEnergyContainer<?> machineEnergy) {
                    machineEnergy.updateEnergyPerTick();
                    machineEnergy.updateMaxEnergy();
                }
            }
        } else if (upgrade == Upgrade.ENERGY) {
            for (IEnergyContainer energyContainer : getEnergyContainers(null)) {
                if (energyContainer instanceof MachineEnergyContainer<?> machineEnergy) {
                    machineEnergy.updateEnergyPerTick();
                    machineEnergy.updateMaxEnergy();
                }
            }
        }
    }
    //End methods ITileUpgradable

    //Methods for implementing ITileContainer
    @Nullable
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        return null;
    }

    @NotNull
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return Collections.emptyList();
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    public void applyInventorySlots(DataComponentGetter input, List<IInventorySlot> slots, AttachedItems attachedItems) {
        List<ItemStack> stacks = attachedItems.containers();
        int size = stacks.size();
        if (size == slots.size()) {
            for (int i = 0; i < size; i++) {
                ItemStack stack = stacks.get(i).copy();
                IInventorySlot slot = slots.get(i);
                if (slot instanceof BasicInventorySlot basicSlot) {
                    basicSlot.setStackUnchecked(stack);
                } else {
                    slot.setStack(stack);
                }
            }
        }
    }

    @Nullable
    public AttachedItems collectInventorySlots(DataComponentMap.Builder builder, List<IInventorySlot> slots) {
        boolean hasNonEmpty = false;
        List<ItemStack> stacks = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            stacks.add(slot.getStack().copy());
            if (!slot.isEmpty()) {
                hasNonEmpty = true;
            }
        }
        return hasNonEmpty ? new AttachedItems(stacks) : null;
    }
    //End methods ITileContainer

    //Methods for implementing IMekanismChemicalHandler
    public boolean shouldDumpRadiation() {
        return canHandleChemicals();
    }

    @Nullable
    public IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        return null;
    }

    @NotNull
    @Override
    public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
        return Collections.emptyList();
    }

    public void applyChemicalTanks(DataComponentGetter input, List<IChemicalTank> tanks, AttachedChemicals attachedChemicals) {
        List<ChemicalStack> stacks = attachedChemicals.containers();
        int size = stacks.size();
        if (size == tanks.size()) {
            for (int i = 0; i < size; i++) {
                tanks.get(i).setStackUnchecked(stacks.get(i).copy());
            }
        }
    }

    @Nullable
    public AttachedChemicals collectChemicalTanks(DataComponentMap.Builder builder, List<IChemicalTank> tanks) {
        //Skip tiles that have no gas tanks and skip the creative chemical tank
        boolean hasNonEmpty = false;
        List<ChemicalStack> stacks = new ArrayList<>(tanks.size());
        boolean skipRadioactive = IRadiationManager.INSTANCE.isRadiationEnabled() && shouldDumpRadiation();
        for (IChemicalTank tank : tanks) {
            if (tank.isEmpty() || skipRadioactive && tank.getStack().isRadioactive()) {
                //If the tank is empty or has a radioactive gas, treat it as empty
                stacks.add(ChemicalStack.EMPTY);
            } else {
                hasNonEmpty = true;
                stacks.add(tank.getStack().copy());
            }
        }
        return hasNonEmpty ? new AttachedChemicals(stacks) : null;
    }
    //End methods IMekanismChemicalHandler

    //Methods for implementing IMekanismFluidHandler
    @Nullable
    protected IFluidTankHolder getInitialFluidTanks(IContentsListener listener) {
        return null;
    }

    @NotNull
    @Override
    public List<IExtendedFluidTank> getFluidTanks(@Nullable Direction side) {
        return Collections.emptyList();
    }
    //End methods IMekanismFluidHandler

    //Methods for implementing IMekanismStrictEnergyHandler
    @Nullable
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        return null;
    }

    @NotNull
    @Override
    public List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return Collections.emptyList();
    }

    @Override
    public long insertEnergy(int container, long amount, @Nullable Direction side, @NotNull Action action) {
        return trackLastEnergy(amount, action, IMekanismStrictEnergyHandler.super.insertEnergy(container, amount, side, action));
    }

    @Override
    public long insertEnergy(long amount, @Nullable Direction side, @NotNull Action action) {
        //Note: Super bypasses calling insertEnergy(int container, ...) so we need to override it here as well
        return trackLastEnergy(amount, action, IMekanismStrictEnergyHandler.super.insertEnergy(amount, side, action));
    }

    private long trackLastEnergy(long amount, @NotNull Action action, long remainder) {
        if (action.execute()) {
            //If for some reason we don't have a level fall back to zero
            lastEnergyTracker.received(level == null ? 0 : level.getGameTime(), amount - remainder);
        }
        return remainder;
    }

    public final long getInputRate() {
        return lastEnergyTracker.getLastEnergyReceived();
    }

    public void applyEnergyContainers(DataComponentGetter input, List<IEnergyContainer> containers, AttachedEnergy attachedEnergy) {
        List<Long> stored = attachedEnergy.containers();
        int size = stored.size();
        if (size == containers.size()) {
            for (int i = 0; i < size; i++) {
                containers.get(i).setEnergy(stored.get(i));
            }
        }
    }

    @Nullable
    public AttachedEnergy collectEnergyContainers(DataComponentMap.Builder builder, List<IEnergyContainer> containers) {
        boolean hasNonEmpty = false;
        List<Long> stored = new ArrayList<>(containers.size());
        for (IEnergyContainer container : containers) {
            stored.add(container.getEnergy());
            if (!container.isEmpty()) {
                hasNonEmpty = true;
            }
        }
        return hasNonEmpty ? new AttachedEnergy(stored) : null;
    }
    //End methods IMekanismStrictEnergyHandler

    //Methods for implementing IInWorldHeatHandler
    @Nullable
    protected IHeatCapacitorHolder getInitialHeatCapacitors(IContentsListener listener, CachedAmbientTemperature ambientTemperature) {
        return null;
    }

    @Override
    public double getAmbientTemperature(@NotNull Direction side) {
        if (canHandleHeat() && ambientTemperature != null) {
            return ambientTemperature.getTemperature(side);
        }
        return ITileHeatHandler.super.getAmbientTemperature(side);
    }

    @Nullable
    @Override
    public IHeatHandler getAdjacent(@NotNull Direction side) {
        if (canHandleHeat() && getHeatCapacitorCount(side) > 0) {
            return getAdjacentUnchecked(side);
        }
        return null;
    }

    @Nullable
    protected IHeatHandler getAdjacentUnchecked(@NotNull Direction side) {
        //Loader-specific adjacent-heat lookup (NeoForge BlockCapabilityCache / Fabric BlockApiLookup) via the service seam.
        return ICapabilityExposureService.INSTANCE.getAdjacentHeat(this, side);
    }

    @NotNull
    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return Collections.emptyList();
    }
    //End methods for IInWorldHeatHandler

    //Methods for implementing ITileActive
    @Override
    public boolean getActive() {
        return isRemote() ? getClientActive() : currentActive;
    }

    protected boolean getClientActive() {
        return activeAttribute != null && activeAttribute.isActive(getBlockState());
    }

    @Override
    public void setActive(boolean active) {
        if (isActivatable() && active != currentActive) {
            BlockState state = getBlockState();
            if (activeAttribute != null) {
                currentActive = active;
                if (getClientActive() != active) {
                    if (active) {
                        //Always turn on instantly
                        level.setBlockAndUpdate(worldPosition, activeAttribute.setActive(state, true));
                    } else {
                        // if the update delay is already zero, we can go ahead and set the state
                        if (updateDelay == 0) {
                            level.setBlockAndUpdate(worldPosition, activeAttribute.setActive(state, currentActive));
                        }
                        // we always reset the update delay when turning off
                        updateDelay = delaySupplier.getAsInt();
                    }
                }
            }
        }
    }
    //End methods ITileActive

    //Methods for implementing IConfigCardAccess
    //Base-level config-card support (the loader-neutral sustained-data round trip). The NeoForge machine leaf overrides
    //all four to also carry frequency data and to invalidate capabilities; non-machine subclasses use these defaults.
    @Override
    public Block getConfigurationDataType() {
        return getBlockState().getBlock();
    }

    @Override
    public void writeConfigurationData(ValueOutput output, Player player) {
        writeSustainedData(output);
    }

    @Override
    public void setConfigurationData(ValueInput input, Player player) {
        readSustainedData(input);
    }

    @Override
    public void configurationDataSet() {
        setChanged();
    }
    //End methods IConfigCardAccess

    //Methods for implementing ITileSound

    /**
     * Used to check if this tile should attempt to play its sound
     */
    protected boolean canPlaySound() {
        return getActive();
    }

    /**
     * Only call this from the client
     */
    protected void updateSound() {
        // If machine sounds are disabled, noop
        if (!hasSound() || !ITileSoundService.INSTANCE.machineSoundsEnabled() || soundEvent == null) {
            return;
        }
        if (canPlaySound() && !isRemoved()) {
            // If sounds are being muted, we can attempt to start them on every tick, only to have them
            // denied by the event bus, so use a cooldown period that ensures we're only trying once every
            // second or so to start a sound.
            if (--playSoundCooldown > 0) {
                return;
            }
            SoundEvent sound = soundEvent.get();
            if (sound != lastSoundEvent) {
                if (activeSound != null) {
                    //The sound changed, stop it so that we can start it back up again
                    ITileSoundService.INSTANCE.stopTileSound(getSoundPos());
                    activeSound = null;
                }
                lastSoundEvent = sound;
            }

            // If this machine isn't fully muffled, and we don't seem to be playing a sound for it, go ahead and
            // play it
            if (!isFullyMuffled() && (activeSound == null || !ITileSoundService.INSTANCE.isActiveSound(activeSound))) {
                activeSound = ITileSoundService.INSTANCE.startTileSound(lastSoundEvent, getSoundCategory(), getInitialVolume(), level.getRandom(), getSoundPos());
            }
            // Always reset the cooldown; either we just attempted to play a sound or we're fully muffled; either way
            // we don't want to try again
            playSoundCooldown = SharedConstants.TICKS_PER_SECOND;
        } else if (activeSound != null) {
            ITileSoundService.INSTANCE.stopTileSound(getSoundPos());
            activeSound = null;
            playSoundCooldown = 0;
        }
    }

    /**
     * Hook: whether the machine's sound is fully muffled by upgrades. Default {@code false} here; the NeoForge leaf
     * overrides it with the {@code TileComponentUpgrade}-reading body.
     */
    protected boolean isFullyMuffled() {
        return false;
    }
    //End methods ITileSound
}
