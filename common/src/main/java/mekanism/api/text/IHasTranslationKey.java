package mekanism.api.text;

import mekanism.api.annotations.MethodsAreNotNullByDefault;

@MethodsAreNotNullByDefault
public interface IHasTranslationKey {

    /**
     * Gets the translation key for this object.
     */
    String getTranslationKey();
}
