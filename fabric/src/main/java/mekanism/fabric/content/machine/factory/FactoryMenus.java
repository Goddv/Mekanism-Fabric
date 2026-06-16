package mekanism.fabric.content.machine.factory;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

/**
 * Transitional Fabric bring-up: registers the ONE generic {@link FactoryMenu} {@link MenuType} via Architectury's
 * {@link MenuRegistry#ofExtended} factory (extended — the open packet carries the {@link FactoryType} ordinal + process
 * count so the client rebuilds the matching dummy container/data + slot layout). Every tier/type of factory opens this
 * single menu type; the client renders {@link FactoryScreen} (registered in the client entrypoint).
 */
public final class FactoryMenus {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<FactoryMenu>> FACTORY = MENUS.register(
          Identifier.fromNamespaceAndPath(MODID, "factory"),
          () -> MenuRegistry.ofExtended(FactoryMenu::fromNetwork));

    private FactoryMenus() {
    }

    public static void init() {
        MENUS.register();
    }
}
