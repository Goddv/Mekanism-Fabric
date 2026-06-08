package mekanism.fabric.text;

import com.mojang.logging.LogUtils;
import mekanism.api.IIncrementalEnum;
import mekanism.api.SupportsColorMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation that the hoisted {@code :common} text-foundation prerequisites ({@link IIncrementalEnum},
 * {@link SupportsColorMap}) load and execute under Fabric's classloader with no NeoForge leakage. These two leaf
 * interfaces are the deepest prerequisites of {@code EnumColor} → the text foundation → {@code ChemicalStack} → the
 * chemical master gate, so this is the first validated step of the chemical-gate hoist. Pure-logic test (no
 * capability/world); grep for {@code [Mekanism/Fabric][text-foundation-selftest] RESULT:}.
 */
public final class FabricTextFoundationSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][text-foundation-selftest]";

    private FabricTextFoundationSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    /** A tiny loader-neutral IIncrementalEnum implementor, to avoid pulling in NeoForge-coupled EnumColor/SecurityMode. */
    private enum TestEnum implements IIncrementalEnum<TestEnum> {
        A, B, C;

        @Override
        public TestEnum byIndex(int index) {
            return values()[Math.floorMod(index, values().length)];
        }
    }

    /** A tiny SupportsColorMap stub exercising the default color math. */
    private record TestColor(int[] rgb) implements SupportsColorMap {
        @Override
        public int getPackedColor() {
            return ARGB.color(rgb[0], rgb[1], rgb[2]);
        }

        @Override
        public int[] getRgbCode() {
            return rgb;
        }

        @Override
        public void setColorFromAtlas(int[] color) {
        }
    }

    private static void validate() {
        boolean ok = false;
        try {
            // IIncrementalEnum: wrap-around next/previous + adjust.
            boolean incOk = TestEnum.A.getNext() == TestEnum.B
                  && TestEnum.C.getNext() == TestEnum.A          // wraps
                  && TestEnum.A.getPrevious() == TestEnum.C       // wraps
                  && TestEnum.A.adjust(2) == TestEnum.C
                  && TestEnum.A.adjust(0) == TestEnum.A;

            // SupportsColorMap: 0-1 float color + packed-with-alpha.
            TestColor color = new TestColor(new int[]{255, 128, 0});
            float[] floats = color.getRgbCodeFloat();
            boolean colorOk = floats.length == 3
                  && Math.abs(color.getColor(0) - 1.0F) < 1.0E-4
                  && Math.abs(color.getColor(1) - 128 / 255F) < 1.0E-4
                  && color.getPackedColor(200) == ARGB.color(200, color.getPackedColor());

            ok = incOk && colorOk;
            LOGGER.info("{} {} text foundation: IIncrementalEnum={} SupportsColorMap={}", TAG, ok ? "OK  " : "FAIL", incOk, colorOk);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL text-foundation test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }
}
