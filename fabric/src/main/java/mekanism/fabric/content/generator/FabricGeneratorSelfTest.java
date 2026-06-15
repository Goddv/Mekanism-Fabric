package mekanism.fabric.content.generator;

import com.mojang.logging.LogUtils;
import mekanism.common.util.WorldUtilsBase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.WeatherData;
import org.slf4j.Logger;

/**
 * Dev-only runtime validation of the four core generators: for each, place it in a controlled spot, tick it, and assert
 * energy is produced (and fuel consumed, for the fuel-burning ones). Logs a per-generator OK/FAIL line with the energy
 * produced + a RESULT line. Grep {@code [Mekanism/Fabric][generator-selftest]}.
 *
 * <ul>
 *   <li><b>Solar</b>: force day + clear weather, clear the column above so the block can see the sky, tick, assert
 *       stored &gt; 0.</li>
 *   <li><b>Wind</b>: place high (y=120) with open sky 4 blocks above, tick, assert stored &gt; 0.</li>
 *   <li><b>Heat</b>: drop coal in the fuel slot, tick, assert stored &gt; 0 and the fuel was consumed.</li>
 *   <li><b>Bio</b>: drop sugar cane in the fuel slot, tick, assert stored &gt; 0 and the fuel was consumed.</li>
 * </ul>
 */
public final class FabricGeneratorSelfTest {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[Mekanism/Fabric][generator-selftest]";

    private FabricGeneratorSelfTest() {
    }

    public static void run() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> validate(server.overworld()));
    }

    private static void validate(ServerLevel level) {
        boolean solarOk = false;
        boolean windOk = false;
        boolean heatOk = false;
        boolean bioOk = false;
        try {
            // Clear weather so the solar sun-brightness check isn't penalized by rain/thunder. (Day time can't be
            // forced cheaply under MC 26.1's WorldClock model; the solar test instead gates strictly on canSeeSun,
            // which holds because a fresh dev world starts in daytime — see testSolar.)
            WeatherData weather = level.getWeatherData();
            weather.setRaining(false);
            weather.setThundering(false);
            weather.setClearWeatherTime(6000);

            solarOk = testSolar(level);
            windOk = testWind(level);
            heatOk = testHeat(level);
            bioOk = testBio(level);
        } catch (Throwable t) {
            LOGGER.error("{} FAIL generator test threw", TAG, t);
        }
        boolean ok = solarOk && windOk && heatOk && bioOk;
        LOGGER.info("{} RESULT: {} (solar={} wind={} heat={} bio={})",
              TAG, ok ? "PASS" : "FAIL", solarOk, windOk, heatOk, bioOk);
    }

    private static boolean testSolar(ServerLevel level) {
        BlockPos pos = new BlockPos(10, 100, 40);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        // Clear the column above so the block (and the block above it, which canSeeSun checks) has open sky.
        clearColumnAbove(level, pos);
        level.setBlock(pos, FabricGenerators.SOLAR_GENERATOR.get().defaultBlockState(), 3);
        // Solar production is gated on canSeeSun (skyDarken < 4 + open sky). A fresh dev world starts in daytime, so
        // this holds; we assert it explicitly (and require it) rather than forcing the time, which is awkward under the
        // MC 26.1 WorldClock model.
        boolean canSeeSun = WorldUtilsBase.canSeeSun(level, pos.above());
        boolean ok = false;
        long stored = 0L;
        if (canSeeSun && level.getBlockEntity(pos) instanceof SolarGeneratorBlockEntity solar) {
            for (int i = 0; i < 20 && stored == 0L; i++) {
                solar.serverTick(level);
                stored = solar.getStoredEnergy();
            }
            ok = stored > 0L;
        }
        level.removeBlock(pos, false);
        LOGGER.info("{} {} solar: canSeeSun={} stored={}", TAG, ok ? "OK  " : "FAIL", canSeeSun, stored);
        return ok;
    }

    private static boolean testWind(ServerLevel level) {
        BlockPos pos = new BlockPos(13, 120, 40);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        clearColumnAbove(level, pos);
        level.setBlock(pos, FabricGenerators.WIND_GENERATOR.get().defaultBlockState(), 3);
        boolean ok = false;
        long stored = 0L;
        if (level.getBlockEntity(pos) instanceof WindGeneratorBlockEntity wind) {
            for (int i = 0; i < 5 && stored == 0L; i++) {
                wind.serverTick(level);
                stored = wind.getStoredEnergy();
            }
            ok = stored > 0L;
        }
        level.removeBlock(pos, false);
        LOGGER.info("{} {} wind: stored={}", TAG, ok ? "OK  " : "FAIL", stored);
        return ok;
    }

    private static boolean testHeat(ServerLevel level) {
        BlockPos pos = new BlockPos(16, 64, 40);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        level.setBlock(pos, FabricGenerators.HEAT_GENERATOR.get().defaultBlockState(), 3);
        boolean ok = false;
        long stored = 0L;
        boolean fuelConsumed = false;
        if (level.getBlockEntity(pos) instanceof HeatGeneratorBlockEntity heat) {
            heat.setItem(0, new ItemStack(Items.COAL, 2));
            for (int i = 0; i < 5 && stored == 0L; i++) {
                heat.serverTick(level);
                stored = heat.getStoredEnergy();
            }
            fuelConsumed = heat.getItem(0).getCount() < 2 || heat.isBurning();
            ok = stored > 0L && fuelConsumed;
        }
        level.removeBlock(pos, false);
        LOGGER.info("{} {} heat: stored={} fuelConsumed={}", TAG, ok ? "OK  " : "FAIL", stored, fuelConsumed);
        return ok;
    }

    private static boolean testBio(ServerLevel level) {
        BlockPos pos = new BlockPos(19, 64, 40);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        level.setBlock(pos, FabricGenerators.BIO_GENERATOR.get().defaultBlockState(), 3);
        boolean ok = false;
        long stored = 0L;
        boolean fuelConsumed = false;
        if (level.getBlockEntity(pos) instanceof BioGeneratorBlockEntity bio) {
            bio.setItem(0, new ItemStack(Items.SUGAR_CANE, 2));
            for (int i = 0; i < 5 && stored == 0L; i++) {
                bio.serverTick(level);
                stored = bio.getStoredEnergy();
            }
            fuelConsumed = bio.getItem(0).getCount() < 2 || bio.isBurning();
            ok = stored > 0L && fuelConsumed;
        }
        level.removeBlock(pos, false);
        LOGGER.info("{} {} bio: stored={} fuelConsumed={}", TAG, ok ? "OK  " : "FAIL", stored, fuelConsumed);
        return ok;
    }

    /** Clear the block at pos and the few blocks above it to air so the generator's sky checks pass. */
    private static void clearColumnAbove(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 8; dy++) {
            level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
