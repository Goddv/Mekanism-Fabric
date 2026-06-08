package mekanism.fabric.content;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the Fabric DataComponent slice (gated behind {@code isDevelopmentEnvironment()} by its
 * caller). Proves, on a live Fabric server, that the {@code :common}
 * {@link mekanism.common.registration.DataComponentDeferredRegister} works end-to-end on Architectury: the demo
 * components land in the {@code DATA_COMPONENT_TYPE} registry, and values round-trip through {@code ItemStack.set}/{@code
 * getOrDefault}. Grep for {@code [Mekanism/Fabric][datacomponent-selftest] RESULT:}.
 */
public final class FabricDataComponentSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][datacomponent-selftest]";

    private FabricDataComponentSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static void validate() {
        boolean ok = false;
        try {
            boolean registered =
                  BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(Identifier.fromNamespaceAndPath("mekanism", "fabric_demo_flag"))
                  && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(Identifier.fromNamespaceAndPath("mekanism", "fabric_demo_count"))
                  && BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(Identifier.fromNamespaceAndPath("mekanism", "fabric_demo_amount"));

            ItemStack stack = new ItemStack(Items.STICK);
            stack.set(FabricDataComponentDemo.DEMO_FLAG.get(), true);
            stack.set(FabricDataComponentDemo.DEMO_COUNT.get(), 42);
            stack.set(FabricDataComponentDemo.DEMO_AMOUNT.get(), 2_000_000L);
            boolean flag = stack.getOrDefault(FabricDataComponentDemo.DEMO_FLAG.get(), false);
            int count = stack.getOrDefault(FabricDataComponentDemo.DEMO_COUNT.get(), 0);
            long amount = stack.getOrDefault(FabricDataComponentDemo.DEMO_AMOUNT.get(), 0L);

            // Round-trip through a copy to exercise the persistent codec path.
            ItemStack copy = stack.copy();
            boolean copyOk = copy.getOrDefault(FabricDataComponentDemo.DEMO_FLAG.get(), false)
                  && copy.getOrDefault(FabricDataComponentDemo.DEMO_COUNT.get(), 0) == 42
                  && copy.getOrDefault(FabricDataComponentDemo.DEMO_AMOUNT.get(), 0L) == 2_000_000L;

            ok = registered && flag && count == 42 && amount == 2_000_000L && copyOk;
            LOGGER.info("{} {} DataComponent register+set/get: registered={} flag={} count={} amount={} copyOk={}",
                  TAG, ok ? "OK  " : "FAIL", registered, flag, count, amount, copyOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL data-component test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
