package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.client.LaunchedBlockRenderer;
import co.blumeplugins.deathsareitems.entity.client.RewardChestRenderer;
import co.blumeplugins.deathsareitems.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.REWARD_CHEST.get(), RewardChestRenderer::new);
        event.registerEntityRenderer(ModEntities.CANNON_TNT.get(), net.minecraft.client.renderer.entity.TntRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAOS_PEARL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.LAUNCHED_BLOCK.get(), LaunchedBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.MASK_TRIDENT.get(), co.blumeplugins.deathsareitems.entity.client.MaskTridentRenderer::new);
    }

    @SubscribeEvent
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
                new net.minecraft.resources.ResourceLocation(DeathsAreItems.MOD_ID, "soul_monochrome"),
                com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE), SoulParticleRenderType::setShader);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.CONFETTI.get(), ConfettiParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GOLD_SMOKE.get(), sprites -> new RewardSmokeParticle.Provider(sprites, true));
        event.registerSpriteSet(ModParticles.GREEN_SMOKE.get(), sprites -> new RewardSmokeParticle.Provider(sprites, false));
        event.registerSpriteSet(ModParticles.HUNGER_MOTE.get(), HungerMoteParticle.Provider::new);
        event.registerSpriteSet(ModParticles.DROWNING_BUBBLE.get(), DrowningBubbleParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SOUL_FLAME.get(), sprites -> new SoulStormParticle.Provider(sprites, 0));
        event.registerSpriteSet(ModParticles.WHITE_SOUL.get(), sprites -> new SoulStormParticle.Provider(sprites, 1));
        event.registerSpriteSet(ModParticles.SOUL_WAVE.get(), sprites -> new SoulStormParticle.Provider(sprites, 3));
        event.registerSpriteSet(ModParticles.SOUL_SMOKE.get(), sprites -> new SoulStormParticle.Provider(sprites, 2));
    }
}
