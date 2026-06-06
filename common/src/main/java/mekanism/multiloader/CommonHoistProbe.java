package mekanism.multiloader;

/**
 * Temporary proof that loader-neutral code in {@code :common} compiles against the common classpath
 * (MC + fabric-loader + architectury, no NeoForge) AND is bundled into the platform jars.
 *
 * <p>Removed once real loader-neutral code (starting with the energy API) is hoisted here.
 */
public final class CommonHoistProbe {

    public static final String MARKER = "common-hoist-pipeline-ok";

    private CommonHoistProbe() {
    }
}
