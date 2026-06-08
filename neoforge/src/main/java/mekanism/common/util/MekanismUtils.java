package mekanism.common.util;

import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismItemAbilities;
import mekanism.api.Upgrade;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.math.MathUtils;
import mekanism.client.MekanismClient;
import mekanism.common.Mekanism;
import mekanism.common.attachments.FrequencyAware;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.frequency.IFrequencyItem;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tags.MekanismTags;
import mekanism.common.tile.interfaces.IUpgradeTile;
import mekanism.common.util.UnitDisplayUtils.EnergyUnit;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities used by Mekanism. All miscellaneous methods are located here.
 *
 * @author AidanBrady
 */
public class MekanismUtils extends MekanismUtilsBase {

    private static final List<UUID> warnedFails = new ArrayList<>();

    //TODO: Evaluate adding an extra optional param to shrink and grow stack that allows for logging if it is mismatched. Defaults to false
    // Deciding on how to implement it into the API will need more thought as we want to keep overriding implementations as simple as
    // possible, and also ideally would use our normal logger instead of the API logger
    public static void logMismatchedStackSize(long actual, long expected) {
        if (expected != actual) {
            Mekanism.logger.error("Stack size changed by a different amount ({}) than requested ({}).", actual, expected);
            if (MekanismAPI.debug) {
                Mekanism.logger.error("Location ", new Exception());
            }
        }
    }

    public static void logExpectedZero(long actual) {
        if (actual != 0L) {
            Mekanism.logger.error("Energy value changed by a different amount ({}) than requested (zero).", actual);
            if (MekanismAPI.debug) {
                Mekanism.logger.error("Location ", new Exception());
            }
        }
    }

    @Nullable
    public static Player tryGetClientPlayer() {
        if (FMLEnvironment.getDist().isClient()) {
            return MekanismClient.tryGetClientPlayer();
        }
        //Note: Ideally we would have some way to get which player is in question on the server
        // as this is mostly used in tooltips, but odds are it won't end up being called
        return null;
    }

    /**
     * Gets the creator's modid if it exists, or falls back to the registry name.
     *
     * @implNote While the default implementation of getCreatorModId falls back to the registry name, it is possible someone is overriding this and not falling back.
     */
    @NotNull
    public static String getModId(@NotNull HolderLookup.Provider registries, @NotNull ItemStack stack) {
        Item item = stack.getItem();
        String modid = item.getCreatorModId(registries, stack);
        if (modid == null) {
            Mekanism.logger.error("Unexpected null registry name for item of class type: {}", item.getClass().getSimpleName());
            return "";
        }
        return modid;
    }

    public static double fractionUpgrades(IUpgradeTile tile, Upgrade type) {
        if (tile.supportsUpgrade(type)) {
            return tile.getComponent().getUpgrades(type) / (double) type.getMax();
        }
        return 0;
    }

    public static float getScale(float prevScale, IExtendedFluidTank tank) {
        return getScale(prevScale, tank.getFluidAmount(), tank.getCapacity(), tank.isEmpty());
    }

