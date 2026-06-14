package mekanism.api.recipes.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Loader-neutral, pure Mojang-DFU re-implementation of the single {@code NeoForgeExtraCodecs} helper that hoisted
 * chemical-ingredient code depends on ({@link #aliasedFieldOf} via {@code CompoundChemicalIngredient.CODEC}).
 *
 * <p><b>NeoForge byte-identity:</b> the bodies of {@link #aliasedFieldOf(Codec, String...)} and its helper
 * {@link #mapWithAlternative(MapCodec, MapCodec)} are reproduced <i>verbatim</i> from
 * {@code net.neoforged.neoforge.common.util.NeoForgeExtraCodecs} (NeoForge 26.1.2.73, file
 * {@code src/main/java/net/neoforged/neoforge/common/util/NeoForgeExtraCodecs.java}). Because the hoisted
 * {@code CompoundChemicalIngredient.CODEC} now routes through this shim on <i>both</i> loaders, the NeoForge recipe
 * JSON / network-sync wire format must remain identical: encode writes only the first name ({@code "children"}); decode
 * accepts the first name <i>or</i> any alias name ({@code "ingredients"}).
 *
 * <p>Only {@code aliasedFieldOf} + {@code mapWithAlternative} are mirrored here. The heavy dispatch helpers
 * ({@code xor}, {@code dispatchMapOrElse}) used by the NeoForge {@code ChemicalIngredientCreator} impl stay on NeoForge
 * and are not part of {@code :common}.
 */
@Internal
public final class MekanismExtraCodecs {

    private MekanismExtraCodecs() {
    }

    /**
     * Verbatim re-implementation of {@code NeoForgeExtraCodecs.aliasedFieldOf}. Reads/writes the first name as the
     * canonical field and additionally accepts each subsequent name as a decode-only alias.
     */
    public static <T> MapCodec<T> aliasedFieldOf(final Codec<T> codec, final String... names) {
        if (names.length == 0)
            throw new IllegalArgumentException("Must have at least one name!");
        MapCodec<T> mapCodec = codec.fieldOf(names[0]);
        for (int i = 1; i < names.length; i++)
            mapCodec = mapWithAlternative(mapCodec, codec.fieldOf(names[i]));
        return mapCodec;
    }

    /**
     * Verbatim re-implementation of {@code NeoForgeExtraCodecs.mapWithAlternative}. Tries the first codec then the
     * second codec for decoding, <b>but only the first for encoding</b>.
     */
    public static <T> MapCodec<T> mapWithAlternative(final MapCodec<T> mapCodec, final MapCodec<? extends T> alternative) {
        return Codec.mapEither(mapCodec, alternative).xmap(either -> either.map(Function.identity(), Function.identity()), Either::left);
    }
}
