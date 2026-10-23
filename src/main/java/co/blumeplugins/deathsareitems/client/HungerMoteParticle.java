package co.blumeplugins.deathsareitems.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/** The staff's hunger icon follows a smooth arc to the moving recipient. */
public final class HungerMoteParticle extends TextureSheetParticle {
    private final Vec3 start;
    private final int recipient;
    private final double sway;
    private HungerMoteParticle(ClientLevel level, double x, double y, double z, int recipient, SpriteSet sprites) {
        super(level, x, y, z);
        this.start = new Vec3(x, y, z); this.recipient = recipient;
        sway = (random.nextDouble() - .5) * .9;
        lifetime = 20; quadSize = .18F; hasPhysics = false;
        pickSprite(sprites);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        var target = level.getEntity(recipient);
        if (target == null || !target.isAlive() || ++age >= lifetime) { remove(); return; }
        double t = (double)age / lifetime, eased = t * t * (3 - 2 * t);
        Vec3 end = target.position().add(0, target.getBbHeight() * .65, 0);
        Vec3 point = start.lerp(end, eased).add(Math.sin(t * Math.PI) * sway, Math.sin(t * Math.PI) * .65, 0);
        setPos(point.x, point.y, point.z);
        quadSize = .18F * (1F - .35F * (float)t);
    }
    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double target, double unusedY, double unusedZ) {
            return new HungerMoteParticle(level, x, y, z, (int)target, sprites);
        }
    }
}
