package mekanism.common.block.attribute;

import java.util.function.Supplier;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.states.BlockStateHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AttributeUpgradeable implements Attribute {

    private final Supplier<? extends Holder<Block>> upgradeBlock;

    public AttributeUpgradeable(Supplier<? extends Holder<Block>> upgradeBlock) {
        this.upgradeBlock = upgradeBlock;
    }

    @NotNull
    public BlockState upgradeResult(@NotNull BlockState current, @NotNull BaseTier tier) {
        return BlockStateHelper.copyStateData(current, upgradeBlock.get());
    }
}
