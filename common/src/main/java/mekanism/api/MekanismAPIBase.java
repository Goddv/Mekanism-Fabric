package mekanism.api;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Iterator;
import java.util.ServiceLoader;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

/**
 * Loader-neutral foundation of {@code MekanismAPI}. Holds the parts of the API entry-point that carry no loader-specific
 * coupling (the service locator, version/id constants, the shared logger and the registry-name helpers) so they can live in
 * {@code :common} and be consumed by loader-neutral API code.
 *
 * @apiNote Access these members through {@code MekanismAPI} as before; they are exposed here only so they can be shared
 * across loaders. This split is transitional: once the chemical/module/robit-skin registry types are themselves
 * loader-neutral, the {@code MekanismAPI} registry fields can move down here and the two classes will collapse into one.
 */
@NothingNullByDefault
@Internal
public class MekanismAPIBase {

    protected MekanismAPIBase() {
    }

    /**
     * The version of the api classes - may not always match the mod's version
     */
    public static final String API_VERSION = "10.8.0";
    /**
     * Mekanism's Mod ID
     */
    public static final String MEKANISM_MODID = "mekanism";
    /**
     * Mekanism debug mode
     */
    public static boolean debug = false;
    /**
     * Logger for use in Mekanism's API classes
     */
    public static final Logger logger = LogUtils.getLogger();

    public static Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(MEKANISM_MODID, path);
    }

    protected static <T> ResourceKey<Registry<T>> registryKey(@SuppressWarnings("unused") Class<T> compileTimeTypeValidator, String path) {
        return ResourceKey.createRegistryKey(rl(path));
    }

    protected static <T> ResourceKey<Registry<MapCodec<? extends T>>> codecRegistryKey(@SuppressWarnings("unused") Class<T> compileTimeTypeValidator, String path) {
        return ResourceKey.createRegistryKey(rl(path));
    }

    /**
     * {@link ResourceKey} for the name of the {@link Chemical} registry. Loader-neutral so {@code :common} chemical code
     * (codecs, tags) can reference it; the registry instance itself is created per-loader (see
     * {@link mekanism.api.chemical.IChemicalRegistryProvider}).
     */
    public static final ResourceKey<Registry<Chemical>> CHEMICAL_REGISTRY_NAME = registryKey(Chemical.class, "chemical");
    /**
     * Constant location representing the name all empty chemicals will be registered under.
     */
    public static final ResourceKey<Chemical> EMPTY_CHEMICAL_KEY = ResourceKey.create(CHEMICAL_REGISTRY_NAME, rl("empty"));

    /**
     * {@link ResourceKey} for the name of the Registry for {@link ChemicalIngredient} ingredient type serializers.
     * Loader-neutral so {@code :common} chemical-ingredient code (the dispatch codec in
     * {@code mekanism.common.recipe.ingredients.ChemicalIngredientCreator}) can reference it; the registry instance
     * itself is created per-loader (NeoForge {@code RegistryBuilder}, Fabric {@code FabricRegistryBuilder}) and reached
     * through {@link mekanism.api.recipes.ingredients.chemical.IChemicalIngredientTypeRegistry}.
     *
     * @since 10.7.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends ChemicalIngredient>>> CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(ChemicalIngredient.class, "chemical_ingredient_type");

    @Internal
    private static final ClassLoader SERVICE_CL = MekanismAPIBase.class.getClassLoader();

    /**
     * Loads a Mekanism service from ServiceLoader, ensuring that the correct classloader is used instead of relying on the context classloader, which may not be correct
     *
     * @param serviceClass the interface class to search for
     *
     * @return the concrete implementation
     *
     * @throws IllegalStateException when an implementation is not found
     */
    @Internal
    public static <SERVICE> SERVICE getService(Class<SERVICE> serviceClass) {
        Iterator<SERVICE> service = ServiceLoader.load(serviceClass, SERVICE_CL).iterator();
        if (service.hasNext()) {
            return service.next();
        }

        IllegalStateException illegalStateException = new IllegalStateException("No valid ServiceImpl for " + serviceClass.getSimpleName() + " found");
        logger.error("Failed to load service", illegalStateException);
        logger.error("CL: {} CCL: {}", SERVICE_CL, Thread.currentThread().getContextClassLoader());
        throw illegalStateException;

    }

}
