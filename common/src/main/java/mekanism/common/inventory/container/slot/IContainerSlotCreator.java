package mekanism.common.inventory.container.slot;

import java.util.function.Consumer;
import mekanism.api.MekanismAPIBase;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.warning.ISupportsWarning;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-specific factory for the GUI container-slot a {@link BasicInventorySlot} exposes via {@code createContainerSlot()}.
 * The real {@code InventoryContainerSlot} transitively pulls in the container-window + config subsystems
 * ({@code SelectedWindowData} &rarr; {@code MekanismConfig}), which are NeoForge-coupled and not yet hoisted, so the
 * hoisted {@code :common} slot routes creation through this service: NeoForge builds the real
 * {@code InventoryContainerSlot}; Fabric returns {@code null} until the GUI/container framework is ported (the menu is
 * built directly for now). Resolved via {@link MekanismAPIBase#getService}.
 */
@Internal
public interface IContainerSlotCreator {

    IContainerSlotCreator INSTANCE = MekanismAPIBase.getService(IContainerSlotCreator.class);

    @Nullable
    Slot create(BasicInventorySlot slot, int x, int y, ContainerSlotType slotType, @Nullable SlotOverlay slotOverlay,
          @Nullable Consumer<ISupportsWarning<?>> warningAdder, Consumer<ItemStack> uncheckedSetter);
}
