package mekanism.fabric.content.machine.factory;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import org.jetbrains.annotations.Nullable;

/**
 * Transitional Fabric bring-up: an {@link ExtendedMenuProvider} that opens the {@link FactoryMenu} for a given
 * {@link FactoryBlockEntity}. Writes the {@link FactoryType} ordinal + the process count into the open packet so the client
 * derives the same {@link FactorySlotLayout}. Returned by the factory block's use handler (via
 * {@link dev.architectury.registry.menu.MenuRegistry#openExtendedMenu}).
 */
public final class FactoryMenuProvider implements ExtendedMenuProvider {

    private final Component title;
    private final Container machine;
    private final ContainerData data;
    private final FactoryType factoryType;
    private final int processes;

    public FactoryMenuProvider(Component title, Container machine, ContainerData data, FactoryType factoryType, int processes) {
        this.title = title;
        this.machine = machine;
        this.data = data;
        this.factoryType = factoryType;
        this.processes = processes;
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeVarInt(factoryType.ordinal());
        buf.writeVarInt(processes);
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FactoryMenu(containerId, playerInventory, machine, data, factoryType, processes);
    }
}
