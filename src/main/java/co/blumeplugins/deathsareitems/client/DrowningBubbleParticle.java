package co.blumeplugins.deathsareitems.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Vanilla bubble artwork that also survives outside water. */
public final class DrowningBubbleParticle extends TextureSheetParticle {
    private DrowningBubbleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites); lifetime = 20 + random.nextInt(16);
        quadSize = .04F + random.nextFloat() * .045F;
        xd = (random.nextDouble() - .5) * .02; yd = .025 + random.nextDouble() * .025; zd = (random.nextDouble() - .5) * .02;
        hasPhysics = false; friction = 1;
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new DrowningBubbleParticle(level, x, y, z, sprites);
        }
    }
}
