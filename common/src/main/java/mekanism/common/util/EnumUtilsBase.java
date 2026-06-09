package mekanism.common.util;

import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.api.tier.BaseTier;
import mekanism.common.entity.RobitPrideSkinData;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tier.FactoryTier;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;

public class EnumUtilsBase {

    EnumUtilsBase() {
    }

    /**
     * Cached collection of armor slot positions from EquipmentSlotType. DO NOT MODIFY THIS LIST.
     */
    public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * Cached collection of hand slot positions from EquipmentSlotType. DO NOT MODIFY THIS LIST.
     */
    public static final EquipmentSlot[] HAND_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    /**
     * Cached value of {@link Direction#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Cached value of the horizontal directions. DO NOT MODIFY THIS LIST.
     *
     * @implNote Index is ordinal() - 2, as the first two elements of {@link Direction} are {@link Direction#DOWN} and {@link Direction#UP}
     */
    public static final Direction[] HORIZONTAL_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * Cached value of {@link RelativeSide#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final RelativeSide[] SIDES = RelativeSide.values();

    /**
     * Cached value of {@link TransmissionType#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final TransmissionType[] TRANSMISSION_TYPES = TransmissionType.values();

    /**
     * Cached value of {@link BaseTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final BaseTier[] TIERS = BaseTier.values();

    /**
     * Cached value of {@link FactoryTier#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final FactoryTier[] FACTORY_TIERS = FactoryTier.values();

    /**
     * Cached value of {@link Upgrade#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final Upgrade[] UPGRADES = Upgrade.values();

    /**
     * Cached value of {@link EquipmentSlot#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final EquipmentSlot[] EQUIPMENT_SLOT_TYPES = EquipmentSlot.values();

    /**
     * Cached value of {@link EnumColor#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final EnumColor[] COLORS = EnumColor.values();

    /**
     * Cached value of {@link RobitPrideSkinData#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final RobitPrideSkinData[] PRIDE_SKINS = RobitPrideSkinData.values();

    /**
     * Cached value of {@link ArmorType#values()}. DO NOT MODIFY THIS LIST.
     */
    public static final ArmorType[] ARMOR_TYPES = ArmorType.values();
}
