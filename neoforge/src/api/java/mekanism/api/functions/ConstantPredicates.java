package mekanism.api.functions;

import java.util.Objects;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to reduce having to create duplicate objects for constant predicates.
 *
 * @implNote The loader-neutral constants live in {@link ConstantPredicatesBase} (shared with {@code :common}); the
 * members below stay here because they reference NeoForge's {@code FluidStack}/{@code TriPredicate} or the not-yet-hoisted
 * {@code ChemicalStack}.
 */
@SuppressWarnings("unchecked")
public class ConstantPredicates extends ConstantPredicatesBase {

    private ConstantPredicates() {
    }

    private static final TriPredicate<Object, Object, Object> alwaysTrueTri = (t, u, v) -> true;
    private static final TriPredicate<Object, Object, Object> alwaysFalseTri = (t, u, v) -> false;

    /**
     * Represents a predicate that checks if a fluid stack is empty.
     *
     * @since 10.5.15
     */
    public static final Predicate<FluidStack> FLUID_EMPTY = FluidStack::isEmpty;
    /**
     * Represents a predicate that checks if the fluid template is null. Templates throw an error during construction if they would refer to an empty stack.
     *
     * @since 10.8.0
     */
    public static final Predicate<@Nullable FluidStackTemplate> INVALID_FLUID_TEMPLATE = Objects::isNull;

    /**
     * Returns a tri predicate that returns {@code true} for any input.
     */
    public static <T, U, V> TriPredicate<T, U, V> alwaysTrueTri() {
        return (TriPredicate<T, U, V>) alwaysTrueTri;
    }

    /**
     * Returns a tri predicate that returns {@code false} for any input.
     */
    public static <T, U, V> TriPredicate<T, U, V> alwaysFalseTri() {
        return (TriPredicate<T, U, V>) alwaysFalseTri;
    }

}
