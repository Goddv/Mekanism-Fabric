package mekanism.common.registries;

import mekanism.api.MekanismAPIBase;
import mekanism.common.registration.MekanismRegistryObject;
import mekanism.common.registration.impl.GameEventDeferredRegister;
import net.minecraft.world.level.gameevent.GameEvent;

public class MekanismGameEvents {

    private MekanismGameEvents() {
    }

    public static final GameEventDeferredRegister GAME_EVENTS = new GameEventDeferredRegister(MekanismAPIBase.MEKANISM_MODID);

    public static final MekanismRegistryObject<GameEvent> SEISMIC_VIBRATION = GAME_EVENTS.register("seismic_vibration", 64);
    public static final MekanismRegistryObject<GameEvent> JETPACK_BURN = GAME_EVENTS.register("jetpack_burn");
    public static final MekanismRegistryObject<GameEvent> GRAVITY_MODULATE = GAME_EVENTS.register("gravity_modulate");
    public static final MekanismRegistryObject<GameEvent> GRAVITY_MODULATE_BOOSTED = GAME_EVENTS.register("gravity_modulate_boosted", 32);
}
