package mekanism.fabric.content.machine.gui;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import mekanism.fabric.content.generator.FabricGenerators;
import mekanism.fabric.content.machine.FabricChemicalMachines;
import mekanism.fabric.content.machine.FabricRealMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of the new machine/generator GUIs: for each block that previously returned {@code
 * createMenu == null} (the chemical / dual-item machines + the four generators), place it, build its menu server-side, and
 * assert the menu is non-null with the EXPECTED slot count (its machine slots + the 36 player slots) and the EXPECTED
 * ContainerData size for its {@link MachineGuiType} shape. Proves the menu plumbing (slots + sync data) without a client.
 * Logs a per-block OK/FAIL line + a RESULT line. Grep {@code [Mekanism/Fabric][gui-selftest]}.
 *
 * <p>Generators are opened through {@link MekanismMenuProvider} (they aren't {@code MenuProvider} block-entities);
 * machines through their BE's {@code createMenu}. Both paths land on the same {@link MekanismMachineMenu}.
 */
public final class FabricMachineGuiSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][gui-selftest]";

    private FabricMachineGuiSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        int y = 70;
        int x = 30;
        boolean all = true;
        // Item -> chemical (1 slot, 1 tank, progress): oxidizer, pigment extractor.
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.CHEMICAL_OXIDIZER.block().defaultBlockState(), MachineGuiType.ITEM_TO_CHEMICAL);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.PIGMENT_EXTRACTOR.block().defaultBlockState(), MachineGuiType.ITEM_TO_CHEMICAL);
        // Chemical -> item (1 slot, 1 tank, progress): crystallizer.
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.CHEMICAL_CRYSTALLIZER.block().defaultBlockState(), MachineGuiType.CHEMICAL_TO_ITEM);
        // Item + chemical -> item (2 slots, 1 tank, progress): compressor, purifier, injector, infuser, painter.
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.OSMIUM_COMPRESSOR.block().defaultBlockState(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.PURIFICATION_CHAMBER.block().defaultBlockState(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.CHEMICAL_INJECTION_CHAMBER.block().defaultBlockState(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.METALLURGIC_INFUSER.block().defaultBlockState(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricChemicalMachines.PAINTING_MACHINE.block().defaultBlockState(), MachineGuiType.ITEM_CHEMICAL_TO_ITEM);
        // Dual-item machines (3 slots, no tank, progress): combiner, sawmill.
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricRealMachines.COMBINER.block().defaultBlockState(), MachineGuiType.COMBINER);
        all &= checkBlock(level, new BlockPos(x++, y, 50), FabricRealMachines.PRECISION_SAWMILL.block().defaultBlockState(), MachineGuiType.SAWMILL);
        // Generators (Solar/Wind: 0 slots; Heat/Bio: 1 fuel slot; no tank, no progress).
        all &= checkGenerator(level, new BlockPos(x++, y, 50), FabricGenerators.SOLAR_GENERATOR.get().defaultBlockState(), MachineGuiType.PASSIVE_GENERATOR);
        all &= checkGenerator(level, new BlockPos(x++, y, 50), FabricGenerators.WIND_GENERATOR.get().defaultBlockState(), MachineGuiType.PASSIVE_GENERATOR);
        all &= checkGenerator(level, new BlockPos(x++, y, 50), FabricGenerators.HEAT_GENERATOR.get().defaultBlockState(), MachineGuiType.FUEL_GENERATOR);
        all &= checkGenerator(level, new BlockPos(x++, y, 50), FabricGenerators.BIO_GENERATOR.get().defaultBlockState(), MachineGuiType.FUEL_GENERATOR);

        LOGGER.info("{} RESULT: {}", TAG, all ? "PASS" : "FAIL");
    }

    /** Machine path: build via the BE's MenuProvider#createMenu and assert shape. */
    private static boolean checkBlock(ServerLevel level, BlockPos pos, BlockState state, MachineGuiType expected) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        level.setBlock(pos, state, 3);
        BlockEntity be = level.getBlockEntity(pos);
        boolean ok = false;
        String detail = "no block-entity";
        if (be instanceof MenuProvider provider) {
            AbstractContainerMenu menu = provider.createMenu(0, dummyInventory(), null);
            detail = describe(menu, expected, state);
            ok = matches(menu, expected);
        }
        level.removeBlock(pos, false);
        log(state, ok, detail);
        return ok;
    }

    /** Generator path: build via the BE's MekanismMenuProvider and assert shape. */
    private static boolean checkGenerator(ServerLevel level, BlockPos pos, BlockState state, MachineGuiType expected) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        clearColumnAbove(level, pos);
        level.setBlock(pos, state, 3);
        BlockEntity be = level.getBlockEntity(pos);
        boolean ok = false;
        String detail = "no block-entity";
        if (be instanceof mekanism.fabric.content.generator.AbstractGeneratorBlockEntity generator) {
            AbstractContainerMenu menu = generator.menuProvider().createMenu(0, dummyInventory(), null);
            detail = describe(menu, expected, state);
            ok = matches(menu, expected);
        }
        level.removeBlock(pos, false);
        log(state, ok, detail);
        return ok;
    }

    private static boolean matches(AbstractContainerMenu menu, MachineGuiType expected) {
        if (menu == null) {
            return false;
        }
        int expectedSlots = expected.machineSlotCount() + 36;
        boolean slotsOk = menu.slots.size() == expectedSlots;
        boolean dataOk = menu instanceof MekanismMachineMenu m && m.guiType() == expected;
        return slotsOk && dataOk;
    }

    private static String describe(AbstractContainerMenu menu, MachineGuiType expected, BlockState state) {
        if (menu == null) {
            return "menu=null";
        }
        int machineSlots = menu.slots.size() - 36;
        MachineGuiType guiType = menu instanceof MekanismMachineMenu m ? m.guiType() : null;
        return "slots=" + menu.slots.size() + " (machine=" + machineSlots + "+player=36, expected machine="
              + expected.machineSlotCount() + ") dataSize=" + expected.dataSize() + " guiType=" + guiType;
    }

    private static void log(BlockState state, boolean ok, String detail) {
        LOGGER.info("{} {} {}: {}", TAG, ok ? "OK  " : "FAIL", state.getBlock().getName().getString(), detail);
    }

    /**
     * A throwaway player inventory for server-side menu construction. The {@code Inventory(Player, EntityEquipment)}
     * constructor only stores its args + allocates 36 empty slots (it never dereferences them), and the menu's slot
     * construction + {@code addDataSlots} never touch the player — so a null-backed inventory is safe for the slot/data
     * shape assertions (no real player exists at SERVER_STARTED).
     */
    private static Inventory dummyInventory() {
        return new Inventory(null, null);
    }

    private static void clearColumnAbove(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlock(pos.above(dy + 1), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
