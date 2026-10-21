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
    private ModParticles() {}
}
