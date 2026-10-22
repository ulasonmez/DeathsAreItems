package co.blumeplugins.deathsareitems.particle;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DeathsAreItems.MOD_ID);
    public static final RegistryObject<SimpleParticleType> CONFETTI = PARTICLES.register("confetti", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GOLD_SMOKE = PARTICLES.register("gold_smoke", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GREEN_SMOKE = PARTICLES.register("green_smoke", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> HUNGER_MOTE = PARTICLES.register("hunger_mote", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> DROWNING_BUBBLE = PARTICLES.register("drowning_bubble", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SOUL_FLAME = PARTICLES.register("soul_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WHITE_SOUL = PARTICLES.register("white_soul", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SOUL_SMOKE = PARTICLES.register("soul_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SOUL_WAVE = PARTICLES.register("soul_wave", () -> new SimpleParticleType(false));
    private ModParticles() {}
}
