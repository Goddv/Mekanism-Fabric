package mekanism.api.recipes.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.function.Function;
import java.util.stream.Stream;
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
 * <p>The dispatch helpers ({@link #xor(MapCodec, MapCodec)}, {@link #dispatchMapOrElse(Codec, Function, Function, MapCodec)})
 * used by the hoisted {@code ChemicalIngredientCreator} impl are also mirrored here, reproduced <i>verbatim</i> from the
 * same {@code NeoForgeExtraCodecs} source (NeoForge 26.1.2.73). Because the hoisted {@code ChemicalIngredientCreator}
 * dispatch now routes through these on <i>both</i> loaders, the encode/decode of chemical-ingredient recipe JSON and
 * network sync must remain identical — so the bodies (and the private {@code XorMapCodec} helper) are not paraphrased.
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

    /**
     * Map dispatch codec with an alternative.
     *
     * <p>The alternative will only be used if there is no {@code "type"} key in the serialized object.
     *
     * @param typeCodec     codec for the dispatch type
     * @param type          function to retrieve the dispatch type from the dispatched type
     * @param codec         function to retrieve the dispatched type map codec from the dispatch type
     * @param fallbackCodec fallback to use when the deserialized object does not have a {@code "type"} key
     * @param <A>           dispatch type
     * @param <E>           dispatched type
     * @param <B>           fallback type
     */
    public static <A, E, B> MapCodec<Either<E, B>> dispatchMapOrElse(Codec<A> typeCodec, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec, MapCodec<B> fallbackCodec) {
        return dispatchMapOrElse("type", typeCodec, type, codec, fallbackCodec);
    }

    /**
     * Map dispatch codec with an alternative.
     *
     * <p>The alternative will only be used if the provided key is not present in the serialized object.
     *
     * @param key           key to dispatch on
     * @param typeCodec     codec for the dispatch type
     * @param type          function to retrieve the dispatch type from the dispatched type
     * @param codec         function to retrieve the dispatched type map codec from the dispatch type
     * @param fallbackCodec fallback to use when the deserialized object does not have a {@code "type"} key
     * @param <A>           dispatch type
     * @param <E>           dispatched type
     * @param <B>           fallback type
     */
    public static <A, E, B> MapCodec<Either<E, B>> dispatchMapOrElse(String key, Codec<A> typeCodec, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec, MapCodec<B> fallbackCodec) {
        var dispatchCodec = typeCodec.dispatchMap(key, type, codec);
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.concat(dispatchCodec.keys(ops), fallbackCodec.keys(ops)).distinct();
            }

            @Override
            public <T> DataResult<Either<E, B>> decode(DynamicOps<T> ops, MapLike<T> input) {
                if (input.get(key) != null) {
                    return dispatchCodec.decode(ops, input).map(Either::left);
                } else {
                    return fallbackCodec.decode(ops, input).map(Either::right);
                }
            }

            @Override
            public <T> RecordBuilder<T> encode(Either<E, B> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.map(
                        dispatched -> dispatchCodec.encode(dispatched, ops, prefix),
                        fallback -> fallbackCodec.encode(fallback, ops, prefix));
            }

            @Override
            public String toString() {
                return "DispatchOrElse[" + dispatchCodec + ", " + fallbackCodec + "]";
            }
        };
    }

    /**
     * Codec that matches exactly one out of two map codecs.
     * Same as {@link Codec#xor} but for {@link MapCodec}s.
     */
    public static <F, S> MapCodec<Either<F, S>> xor(MapCodec<F> first, MapCodec<S> second) {
        return new XorMapCodec<>(first, second);
    }

    private static final class XorMapCodec<F, S> extends MapCodec<Either<F, S>> {
        private final MapCodec<F> first;
        private final MapCodec<S> second;

        private XorMapCodec(MapCodec<F> first, MapCodec<S> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.concat(first.keys(ops), second.keys(ops)).distinct();
        }

        @Override
        public <T> DataResult<Either<F, S>> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<Either<F, S>> firstResult = first.decode(ops, input).map(Either::left);
            DataResult<Either<F, S>> secondResult = second.decode(ops, input).map(Either::right);
            var firstValue = firstResult.result();
            var secondValue = secondResult.result();
            if (firstValue.isPresent() && secondValue.isPresent()) {
                return DataResult.error(
                        () -> "Both alternatives read successfully, cannot pick the correct one; first: " + firstValue.get() + " second: "
                                + secondValue.get(),
                        firstValue.get());
            } else if (firstValue.isPresent()) {
                return firstResult;
            } else if (secondValue.isPresent()) {
                return secondResult;
            } else {
                return firstResult.apply2((x, y) -> y, secondResult);
            }
        }

        @Override
        public <T> RecordBuilder<T> encode(Either<F, S> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return input.map(x -> first.encode(x, ops, prefix), x -> second.encode(x, ops, prefix));
        }

        @Override
        public String toString() {
            return "XorMapCodec[" + first + ", " + second + "]";
        }
    }
}
