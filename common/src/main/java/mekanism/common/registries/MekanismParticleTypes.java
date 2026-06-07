package mekanism.common.registries;

import mekanism.api.MekanismAPIBase;
import mekanism.common.particle.LaserParticleType;
import mekanism.common.registration.MekanismRegistryObject;
import mekanism.common.registration.impl.ParticleTypeDeferredRegister;
import net.minecraft.core.particles.SimpleParticleType;

public class MekanismParticleTypes {

    private MekanismParticleTypes() {
    }

    public static final ParticleTypeDeferredRegister PARTICLE_TYPES = new ParticleTypeDeferredRegister(MekanismAPIBase.MEKANISM_MODID);

    public static final MekanismRegistryObject<LaserParticleType> LASER = PARTICLE_TYPES.register("laser", LaserParticleType::new);
    public static final MekanismRegistryObject<SimpleParticleType> JETPACK_FLAME = PARTICLE_TYPES.register("jetpack_flame", () -> new SimpleParticleType(false) {});
    public static final MekanismRegistryObject<SimpleParticleType> JETPACK_SMOKE = PARTICLE_TYPES.register("jetpack_smoke", () -> new SimpleParticleType(false) {});
    public static final MekanismRegistryObject<SimpleParticleType> SCUBA_BUBBLE = PARTICLE_TYPES.register("scuba_bubble", () -> new SimpleParticleType(false) {});
    public static final MekanismRegistryObject<SimpleParticleType> RADIATION = PARTICLE_TYPES.register("radiation", () -> new SimpleParticleType(false) {});
}
