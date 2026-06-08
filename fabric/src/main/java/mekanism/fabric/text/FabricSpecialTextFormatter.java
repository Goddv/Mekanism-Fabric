package mekanism.fabric.text;

import mekanism.api.text.ISpecialTextFormatter;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric implementation of {@link ISpecialTextFormatter}. The NeoForge fluid/resource component types it would format
 * don't exist on Fabric, so this is a no-op for now (unknown types fall back to {@code TextComponentUtil}'s generic
 * string handling). When the fluid system is ported (Phase 5), this gains Fabric fluid formatting. Registered via
 * META-INF/services/mekanism.api.text.ISpecialTextFormatter.
 */
public class FabricSpecialTextFormatter implements ISpecialTextFormatter {

    @Nullable
    @Override
    public MutableComponent format(Object component) {
        return null;
    }
}
