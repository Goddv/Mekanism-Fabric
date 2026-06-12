package mekanism.common.tile.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToLongFunction;
import mekanism.api.Action;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IContentsListener;
import mekanism.api.MekanismItemAbilities;
import mekanism.api.SerializationConstants;
import mekanism.api.Upgrade;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IMekanismFluidHandler;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IHeatHandler;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import mekanism.api.math.MathUtils;
import mekanism.api.radiation.IRadiationManager;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.api.security.SecurityMode;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.Mekanism;
import mekanism.common.attachments.FilterAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.heat.AttachedHeat;
import mekanism.common.attachments.containers.heat.HeatCapacitorData;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.block.attribute.AttributeSound;
import mekanism.common.block.attribute.AttributeStateActive;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.AttributeUpgradeSupport;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.attribute.Attributes.AttributeComparator;
import mekanism.common.block.attribute.Attributes.AttributeComputerIntegration;
import mekanism.common.block.attribute.Attributes.AttributeRedstone;
import mekanism.common.block.attribute.Attributes.AttributeSecurity;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.heat.BasicHeatCapacitor;
import mekanism.common.capabilities.heat.CachedAmbientTemperature;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.capabilities.holder.heat.IHeatCapacitorHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.filter.FilterManager;
import mekanism.common.integration.computer.BoundMethodHolder;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.FactoryRegistry;
import mekanism.common.integration.computer.IComputerTile;
import mekanism.common.integration.computer.MethodRestriction;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.inventory.container.ITrackableContainer;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.container.sync.SyncableFluidStack;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import mekanism.common.inventory.container.sync.dynamic.SyncMapper;
import mekanism.common.item.ItemConfigurationCard;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.lib.chunkloading.IChunkLoader;
import mekanism.common.lib.frequency.IFrequencyHandler;
import mekanism.common.lib.frequency.TileComponentFrequency;
import mekanism.common.lib.security.BlockSecurityUtils;
import mekanism.common.lib.security.ISecurityTile;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tags.MekanismTags;
import mekanism.common.tile.component.IGuiTileComponent;
import mekanism.common.tile.component.ITileComponent;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.interfaces.ITileFilterHolder;
import mekanism.common.tile.interfaces.ITileRadioactive;
import mekanism.common.tile.interfaces.ITileUpgradable;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.RegistryUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.util.Util;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge leaf of the machine tile. Retains everything that names NeoForge or client-only types: the
 * {@code ContainerType.TYPES} save/load hooks, the dynamic container sync ({@code addContainerTrackers}/{@code SyncMapper}),
 * the computer integration ({@code @ComputerMethod}), the frequency/security/upgrade {@code TileComponent}s, the wrench/gui
 * cluster, the radiation hooks and the upgrade energy math. The bulk of the tile lives in the loader-neutral
 * {@link TileEntityMekanismBase}; this class only adds the NeoForge-coupled pieces back on top. FQN preserved because
 * registration ({@code MekanismTileEntityTypes}) and {@code ContainerType}'s static method-refs bind this concrete type.
 */
