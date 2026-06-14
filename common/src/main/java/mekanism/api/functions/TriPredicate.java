package mekanism.api.functions;

import java.util.Objects;

/**
 * A predicate that takes three arguments and returns a boolean.
 *
 * <p>Loader-neutral counterpart of NeoForge's {@code net.neoforged.neoforge.common.util.TriPredicate}, reproduced
 * verbatim (same {@code test}/{@code and}/{@code or}/{@code negate} contract) so the recipe-lookup caches, tank specs,
 * and machine factories that use it can live in {@code :common}. Functionally identical — lambdas/method references bind
 * to either interface the same way, so swapping the import is byte-neutral on NeoForge.
 */
@FunctionalInterface
public interface TriPredicate<T, U, V> {

    boolean test(T t, U u, V v);

    default TriPredicate<T, U, V> and(TriPredicate<? super T, ? super U, ? super V> other) {
        Objects.requireNonNull(other);
        return (T t, U u, V v) -> test(t, u, v) && other.test(t, u, v);
    }

    default TriPredicate<T, U, V> negate() {
        return (T t, U u, V v) -> !test(t, u, v);
    }

    default TriPredicate<T, U, V> or(TriPredicate<? super T, ? super U, ? super V> other) {
        Objects.requireNonNull(other);
        return (T t, U u, V v) -> test(t, u, v) || other.test(t, u, v);
    }
}
