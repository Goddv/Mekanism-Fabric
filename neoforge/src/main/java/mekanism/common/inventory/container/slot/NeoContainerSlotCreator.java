package mekanism.common.inventory.container.slot;

import java.util.function.Consumer;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.warning.ISupportsWarning;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge {@link IContainerSlotCreator}: builds the real {@link InventoryContainerSlot} (unchanged behavior).
 */
public class NeoContainerSlotCreator implements IContainerSlotCreator {

    @Override
    public Slot create(BasicInventorySlot slot, int x, int y, ContainerSlotType slotType, @Nullable SlotOverlay slotOverlay,
          @Nullable Consumer<ISupportsWarning<?>> warningAdder, Consumer<ItemStack> uncheckedSetter) {
        return new InventoryContainerSlot(slot, x, y, slotType, slotOverlay, warningAdder, uncheckedSetter);
    }
}
