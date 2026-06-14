package mekanism.fabric.recipe;

import com.mojang.logging.LogUtils;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPIBase;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.fabric.content.machine.ChemicalMachineBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the FIRST chemical-processing machine on Fabric: the Chemical Oxidizer
 * (item input &rarr; chemical output). Proves: (A) the {@code mekanism:oxidizing} {@link net.minecraft.world.item.crafting.RecipeType}
 * + {@link net.minecraft.world.item.crafting.RecipeSerializer} are registered under their shared id; (B) end-to-end — the
 * placed chemical_oxidizer looks up its bundled {@code oxidizing} datapack recipe (redstone -&gt; fabric_demo_chemical x100)
 * via the vanilla recipe manager, advances progress while consuming energy, and on completion consumes the input item and
 * fills its internal chemical tank with the recipe's output chemical. Grep {@code [Mekanism/Fabric][chem-machine-selftest]}.
 */
public final class FabricChemicalMachineSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][chem-machine-selftest]";

    private static final Identifier OXIDIZING_ID = Identifier.fromNamespaceAndPath("mekanism", "oxidizing");
    private static final ResourceKey<Chemical> DEMO_CHEMICAL_KEY = ResourceKey.create(
          MekanismAPIBase.CHEMICAL_REGISTRY_NAME, Identifier.fromNamespaceAndPath("mekanism", "fabric_demo_chemical"));
    private static final Identifier CHEMICAL_OXIDIZER_ID = Identifier.fromNamespaceAndPath("mekanism", "chemical_oxidizer");

    private FabricChemicalMachineSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean ok = false;
        ChemicalStack produced = ChemicalStack.EMPTY;
        boolean registrationOk = false;
        boolean inputConsumed = false;
        try {
            // (A) Registration under the shared id.
            registrationOk = BuiltInRegistries.RECIPE_TYPE.containsKey(OXIDIZING_ID)
                  && BuiltInRegistries.RECIPE_SERIALIZER.containsKey(OXIDIZING_ID);

            // (B) End-to-end: place the chemical oxidizer, feed it redstone + energy, tick it, read the output tank.
            BlockPos pos = new BlockPos(8, 64, 30);
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            Block block = BuiltInRegistries.BLOCK.getValue(CHEMICAL_OXIDIZER_ID);
            level.setBlock(pos, block.defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof ChemicalMachineBlockEntity machine) {
                machine.setItem(0, new ItemStack(Items.REDSTONE, 8));
                machine.getEnergyContainers(null).getFirst().insert(1_000_000L, Action.EXECUTE, AutomationType.INTERNAL);
                // One operation takes MAX_PROGRESS ticks; run a little past that to complete one.
                for (int i = 0; i < ChemicalMachineBlockEntity.MAX_PROGRESS + 5 && machine.getOutputTank().isEmpty(); i++) {
                    machine.serverTick();
                }
                produced = machine.getOutputTank().getStack();
                inputConsumed = machine.getItem(0).getCount() < 8;
            }
            level.removeBlock(pos, false);

            boolean producedOk = !produced.isEmpty()
                  && produced.typeHolder().is(DEMO_CHEMICAL_KEY)
                  && produced.amount() > 0;
            ok = registrationOk && producedOk && inputConsumed;
            LOGGER.info("{} {} registration={} producedChemical={} amount={} inputConsumed={}",
                  TAG, ok ? "OK  " : "FAIL", registrationOk,
                  produced.isEmpty() ? "<empty>" : produced.typeHolder().getRegisteredName(),
                  produced.amount(), inputConsumed);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL chemical-machine test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
