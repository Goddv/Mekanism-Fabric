package mekanism.common.tile.interfaces;

import mekanism.common.util.MekanismUtilsBase;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

public interface ITileDirectional {

    default boolean isDirectional() {
        return true;
    }

    void setFacing(@NotNull Direction direction);

    @NotNull
    Direction getDirection();

    @NotNull
    default Direction getOppositeDirection() {
        return getDirection().getOpposite();
    }

    @NotNull
    default Direction getRightSide() {
        return MekanismUtilsBase.getRight(getDirection());
    }

    @NotNull
    default Direction getLeftSide() {
        return MekanismUtilsBase.getLeft(getDirection());
    }
}