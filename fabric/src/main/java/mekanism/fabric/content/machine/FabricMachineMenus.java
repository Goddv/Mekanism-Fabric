package mekanism.fabric.content.machine;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

/**
 * Transitional Fabric bring-up: registers the {@link MachineMenu} {@link MenuType} via Architectury's loader-neutral
 * {@link MenuRegistry#of} factory. The server opens it ({@code MenuRegistry.openMenu}) from the machine block's use
 * handler; the client renders {@link MachineScreen} (registered in the client entrypoint). Proves the menu/GUI primitive
 * on Fabric — the real Mekanism GUI layouts arrive with the machine-framework migration.
 */
public final class FabricMachineMenus {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<MachineMenu>> MACHINE = MENUS.register(
          Identifier.fromNamespaceAndPath(MODID, "machine"),
          () -> MenuRegistry.of(MachineMenu::new));

    private FabricMachineMenus() {
    }

    public static void init() {
        MENUS.register();
    }
}
