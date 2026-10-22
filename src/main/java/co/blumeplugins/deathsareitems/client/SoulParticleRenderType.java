package co.blumeplugins.deathsareitems.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/** Recolor every original animation frame without replacing its pixels or alpha. */
public final class SoulParticleRenderType {
    private static ShaderInstance shader;
    static void setShader(ShaderInstance value) { shader = value; }
    public static final ParticleRenderType MONOCHROME = create(true);
    public static final ParticleRenderType PIXEL_FLAME = create(false);
    private static ParticleRenderType create(boolean monochrome) { return new ParticleRenderType() {
        @Override public void begin(BufferBuilder buffer, TextureManager textures) {
            RenderSystem.depthMask(true);
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("Desaturate").set(monochrome ? 1F : 0F);
            shader.safeGetUniform("AlphaCutoff").set(monochrome ? .1F : .8F);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
        @Override public void end(Tesselator tesselator) { tesselator.end(); }
        @Override public String toString() { return monochrome ? "DEATHSAREITEMS_SOUL_MONOCHROME" : "DEATHSAREITEMS_PIXEL_FLAME"; }
    }; }
    private SoulParticleRenderType() {}
}
