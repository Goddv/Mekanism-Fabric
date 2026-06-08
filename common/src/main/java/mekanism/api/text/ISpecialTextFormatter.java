package mekanism.api.text;

import mekanism.api.MekanismAPIBase;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-specific hook for {@link TextComponentUtil} to format component types that live in a particular loader's API
 * (NeoForge {@code FluidStack}/{@code FluidStackTemplate}/{@code FluidResource}/{@code ItemResource} and the
 * NeoForge-extended {@code Fluid#getFluidType}). {@code TextComponentUtil} itself stays loader-neutral in {@code :common}
 * (it can't reference those types), and delegates the unknown/loader-specific cases here. Resolved per loader via
 * {@link MekanismAPIBase#getService}; each loader registers an impl (NeoForge: fluid/resource formatting; Fabric: no-op
 * until the fluid system is ported).
 *
 * <p>This is the static-method equivalent of the inheritance-split trick used elsewhere — static methods can't be
 * overridden by a subclass, so the loader-specific behaviour is injected through a service instead.
 */
@Internal
public interface ISpecialTextFormatter {

    ISpecialTextFormatter INSTANCE = MekanismAPIBase.getService(ISpecialTextFormatter.class);

    /**
     * Formats a component type this loader knows about but {@code :common} cannot reference directly.
     *
     * @param component the component object (already not one of the vanilla/common types TextComponentUtil handles).
     *
     * @return the formatted component, or {@code null} if this formatter does not handle the given type.
     */
    @Nullable
    MutableComponent format(Object component);

    /**
     * Optional dev-only diagnostic when an {@link Item} is passed directly to a translate method (loader-specific because
     * it historically used the loader's environment check + logger). Default: no-op.
     */
    default void logLiteralItemUsage(Item item) {
    }
}
