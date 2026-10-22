package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class AbilityMessages {
    private static final Map<UUID, Notice> NOTICES = new HashMap<>();
    public static void show(ServerPlayer player, Component text, int ticks) {
        NOTICES.put(player.getUUID(), new Notice(text, ticks));
        player.displayClientMessage(text, true);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Notice notice = NOTICES.get(player.getUUID());
        if (notice == null) return;
        if (--notice.ticks <= 0) { NOTICES.remove(player.getUUID()); player.displayClientMessage(Component.empty(), true); }
        else if (notice.ticks % 10 == 0) player.displayClientMessage(notice.text, true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { NOTICES.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { NOTICES.clear(); }
    private static final class Notice {
        final Component text; int ticks;
        Notice(Component text, int ticks) { this.text = text; this.ticks = ticks; }
    }
}
