package mekanism.common.lib.transmitter;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.text.IHasTranslationKey;
import mekanism.api.text.ILangEntry;
import mekanism.common.MekanismLang;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

@NothingNullByDefault
public enum TransmissionType implements IHasTranslationKey, StringRepresentable {
    ENERGY("EnergyNetwork", "energy", MekanismLang.TRANSMISSION_TYPE_ENERGY),
    FLUID("FluidNetwork", "fluids", MekanismLang.TRANSMISSION_TYPE_FLUID),
    CHEMICAL("ChemicalNetwork", "chemicals", MekanismLang.TRANSMISSION_TYPE_CHEMICALS),
    ITEM("InventoryNetwork", "items", MekanismLang.TRANSMISSION_TYPE_ITEM),
    HEAT("HeatNetwork", "heat", MekanismLang.TRANSMISSION_TYPE_HEAT);

    public static final Codec<TransmissionType> CODEC = StringRepresentable.fromEnum(TransmissionType::values);
    public static final IntFunction<TransmissionType> BY_ID = ByIdMap.continuous(TransmissionType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, TransmissionType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, TransmissionType::ordinal);

    private final String name;
    private final String transmission;
    private final ILangEntry langEntry;

    TransmissionType(String name, String transmission, ILangEntry langEntry) {
        this.name = name;
        this.transmission = transmission;
        this.langEntry = langEntry;
    }

    public String getName() {
        return name;
    }

    public String getTransmission() {
        return transmission;
    }

    public ILangEntry getLangEntry() {
        return langEntry;
    }

    @Override
    public String getTranslationKey() {
        return langEntry.getTranslationKey();
    }

    @Override
    public String getSerializedName() {
        return transmission;
    }
}