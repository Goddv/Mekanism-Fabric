package mekanism.common.attachments.containers.creator;

import mekanism.common.attachments.containers.ContainerType;
import net.minecraft.world.item.ItemStack;
import mekanism.api.IValueIOSerializable;

@FunctionalInterface
public interface IBasicContainerCreator<CONTAINER extends IValueIOSerializable> {

    CONTAINER create(ContainerType<? super CONTAINER, ?, ?> containerType, ItemStack attachedTo, int containerIndex);
}