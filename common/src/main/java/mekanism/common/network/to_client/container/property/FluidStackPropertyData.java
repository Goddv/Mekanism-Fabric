package mekanism.common.network.to_client.container.property;

import mekanism.api.fluid.IFluidStack;
import mekanism.api.fluid.IFluidStackProvider;
import mekanism.common.inventory.container.IPropertyDataReceiver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class FluidStackPropertyData extends PropertyData {

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackPropertyData> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.SHORT, PropertyData::getProperty,
          IFluidStackProvider.INSTANCE.optionalStreamCodec(), data -> data.value,
          FluidStackPropertyData::new
    );

    @NotNull
    private final IFluidStack value;

    public FluidStackPropertyData(short property, @NotNull IFluidStack value) {
        super(PropertyType.FLUID_STACK, property);
        this.value = value;
    }

    @Override
    public void handleWindowProperty(IPropertyDataReceiver receiver) {
        receiver.handleWindowProperty(getProperty(), value);
    }
}
