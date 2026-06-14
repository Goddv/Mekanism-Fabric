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

    // CHEMICAL_REGISTRY_NAME + EMPTY_CHEMICAL_KEY moved to MekanismAPIBase (:common) so loader-neutral chemical code
    // can reference them; inherited here, so existing MekanismAPI.CHEMICAL_REGISTRY_NAME / EMPTY_CHEMICAL_KEY call
    // sites keep working unchanged.

    // CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME moved to MekanismAPIBase (:common) so the hoisted loader-neutral
    // ChemicalIngredientCreator dispatch can reference it; inherited here, so existing
    // MekanismAPI.CHEMICAL_INGREDIENT_TYPE_REGISTRY_NAME call sites (incl. the CHEMICAL_INGREDIENT_TYPES builder below)
    // keep working unchanged.

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
