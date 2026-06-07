package mekanism.common.registration.impl;

import java.util.function.Supplier;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.common.registration.MekanismRegister;
import mekanism.common.registration.MekanismRegistryObject;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

/**
 * Loader-neutral {@link ParticleType} register (Architectury-backed via {@link MekanismRegister}). Returns the registered
 * particle type's specific subtype (consumers use it via {@code get()} as the concrete {@code ParticleType}/{@code
 * ParticleOptions}).
 */
@NothingNullByDefault
public class ParticleTypeDeferredRegister extends MekanismRegister<ParticleType<?>> {

    public ParticleTypeDeferredRegister(String modid) {
        super(modid, Registries.PARTICLE_TYPE);
    }

    public <PARTICLE extends ParticleType<?>> MekanismRegistryObject<PARTICLE> register(String name, Supplier<? extends PARTICLE> supplier) {
        return new MekanismRegistryObject<>(doRegister(name, supplier));
    }
}
