package co.blumeplugins.deathsareitems.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Animated vanilla souls and sonic waves use a monochrome shader before tinting. */
public final class SoulStormParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final int kind;
    private SoulStormParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, int kind) {
        super(level, x, y, z); this.sprites = sprites; this.kind = kind;
        hasPhysics = false; friction = .94F;
        xd = dx; yd = dy + (kind == 0 ? .014 : .003); zd = dz;
        lifetime = kind == 1 ? 12 : kind == 3 ? 16 : 20;
        quadSize = kind == 0 ? .23F : kind == 1 ? .20F : kind == 3 ? 1.1F : .40F;
        if (kind == 0 || kind == 1) setColor(1, 1, 1);
        else if (kind == 3) {
            boolean purple = random.nextBoolean();
            setColor(purple ? .20F : .025F, .008F, purple ? .30F : .04F);
        } else setColor(.008F, .004F, .012F);
        setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        super.tick(); setSpriteFromAge(sprites);
        alpha = Math.min(1F, (lifetime - age) / 6F);
    }
    @Override protected int getLightColor(float partialTick) { return kind == 2 ? super.getLightColor(partialTick) : 240; }
    @Override public ParticleRenderType getRenderType() { return kind == 0 ? SoulParticleRenderType.PIXEL_FLAME : kind == 1 || kind == 3
                ? SoulParticleRenderType.MONOCHROME : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites, int kind) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SoulStormParticle(level, x, y, z, dx, dy, dz, sprites, kind);
        }
    }
}
