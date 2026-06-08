package mekanism.api.text;

import mekanism.api.annotations.MethodsAreNotNullByDefault;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;
import org.jetbrains.annotations.NotNull;

/**
 * Helper interface that also implements Neo's TranslatableEnum interface. Loader-specific (NeoForge) because it bridges
 * to {@code net.neoforged.neoforge.common.TranslatableEnum}; extracted out of {@link IHasTextComponent} (now loader-neutral
 * in {@code :common}) so the base interface can be shared across loaders.
 *
 * @since 10.7.3
 */
@MethodsAreNotNullByDefault
public interface IHasEnumNameTextComponent extends IHasTextComponent, TranslatableEnum {

    @NotNull
    @Override
    default Component getTranslatedName() {
        return getTextComponent();
    }
}
