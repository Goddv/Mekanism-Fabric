package mekanism.common.network.to_client.container.property;

import io.netty.buffer.ByteBuf;
import mekanism.common.inventory.container.IPropertyDataReceiver;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class BooleanPropertyData extends PropertyData {

    public static final StreamCodec<ByteBuf, BooleanPropertyData> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.SHORT, PropertyData::getProperty,
          ByteBufCodecs.BOOL, data -> data.value,
          BooleanPropertyData::new
    );

    private final boolean value;

    public BooleanPropertyData(short property, boolean value) {
        super(PropertyType.BOOLEAN, property);
        this.value = value;
    }

    @Override
    public void handleWindowProperty(IPropertyDataReceiver receiver) {
        receiver.handleWindowProperty(getProperty(), value);
    }
}