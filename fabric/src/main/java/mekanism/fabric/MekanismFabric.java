package mekanism.fabric;

import com.mojang.logging.LogUtils;
import mekanism.common.registries.MekanismGameEvents;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.registries.MekanismSounds;
import mekanism.fabric.chemical.FabricChemicalIngredientTypes;
import mekanism.fabric.chemical.FabricChemicalRegistry;
import mekanism.fabric.chemical.FabricChemicalSelfTest;
import mekanism.fabric.config.FabricConfigSelfTest;
import mekanism.fabric.fluid.FabricFluidSelfTest;
import mekanism.fabric.content.FabricBringUpContent;
import mekanism.fabric.content.FabricDataComponentDemo;
import mekanism.fabric.content.FabricDataComponentSelfTest;
import mekanism.fabric.content.energy.FabricEnergyBlockDemo;
import mekanism.fabric.content.generator.FabricGenerators;
import mekanism.fabric.content.generator.FabricGeneratorSelfTest;
import mekanism.fabric.content.machine.FabricAutoIoSelfTest;
import mekanism.fabric.content.machine.FabricChemicalMachines;
import mekanism.fabric.content.machine.FabricMachineMenus;
import mekanism.fabric.content.machine.FabricRealMachines;
import mekanism.fabric.content.power.FabricPowerInfrastructure;
import mekanism.fabric.content.power.FabricPowerSelfTest;
import mekanism.fabric.content.transmitter.FabricTransmitterSelfTest;
import mekanism.fabric.content.transmitter.FabricTransmitters;
import mekanism.fabric.energy.FabricEnergySelfTest;
import mekanism.fabric.heat.FabricHeatSelfTest;
import mekanism.fabric.recipe.FabricChemicalMachineSelfTest;
import mekanism.fabric.recipe.FabricRecipeSelfTest;
import mekanism.fabric.recipe.MekanismChemicalRecipeTypesRegistrar;
import mekanism.fabric.recipe.MekanismDualItemRecipeTypesRegistrar;
import mekanism.fabric.recipe.MekanismRecipeTypesRegistrar;
import mekanism.fabric.registration.FabricRegistrationSelfTest;
import mekanism.fabric.text.FabricTextFoundationSelfTest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Fabric entrypoint for the Mekanism multi-loader port.
 *
 * <p>Early bring-up build: real content is wired up as loader-neutral code is hoisted into {@code :common}. The first
 * real registry — Mekanism's {@link MekanismSounds sound events} — now registers here through the Architectury-based
 * registration framework shared with NeoForge. In a development environment it additionally runs the energy and
 * registration self-tests on a live server; those are skipped in production.
 */
public final class MekanismFabric implements ModInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("[Mekanism/Fabric] Fabric build initialized — Architectury no-remap toolchain OK.");
        // Create Mekanism's custom `chemical` registry FIRST (custom registries must be built during mod init, before
        // registries freeze). The hoisted :common Chemical/ChemicalStack reach it via IChemicalRegistryProvider.
        FabricChemicalRegistry.init();
        // Create the chemical_ingredient_type serializer registry + register the six type serializers (mirrors NeoForge's
        // MekanismChemicalIngredientTypes). Must come right after the chemical registry and before registries freeze —
        // it backs the dispatch "type" key in the hoisted :common ChemicalIngredientCreator (via
        // IChemicalIngredientTypeRegistry), giving Fabric a REAL chemical-ingredient creator + dispatch.
        FabricChemicalIngredientTypes.init();
        // Real content registration (shared loader-neutral path; finalized per-loader).
        MekanismSounds.SOUND_EVENTS.register();
        MekanismGameEvents.GAME_EVENTS.register();
        MekanismParticleTypes.PARTICLE_TYPES.register();
        // Transitional: a slice of real simple items + a Mekanism creative tab, so content is visible
        // in-game on Fabric. Removed once the full item/block framework is migrated to :common.
        FabricBringUpContent.init();
        // Transitional: a functional energy block (block-entity that stores energy + exposes the strict-energy
        // capability via BlockApiLookup) — the foundation pattern for real Mekanism machines on Fabric.
        FabricEnergyBlockDemo.init();
        // Transitional: REAL Mekanism machine blocks (enrichment chamber, crusher, ...) — real ids/models with
        // facing+active states, backed by a functional machine block-entity (energy + item I/O + processing).
        FabricRealMachines.init();
        // Transitional: the machine container-menu (GUI) type — opened from the machine block's use handler;
        // the client screen is registered in MekanismFabricClient.
        FabricMachineMenus.init();
        // Transitional: the enriching RecipeType + RecipeSerializer (shared :common recipe classes), so machines run
        // REAL datapack recipes instead of the demo move-item loop.
        MekanismRecipeTypesRegistrar.init();
        // Transitional: the oxidizing (item -> chemical) RecipeType + RecipeSerializer (shared :common recipe classes),
        // backing the first chemical-processing machine on Fabric.
        MekanismChemicalRecipeTypesRegistrar.init();
        // Transitional: the combining (item+item->item) + sawing (item->item+chance) RecipeTypes + RecipeSerializers
        // (shared :common recipe classes), backing the Combiner + Precision Sawmill dual-item machines on Fabric.
        MekanismDualItemRecipeTypesRegistrar.init();
        // Transitional: the FIRST chemical-processing machine — the Chemical Oxidizer (item input -> chemical output),
        // backed by a functional chemical-output block-entity (energy + item input + chemical tank + oxidizing recipes).
        FabricChemicalMachines.init();
        // Transitional: a fuel-burning generator + energy cables so machines can be powered in-game (generator ->
        // cable -> machine) for manual testing, until the real generators + transmitter network are ported.
        FabricPowerInfrastructure.init();
        // Transitional: the four core Mekanism generators (Solar/Wind/Heat/Bio) from the separate mekanismgenerators
        // module — real blocks/models, each a BasicEnergyContainer exposed via the energy capability so the demo cable
        // relays their output. Must come AFTER FabricPowerInfrastructure (reuses the same energy capability/cable).
        FabricGenerators.init();
        // Transitional: the four core Mekanism transmitters (basic tier) — Universal Cable (energy), Pressurized Tube
        // (chemical), Thermodynamic Conductor (heat), Logistical Transporter (items) — as functional adjacent-relays.
        // Must come AFTER FabricPowerInfrastructure (the energy cable relay logic + capability are reused). NOT the real
        // Mekanism transmitter-network graph; connected multipart rendering is likewise deferred.
        FabricTransmitters.init();
        // Transitional: leaf DataComponentTypes via the :common DataComponentDeferredRegister framework, proving the
        // DataComponent registration shape on Fabric (the real MekanismDataComponents reuses this framework).
        FabricDataComponentDemo.init();
        // Dev-only bring-up validation; skipped in production.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            FabricEnergySelfTest.run();
            FabricHeatSelfTest.run();
            FabricDataComponentSelfTest.run();
            FabricTextFoundationSelfTest.run();
            FabricChemicalSelfTest.run();
            FabricFluidSelfTest.run();
            FabricRecipeSelfTest.run();
            FabricChemicalMachineSelfTest.run();
            FabricPowerSelfTest.run();
            FabricTransmitterSelfTest.run();
            FabricGeneratorSelfTest.run();
            FabricAutoIoSelfTest.run();
            FabricConfigSelfTest.run();
            FabricRegistrationSelfTest.run();
        }
    }
}
