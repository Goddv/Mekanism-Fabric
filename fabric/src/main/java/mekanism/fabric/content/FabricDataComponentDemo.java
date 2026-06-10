package mekanism.fabric.content;

import java.util.UUID;
import mekanism.common.registration.DataComponentDeferredRegister;
import mekanism.common.registration.MekanismRegistryObject;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

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
    //Exercise the helper surface the real MekanismDataComponents will reuse once its payload closures hoist.
    public static final MekanismRegistryObject<DataComponentType<UUID>> DEMO_UUID = DATA_COMPONENTS.registerUUID("fabric_demo_uuid");
    public static final MekanismRegistryObject<DataComponentType<Component>> DEMO_NAME = DATA_COMPONENTS.registerComponent("fabric_demo_name");
    public static final MekanismRegistryObject<DataComponentType<Long>> DEMO_NONNEG_LONG = DATA_COMPONENTS.registerNonNegativeLong("fabric_demo_nonneg_long");
    public static final MekanismRegistryObject<DataComponentType<ResourceKey<Item>>> DEMO_RKEY = DATA_COMPONENTS.registerResourceKey("fabric_demo_rkey", Registries.ITEM);

    private FabricDataComponentDemo() {
    }

    public static void init() {
        DATA_COMPONENTS.register();
    }
}
