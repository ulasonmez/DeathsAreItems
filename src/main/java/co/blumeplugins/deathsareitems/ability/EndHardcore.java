package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Personal hardcore starts at the first End entry and remains active across dimensions and restarts. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class EndHardcore {
    private static final String ACTIVE = "deathsareitems.EndHardcore", DEAD = "deathsareitems.EndHardcoreDead";
    private static CompoundTag data(Player player) {
        var tag = player.getPersistentData();
        if (!tag.contains(Player.PERSISTED_NBT_TAG)) tag.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return tag.getCompound(Player.PERSISTED_NBT_TAG);
    }
    public static boolean active(Player player) { return data(player).getBoolean(ACTIVE); }
    public static void enter(ServerPlayer player) {
        data(player).putBoolean(ACTIVE, true);
        sync(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getTo().equals(Level.END)) enter(player); else sync(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player)) data(player).putBoolean(DEAD, true);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (data(event.getOriginal()).getBoolean(ACTIVE)) data(event.getEntity()).putBoolean(ACTIVE,true);
        if (data(event.getOriginal()).getBoolean(DEAD)) data(event.getEntity()).putBoolean(DEAD,true);
    }
    private static void enforce(ServerPlayer player) {
        if (data(player).getBoolean(DEAD)) player.setGameMode(GameType.SPECTATOR);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.isEndConquered()) enforce(player);
            sync(player);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().dimension().equals(Level.END)) enter(player);
            enforce(player); sync(player);
        }
    }
    private static void sync(ServerPlayer player) {
        co.blumeplugins.deathsareitems.network.ModNetwork.hardcore(player, active(player));
    }
    private EndHardcore() {}
}
