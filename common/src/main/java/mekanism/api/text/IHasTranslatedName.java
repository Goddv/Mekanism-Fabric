package mekanism.api.text;

import mekanism.api.annotations.NothingNullByDefault;
import net.minecraft.network.chat.Component;

/**
 * Loader-neutral counterpart to {@link IHasEnumNameTranslationKey}: provides a translated display name without bridging
 * to NeoForge's {@code TranslatableEnum}. Enums/objects that only need {@code getTranslationKey()} + {@code getTranslatedName()}
 * (not the NeoForge config-dropdown {@code TranslatableEnum} contract) implement this so they can live in {@code :common}.
 */
@NothingNullByDefault
public interface IHasTranslatedName extends IHasTranslationKey {

    default Component getTranslatedName() {
        return TextComponentUtil.translate(getTranslationKey());
    }
}
