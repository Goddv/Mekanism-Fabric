package mekanism.fabric.content.machine.gui;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

/**
 * Transitional Fabric bring-up: registers the ONE generic {@link MekanismMachineMenu} {@link MenuType} via Architectury's
 * {@link MenuRegistry#ofExtended} factory — extended because the open packet carries the {@link MachineGuiType} ordinal so
 * the client can build the matching dummy container/data + slot layout. Every chemical / dual-item machine + every
 * generator opens this single menu type (with its own shape); the client renders {@link MekanismMachineScreen} (registered
 * in the client entrypoint). Distinct from the original {@code FabricMachineMenus.MACHINE} item-machine menu (kept as-is).
 */
public final class MekanismMachineMenus {

    private static final String MODID = "mekanism";

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(MODID, Registries.MENU);

    public static final RegistrySupplier<MenuType<MekanismMachineMenu>> MEKANISM_MACHINE = MENUS.register(
          Identifier.fromNamespaceAndPath(MODID, "mekanism_machine"),
          () -> MenuRegistry.ofExtended(MekanismMachineMenu::fromNetwork));

    private MekanismMachineMenus() {
    }

    public static void init() {
        MENUS.register();
    }
}
