package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class ClientEffects {
    private ClientEffects() {}

    public static void handle(boolean celebration, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (celebration) {
            minecraft.getToasts().addToast(new RewardToast(stack));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.1F, .75F));
        } else {
            minecraft.gameRenderer.displayItemActivation(stack);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.TOTEM_USE, 1F, .7F));
        }
    }

    private static final class RewardToast implements Toast {
        private final ItemStack icon;
        RewardToast(ItemStack icon) { this.icon = icon.isEmpty() ? new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get()) : icon.copy(); }
        @Override
        public Visibility render(GuiGraphics graphics, ToastComponent toasts, long time) {
            graphics.blit(TEXTURE, 0, 0, 0, 0, width(), height());
            graphics.renderFakeItem(icon, 8, 8);
            graphics.drawString(toasts.getMinecraft().font, Component.translatable("toast.deathsareitems.title"), 30, 7, 0xFFFF55, false);
            graphics.drawString(toasts.getMinecraft().font, Component.translatable("toast.deathsareitems.description"), 30, 18, 0xFFFFFF, false);
            return time >= 4500 ? Visibility.HIDE : Visibility.SHOW;
        }
    }
}
