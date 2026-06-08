package mekanism.fabric.content;

import mekanism.common.registration.DataComponentDeferredRegister;
import mekanism.common.registration.MekanismRegistryObject;
import net.minecraft.core.component.DataComponentType;

/**
 * Transitional Fabric bring-up: registers a few leaf {@link DataComponentType}s (boolean/int/long, vanilla codecs) via
 * the loader-neutral {@code :common} {@link DataComponentDeferredRegister}, proving the DataComponent registration +
 * ItemStack set/get/persistence chain on Fabric. Uses {@code fabric_demo_*} ids (clearly transitional, no clash with the
 * real {@code MekanismDataComponents} which is NeoForge-only). Removed once the real MekanismDataComponents migrates to
 * {@code :common} (its ~29 leaf components reuse this exact framework; its chemical/fluid/frequency-coupled ones wait on
 * the data-type closure).
 */
public final class FabricDataComponentDemo {

    private static final String MODID = "mekanism";
    private static final DataComponentDeferredRegister DATA_COMPONENTS = new DataComponentDeferredRegister(MODID);

    public static final MekanismRegistryObject<DataComponentType<Boolean>> DEMO_FLAG = DATA_COMPONENTS.registerBoolean("fabric_demo_flag");
    public static final MekanismRegistryObject<DataComponentType<Integer>> DEMO_COUNT = DATA_COMPONENTS.registerNonNegativeInt("fabric_demo_count");
    public static final MekanismRegistryObject<DataComponentType<Long>> DEMO_AMOUNT = DATA_COMPONENTS.registerLong("fabric_demo_amount");

    private FabricDataComponentDemo() {
    }

    public static void init() {
        DATA_COMPONENTS.register();
    }
}
