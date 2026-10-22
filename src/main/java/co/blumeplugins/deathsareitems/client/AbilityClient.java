package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.HungerAbility;
import co.blumeplugins.deathsareitems.ability.MaskChargeInput;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID, value = Dist.CLIENT)
public final class AbilityClient {
    private static final net.minecraft.resources.ResourceLocation ICONS = new net.minecraft.resources.ResourceLocation("minecraft", "textures/gui/icons.png");
    private static boolean held;
    private static final MaskChargeInput CHARGE_INPUT = new MaskChargeInput();
    private static int reserve;
    private static final java.util.Map<Integer, Float> PENDING = new java.util.HashMap<>();
    public static void state(int id, float scale, int food) {
        var mc = Minecraft.getInstance(); if (mc.level == null) return;
        var entity = mc.level.getEntity(id);
        if (entity == null && food < 0 && PENDING.size() < 512) PENDING.put(id,scale);
        if (entity != null && entity != mc.player) {
            entity.getPersistentData().putFloat(HungerAbility.SCALE, scale); entity.refreshDimensions();
        }
        if (food >= 0 && mc.player != null && id == mc.player.getId()) reserve = food;
    }
    private static boolean canCharge() {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.screen == null && mc.isWindowActive() && !mc.isPaused()
                && mc.player.isAlive() && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DROWNING_MASK.get())
                && mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty();
    }
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        boolean eligible = canCharge();
        // Observe the press before vanilla can place and consume the final held block this tick.
        CHARGE_INPUT.update(true, eligible);
        if (eligible) { event.setCanceled(true); event.setSwingHand(false); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level != null) PENDING.entrySet().removeIf(entry -> {
            var entity = mc.level.getEntity(entry.getKey());
            if (entity == null) return false;
            entity.getPersistentData().putFloat(HungerAbility.SCALE,entry.getValue()); entity.refreshDimensions(); return true;
        });
        boolean down = CHARGE_INPUT.update(mc.options.keyUse.isDown(), canCharge());
        if (mc.player != null && (held != down || (down && mc.player.tickCount % 5 == 0))) ModNetwork.input(down);
        held = down;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void pre(RenderLivingEvent.Pre<?, ?> event) {
        float scale = HungerAbility.scale(event.getEntity());
        event.getPoseStack().pushPose(); event.getPoseStack().scale(scale, scale, scale);
    }
    @SubscribeEvent public static void post(RenderLivingEvent.Post<?, ?> event) { event.getPoseStack().popPose(); }
    @SubscribeEvent public static void hud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || reserve <= 0) return;
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.options.hideGui) return;
        if (!mc.gameMode.canHurtPlayer()) return;
        int x = event.getWindow().getGuiScaledWidth() / 2 - 91;
        var gui = (net.minecraftforge.client.gui.overlay.ForgeGui)mc.gui;
        // Health has already reserved its rows. Insert food here, before armor renders.
        int y = event.getWindow().getGuiScaledHeight() - gui.leftHeight;
        var graphics = event.getGuiGraphics();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        int icons = (Math.min(100, reserve) + 1) / 2;
        gui.leftHeight += ((icons + 9) / 10) * 10;
        for (int i = 0; i < icons; i++) {
            int iconX = x + (i % 10) * 8, iconY = y - (i / 10) * 10;
            graphics.blit(ICONS, iconX, iconY, 16, 27, 9, 9);
            int u = reserve >= (i + 1) * 2 ? 52 : 61;
            graphics.blit(ICONS, iconX, iconY, u, 27, 9, 9);
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    public static void food(double x, double y, double z, int recipient) {
        var mc = Minecraft.getInstance();
        if (mc.level != null) mc.level.addParticle(co.blumeplugins.deathsareitems.particle.ModParticles.HUNGER_MOTE.get(),
                x, y, z, recipient, 0, 0);
    }

    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel().isClientSide()) { held=false; CHARGE_INPUT.reset(); reserve=0; PENDING.clear(); } }
}
