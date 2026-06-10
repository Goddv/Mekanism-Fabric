package mekanism.fabric.content;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the Fabric DataComponent slice (gated behind {@code isDevelopmentEnvironment()} by its
 * caller). Proves, on a live Fabric server, that the {@code :common}
 * {@link mekanism.common.registration.DataComponentDeferredRegister} works end-to-end on Architectury: the demo
 * components land in the {@code DATA_COMPONENT_TYPE} registry, values round-trip through {@code ItemStack.set}/{@code
 * getOrDefault} + a copy, and (for the registry-free helpers) through a genuine persistent-codec NBT encode/decode.
 * This exercises the full leaf helper surface the real {@code MekanismDataComponents} will reuse once its payload
 * closures hoist. Grep for {@code [Mekanism/Fabric][datacomponent-selftest] RESULT:}.
 */
public final class FabricDataComponentSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][datacomponent-selftest]";

    private FabricDataComponentSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static boolean inRegistry(String name) {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(Identifier.fromNamespaceAndPath("mekanism", name));
    }

    /** Genuinely exercise a component's PERSISTENT codec via a registry-free NBT encode/decode round-trip. */
    private static <T> boolean codecRoundTrip(DataComponentType<T> type, T value) {
        Codec<T> codec = type.codec();
        if (codec == null) {
            return false;
        }
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        T decoded = codec.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        return value.equals(decoded);
    }

    private static void validate() {
        boolean ok = false;
        try {
            boolean registered = inRegistry("fabric_demo_flag") && inRegistry("fabric_demo_count") && inRegistry("fabric_demo_amount")
                  && inRegistry("fabric_demo_uuid") && inRegistry("fabric_demo_name") && inRegistry("fabric_demo_nonneg_long")
                  && inRegistry("fabric_demo_rkey");

            ItemStack stack = new ItemStack(Items.STICK);
            UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");
            Component name = Component.literal("selftest");
            ResourceKey<Item> rk = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "stick"));
            stack.set(FabricDataComponentDemo.DEMO_FLAG.get(), true);
            stack.set(FabricDataComponentDemo.DEMO_COUNT.get(), 42);
            stack.set(FabricDataComponentDemo.DEMO_AMOUNT.get(), 2_000_000L);
            stack.set(FabricDataComponentDemo.DEMO_UUID.get(), id);
            stack.set(FabricDataComponentDemo.DEMO_NAME.get(), name);
            stack.set(FabricDataComponentDemo.DEMO_NONNEG_LONG.get(), 5_000_000L);
            stack.set(FabricDataComponentDemo.DEMO_RKEY.get(), rk);

            boolean flag = stack.getOrDefault(FabricDataComponentDemo.DEMO_FLAG.get(), false);
            int count = stack.getOrDefault(FabricDataComponentDemo.DEMO_COUNT.get(), 0);
            long amount = stack.getOrDefault(FabricDataComponentDemo.DEMO_AMOUNT.get(), 0L);

            // Round-trip through a copy to exercise the component-map copy + set/get for the new components.
            ItemStack copy = stack.copy();
            boolean copyOk = copy.getOrDefault(FabricDataComponentDemo.DEMO_FLAG.get(), false)
                  && copy.getOrDefault(FabricDataComponentDemo.DEMO_COUNT.get(), 0) == 42
                  && copy.getOrDefault(FabricDataComponentDemo.DEMO_AMOUNT.get(), 0L) == 2_000_000L
                  && id.equals(copy.getOrDefault(FabricDataComponentDemo.DEMO_UUID.get(), id))
                  && name.equals(copy.getOrDefault(FabricDataComponentDemo.DEMO_NAME.get(), Component.empty()))
                  && copy.getOrDefault(FabricDataComponentDemo.DEMO_NONNEG_LONG.get(), 0L) == 5_000_000L
                  && rk.equals(copy.getOrDefault(FabricDataComponentDemo.DEMO_RKEY.get(), rk));

            // Genuinely exercise the PERSISTENT codecs (registry-free helpers) via real NBT encode/decode.
            boolean codecOk = codecRoundTrip(FabricDataComponentDemo.DEMO_UUID.get(), id)
                  && codecRoundTrip(FabricDataComponentDemo.DEMO_RKEY.get(), rk)
                  && codecRoundTrip(FabricDataComponentDemo.DEMO_NONNEG_LONG.get(), 5_000_000L);

            ok = registered && flag && count == 42 && amount == 2_000_000L && copyOk && codecOk;
            LOGGER.info("{} {} DataComponent register+set/get+codec: registered={} flag={} count={} amount={} copyOk={} codecOk={}",
                  TAG, ok ? "OK  " : "FAIL", registered, flag, count, amount, copyOk, codecOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL data-component test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
