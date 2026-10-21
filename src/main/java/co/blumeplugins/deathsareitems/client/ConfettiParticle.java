package co.blumeplugins.deathsareitems.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Flat, tumbling paper quads rather than a reskinned round dust particle. */
public final class ConfettiParticle extends TextureSheetParticle {
    private static final int[] COLORS = {0xFFBAE8, 0xB4F3FF, 0xFFF1B0, 0xCAFFBB, 0xDEC7FF, 0xFFC0B0};
    private final float spin;
    private final float phase;

    private ConfettiParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        xd = dx; yd = Math.abs(dy) + .16; zd = dz;
        lifetime = 75 + random.nextInt(45);
        quadSize = .055F + random.nextFloat() * .055F;
        gravity = .035F;
        friction = .965F;
        int color = COLORS[random.nextInt(COLORS.length)];
        setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F);
        spin = (random.nextFloat() - .5F) * .3F;
        phase = random.nextFloat() * Mth.TWO_PI;
        roll = phase;
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        roll += spin;
        xd += Mth.sin(age * .12F + phase) * .0015;
        zd += Mth.cos(age * .1F + phase) * .0015;
        alpha = Mth.clamp((lifetime - age) / 20F, 0, 1);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - cameraPos.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - cameraPos.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - cameraPos.z);
        Quaternionf orientation = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTick, oRoll, roll));
        float flutter = .15F + Math.abs(Mth.cos((age + partialTick) * .18F + phase)) * .85F;
        Vector3f[] vertices = {new Vector3f(-1, -.6F, 0), new Vector3f(-1, .6F, 0), new Vector3f(1, .6F, 0), new Vector3f(1, -.6F, 0)};
        float u = (sprite.getU0() + sprite.getU1()) * .5F;
        float v = (sprite.getV0() + sprite.getV1()) * .5F;
        for (Vector3f vertex : vertices) {
            vertex.mul(flutter, 1, 1).rotate(orientation).mul(quadSize).add(px, py, pz);
            consumer.vertex(vertex.x, vertex.y, vertex.z).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
        }
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new ConfettiParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
