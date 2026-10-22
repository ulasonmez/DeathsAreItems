package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class HungerAbility {
    public static final String SCALE = "deathsareitems.Scale", RESERVE = "deathsareitems.FoodReserve";
    private static final String OLD_TEAM = "deathsareitems.HungerOldTeam", GLOW = "deathsareitems.HungerGlow";
    private static final Map<UUID, Drain> DRAINS = new HashMap<>();
    public static float scale(Entity entity) {
        return entity.getPersistentData().contains(SCALE) ? Math.max(.01F, entity.getPersistentData().getFloat(SCALE)) : 1F;
    }
    public static boolean start(ServerPlayer owner, LivingEntity mob) {
        if (DRAINS.containsKey(mob.getUUID()) || DRAINS.size() >= 64) return false;
        var data = mob.getPersistentData();
        data.putString(OLD_TEAM, mob.getTeam() == null ? "" : mob.getTeam().getName());
        data.putBoolean(GLOW, mob.isCurrentlyGlowing());
        var board = mob.level().getScoreboard();
        PlayerTeam team = board.getPlayerTeam("dai_hunger");
        if (team == null) { team = board.addPlayerTeam("dai_hunger"); team.setColor(ChatFormatting.RED); }
        board.addPlayerToTeam(mob.getScoreboardName(), team); mob.setGlowingTag(true);
        DRAINS.put(mob.getUUID(), new Drain(mob, owner.getUUID(), scale(mob)));
        return true;
    }
    private static void cleanup(LivingEntity mob) {
        var data = mob.getPersistentData();
        if (!data.contains(OLD_TEAM)) return;
        var board = mob.level().getScoreboard();
        var current = board.getPlayersTeam(mob.getScoreboardName());
        if (current != null && current.getName().equals("dai_hunger")) {
            board.removePlayerFromTeam(mob.getScoreboardName());
            var previous = board.getPlayerTeam(data.getString(OLD_TEAM));
            if (previous != null) board.addPlayerToTeam(mob.getScoreboardName(), previous);
        }
        mob.setGlowingTag(data.getBoolean(GLOW)); data.remove(OLD_TEAM); data.remove(GLOW);
    }
    @SubscribeEvent public static void size(EntityEvent.Size event) {
        if (!event.getEntity().isAddedToWorld()) return;
        float scale = scale(event.getEntity());
        if (scale < .999F) { event.setNewSize(event.getNewSize().scale(scale)); event.setNewEyeHeight(event.getNewEyeHeight() * scale); }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity mob)) return;
        if (event.loadedFromDisk()) cleanup(mob);
        if (scale(mob) < .999F) event.getLevel().getServer().execute(mob::refreshDimensions);
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getTarget() instanceof LivingEntity mob && scale(mob) < .999F)
            ModNetwork.state(p, mob.getId(), scale(mob), -1);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var iterator = DRAINS.values().iterator();
        while (iterator.hasNext()) {
            Drain drain = iterator.next(); if (drain.mob.level() != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(drain.owner);
            if (!drain.mob.isAlive() || owner == null || !owner.isAlive() || owner.level() != level || drain.mob.distanceToSqr(owner) > 32 * 32) {
                cleanup(drain.mob); iterator.remove(); continue;
            }
            if (++drain.age % 20 == 0 && drain.age <= 100) {
                float next = Math.max(.25F, drain.original * (1F - .1F * (drain.age / 20)));
                drain.mob.getPersistentData().putFloat(SCALE, next); drain.mob.refreshDimensions();
                ModNetwork.trackingState(drain.mob, next);
                drain.mob.animateHurt(0); drain.mob.playSound(SoundEvents.GENERIC_HURT, .7F, 1.1F);
                Vec3 start = drain.mob.position().add((level.random.nextDouble() - .5) * .8, drain.mob.getBbHeight() * .7, (level.random.nextDouble() - .5) * .8);
                ModNetwork.food(drain.mob, start, owner);
                drain.deliveries.add(drain.age + 20);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, start.x, start.y, start.z, 3, .15, .15, .15, .03);
            }
            if (drain.deliveries.remove(Integer.valueOf(drain.age))) {
                int food = owner.getFoodData().getFoodLevel();
                int restored = Math.min(2, 20 - food);
                owner.getFoodData().setFoodLevel(food + restored);
                int reserve = Math.min(100, owner.getPersistentData().getInt(RESERVE) + 2 - restored);
                owner.getPersistentData().putInt(RESERVE, reserve);
                ModNetwork.state(owner, owner.getId(), 1, reserve);
                level.playSound(null, owner.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, .8F, .9F + level.random.nextFloat() * .3F);
            }
            // The last bite arrives after the channel and glow have finished.
            if (drain.age == 100) cleanup(drain.mob);
            if (drain.age >= 120) { iterator.remove(); }
        }
    }
    @SubscribeEvent public static void foodTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        var data = p.getPersistentData();
        int reserve = data.getInt(RESERVE), food = p.getFoodData().getFoodLevel();
        if (food < 20 && reserve > 0) {
            int restored = Math.min(20 - food, reserve); p.getFoodData().setFoodLevel(food + restored); reserve -= restored;
            data.putInt(RESERVE, reserve);
        }
        if (reserve != data.getInt("deathsareitems.SyncedFoodReserve") || p.tickCount % 20 == 0) {
            data.putInt("deathsareitems.SyncedFoodReserve", reserve);
            ModNetwork.state(p, p.getId(), 1, reserve);
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData(); var data = event.getEntity().getPersistentData();
        data.putInt(RESERVE, old.getInt(RESERVE));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { DRAINS.values().forEach(d -> cleanup(d.mob)); DRAINS.clear(); }
    private static final class Drain {
        final LivingEntity mob; final UUID owner; final float original; final List<Integer> deliveries = new ArrayList<>(); int age;
        Drain(LivingEntity mob, UUID owner, float original) { this.mob=mob; this.owner=owner; this.original=original; }
    }
}
