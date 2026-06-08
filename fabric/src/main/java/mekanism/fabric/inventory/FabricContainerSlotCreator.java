package mekanism.fabric.inventory;

import java.util.function.Consumer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.IContainerSlotCreator;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.warning.ISupportsWarning;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric {@link IContainerSlotCreator}: the real Mekanism container/GUI framework (InventoryContainerSlot &rarr;
 * SelectedWindowData &rarr; MekanismConfig) is not yet ported to Fabric, so this returns {@code null} for now (the
 * transitional Fabric machine menu builds its slots directly). Replaced when the container/GUI subsystem is hoisted.
 */
public class FabricContainerSlotCreator implements IContainerSlotCreator {

    @Override
    @Nullable
    public Slot create(BasicInventorySlot slot, int x, int y, ContainerSlotType slotType, @Nullable SlotOverlay slotOverlay,
          @Nullable Consumer<ISupportsWarning<?>> warningAdder, Consumer<ItemStack> uncheckedSetter) {
        return null;
    }
}
