package mekanism.api.text;

import mekanism.api.annotations.MethodsAreNotNullByDefault;

/**
 * Helper interface for enums that supply a {@link net.minecraft.network.chat.Component} name. Now loader-neutral in
 * {@code :common}.
 *
 * <p>Historically this also implemented NeoForge's {@code net.neoforged.neoforge.common.TranslatableEnum} (providing a
 * {@code getTranslatedName()} default) so NeoForge's config-dropdown GUI could auto-translate the enum. That coupling was
 * severed: no caller anywhere does {@code instanceof TranslatableEnum}/{@code instanceof IHasEnumNameTextComponent}, and
 * none of this interface's implementors (SecurityMode, RedstoneControl, GasMode, ConnectionType, …) are ever passed to a
 * config {@code defineEnum} (the only {@code TranslatableEnum} consumer) — those config-bound enums use the separate
 * {@code IHasEnumNameTranslationKey} bridge, which is left untouched. So dropping the supertype is behaviorally identical
 * on NeoForge (it is NOT byte-identical: the ~14 implementor class files lose {@code TranslatableEnum} from their
 * {@code implements} list and the inherited {@code getTranslatedName()} default, neither of which is consumed).
 *
 * @since 10.7.3
 */
@MethodsAreNotNullByDefault
public interface IHasEnumNameTextComponent extends IHasTextComponent {
}
