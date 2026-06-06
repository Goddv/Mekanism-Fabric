package mekanism.api;

import com.mojang.serialization.MapCodec;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import mekanism.api.gear.ModuleData;
import mekanism.api.recipes.ingredients.chemical.ChemicalIngredient;
import mekanism.api.robit.RobitSkin;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Mekanism's API entry-point.
 *
 * @implNote The loader-neutral foundation (service locator, version/id constants, logger and the registry-name helpers)
 * lives in {@link MekanismAPIBase} so it can be shared with {@code :common}; the registry-name keys and registry instances
 * below stay here because their element types and {@link RegistryBuilder}/{@link DeferredHolder} are still NeoForge-bound.
 */
@NothingNullByDefault
public class MekanismAPI extends MekanismAPIBase {

    private MekanismAPI() {
    }

    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link Chemical chemicals}.
     *
     * @apiNote When registering {@link Chemical chemicals} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.7.0
     */
    public static final ResourceKey<Registry<Chemical>> CHEMICAL_REGISTRY_NAME = registryKey(Chemical.class, "chemical");

    /**
     * Constant location representing the name all empty chemicals will be registered under.
     *
     * @since 10.6.0
     */
    public static final ResourceKey<Chemical> EMPTY_CHEMICAL_KEY = ResourceKey.create(CHEMICAL_REGISTRY_NAME, rl("empty"));

    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link ChemicalIngredient} ingredient type serializers.
     *
     * @apiNote When registering chemical ingredient types using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.7.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends ChemicalIngredient>>> CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME = codecRegistryKey(ChemicalIngredient.class, "chemical_ingredient_type");

    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link ModuleData modules}.
     *
     * @apiNote When registering {@link ModuleData modules} using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final ResourceKey<Registry<ModuleData<?>>> MODULE_REGISTRY_NAME = registryKey((Class) ModuleData.class, "module");
    /**
     * Gets the {@link ResourceKey} representing the name of the Datapack Registry for {@link RobitSkin robit skins}.
     *
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<RobitSkin>> ROBIT_SKIN_REGISTRY_NAME = registryKey(RobitSkin.class, "robit_skin");
    /**
     * Gets the {@link ResourceKey} representing the name of the Registry for {@link RobitSkin robit skin} serializers.
     *
     * @apiNote When registering {@link RobitSkin robit skin} serializers using {@link DeferredRegister}, use this field to get access to the {@link ResourceKey}.
     * @since 10.4.0
     */
    public static final ResourceKey<Registry<MapCodec<? extends RobitSkin>>> ROBIT_SKIN_SERIALIZER_REGISTRY_NAME = codecRegistryKey(RobitSkin.class, "robit_skin_serializer");

    /**
     * Gets the Registry for {@link Chemical}.
     *
     * @see #CHEMICAL_REGISTRY_NAME
     * @since 10.7.0
     */
    public static final DefaultedRegistry<Chemical> CHEMICAL_REGISTRY = (DefaultedRegistry<Chemical>) new RegistryBuilder<>(CHEMICAL_REGISTRY_NAME)
          .defaultKey(EMPTY_CHEMICAL_KEY)
          .sync(true)
          .create();

    /**
     * Gets the Registry for {@link ChemicalIngredient} type serializers.
     *
     * @see #CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME
     * @since 10.7.0
     */
    public static final Registry<MapCodec<? extends ChemicalIngredient>> CHEMICAL_INGREDIENT_TYPES = new RegistryBuilder<>(CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME)
          .sync(true)
          .create();

    /**
     * Gets the Registry for {@link ModuleData}.
     *
     * @see #MODULE_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<ModuleData<?>> MODULE_REGISTRY = new RegistryBuilder<>(MODULE_REGISTRY_NAME)
          .sync(true)
          .create();
    /**
     * Gets the Registry for {@link RobitSkin} serializers.
     *
     * @see #ROBIT_SKIN_SERIALIZER_REGISTRY_NAME
     * @since 10.5.0
     */
    public static final Registry<MapCodec<? extends RobitSkin>> ROBIT_SKIN_SERIALIZER_REGISTRY = new RegistryBuilder<>(ROBIT_SKIN_SERIALIZER_REGISTRY_NAME)
          .create();

    /**
     * Holder for the empty Chemical instance.
     *
     * @since 10.7.11
     */
    public static final Holder<Chemical> EMPTY_CHEMICAL_HOLDER = DeferredHolder.create(EMPTY_CHEMICAL_KEY);

}
