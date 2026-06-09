package mekanism.common.inventory.container;

import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral receiver for {@link mekanism.common.network.to_client.container.property.PropertyData} window property updates. This breaks the cycle between
 * {@code PropertyData} and {@code MekanismContainer} by only exposing the loader-neutral {@code handleWindowProperty} overloads. The coupled overloads (e.g.
 * {@code FluidStack}) remain on {@code MekanismContainer} only.
 */
public interface IPropertyDataReceiver {

    void handleWindowProperty(short property, boolean value);

    void handleWindowProperty(short property, byte value);

    void handleWindowProperty(short property, short value);

    void handleWindowProperty(short property, int value);

    void handleWindowProperty(short property, long value);

    void handleWindowProperty(short property, float value);

    void handleWindowProperty(short property, double value);

    void handleWindowProperty(short property, @NotNull ItemStack value);

    void handleWindowProperty(short property, @Nullable BlockPos value);

    void handleWindowProperty(short property, @NotNull ChemicalStack value);

    void handleWindowProperty(short property, byte[] value);
}
