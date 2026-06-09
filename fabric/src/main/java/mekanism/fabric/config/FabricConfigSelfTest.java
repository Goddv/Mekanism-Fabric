package mekanism.fabric.config;

import com.mojang.logging.LogUtils;
import mekanism.common.config.IConfigBuilder;
import mekanism.common.config.IConfigBuilderFactory;
import mekanism.common.config.IConfigSpec;
import mekanism.common.config.value.IConfigValue;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation for the Fabric config slice (gated behind {@code isDevelopmentEnvironment()} by its caller).
 * Proves three things with no NeoForge on the classpath:
 *
 * <ol>
 *     <li><b>The {@link IConfigBuilderFactory} service resolves on Fabric</b> — {@code IConfigBuilderFactory.INSTANCE}
 *     (via {@code MekanismAPIBase.getService} → {@code ServiceLoader}) yields {@link FabricConfigBuilderFactory}.</li>
 *     <li><b>The defaults-first read path returns declared defaults</b> — a value defined through the builder reads back
 *     its declared default via {@link IConfigValue#get()} / {@link IConfigValue#getDefault()}, and an in-memory
 *     {@code set} is visible through {@code get} while {@code getDefault} is unchanged.</li>
 *     <li><b>The built {@link IConfigSpec} reports loaded</b> — so the {@code Cached*Value} read path treats values as
 *     live (and never NPEs on the never-reloaded Fabric path).</li>
 * </ol>
 *
 * Results are logged with a {@code [Mekanism/Fabric][config-selftest]} tag; grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricConfigSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][config-selftest]";

    private FabricConfigSelfTest() {
    }

    public static void run() {
        boolean ok = runTest();
        LOGGER.info("{} RESULT: => {}", TAG, ok ? "PASS" : "FAIL");
    }

    private static boolean runTest() {
        try {
            IConfigBuilder builder = IConfigBuilderFactory.INSTANCE.create();
            boolean factoryOk = builder instanceof DefaultConfigBuilder;
            log(factoryOk, "factory resolved: " + IConfigBuilderFactory.INSTANCE.getClass().getName());

            //Define a value with a known default through the loader-neutral seam.
            IConfigValue<Integer> value = builder.comment("self-test").push("section")
                  .defineInRange("answer", 42, 0, 100);
            builder.pop();
            IConfigSpec spec = builder.build();

            boolean defaultOk = value.get() == 42 && value.getDefault() == 42;
            log(defaultOk, "default read back: get=" + value.get() + " default=" + value.getDefault());

            value.set(7);
            boolean setOk = value.get() == 7 && value.getDefault() == 42;
            log(setOk, "in-memory set: get=" + value.get() + " default=" + value.getDefault());

            boolean specOk = spec.isLoaded();
            log(specOk, "spec isLoaded()=" + specOk);

            return factoryOk && defaultOk && setOk && specOk;
        } catch (Throwable t) {
            LOGGER.error("{} FAIL config self-test threw", TAG, t);
            return false;
        }
    }

    private static void log(boolean ok, String msg) {
        LOGGER.info("{} {} {}", TAG, ok ? "OK  " : "FAIL", msg);
    }
}
