package co.blumeplugins.deathsareitems.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Campfire sprites, tinted before rendering, with a slow rising and fading plume. */
public final class RewardSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private RewardSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, boolean gold) {
        super(level, x, y, z); this.sprites=sprites;
        xd=dx * .15; yd=.025 + Math.abs(dy) * .1; zd=dz * .15;
        lifetime=65 + random.nextInt(35); quadSize=.2F + random.nextFloat() * .22F; friction=.97F;
        setColor(gold ? 1F : .25F, gold ? .91F : 1F, .08F); alpha=.8F; setSpriteFromAge(sprites);
    }
    @Override public void tick() { super.tick(); yd += .0006; setSpriteFromAge(sprites); alpha=Math.min(.8F, (lifetime-age)/30F); }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites; private final boolean gold;
        public Provider(SpriteSet sprites, boolean gold) { this.sprites=sprites; this.gold=gold; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new RewardSmokeParticle(level,x,y,z,dx,dy,dz,sprites,gold);
        }
    }
}
