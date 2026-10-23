package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Render vanilla health with the native hardcore texture row for this player's last life. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID, value = Dist.CLIENT)
public final class EndHardcoreHud {
    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static boolean active;
    public static void setActive(boolean value) { active = value; }

    @SubscribeEvent public static void health(RenderGuiOverlayEvent.Pre event) {
        if (!active || !event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        var gui = (ForgeGui)mc.gui;
        if (!gui.shouldDrawSurvivalElements() || mc.level.getLevelData().isHardcore()) return;
        event.setCanceled(true);
        gui.setupOverlayRenderState(true, false);
        // Keep vanilla health animation, absorption, status effects and row reservation intact.
        GuiGraphics hearts = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
            @Override public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
                event.getGuiGraphics().blit(texture, x, y, u,
                        texture.equals(ICONS) && v == 0 ? 45 : v, width, height);
            }
        };
        gui.renderHealth(event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), hearts);
    }

    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) active = false;
    }
    private EndHardcoreHud() {}
}