//TODO: We need to move the "supports" methods into the source interfaces so that we make sure they get checked before being used
public abstract class TileEntityMekanism extends CapabilityTileEntity implements IFrequencyHandler, ISecurityTile, IComputerTile, ITileRadioactive,
      ITileUpgradable, IMekanismInventory, ITrackableContainer {

    private final Holder<Block> blockProvider;

    //Variables for handling ITileUpgradable
    //TODO: Convert this to being private
    protected TileComponentUpgrade upgradeComponent;
    //End variables ITileUpgradable

    //Variables for handling IFrequencyHandler
    protected final TileComponentFrequency frequencyComponent;
    //End variables IFrequencyHandler

    //Variables for handling ITileSecurity
    private TileComponentSecurity securityComponent;
    //End variables ITileSecurity

    @Nullable
    private String containerDescription;

    public TileEntityMekanism(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(((IHasTileEntity<? extends BlockEntity>) blockProvider.value()).getTileType(), pos, state);
        this.blockProvider = blockProvider;
        setSupportedTypes(this.blockProvider);
        presetVariables();
        IContentsListener saveOnlyListener = this::markForSave;

        IChemicalTankHolder initialChemicalTanks = getInitialChemicalTanks(getListener(ContainerType.CHEMICAL, saveOnlyListener));
        IFluidTankHolder initialFluidTanks = getInitialFluidTanks(getListener(ContainerType.FLUID, saveOnlyListener));
        IEnergyContainerHolder initialEnergyContainers = getInitialEnergyContainers(getListener(ContainerType.ENERGY, saveOnlyListener));
        IInventorySlotHolder initialInventory = getInitialInventory(getListener(ContainerType.ITEM, saveOnlyListener));
        CachedAmbientTemperature ambientTemperature = new CachedAmbientTemperature(this::getLevel, this::getBlockPos);
        IHeatCapacitorHolder initialHeatCapacitors = getInitialHeatCapacitors(getListener(ContainerType.HEAT, saveOnlyListener), ambientTemperature);
        //Build + register the capability handler managers (on the NeoForge CapabilityTileEntity superclass).
        buildAndRegisterManagers(initialChemicalTanks, initialFluidTanks, initialEnergyContainers, initialInventory, initialHeatCapacitors);
        if (canHandleHeat()) {
            this.ambientTemperature = ambientTemperature;
        } else {
            this.ambientTemperature = null;
        }
        //Reassign the loader-neutral base default to the real config-backed delay (see TileEntityMekanismBase#delaySupplier).
        delaySupplier = MekanismConfig.general.blockDeactivationDelay;

        frequencyComponent = new TileComponentFrequency(this);
        if (supportsUpgrades()) {
            upgradeComponent = new TileComponentUpgrade(this);
        }
        if (hasSecurity()) {
            securityComponent = new TileComponentSecurity(this);
        }
        //Reassign the non-final base soundEvent field (resolved off AttributeSound, which is NeoForge-only).
        soundEvent = hasSound() ? Attribute.getOrThrow(this.blockProvider, AttributeSound.class).getSound() : null;
    }

    private void setSupportedTypes(Holder<Block> block) {
        //Used to get any data we may need
        supportsUpgrades = Attribute.has(block, AttributeUpgradeSupport.class);
        canBeUpgraded = Attribute.has(block, AttributeUpgradeable.class);
        isDirectional = Attribute.has(block, AttributeStateFacing.class);
        supportsRedstone = Attribute.has(block, AttributeRedstone.class);
        hasSound = Attribute.has(block, AttributeSound.class);
        hasGui = Attribute.has(block, AttributeGui.class);
        hasBounding = Attribute.has(block, AttributeHasBounding.class);
        hasSecurity = Attribute.has(block, AttributeSecurity.class);
        activeAttribute = Attribute.get(block, AttributeStateActive.class);
        isActivatable = hasSound || activeAttribute != null;
        supportsComparator = Attribute.has(block, AttributeComparator.class);
        supportsComputers = Mekanism.hooks.computerCompatEnabled() && Attribute.has(block, AttributeComputerIntegration.class);
        hasChunkloader = this instanceof IChunkLoader;
        nameable = hasGui() && !Attribute.getOrThrow(getBlockHolder(), AttributeGui.class).hasCustomName();
    }

    @Override
    public final Holder<Block> getBlockHolder() {
        return blockProvider;
    }

    /**
     * Should data related to the given type be persisted in this tile save
     */
    public boolean persists(ContainerType<?, ?, ?> type) {
        return type.canHandle(this);
    }

    /**
     * Should data related to the given type be transferred to the item
     */
    public boolean persistsToItem(ContainerType<?, ?, ?> type) {
        return persists(type);
    }

    /**
     * Should data related to the given type be synced to the client in the GUI
     */
    public boolean syncs(ContainerType<?, ?, ?> type) {
        return persists(type);
    }

    @Override
    public final boolean hasSecurity() {
        return hasSecurity;
    }

    @Override
    public final boolean hasComputerSupport() {
        return supportsComputers;
    }

    @NotNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Component getName() {
        return hasCustomName() ? getCustomName() : TextComponentUtil.build(getBlockHolder());
    }

    @NotNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Component getDisplayName() {
        if (isNameable()) {
            return hasCustomName() ? getCustomName() : TextComponentUtil.translate(getContainerDescription());
        }
        return TextComponentUtil.build(getBlockHolder());
    }

    private String getContainerDescription() {
        if (containerDescription == null) {
            containerDescription = Util.makeDescriptionId("container", RegistryUtils.getName(getBlockHolder()));
        }
        return containerDescription;
    }

    protected WrenchResult tryWrenchDismantle(BlockState state, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (IRadiationManager.INSTANCE.isRadiationEnabled() && getRadiationScale() > 0) {
                //Don't allow dismantling radioactive blocks
                return WrenchResult.RADIOACTIVE;
            }
            WorldUtils.dismantleBlock(state, getLevel(), worldPosition, this, player, stack);
            return WrenchResult.DISMANTLED;
        }
        return WrenchResult.PASS;
    }

    protected WrenchResult tryWrenchRotate(BlockState state, Player player, ItemStack stack) {
        //Special ITileDirectional handling
        if (isDirectional()) {
            AttributeStateFacing attribute = Attribute.getOrThrow(getBlockHolder(), AttributeStateFacing.class);
            if (attribute.canRotate()) {
                setFacing(MekanismUtils.rotate(getDirection(), attribute.getFacingProperty() == BlockStateProperties.FACING));
                return WrenchResult.SUCCESS;
            }
        }
        return WrenchResult.PASS;
    }

    public WrenchResult tryWrench(BlockState state, Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return WrenchResult.PASS;
        }
        WrenchResult result = WrenchResult.PASS;
        boolean canRotate = stack.canPerformAction(MekanismItemAbilities.WRENCH_ROTATE);
        boolean canDismantle = stack.canPerformAction(MekanismItemAbilities.WRENCH_DISMANTLE);
        if (!canRotate && !canDismantle) {
            if (stack.canPerformAction(MekanismItemAbilities.WRENCH_EMPTY) || stack.canPerformAction(MekanismItemAbilities.WRENCH_CONFIGURE)) {
                //The stack provides some wrench actions, it is likely intentional that it can't rotate or dismantle blocks
                return result;
            }
            //If the item doesn't explicitly declare the ability to rotate or dismantle,
            // then mark that it can do both if it is in the configurator tag
            canRotate = canDismantle = stack.is(MekanismTags.Items.CONFIGURATORS);
        }
        if (canRotate || canDismantle) {
            if (hasSecurity() && !IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, getWorldNN(), worldPosition, this)) {
                return WrenchResult.NO_SECURITY;
            } else if (canDismantle) {
                result = tryWrenchDismantle(state, player, stack);
            }
            if (result == WrenchResult.PASS && canRotate) {
                result = tryWrenchRotate(state, player, stack);
            }
        }
        return result;
    }

    public InteractionResult openGui(Player player) {
        //Everything that calls this has isRemote being false but add the check just in case anyway
        if (hasGui() && !isRemote() && !player.isShiftKeyDown()) {
            if (hasSecurity() && !IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, player.level(), worldPosition, this)) {
                return InteractionResult.FAIL;
            }
            //Pass on this activation if the player is rotating with a configurator
            ItemStack stack = player.getMainHandItem();
            if (isDirectional() && !stack.isEmpty() && stack.getItem() instanceof ItemConfigurator configurator) {
                if (configurator.getMode(stack) == ItemConfigurator.ConfiguratorMode.ROTATE) {
                    return InteractionResult.PASS;
                }
            }
            //Pass on this activation if the player is using a configuration card (and this tile supports the capability)
            if (!stack.isEmpty() && stack.getItem() instanceof ItemConfigurationCard &&
                WorldUtils.getCapability(level, Capabilities.CONFIG_CARD, worldPosition, null, this, null) != null) {
                return InteractionResult.PASS;
            }

            player.openMenu(Attribute.getOrThrow(getBlockHolder(), AttributeGui.class).getProvider(this, true), buffer -> {
                buffer.writeBlockPos(worldPosition);
                encodeExtraContainerData(buffer);
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    //TODO - 1.18: Optimize what gets ticks registered to it
    public static void tickClient(Level level, BlockPos pos, BlockState state, TileEntityMekanism tile) {
        if (tile.hasSound()) {
            tile.updateSound();
        }
        tile.onUpdateClient();
        //None of our impls currently care about the ticker in their onUpdateClient methods
        //tile.ticker++;
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, TileEntityMekanism tile) {
        if (tile.hasBounding && tile.syncMasterToBounding) {
            //TODO: Evaluate checking every x ticks to make sure we have bounding blocks (at least if we haven't already checked) in case we are missing them
            // for example if someone set the main block by using a command
            tile.syncMasterToBounding = false;
            AttributeHasBounding hasBounding = Attribute.get(state, AttributeHasBounding.class);
            if (hasBounding != null) {
                //Note: In theory we only ever set syncMasterToBounding if we know this has bounding blocks, but validate it
                hasBounding.syncMasterPosition(level, pos, state);
            }
        }
        tile.frequencyComponent.tickServer(level, pos);
        if (tile.supportsUpgrades()) {
            tile.upgradeComponent.tickServer();
        }
        if (tile.hasChunkloader) {
            ((IChunkLoader) tile).getChunkLoader().tickServer();
        }
        if (tile.isActivatable()) {
            if (tile.updateDelay > 0) {
                tile.updateDelay--;
                if (tile.updateDelay == 0 && tile.getClientActive() != tile.currentActive) {
                    //If it doesn't match, and we are done with the delay period, then update it
                    level.setBlockAndUpdate(pos, tile.activeAttribute.setActive(state, tile.currentActive));
                }
            }
        }
        boolean sendUpdatePacket = tile.onUpdateServer();
        if (tile.updateRadiationScale()) {
            sendUpdatePacket = true;
        }
        //TODO - 1.18: More generic "needs update" flag that we set that then means we don't end up sending an update packet more than once per tick
        if (tile.canHandleHeat()) {
            // update heat after server tick as we now have simulated changes
            // we use persists, as only one reference should update
            tile.updateHeatCapacitors(null);
        }
        //Set that we received zero energy so if it is a different tick than we last had,
        // and we don't actually receive anything then we will properly update it to zero
        tile.lastEnergyTracker.received(level.getGameTime(), 0L);
        //Only update the comparator state if we support comparators and need to update comparators
        if (tile.supportsComparator() && tile.updateComparators && !state.isAir()) {
            int newRedstoneLevel = tile.getRedstoneLevel();
            if (newRedstoneLevel != tile.currentRedstoneLevel) {
                tile.currentRedstoneLevel = newRedstoneLevel;
                tile.notifyComparatorChange();
            }
            tile.updateComparators = false;
        }
        tile.ticker++;
        if (tile.supportsRedstone()) {
            tile.redstoneLastTick = tile.redstone;
        }
        if (sendUpdatePacket) {
            tile.sendUpdatePacket();
        }
    }

    //ContainerType.TYPES save/load hooks (kept on the NeoForge leaf because ContainerType is NeoForge-capability-native).
    //These are invoked from TileEntityMekanismBase at the exact original interleave points so NBT bytes are unchanged.
    @Override
    protected void loadAdditionalContainers(@NotNull ValueInput input) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            if (type.canHandle(this) && persists(type)) {
                type.readFrom(input, this);
            }
        }
    }

    @Override
    protected void saveAdditionalContainers(@NotNull ValueOutput output) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            if (type.canHandle(this) && persists(type)) {
                type.saveTo(output, this);
            }
        }
    }

    @Override
    protected void applyImplicitComponentsLeaf(@NotNull DataComponentGetter input) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            if (persistsToItem(type)) {
                type.copyToTile(this, input);
            }
        }
        if (this instanceof ITileFilterHolder<?> filterHolder) {
            FilterAware filterAware = input.get(MekanismDataComponents.FILTER_AWARE);
            if (filterAware != null) {
                //TODO - 1.20.4: Do we need to copy these or can we just pass the raw instance?
                filterHolder.getFilterManager().trySetFilters(filterAware.filters());
            }
        }
        if (supportsRedstone()) {
            setControlType(input.getOrDefault(MekanismDataComponents.REDSTONE_CONTROL, getControlType()));
        }
    }

    @Override
    protected void addRemapEntriesLeaf(List<DataComponentType<?>> remapEntries) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            if (persistsToItem(type) && !remapEntries.contains(type.getComponentType().get())) {
                //Ensure we add any container types that we only conditionally added
                remapEntries.add(type.getComponentType().get());
            }
        }
        if (this instanceof ITileFilterHolder<?> && !remapEntries.contains(MekanismDataComponents.FILTER_AWARE.get())) {
            remapEntries.add(MekanismDataComponents.FILTER_AWARE.get());
        }
    }

    @Override
    protected void collectImplicitComponentsLeaf(@NotNull DataComponentMap.Builder builder) {
        for (ContainerType<?, ?, ?> type : ContainerType.TYPES) {
            if (persistsToItem(type)) {
                type.copyFromTile(this, builder);
            }
        }
        if (this instanceof ITileFilterHolder<?> filterHolder) {
            FilterManager<?> filterManager = filterHolder.getFilterManager();
            if (!filterManager.getFilters().isEmpty()) {
                builder.set(MekanismDataComponents.FILTER_AWARE, new FilterAware(List.copyOf(filterManager.getFilters())));
            }
        }
        if (supportsRedstone()) {
            builder.set(MekanismDataComponents.REDSTONE_CONTROL, controlType);
        }
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        // setup dynamic container syncing
        SyncMapper.INSTANCE.setup(container, getClass(), () -> this);

        for (ITileComponent component : components) {
            if (component instanceof IGuiTileComponent guiComponent) {
                guiComponent.trackForMainContainer(container);
            }
        }
        if (supportsRedstone()) {
            container.track(SyncableEnum.create(RedstoneControl.BY_ID, RedstoneControl.DISABLED, () -> controlType, value -> controlType = value));
            container.track(SyncableBoolean.create(this::isPowered, value -> redstone = value));
            container.track(SyncableBoolean.create(this::wasPowered, value -> redstoneLastTick = value));
        }
        boolean isClient = isRemote();
        if (canHandleChemicals() && syncs(ContainerType.CHEMICAL)) {
            List<IChemicalTank> chemicalTanks = getChemicalTanks(null);
            for (IChemicalTank chemicalTank : chemicalTanks) {
                container.track(SyncableChemicalStack.create(chemicalTank, isClient));
            }
        }
        if (canHandleFluid() && syncs(ContainerType.FLUID)) {
            List<IExtendedFluidTank> fluidTanks = getFluidTanks(null);
            for (IExtendedFluidTank fluidTank : fluidTanks) {
                container.track(SyncableFluidStack.create(fluidTank, isClient));
            }
        }
        if (canHandleHeat() && syncs(ContainerType.HEAT)) {
            List<IHeatCapacitor> heatCapacitors = getHeatCapacitors(null);
            for (IHeatCapacitor capacitor : heatCapacitors) {
                container.track(SyncableDouble.create(capacitor::getHeat, capacitor::setHeat));
                if (capacitor instanceof BasicHeatCapacitor heatCapacitor) {
                    container.track(SyncableDouble.create(capacitor::getHeatCapacity, capacity -> heatCapacitor.setHeatCapacity(capacity, false)));
                }
            }
        }
        if (canHandleEnergy() && syncs(ContainerType.ENERGY)) {
            trackLastEnergy(container);
            List<IEnergyContainer> energyContainers = getEnergyContainers(null);
            for (IEnergyContainer energyContainer : energyContainers) {
                if (energyContainer instanceof MachineEnergyContainer<?> machineEnergy) {
                    if (supportsUpgrades() || machineEnergy.adjustableRates()) {
                        container.track(SyncableLong.create(machineEnergy::getMaxEnergy, machineEnergy::setMaxEnergy));
                        container.track(SyncableLong.create(machineEnergy::getEnergyPerTick, machineEnergy::setEnergyPerTick));
                    }
                }
                //Ensure energy is synced after the max energy adjustment is synced so that the client doesn't try to clamp what the energy is to the max value
                container.track(SyncableLong.create(energyContainer::getEnergy, energyContainer::setEnergy));
            }
        }
    }

    protected void trackLastEnergy(MekanismContainer container) {
        container.track(SyncableLong.create(lastEnergyTracker::getLastEnergyReceived, lastEnergyTracker::setLastEnergyReceived));
    }

    @Override
    public void handleUpdateTag(@NotNull ValueInput input) {
        //we do NOT call the full load, as it will call a load (like from disk) and BEs will never see their changes;
        //route through the base helper so only TileEntityUpdateable.loadAdditional runs (byte-identical to the former super call)
        loadAdditionalFromUpdateTag(input);
        for (ITileComponent component : components) {
            component.readFromUpdateTag(input);
        }
        radiationScale = input.getFloatOr(SerializationConstants.RADIATION, radiationScale);
    }

    @Override
    public TileComponentFrequency getFrequencyComponent() {
        return frequencyComponent;
    }

    //Methods for implementing ITileDirectional
    @NotNull
    @Override
    @ComputerMethod(restriction = MethodRestriction.DIRECTIONAL)
    public final Direction getDirection() {
        if (isDirectional()) {
            if (cachedDirection != null) {
                return cachedDirection;
            }
            BlockState state = getBlockState();
            cachedDirection = Attribute.getFacing(state);
            if (cachedDirection != null) {
                return cachedDirection;
            } else if (!getType().isValid(state)) {
                //This is probably always true if we couldn't get the direction it is facing
                // but double check just in case before logging
                Mekanism.logger.warn("Error invalid block for tile {} at {} in {}. Unable to get direction, falling back to north, "
                                     + "things will probably not work correctly. This is almost certainly due to another mod incorrectly "
                                     + "trying to move this tile and not properly updating the position.",
                      Util.getRegisteredName(BuiltInRegistries.BLOCK_ENTITY_TYPE, getType()), worldPosition, level);
            }
        }
        //TODO: Remove, give it some better default, or allow it to be null
        // (this is used by some things like non directional blocks with energy configs)
        return Direction.NORTH;
    }

    @Override
    public void setFacing(@NotNull Direction direction) {
        setFacing(direction, true);
    }

    public void setFacing(@NotNull Direction direction, boolean notifyCaps) {
        if (isDirectional() && direction != cachedDirection && level != null) {
            invalidateDirectionCaches(direction);
            BlockState state = Attribute.setFacing(getBlockState(), direction);
            if (state != null) {
                level.setBlockAndUpdate(worldPosition, state);
                if (notifyCaps) {
                    //Clear cached capabilities as it is possible it changed on one of the sides
                    invalidateCapabilitiesFull();
                }
            }
        }
    }
    //End methods ITileDirectional

    //Methods for implementing ITileRedstone
    @Override
    @ComputerMethod(nameOverride = "getRedstoneMode", restriction = MethodRestriction.REDSTONE_CONTROL)
    public RedstoneControl getControlType() {
        return controlType;
    }
    //End methods ITileRedstone

    //Methods for implementing IComparatorSupport
    /**
     * @param type Type of container that got updated
     *
     * @implNote It can be assumed {@link #supportsComparator()} is true before this is called.
     */
    protected boolean makesComparatorDirty(ContainerType<?, ?, ?> type) {
        //Assume that items make it dirty unless otherwise overridden, as we use this before we can call hasInventory
        // and if we aren't using an inventory as our comparator thing we will be overriding this method anyway
        // and if we don't have an inventory we can't assign this listener to anything as adding slots and assigning it
        // is what binds the listener to the main tile
        return type == ContainerType.ITEM;
    }

    protected final IContentsListener getListener(ContainerType<?, ?, ?> type, IContentsListener saveOnlyListener) {
        //If we don't support comparators we can just skip having a special one that only marks for save as our
        // setChanged won't actually do anything so there is no reason to bother creating a save only listener
        return !supportsComparator() || makesComparatorDirty(type) ? this : saveOnlyListener;
    }

    @Override
    @ComputerMethod(nameOverride = "getComparatorLevel", restriction = MethodRestriction.COMPARATOR)
    public int getCurrentRedstoneLevel() {
        return currentRedstoneLevel;
    }
    //End methods IComparatorSupport

    //Methods for implementing ITileUpgradable
    @NotNull
    @Override
    public Set<Upgrade> getSupportedUpgrade() {
        return super.getSupportedUpgrade();
    }

    @Override
    public boolean supportsUpgrade(Upgrade upgradeType) {
        return supportsUpgrades() && getComponent().supports(upgradeType);
    }

    @Override
    public TileComponentUpgrade getComponent() {
        return upgradeComponent;
    }

    @Override
    public long getUpgradedMaxEnergy(long base) {
        //MachineEnergyContainer (now :common) calls this only on the ENERGY-upgrade path; getMaxEnergy itself also
        //no-ops when upgrades are unsupported, so this is the verbatim relocation of the former inline call.
        return MekanismUtils.getMaxEnergy(this, base);
    }

    @Override
    public long getUpgradedEnergyPerTick(long base) {
        //Verbatim relocation of MachineEnergyContainer.updateEnergyPerTick()'s former body: only adjust when the tile
        //supports upgrades AND an ENERGY/SPEED upgrade; otherwise return base unchanged (== the per-tick that was already
        //set, since currentEnergyPerTick is only ever the base or this adjusted value).
        if (supportsUpgrades()) {
            TileComponentUpgrade upgradeComponent = getComponent();
            if (upgradeComponent.supports(Upgrade.ENERGY) || upgradeComponent.supports(Upgrade.SPEED)) {
                return MekanismUtils.getEnergyPerTick(this, base);
            }
        }
        return base;
    }
    //End methods ITileUpgradable

    //Methods for implementing IMekanismChemicalHandler
    /**
     * @apiNote Only call on server.
     */
    private boolean updateRadiationScale() {
        if (shouldDumpRadiation()) {
            float scale = ITileRadioactive.calculateRadiationScale(getChemicalTanks(null));
            if (Math.abs(scale - radiationScale) > 0.05F) {
                radiationScale = scale;
                return true;
            }
        }
        return false;
    }

    @Override
    public float getRadiationScale() {
        return IRadiationManager.INSTANCE.isRadiationEnabled() ? radiationScale : 0;
    }
    //End methods IMekanismChemicalHandler

    //Methods for implementing IInWorldHeatHandler
    //Heat data-component (de)serialization helpers; kept on the leaf because they touch NeoForge AttachedHeat/
    //HeatCapacitorData. Referenced as method-refs (TileEntityMekanism::applyHeatCapacitors/collectHeatCapacitors) from
    //ContainerType, which still resolve via this concrete leaf type.
    public void applyHeatCapacitors(DataComponentGetter input, List<IHeatCapacitor> capacitors, AttachedHeat attachedHeat) {
        List<HeatCapacitorData> stored = attachedHeat.containers();
        int size = stored.size();
        if (size == capacitors.size()) {
            for (int i = 0; i < size; i++) {
                IHeatCapacitor capacitor = capacitors.get(i);
                HeatCapacitorData data = stored.get(i);
                if (data.heat().isPresent()) {
                    capacitor.setHeat(data.heat().getAsDouble());
                }
                if (capacitor instanceof BasicHeatCapacitor basic) {
                    basic.setHeatCapacity(data.capacity(), false);
                }
            }
        }
    }

    @Nullable
    public AttachedHeat collectHeatCapacitors(DataComponentMap.Builder builder, List<IHeatCapacitor> capacitors) {
        List<HeatCapacitorData> stored = new ArrayList<>(capacitors.size());
        for (IHeatCapacitor capacitor : capacitors) {
            if (capacitor.isAmbientTemperature()) {
                stored.add(new HeatCapacitorData(capacitor.getHeatCapacity()));
            } else {
                stored.add(new HeatCapacitorData(capacitor.getHeat(), capacitor.getHeatCapacity()));
            }
        }
        return new AttachedHeat(stored);
    }
    //End methods for IInWorldHeatHandler

    //Methods for implementing IConfigCardAccess
    @Override
    public void writeConfigurationData(ValueOutput output, Player player) {
        writeSustainedData(output);
        getFrequencyComponent().writeConfiguredFrequencies(output);
    }

    @Override
    public void setConfigurationData(ValueInput input, Player player) {
        readSustainedData(input);
        getFrequencyComponent().readConfiguredFrequencies(input, player);
    }

    @Override
    public Block getConfigurationDataType() {
        return getBlockState().getBlock();
    }

    @Override
    public void configurationDataSet() {
        setChanged();
        invalidateCapabilitiesFull();
        sendUpdatePacket();
        WorldUtils.notifyLoadedNeighborsOfTileChange(getLevel(), this.getBlockPos());
    }
    //End methods IConfigCardAccess

    //Methods for implementing ITileSecurity
    @Override
    public TileComponentSecurity getSecurity() {
        return securityComponent;
    }

    @Override
    public void onSecurityChanged(@NotNull SecurityMode old, @NotNull SecurityMode mode) {
        if (!isRemote() && hasGui() && level != null) {
            BlockSecurityUtils.get().securityChanged(playersUsing, level, worldPosition, this, old, mode);
        }
    }
    //End methods ITileSecurity

    //Methods for implementing ITileSound
    @Override
    protected boolean isFullyMuffled() {
        if (hasSound() && supportsUpgrade(Upgrade.MUFFLING)) {
            return getComponent().getUpgrades(Upgrade.MUFFLING) >= Upgrade.MUFFLING.getMax();
        }
        return false;
    }
    //End methods ITileSound

    //Methods relating to IComputerTile
    // Note: Some methods are elsewhere if we are exposing pre-existing implementations
    @Override
    public String getComputerName() {
        if (hasComputerSupport()) {
            return Attribute.getOrThrow(getBlockHolder(), AttributeComputerIntegration.class).name();
        }
        return "";
    }

    public void validateSecurityIsPublic() throws ComputerException {
        if (hasSecurity() && IBlockSecurityUtils.INSTANCE.getSecurityMode(getWorldNN(), worldPosition, this) != SecurityMode.PUBLIC) {
            throw new ComputerException("Setter not available due to machine security not being public.");
        }
    }

    @Override
    public void getComputerMethods(BoundMethodHolder holder) {
        IComputerTile.super.getComputerMethods(holder);
        for (ITileComponent component : components) {
            //Allow any supported components to add their computer methods as well
            // For example side config, ejector, and upgrade components
            FactoryRegistry.bindTo(holder, component);
        }
    }

    //TODO: If we ever end up using the part of our API that allows for multiple energy containers, it may be worth exposing
    // overloaded versions of these methods that take the container index as a parameter if anyone ends up running into a case
    // where being able to get a specific container's stored energy would be useful to their program. Alternatively we could
    // probably make use of our synthetic computer method wrapper to just add extra methods so then have it basically create
    // getEnergy, getEnergyFE for us with us only having to define getEnergy
    @ComputerMethod(nameOverride = "getEnergy", restriction = MethodRestriction.ENERGY)
    long getTotalEnergy() {
        return getTotalEnergy(IEnergyContainer::getEnergy);
    }

    @ComputerMethod(nameOverride = "getMaxEnergy", restriction = MethodRestriction.ENERGY)
    long getTotalMaxEnergy() {
        return getTotalEnergy(IEnergyContainer::getMaxEnergy);
    }

    @ComputerMethod(nameOverride = "getEnergyNeeded", restriction = MethodRestriction.ENERGY)
    long getTotalEnergyNeeded() {
        return getTotalEnergy(IEnergyContainer::getNeeded);
    }

    private long getTotalEnergy(ToLongFunction<IEnergyContainer> getter) {
        long total = 0;
        List<IEnergyContainer> energyContainers = getEnergyContainers(null);
        for (IEnergyContainer energyContainer : energyContainers) {
            total = MathUtils.addClamped(total, getter.applyAsLong(energyContainer));
        }
        return total;
    }

    @ComputerMethod(nameOverride = "getEnergyFilledPercentage", restriction = MethodRestriction.ENERGY)
    double getTotalEnergyFilledPercentage() {
        long stored = 0;
        long max = 0;
        List<IEnergyContainer> energyContainers = getEnergyContainers(null);
        for (IEnergyContainer energyContainer : energyContainers) {
            stored = MathUtils.addClamped(stored, energyContainer.getEnergy());
            max = MathUtils.addClamped(max, energyContainer.getMaxEnergy());
        }
        return MathUtils.divideToLevel(stored, max);
    }

    @ComputerMethod(restriction = MethodRestriction.REDSTONE_CONTROL, requiresPublicSecurity = true)
    void setRedstoneMode(RedstoneControl type) throws ComputerException {
        validateSecurityIsPublic();
        if (!supportsMode(type)) {
            throw new ComputerException("Unsupported redstone control mode: %s", type);
        }
        setControlType(type);
    }
    //End methods IComputerTile
}
