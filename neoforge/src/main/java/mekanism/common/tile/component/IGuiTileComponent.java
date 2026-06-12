package mekanism.common.tile.component;

import mekanism.common.inventory.container.MekanismContainer;

/**
 * NeoForge-only extension of {@link ITileComponent} for components that push tracked state to the main GUI container.
 * {@code trackForMainContainer} touches {@code MekanismContainer} (the NeoForge GUI/menu framework, not yet on
 * {@code :common}), so it is kept off the loader-neutral {@link ITileComponent} (which carries only the NBT /
 * data-component path). The hoisted tile iterates its components and dispatches this only to the {@code IGuiTileComponent}
 * ones; on Fabric (no Mekanism container framework yet) no component implements this, so GUI tracking is simply skipped.
 */
public interface IGuiTileComponent extends ITileComponent {

    default void trackForMainContainer(MekanismContainer container) {
    }
}
