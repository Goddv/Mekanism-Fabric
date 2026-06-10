package mekanism.common.block.basic;

import mekanism.common.block.BlockMekanismBase;
import mekanism.common.resource.BlockResourceInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BlockResource extends BlockMekanismBase {

    @NotNull
    private final BlockResourceInfo resource;

    //TODO: Isn't as "generic"? So make it be from one BlockType thing?
    public BlockResource(BlockBehaviour.Properties properties, @NotNull BlockResourceInfo resource) {
        super(resource.modifyProperties(properties.requiresCorrectToolForDrops()));
        this.resource = resource;
    }

    @NotNull
    public BlockResourceInfo getResourceInfo() {
        return resource;
    }

    // NeoForge runtime override of IBlockExtension#isPortalFrame(BlockState, BlockGetter, BlockPos) — that method does
    // not exist on vanilla Block, so no @Override is possible here (this class is now in :common). Virtual dispatch by
    // signature still overrides the patched default on NeoForge, preserving Refined Obsidian portal frames. If NeoForge
    // ever changes that extension method's signature this silently stops overriding — re-verify against IBlockExtension
    // on NeoForge updates. (On Fabric there is no IBlockExtension, so this method is simply inert.)
    public boolean isPortalFrame(BlockState state, BlockGetter world, BlockPos pos) {
        return resource.isPortalFrame();
    }
}