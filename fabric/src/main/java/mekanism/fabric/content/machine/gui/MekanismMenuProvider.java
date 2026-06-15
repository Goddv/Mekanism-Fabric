package mekanism.fabric.content.machine.gui;

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
 * Transitional Fabric bring-up: a small {@link ExtendedMenuProvider} that opens the generic {@link MekanismMachineMenu}
 * for a given block-entity. Each chemical / dual-item machine + generator BE returns one of these from its
 * {@code createMenu}-equivalent (and the block's use handler calls {@link
 * dev.architectury.registry.menu.MenuRegistry#openExtendedMenu}). Writes the {@link MachineGuiType} ordinal into the open
 * packet so the client builds the matching layout. Keeps the BEs free of menu/networking boilerplate.
 */
public final class MekanismMenuProvider implements ExtendedMenuProvider {

    private final Component title;
    private final Container machine;
    private final ContainerData data;
    private final MachineGuiType guiType;

    public MekanismMenuProvider(Component title, Container machine, ContainerData data, MachineGuiType guiType) {
        this.title = title;
        this.machine = machine;
        this.data = data;
        this.guiType = guiType;
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeVarInt(guiType.ordinal());
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MekanismMachineMenu(containerId, playerInventory, machine, data, guiType);
    }
}
