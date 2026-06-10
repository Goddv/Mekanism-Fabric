package mekanism.fabric.registration;

import com.mojang.logging.LogUtils;
import mekanism.common.block.basic.BlockResource;
import mekanism.common.registries.MekanismGameEvents;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.registries.MekanismSounds;
import mekanism.common.resource.BlockResourceInfo;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation that Mekanism's real content registers on Fabric through the Architectury-based
 * registration framework. Gated behind {@code isDevelopmentEnvironment()} by its caller; the actual registration happens
 * unconditionally in the entrypoint. Confirms representative entries from each migrated registry (sounds, game events)
 * are present in their vanilla registries and resolvable via their registry objects. Grep for {@code RESULT:} to see PASS/FAIL.
 */
public final class FabricRegistrationSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][registration-selftest]";

    private FabricRegistrationSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate());
    }

    private static void validate() {
        boolean ok = false;
        try {
            boolean soundOk = check("sound", BuiltInRegistries.SOUND_EVENT.containsKey(MekanismSounds.ENRICHMENT_CHAMBER.getId()),
                  MekanismSounds.ENRICHMENT_CHAMBER.get() != null, MekanismSounds.ENRICHMENT_CHAMBER.getId());
            boolean gameEventOk = check("game_event", BuiltInRegistries.GAME_EVENT.containsKey(MekanismGameEvents.JETPACK_BURN.getId()),
                  MekanismGameEvents.JETPACK_BURN.get() != null, MekanismGameEvents.JETPACK_BURN.getId());
            boolean particleOk = check("particle", BuiltInRegistries.PARTICLE_TYPE.containsKey(MekanismParticleTypes.LASER.getId()),
                  MekanismParticleTypes.LASER.get() != null, MekanismParticleTypes.LASER.getId());
            var enrichedIron = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "enriched_iron");
            var blockOsmium = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "block_osmium");
            var mekTab = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "mekanism");
            boolean itemOk = check("item", BuiltInRegistries.ITEM.containsKey(enrichedIron), BuiltInRegistries.ITEM.getValue(enrichedIron) != null, enrichedIron);
            boolean blockOk = check("block", BuiltInRegistries.BLOCK.containsKey(blockOsmium), BuiltInRegistries.ITEM.containsKey(blockOsmium), blockOsmium);
            boolean tabOk = check("creative_tab", BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(mekTab), true, mekTab);
            var machineMenu = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "machine");
            boolean menuOk = check("menu", BuiltInRegistries.MENU.containsKey(machineMenu), BuiltInRegistries.MENU.getValue(machineMenu) != null, machineMenu);
            //Real resource blocks (S-RESOURCE): block_refined_obsidian must be a BlockResource with the right info + hardness.
            //getDestroySpeed(null, null) is a null-safe pure field read in 26.1 (returns the strength() hardness, 50 for
            //refined obsidian) — verified by decompile; the getResourceInfo() check alone is also sufficient.
            var blockRefinedObsidian = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "block_refined_obsidian");
            Block obsidian = BuiltInRegistries.BLOCK.getValue(blockRefinedObsidian);
            boolean resourceOk = check("resource_block",
                  BuiltInRegistries.BLOCK.containsKey(blockRefinedObsidian)
                        && obsidian instanceof BlockResource resource
                        && resource.getResourceInfo() == BlockResourceInfo.REFINED_OBSIDIAN
                        && obsidian.defaultBlockState().getDestroySpeed(null, null) == 50.0F,
                  BuiltInRegistries.ITEM.containsKey(blockRefinedObsidian), blockRefinedObsidian);
            var blockRefinedGlowstone = net.minecraft.resources.Identifier.fromNamespaceAndPath("mekanism", "block_refined_glowstone");
            boolean glowOk = check("resource_block_light",
                  BuiltInRegistries.BLOCK.containsKey(blockRefinedGlowstone)
                        && BuiltInRegistries.BLOCK.getValue(blockRefinedGlowstone).defaultBlockState().getLightEmission() == 15,
                  true, blockRefinedGlowstone);
            long mekSounds = BuiltInRegistries.SOUND_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekGameEvents = BuiltInRegistries.GAME_EVENT.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekParticles = BuiltInRegistries.PARTICLE_TYPE.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekItems = BuiltInRegistries.ITEM.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekBlocks = BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            long mekMenus = BuiltInRegistries.MENU.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count();
            ok = soundOk && gameEventOk && particleOk && itemOk && blockOk && tabOk && menuOk && resourceOk && glowOk;
            LOGGER.info("{} {} Architectury registration: sounds={} gameEvents={} particles={} items={} blocks={} tabs={} menus={}",
                  TAG, ok ? "OK  " : "FAIL", mekSounds, mekGameEvents, mekParticles, mekItems, mekBlocks,
                  BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream().filter(k -> k.getNamespace().equals("mekanism")).count(), mekMenus);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL registration test threw", TAG, t);
        }
        LOGGER.info("{} RESULT: {}", TAG, ok ? "PASS" : "FAIL");
    }

    private static boolean check(String kind, boolean inRegistry, boolean supplierResolved, Object id) {
        boolean ok = inRegistry && supplierResolved;
        LOGGER.info("{} {} {} probe={} inRegistry={} supplierResolved={}", TAG, ok ? "OK  " : "FAIL", kind, id, inRegistry, supplierResolved);
        return ok;
    }
}
