package mekanism.common.attachments.containers.creator;

import mekanism.common.attachments.containers.IAttachedContainers;
import net.minecraft.nbt.CompoundTag;
import mekanism.api.IValueIOSerializable;

public interface IContainerCreator<CONTAINER extends IValueIOSerializable, ATTACHED extends IAttachedContainers<?, ATTACHED>> extends IBasicContainerCreator<CONTAINER> {

    int totalContainers();

    ATTACHED initStorage(int containers);
}