    public static long getBaseUsage(IUpgradeTile tile, int def) {
        if (tile.supportsUpgrades()) {
            //getGasPerTickMean * required ticks (not rounded)
            if (tile.supportsUpgrade(Upgrade.CHEMICAL)) {
                // def * (upgradeMultiplier ^ ((2 * speed - gas) / 8)) * (upgradeMultiplier ^ (-speed / 8)) =
                // def * upgradeMultiplier ^ ((speed - gas) / 8)
                //TODO: We may want to validate this provides the numbers we desire if we ever end up with any machines
                // that use this that are not statistical and have gas upgrades so would go through this code path
                return Math.round(def * Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(),
                      fractionUpgrades(tile, Upgrade.SPEED) - fractionUpgrades(tile, Upgrade.CHEMICAL)));
            }
            //If it doesn't support gas upgrades, we can fall through to the default value as the math would be:
            // def * (upgradeMultiplier ^ (speed / 8)) * (upgradeMultiplier ^ (-speed / 8)) =
            // def * 1
        }
        return def;
    }

    /**
     * Gets the operating ticks required for a machine via its upgrades.
     *
     * @param tile - tile containing upgrades
     * @param def  - the original, default ticks required
     *
     * @return required operating ticks
     */
    public static int getTicks(IUpgradeTile tile, int def) {
        if (tile.supportsUpgrades()) {
            return Math.max(1, MathUtils.clampToInt(getTicksD(tile, def)));
        }
        return def;
    }

    /**
     * Gets the operating ticks required for a machine via its upgrades.
     *
     * @param tile - tile containing upgrades
     * @param def  - the original, default ticks required
     *
     * @return required operating ticks
     */
    public static double getTicksD(IUpgradeTile tile, int def) {
        return def * Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(), -fractionUpgrades(tile, Upgrade.SPEED));
    }

    /**
     * Get the amount of operations per tick, accounting for bonus operations from non-default upgrade modifiers. Fractional operations are ignored
     *
     * @param tile              - tile containing upgrades
     * @param defTicks          - the original, default ticks required
     * @param defaultOperations - the original, default operations (usually 1)
     *
     * @return max operations to do in one tick. If speed is not < 1 tick return the default
     */
    public static int getOperationsPerTick(IUpgradeTile tile, int defTicks, int defaultOperations) {
        double ticksD = getTicksD(tile, defTicks);
        if (ticksD >= 1) {
            return defaultOperations;
        }
        return MathUtils.clampToInt(Math.max(1, 1 / ticksD) * defaultOperations);
    }

    /**
     * Gets the energy required per tick for a machine via its upgrades.
     *
     * @param tile - tile containing upgrades
     * @param def  - the original, default energy required
     *
     * @return required energy per tick
     */
    public static long getEnergyPerTick(IUpgradeTile tile, long def) {
        if (tile.supportsUpgrades()) {
            return MathUtils.ceilToLong(def * Math.pow(
                  MekanismConfig.general.maxUpgradeMultiplier.get(),
                  2 * fractionUpgrades(tile, Upgrade.SPEED) - fractionUpgrades(tile, Upgrade.ENERGY)
            ));
        }
        return def;
    }

    /**
     * Gets the secondary energy multiplier required per tick for a machine via upgrades.
     *
     * @param tile - tile containing upgrades
     *
     * @return max secondary energy per tick
     */
    public static double getGasPerTickMeanMultiplier(IUpgradeTile tile) {
        if (tile.supportsUpgrades()) {
            if (tile.supportsUpgrade(Upgrade.CHEMICAL)) {
                return Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(), 2 * fractionUpgrades(tile, Upgrade.SPEED) - fractionUpgrades(tile, Upgrade.CHEMICAL));
            }
            return Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(), fractionUpgrades(tile, Upgrade.SPEED));
        }
        return 1;
    }

    /**
     * Gets the maximum energy for a machine via its upgrades.
     *
     * @param tile - tile containing upgrades
     * @param def  - original, default max energy
     *
     * @return max energy
     */
    public static long getMaxEnergy(IUpgradeTile tile, long def) {
        if (tile.supportsUpgrades()) {
            return MathUtils.clampToLong(def * (Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(), fractionUpgrades(tile, Upgrade.ENERGY))));
        }
        return def;
    }

    /**
     * Gets the maximum energy for a machine's item form via its upgrades.
     *
     * @param energyUpgrades number of installed energy upgrades
     * @param def            original, default max energy
     *
     * @return max energy
     */
    public static long getMaxEnergy(int energyUpgrades, long def) {
        return MathUtils.clampToLong(def * (Math.pow(MekanismConfig.general.maxUpgradeMultiplier.get(), energyUpgrades / (double) Upgrade.ENERGY.getMax())));
    }

    /**
     * Gets a ResourceLocation with a defined resource type and name.
     *
     * @param type - type of resource to retrieve
     * @param name - simple name of file to retrieve as a ResourceLocation
     *
     * @return the corresponding ResourceLocation
     */
    public static Identifier getResource(ResourceType type, String name) {
        return Mekanism.rl(type.getPrefix() + name);
    }

    public static boolean lighterThanAirGas(FluidStack stack) {
        return stack.is(Tags.Fluids.GASEOUS) && stack.getFluidType().getDensity(stack) <= 0;
    }

    /**
     * @apiNote Only call on the client.
     */
    public static void addFrequencyItemTooltip(ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IFrequencyItem frequencyItem)) {//Note: This shouldn't be empty, but we validate it just in case
            return;
        }
        DataComponentType<? extends FrequencyAware<?>> frequencyComponent = MekanismDataComponents.getFrequencyComponent(frequencyItem.getFrequencyType());
        if (frequencyComponent != null) {
            stack.addToTooltip(frequencyComponent, context, tooltipDisplay, tooltipAdder, flag);
        }
    }

    public static Component getEnergyDisplayShort(long energy) {
        EnergyUnit configured = EnergyUnit.getConfigured();
        return UnitDisplayUtils.getDisplayShort(configured.convertToDouble(energy), configured);
    }

    /**
     * Convert from the unit defined in the configuration to joules.
     *
     * @param energy - energy to convert
     *
     * @return energy converted to joules
     */
    public static long convertToJoules(long energy) {
        return EnergyUnit.getConfigured().convertFrom(energy);
    }

    /**
     * Gets a rounded energy display of a defined amount of energy.
     *
     * @param temp - temperature to display
     *
     * @return rounded energy display
     */
    public static Component getTemperatureDisplay(double temp, TemperatureUnit unit, boolean shift) {
        double tempKelvin = unit.convertToK(temp, true);
        return UnitDisplayUtils.getDisplayShort(tempKelvin, MekanismConfig.common.tempUnit.get(), shift);
    }

    /**
     * Checks if the stack can be used as a wrench for dismantling purposes
     */
    public static boolean canUseAsWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (stack.canPerformAction(MekanismItemAbilities.WRENCH_DISMANTLE)) {
            return true;
        } else if (stack.is(MekanismTags.Items.CONFIGURATORS)) {
            //If it is in the tag, validate it isn't exposing one of the other wrench action types, as then it probably wants to act
            // as only that type instead of as any type
            return !stack.canPerformAction(MekanismItemAbilities.WRENCH_ROTATE) &&
                   !stack.canPerformAction(MekanismItemAbilities.WRENCH_EMPTY) &&
                   !stack.canPerformAction(MekanismItemAbilities.WRENCH_CONFIGURE);
        }
        return false;
    }

    @NotNull
    public static String getLastKnownUsername(@Nullable UUID uuid) {
        if (uuid == null) {
            return "<???>";
        }
        String ret = UsernameCache.getLastKnownUsername(uuid);
        if (ret == null && !warnedFails.contains(uuid) && EffectiveSide.get().isServer()) { // see if MC/Yggdrasil knows about it?!
            Optional<NameAndId> nameToIdCache = ServerLifecycleHooks.getCurrentServer().services().nameToIdCache().get(uuid);
            if (nameToIdCache.isPresent()) {
                ret = nameToIdCache.get().name();
            }
        }
        if (ret == null && !warnedFails.contains(uuid)) {
            Mekanism.logger.warn("Failed to retrieve username for UUID {}, you might want to add it to the JSON cache", uuid);
            warnedFails.add(uuid);
        }
        return ret == null ? "<" + uuid + ">" : ret;
    }

    /**
     * Copy of {@link MobEffectInstance#tick(LivingEntity, Runnable)}, but modified to not apply the effect to avoid extra damage and the like.
     */
    public static void speedUpEffectSafely(LivingEntity entity, MobEffectInstance effectInstance) {
        if (effectInstance.getDuration() > 0) {
            effectInstance.tickDownDuration();
            int remainingDuration = effectInstance.getDuration();
            if (remainingDuration == 0 && effectInstance.hiddenEffect != null) {
                effectInstance.setDetailsFrom(effectInstance.hiddenEffect);
                effectInstance.hiddenEffect = effectInstance.hiddenEffect.hiddenEffect;
                onChangedPotionEffect(entity, effectInstance, true);
            }
        }
    }

    /**
     * Copy of {@link LivingEntity#onEffectUpdated(MobEffectInstance, boolean, Entity)} due to not being able to AT the method as it is protected.
     */
    @SuppressWarnings("JavadocReference")
    private static void onChangedPotionEffect(LivingEntity entity, MobEffectInstance effectInstance, boolean reapply) {
        entity.effectsDirty = true;
        if (reapply && !entity.level().isClientSide()) {
            MobEffect effect = effectInstance.getEffect().value();
            effect.removeAttributeModifiers(entity.getAttributes());
            effect.addAttributeModifiers(entity.getAttributes(), effectInstance.getAmplifier());
            entity.refreshDirtyAttributes();
        }
        if (!entity.level().isClientSide()) {
            entity.sendEffectToPassengers(effectInstance);
        }
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(entity.getId(), effectInstance, false));
            CriteriaTriggers.EFFECTS_CHANGED.trigger(player, null);
        }
    }

    public static boolean isSameTypeFactory(Holder<Block> block, Block factoryBlockType) {
        AttributeFactoryType attribute = Attribute.get(block, AttributeFactoryType.class);
        if (attribute != null) {
            AttributeFactoryType otherType = Attribute.get(factoryBlockType, AttributeFactoryType.class);
            return otherType != null && attribute.getFactoryType() == otherType.getFactoryType();
        }
        return false;
    }

    @FunctionalInterface
    public interface ModifyPlayerBounding {

        AABB modify(AABB bounding, double data);
    }

    /**
     * Similar in concept to {@link net.minecraft.world.entity.Entity#updateFluidHeightAndDoFluidPushing()} except calculates if a given portion of the player is in the
     * fluids.
     */
    public static Map<FluidType, FluidInDetails> getFluidsIn(Player player, double data, ModifyPlayerBounding modifyBoundingBox) {
        AABB bb = modifyBoundingBox.modify(player.getBoundingBox().deflate(0.001), data);
        int xMin = Mth.floor(bb.minX);
        int xMax = Mth.ceil(bb.maxX);
        int yMin = Mth.floor(bb.minY);
        int yMax = Mth.ceil(bb.maxY);
        int zMin = Mth.floor(bb.minZ);
        int zMax = Mth.ceil(bb.maxZ);
        if (!player.level().hasChunksAt(xMin, yMin, zMin, xMax, yMax, zMax)) {
            //If the position isn't actually loaded, just return there isn't any fluids
            return Collections.emptyMap();
        }
        Map<FluidType, FluidInDetails> fluidsIn = new IdentityHashMap<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = xMin; x < xMax; ++x) {
            for (int y = yMin; y < yMax; ++y) {
                for (int z = zMin; z < zMax; ++z) {
                    mutablePos.set(x, y, z);
                    FluidState fluidState = player.level().getFluidState(mutablePos);
                    if (!fluidState.isEmpty()) {
                        double fluidY = y + fluidState.getHeight(player.level(), mutablePos);
                        if (bb.minY <= fluidY) {
                            //The fluid intersects the bounding box
                            FluidInDetails details = fluidsIn.computeIfAbsent(fluidState.getFluidType(), f -> new FluidInDetails());
                            details.positions.put(mutablePos.immutable(), fluidState);
                            double actualFluidHeight;
                            if (fluidY > bb.maxY) {
                                //Fluid goes past the top of the bounding box, limit it to the top
                                // We do the max of the bottom of the bounding box and our current block so that
                                // if we are floating above the bottom we don't take the area below us into account
                                actualFluidHeight = bb.maxY - Math.max(bb.minY, y);
                            } else {
                                // We do the max of the bottom of the bounding box and our current block so that
                                // if we are floating above the bottom we don't take the area below us into account
                                actualFluidHeight = fluidY - Math.max(bb.minY, y);
                            }
                            details.heights.merge(ChunkPos.pack(x, z), actualFluidHeight, Double::sum);
                        }
                    }
                }
            }
        }
        return fluidsIn;
    }

    public static void veinMineArea(IEnergyContainer energyContainer, long energyRequired, long baseBlastEnergy, long baseVeinEnergy,
          Level world, BlockPos pos, ServerPlayer player, ItemStack stack, Item usedTool, Object2IntMap<BlockPos> found, BlastEnergyFunction blastEnergy,
          VeinEnergyFunction veinEnergy) {
        long energyUsed = 0L;
        long energyAvailable = energyContainer.getEnergy();
        //Subtract from our available energy the amount that we will require to break the target block
        energyAvailable = energyAvailable - energyRequired;
        Stat<Item> itemStat = Stats.ITEM_USED.get(usedTool);
        for (ObjectIterator<Object2IntMap.Entry<BlockPos>> iterator = Object2IntMaps.fastIterator(found); iterator.hasNext(); ) {
            Object2IntMap.Entry<BlockPos> foundEntry = iterator.next();
            BlockPos foundPos = foundEntry.getKey();
            if (pos.equals(foundPos)) {
                continue;
            }
            BlockState targetState = world.getBlockState(foundPos);
            if (targetState.isAir()) {
                continue;
            }
            float hardness = targetState.getDestroySpeed(world, foundPos);
            if (hardness == Block.INDESTRUCTIBLE) {
                continue;
            }
            int distance = foundEntry.getIntValue();
            long destroyEnergy = distance == 0 ? blastEnergy.calc(baseBlastEnergy, hardness) : veinEnergy.calc(baseVeinEnergy, hardness, distance, targetState);
            if (energyUsed + destroyEnergy >= energyAvailable) {
                //If we don't have energy to break the block continue
                //Note: We do not break as given the energy scales with hardness, so it is possible we still have energy to break another block
                // Given we validate the blocks are the same but their block states may be different thus making them have different
                // block hardness values in a modded context
                continue;
            }
            //TODO - 26.1: Check about if we need to fire this on the client as well, or maybe just default mark it as notifying the client?
            BreakBlockEvent event = CommonHooks.fireBlockBreak(world, player.gameMode.getGameModeForPlayer(), player, foundPos, targetState);
            if (event.isCanceled()) {
                //If we can't actually break the block continue (this allows mods to stop us from vein mining into protected land)
                continue;
            }
            //Otherwise, break the block
            FluidState fluidState = targetState.getFluidState();
            //Get the tile now so that we have it for when we try to harvest the block
            BlockEntity tileEntity = WorldUtils.getTileEntity(world, foundPos);
            //Update what the state will be if the player is destroying it, so that things like angering piglins, and firing block destroy game events occur
            // This also ensures that things like decorated pots are able to properly update to cracked and drop sherds rather than the pot block itself
            targetState = targetState.getBlock().playerWillDestroy(world, foundPos, targetState, player);
            Block block = targetState.getBlock();
            //Remove the block
            if (targetState.onDestroyedByPlayer(world, foundPos, player, stack, true, fluidState)) {
                block.destroy(world, foundPos, targetState);
                //Harvest the block allowing it to handle block drops, incrementing block mined count, and adding exhaustion
                block.playerDestroy(world, player, foundPos, targetState, tileEntity, stack);
                player.awardStat(itemStat);
                //Mark that we used that portion of the energy
                energyUsed += destroyEnergy;
            }
        }
        energyContainer.extract(energyUsed, Action.EXECUTE, AutomationType.MANUAL);
    }

    public enum ResourceType {
        GUI("gui"),
        GUI_BUTTON("gui/button"),
        GUI_BAR("gui/bar"),
        GUI_GAUGE("gui/gauge"),
        GUI_HUD("gui/hud"),
        GUI_ICONS("gui/icons"),
        GUI_MODE("gui/mode"),
        GUI_PROGRESS("gui/progress"),
        GUI_RADIAL("gui/radial"),
        GUI_SLOT("gui/slot"),
        GUI_TAB("gui/tabs"),
        SOUND("sound"),
        RENDER("render"),
        TEXTURE_BLOCKS("textures/block"),
        TEXTURE_ITEMS("textures/item"),
        MODEL("models"),
        INFUSE("infuse"),
        PIGMENT("pigment"),
        SLURRY("slurry");

        private final String prefix;

        ResourceType(String s) {
            prefix = s;
        }

        public String getPrefix() {
            return prefix + "/";
        }
    }

    public static class FluidInDetails {

        private final Map<BlockPos, FluidState> positions = new HashMap<>();
        private final Long2DoubleMap heights = new Long2DoubleArrayMap();

        public Map<BlockPos, FluidState> getPositions() {
            return positions;
        }

        public double getMaxHeight() {
            return heights.values().doubleStream().max().orElse(0);
        }
    }

    @FunctionalInterface
    public interface BlastEnergyFunction {

        long calc(long baseBlastEnergy, float hardness);
    }

    @FunctionalInterface
    public interface VeinEnergyFunction {

        long calc(long baseVeinEnergy, float hardness, int distance, BlockState state);
    }
}