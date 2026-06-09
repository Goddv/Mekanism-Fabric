package mekanism.common.attachments.containers.fluid;

import mekanism.api.AutomationType;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.fluid.ExtendedFluidHandlerUtils;
import mekanism.api.fluid.FluidActions;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.fluid.IFluidStack;
import mekanism.common.attachments.containers.ComponentBackedHandler;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.fluid.NeoFluidStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/**
 * Item-stack fluid capability for attached fluids. Exposes the NeoForge {@link IFluidHandlerItem} surface (the
 * {@code Capabilities.FLUID} item cap) while internally managing Mekanism's loader-neutral {@link IExtendedFluidTank}s.
 * The {@link IFluidStack}&harr;{@link FluidStack} conversion happens here via {@link NeoFluidStack}.
 */
@NothingNullByDefault
public class ComponentBackedFluidHandler extends ComponentBackedHandler<FluidStack, IExtendedFluidTank, AttachedFluids> implements IFluidHandlerItem {

    public ComponentBackedFluidHandler(ItemStack attachedTo, int totalTanks) {
        super(attachedTo, totalTanks);
    }

    @Override
    protected ContainerType<IExtendedFluidTank, AttachedFluids, ?> containerType() {
        return ContainerType.FLUID;
    }

    @Override
    public int getTanks() {
        return size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return getContents(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        IExtendedFluidTank fluidTank = getContainer(tank);
        return fluidTank == null ? 0 : fluidTank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        IExtendedFluidTank fluidTank = getContainer(tank);
        return fluidTank != null && fluidTank.isFluidValid(NeoFluidStack.wrap(stack));
    }

    @Override
    public int fill(FluidStack stack, FluidAction action) {
        //Note: matches the legacy default IExtendedFluidHandler#fill which distributed via the null (internal) side
        IFluidStack toInsert = NeoFluidStack.wrap(stack);
        IFluidStack remainder = ExtendedFluidHandlerUtils.insert(toInsert, FluidActions.from(action), AutomationType.handler(null), size(), this);
        return (int) Math.min(toInsert.getAmount() - remainder.getAmount(), Integer.MAX_VALUE);
    }

    @Override
    public FluidStack drain(FluidStack stack, FluidAction action) {
        return NeoFluidStack.unwrap(ExtendedFluidHandlerUtils.extract(NeoFluidStack.wrap(stack), FluidActions.from(action), AutomationType.handler(null), size(), this));
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        return NeoFluidStack.unwrap(ExtendedFluidHandlerUtils.extract((long) amount, FluidActions.from(action), AutomationType.handler(null), size(), this));
    }

    @Override
    public ItemStack getContainer() {
        return attachedTo;
    }
}
