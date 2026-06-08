package mekanism.common.inventory.slot;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;

/**
 * NeoForge-only: exposes a {@link BasicInventorySlot} to NeoForge's item-transfer API as an {@link ItemAccess}. Extracted
 * from {@code BasicInventorySlot} so the slot itself can be loader-neutral ({@code :common}); the slot's item-capability
 * exposure is a NeoForge concern (Fabric exposes inventories through fabric-transfer / BlockApiLookup instead).
 */
public final class SlotResourceHandler extends ItemStackResourceHandler {

    private final BasicInventorySlot slot;

    private SlotResourceHandler(BasicInventorySlot slot) {
        this.slot = slot;
    }

    /** Builds the single-slot {@link ItemAccess} previously returned by {@code BasicInventorySlot.itemAccess()}. */
    public static ItemAccess itemAccess(BasicInventorySlot slot) {
        return ItemAccess.forHandlerIndex(new SlotResourceHandler(slot), 0);
    }

    @Override
    protected ItemStack getStack() {
        return slot.getStack().copy();
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, 1);
        return slot.getStack().count();
    }

    @Override
    protected void setStack(ItemStack stack) {
        slot.setStackUnchecked(stack);
    }

    @Override
    protected boolean isValid(ItemResource resource) {
        return slot.isItemValid(resource.toStack());
    }
}
