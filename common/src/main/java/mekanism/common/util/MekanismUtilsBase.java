package mekanism.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import mekanism.api.MekanismAPITags;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MekanismUtilsBase {

    public static final float ONE_OVER_ROOT_TWO = 1 / Mth.SQRT_OF_TWO;
    //TODO - 1.21: Re-evaluate uses of this and the shared constants constant as some potentially should be switched to use the level's tickrate
    // and in fact the first pass was spent just trying to convert use cases to referencing constants rather than also figuring out if they should
    // be transitioned over to the level's tickrate
    public static final int TICKS_PER_HALF_SECOND = SharedConstants.TICKS_PER_SECOND / 2;

    MekanismUtilsBase() {
    }

    public static Component logFormat(Object message) {
        return logFormat(EnumColor.GRAY, message);
    }

    public static Component logFormat(EnumColor messageColor, Object message) {
        return MekanismLang.LOG_FORMAT.translateColored(EnumColor.DARK_BLUE, MekanismLang.MEKANISM, messageColor, message);
    }

    public static boolean isTickingNormally(@Nullable Level level) {
        //Same as Minecraft#isLevelRunningNormally
        return level == null || level.tickRateManager().runsNormally();
    }

    public static boolean isRightArm(LivingEntity entity, InteractionHand hand) {
        return (entity.getMainArm() == HumanoidArm.RIGHT) == (hand == InteractionHand.MAIN_HAND);
    }

    /**
     * Gets the left side of a certain orientation.
     *
     * @param orientation Current orientation of the machine
     *
     * @return left side
     */
    public static Direction getLeft(Direction orientation) {
        return orientation.getClockWise();
    }

    /**
     * Gets the right side of a certain orientation.
     *
     * @param orientation Current orientation of the machine
     *
     * @return right side
     */
    public static Direction getRight(Direction orientation) {
        return orientation.getCounterClockWise();
    }

    public static Direction rotate(Direction orientation, boolean supportY) {
        if (supportY) {
            return switch (orientation) {
                case UP -> Direction.NORTH;
                case DOWN -> Direction.SOUTH;
                case NORTH -> Direction.EAST;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.UP;
                case EAST -> Direction.DOWN;
            };
        }
        return orientation.getClockWise();
    }

    public static float getScale(float prevScale, IChemicalTank tank) {
        return getScale(prevScale, tank.getStored(), tank.getCapacity(), tank.isEmpty());
    }

    public static float getScale(float prevScale, int stored, int capacity, boolean empty) {
        return getScale(prevScale, capacity == 0 ? 0 : stored / (float) capacity, empty, stored == capacity);
    }

    public static float getScale(float prevScale, long stored, long capacity, boolean empty) {
        return getScale(prevScale, capacity == 0 ? 0 : (float) (stored / (double) capacity), empty, stored == capacity);
    }

    public static float getScale(float prevScale, IEnergyContainer container) {
        float targetScale;
        long stored = container.getEnergy();
        long capacity = container.getMaxEnergy();
        if (capacity == 0L) {
            targetScale = 0;
        } else {
            targetScale = (float) ((double) stored / capacity);
        }
        return getScale(prevScale, targetScale, container.isEmpty(), stored == capacity);
    }

    public static float getScale(float prevScale, float targetScale, boolean empty, boolean full) {
        float difference = Math.abs(prevScale - targetScale);
        if (difference > 0.01) {
            return (9 * prevScale + targetScale) / 10;
        } else if (!empty && full && difference > 0) {
            //If we are full but our difference is less than 0.01, but we want to get our scale all the way up to the target
            // instead of leaving it at a value just under. Note: We also check that we are not empty as we technically may
            // be both empty and full if the current capacity is zero
            return targetScale;
        } else if (!empty && prevScale == 0) {
            //If we have any contents make sure we end up rendering it
            return targetScale;
        }
        if (empty && prevScale < 0.01) {
            //If we are empty and have a very small amount just round it down to empty
            return 0;
        }
        return prevScale;
    }

    public static boolean scaleChanged(float scale, float prevScale) {
        if (Mth.equal(scale, prevScale)) {
            //If we max out our scale bounds, force an update regardless
            return scale != prevScale && scale == 0 || scale == 1 || prevScale == 1 || prevScale == 0;
        }
        return true;
    }

    public static boolean isLiquidBlock(Block block) {
        //Treat bubble columns as liquids
        return block instanceof LiquidBlock || block instanceof BubbleColumnBlock;
    }

    /**
     * Ray-traces what block a player is looking at.
     *
     * @param player - player to raytrace
     *
     * @return raytraced value
     */
    public static BlockHitResult rayTrace(Player player) {
        return rayTrace(player, ClipContext.Fluid.NONE);
    }

    public static BlockHitResult rayTrace(Player player, ClipContext.Fluid fluidMode) {
        return rayTrace(player, player.blockInteractionRange(), fluidMode);
    }

    public static BlockHitResult rayTrace(Player player, double reach) {
        return rayTrace(player, reach, ClipContext.Fluid.NONE);
    }

    public static BlockHitResult rayTrace(Player player, double reach, ClipContext.Fluid fluidMode) {
        Vec3 headVec = getHeadVec(player);
        Vec3 lookVec = player.getViewVector(1);
        Vec3 endVec = headVec.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
        return player.level().clip(new ClipContext(headVec, endVec, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    /**
     * Gets the head vector of a player for a ray trace.
     *
     * @param player - player to check
     *
     * @return head location
     */
    private static Vec3 getHeadVec(Player player) {
        double posY = player.getY() + player.getEyeHeight();
        if (player.isCrouching()) {
            posY -= 0.08;
        }
        return new Vec3(player.getX(), posY, player.getZ());
    }

    public static long calculateUsage(long capacity) {
        return Math.max((long) (0.005 * capacity), 1);
    }

    /**
     * Converts a list of slots into a simple crafting input.
     *
     * @param resize True to clamp the stacks to a size of one.
     */
    public static CraftingInput.Positioned getCraftingInput(int width, int height, List<ItemStack> slots, boolean resize) {
        if (width * height != slots.size()) {
            throw new IllegalStateException("Expected there to be a slot for every index in a " + width + " by " + height + " grid.");
        }
        List<ItemStack> stacks = new ArrayList<>(slots.size());
        for (ItemStack slot : slots) {
            //Note: copyWithCount returns EMPTY if the stack is empty, so we can skip checking
            if (resize) {
                stacks.add(slot.copyWithCount(1));
            } else {
                stacks.add(slot.copy());
            }
        }
        return CraftingInput.ofPositioned(width, height, stacks);
    }

    /**
     * Converts a list of slots into a simple crafting input.
     *
     * @param resize True to clamp the stacks to a size of one.
     */
    public static CraftingInput.Positioned getCraftingInputSlots(int width, int height, List<IInventorySlot> slots, boolean resize) {
        if (width * height != slots.size()) {
            throw new IllegalStateException("Expected there to be a slot for every index in a " + width + " by " + height + " grid.");
        }
        List<ItemStack> stacks = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            ItemStack stack = slot.getStack();
            //Note: copyWithCount returns EMPTY if the stack is empty, so we can skip checking
            if (resize) {
                stacks.add(stack.copyWithCount(1));
            } else {
                stacks.add(stack.copy());
            }
        }
        return CraftingInput.ofPositioned(width, height, stacks);
    }

    public static boolean shouldSpeedUpEffect(MobEffectInstance effectInstance) {
        //Only allow speeding up effects that can be sped up by milk. Also validate it isn't blacklisted by the modpack
        //TODO - 26.1 milk now cures all effects, do we need to change anything?
        return !effectInstance.getEffect().is(MekanismAPITags.MobEffects.SPEED_UP_BLACKLIST);
    }

    /**
     * Performs a set of actions, until we find a success or run out of actions.
     *
     * @implNote Only returns that we failed if all the tested actions failed.
     */
    @SafeVarargs
    public static InteractionResult performActions(InteractionResult firstAction, Supplier<InteractionResult>... secondaryActions) {
        if (firstAction.consumesAction()) {
            return firstAction;
        }
        InteractionResult result = firstAction;
        boolean hasFailed = result == InteractionResult.FAIL;
        for (Supplier<InteractionResult> secondaryAction : secondaryActions) {
            result = secondaryAction.get();
            if (result.consumesAction()) {
                //If we were successful
                return result;
            }
            hasFailed &= result == InteractionResult.FAIL;
        }
        if (hasFailed) {
            //If at least one step failed, consider ourselves unsuccessful
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /**
     * @param amount   Amount currently stored
     * @param capacity Total amount that can be stored.
     *
     * @return A redstone level based on the percentage of the amount stored.
     */
    public static int redstoneLevelFromContents(long amount, long capacity) {
        double fractionFull = capacity == 0 ? 0 : ((double) amount / capacity);
        return Mth.lerpDiscrete((float) fractionFull, Redstone.SIGNAL_NONE, Redstone.SIGNAL_MAX);
    }

    /**
     * Calculates the redstone level based on the percentage of amount stored. Like {@code ItemHandlerHelper#calcRedstoneFromInventory(IItemHandler)} except without
     * limiting slots to the max stack size of the item to allow for better support for bins
     *
     * @return A redstone level based on the percentage of the amount stored.
     */
    public static int redstoneLevelFromContents(List<IInventorySlot> slots) {
        long totalCount = 0;
        long totalLimit = 0;
        for (IInventorySlot slot : slots) {
            if (slot.isEmpty()) {
                totalLimit += slot.getLimit(ItemStack.EMPTY);
            } else {
                totalCount += slot.getCount();
                totalLimit += slot.getLimit(slot.getStack());
            }
        }
        return redstoneLevelFromContents(totalCount, totalLimit);
    }

    /**
     * Checks whether the player is in creative or spectator mode.
     *
     * @param player the player to check.
     *
     * @return true if the player is neither in creative mode, nor in spectator mode.
     */
    public static boolean isPlayingMode(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * Helper to read the parameter names from the format saved by our annotation processor param name mapper.
     */
    public static List<String> getParameterNames(@Nullable JsonObject classMethods, String method, String signature) {
        if (classMethods != null) {
            JsonObject signatures = classMethods.getAsJsonObject(method);
            if (signatures != null) {
                JsonElement params = signatures.get(signature);
                if (params != null) {
                    if (params.isJsonArray()) {
                        JsonArray paramArray = params.getAsJsonArray();
                        List<String> paramNames = new ArrayList<>(paramArray.size());
                        for (JsonElement param : paramArray) {
                            paramNames.add(param.getAsString());
                        }
                        return Collections.unmodifiableList(paramNames);
                    }
                    return Collections.singletonList(params.getAsString());
                }
            }
        }
        return Collections.emptyList();
    }

    public static Iterable<ItemStack> getArmorSlots(LivingEntity livingEntity) {
        return Arrays.asList(
              livingEntity.getItemBySlot(EquipmentSlot.HEAD),
              livingEntity.getItemBySlot(EquipmentSlot.BODY),//animals
              livingEntity.getItemBySlot(EquipmentSlot.CHEST),
              livingEntity.getItemBySlot(EquipmentSlot.LEGS),
              livingEntity.getItemBySlot(EquipmentSlot.FEET)
        );
    }
}
