package mekanism.common.registration.impl;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.common.registration.MekanismRegister;
import mekanism.common.registration.MekanismRegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Loader-neutral {@link GameEvent} register (Architectury-backed via {@link MekanismRegister}).
 */
@NothingNullByDefault
public class GameEventDeferredRegister extends MekanismRegister<GameEvent> {

    public GameEventDeferredRegister(String modid) {
        super(modid, Registries.GAME_EVENT);
    }

    public MekanismRegistryObject<GameEvent> register(String name) {
        return register(name, 16);
    }

    public MekanismRegistryObject<GameEvent> register(String name, int notificationRadius) {
        return new MekanismRegistryObject<>(doRegister(name, () -> new GameEvent(notificationRadius)));
    }
}
