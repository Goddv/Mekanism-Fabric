package mekanism.common.util.text;

import mekanism.api.MekanismAPI;
import mekanism.api.text.ISpecialTextFormatter;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge implementation of {@link ISpecialTextFormatter}: formats the NeoForge-API component types that the
 * loader-neutral {@code TextComponentUtil} (in {@code :common}) can no longer reference directly — {@code ItemResource},
 * {@code FluidStack}, {@code FluidStackTemplate}, {@code FluidResource}, and the NeoForge-extended {@code Fluid}. This
 * preserves the exact formatting behaviour those cases had before {@code TextComponentUtil} was hoisted to {@code :common}.
 * Registered via META-INF/services/mekanism.api.text.ISpecialTextFormatter.
 */
public class SpecialTextFormatter implements ISpecialTextFormatter {

    @Nullable
    @Override
    public MutableComponent format(Object component) {
        return switch (component) {
            case ItemResource resource -> resource.getHoverName().copy();
            case FluidStack stack -> stack.getHoverName().copy();
            case FluidStackTemplate template -> template.create().getHoverName().copy();
            case FluidResource resource -> resource.getHoverName().copy();
            case Fluid fluid -> fluid.getFluidType().getDescription().copy();
            case Level level -> level.getDescription().copy();
            default -> null;
        };
    }

    @Override
    public void logLiteralItemUsage(Item item) {
        if (!FMLEnvironment.isProduction()) {
            MekanismAPI.logger.error("Item instance ({}) passed directly to translate method", item, new Exception());
        }
    }
}
