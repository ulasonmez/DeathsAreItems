package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.custom.ChaosPearlEntity;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class PearlAbility {
    private static final Map<UUID, Burst> BURSTS = new LinkedHashMap<>();
    private static final String IMPACT = "deathsareitems.PearlImpact";
    public static boolean holdsShield(Player p) { return p.getMainHandItem().is(ModItems.ENDER_PEARL_SHIELD.get()) || p.getOffhandItem().is(ModItems.ENDER_PEARL_SHIELD.get()); }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void vanillaPearl(EntityTeleportEvent.EnderPearl event) {
        if (event.isCanceled()) return;
        ServerPlayer p = event.getPlayer(); p.getPersistentData().putLong(IMPACT, p.level().getGameTime());
        if (holdsShield(p)) event.setAttackDamage(0);
    }
    public static boolean isPearlImpact(LivingEntity p) {
        return p.getPersistentData().contains(IMPACT) && p.getPersistentData().getLong(IMPACT) == p.level().getGameTime();
    }
    public static boolean throwPearl(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return false;
        ChaosPearlEntity pearl = ModEntities.CHAOS_PEARL.get().create(p.serverLevel()); if (pearl == null) return false;
        pearl.setOwner(p); pearl.setPos(p.getEyePosition().add(0,-.12,0));
        pearl.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.65F, .3F);
        if (!p.level().addFreshEntity(pearl)) return false;
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, .8F, .65F);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .7F, 1.4F);
        return true;
    }
    @SubscribeEvent public static void block(LivingAttackEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer p && holdsShield(p)
                && p.isBlocking() && p.getUseItem().is(ModItems.ENDER_PEARL_SHIELD.get())
                && event.getSource().getEntity() instanceof Mob mob) startBurst(p, mob);
    }
    @SubscribeEvent public static void defend(LivingHurtEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer p && holdsShield(p)
                && event.getSource().getEntity() instanceof Mob mob) startBurst(p, mob);
    }
    public static boolean startBurst(ServerPlayer p, Mob mob) {
        if (!mob.isAlive() || mob.isInvulnerable() || mob.level() != p.level() || mob.distanceToSqr(p) > 128 * 128
                || MobRestraints.busy(mob) || BURSTS.size() >= 16
                || BURSTS.values().stream().anyMatch(b -> b.mob == mob)) return false;
        UUID id = UUID.randomUUID(); Burst burst = new Burst(p, mob, id); BURSTS.put(id, burst);
        MobRestraints.acquire(mob, burst.restraint); MobRestraints.freeze(mob, mob.position());
        Vec3 center = mob.position().add(0,Math.min(.7, mob.getBbHeight() * .4),0);
        for (int i = 0; i < 60; i++) {
            ChaosPearlEntity pearl = ModEntities.CHAOS_PEARL.get().create(p.serverLevel()); if (pearl == null) continue;
            double azimuth = i * Math.PI * (3 - Math.sqrt(5));
            double y = .28 + (i + .5) / 60 * .68, horizontal = Math.sqrt(1-y*y), speed = .65 + (i % 7) * .09;
            pearl.setOwner(p); pearl.retaliation(mob.getUUID(), id);
            pearl.setPos(center); pearl.setDeltaMovement(Math.cos(azimuth)*horizontal*speed, y*speed, Math.sin(azimuth)*horizontal*speed);
            pearl.getPersistentData().putBoolean("deathsareitems.TemporaryVisual", true);
            if (p.level().addFreshEntity(pearl)) burst.pearls.add(pearl);
        }
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 100, .3, .6, .3, .6);
        p.level().playSound(null, mob.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, .9F, .75F);
        return true;
    }
    public static void land(ChaosPearlEntity pearl, UUID victim, UUID id, Vec3 hit) {
        if (!(pearl.level() instanceof ServerLevel level)) return;
        if (victim != null) {
            Burst b = BURSTS.get(id);
            if (b != null && b.mob.getUUID().equals(victim) && b.destinations.size() < 60) b.destinations.add(hit);
        } else if (pearl.getOwner() instanceof ServerPlayer p && p.level() == level && p.isAlive() && !p.isSleeping()) {
            Vec3 destination = safe(level, p, hit);
            if (destination == null) return;
            var event = new EntityTeleportEvent.EnderPearl(p, destination.x, destination.y, destination.z, pearl, 0F, new net.minecraft.world.phys.BlockHitResult(hit, Direction.UP, BlockPos.containing(hit), false));
            if (MinecraftForge.EVENT_BUS.post(event)) return;
            destination = safe(level, p, new Vec3(event.getTargetX(),event.getTargetY(),event.getTargetZ()));
            if (destination == null) return;
            effect(level, p.position()); p.stopRiding(); p.connection.teleport(destination.x,destination.y,destination.z,p.getYRot(),p.getXRot());
            p.fallDistance = 0; effect(level, destination);
        }
    }
    /** Keep teleports loaded, inside the border and outside solid blocks, even on wall impacts. */
    public static Vec3 safe(ServerLevel level, Entity entity, Vec3 hit) {
        for (int dy = 0; dy <= 3; dy++) for (double[] offset : new double[][]{{0,0},{.7,0},{-.7,0},{0,.7},{0,-.7}}) {
            Vec3 candidate = hit.add(offset[0],dy + .08,offset[1]); BlockPos cell = BlockPos.containing(candidate);
            if (!level.hasChunkAt(cell) || level.isOutsideBuildHeight(cell) || !level.getWorldBorder().isWithinBounds(cell)) continue;
            if (level.noCollision(entity, entity.getBoundingBox().move(candidate.subtract(entity.position())))) return candidate;
        }
        return null;
    }
    private static void effect(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + .7, pos.z, 24, .3, .6, .3, .25);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .55F, .85F + level.random.nextFloat() * .4F);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var iterator = BURSTS.values().iterator();
        while (iterator.hasNext()) {
            Burst b = iterator.next(); if (b.level != level) continue;
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(b.owner);
            if (p == null || !p.isAlive() || p.level() != level || !b.mob.isAlive() || b.mob.level() != level || ++b.age > 240) {
                b.cleanup(); iterator.remove(); continue;
            }
            MobRestraints.freeze(b.mob, b.mob.position());
            // Landings queue instead of sixty same-tick teleports collapsing to one visible position.
            if (!b.destinations.isEmpty() && b.age % 2 == 0) {
                Vec3 at = b.destinations.removeFirst(); Vec3 destination = safe(level, b.mob, at);
                if (destination != null) {
                    effect(level, b.mob.position()); b.mob.teleportTo(destination.x,destination.y,destination.z);
                    MobRestraints.freeze(b.mob, destination); effect(level, destination);
                }
                b.impacts++; b.mob.invulnerableTime = 0;
                DeathDamage.hurt(b.mob, DeathDamage.CHAOS, p, b.impacts >= 60 ? Float.MAX_VALUE : b.damage);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { BURSTS.values().forEach(Burst::cleanup); BURSTS.clear(); }
    private static final class Burst {
        final ServerLevel level; final UUID owner; final Mob mob; final String restraint;
        final LinkedList<Vec3> destinations = new LinkedList<>(); final List<ChaosPearlEntity> pearls = new ArrayList<>(); int age, impacts; final float damage;
        Burst(ServerPlayer owner, Mob mob, UUID id) { this.level=owner.serverLevel(); this.owner=owner.getUUID(); this.mob=mob; this.restraint="pearl."+id; this.damage=Math.max(.01F,mob.getHealth()/61F); }
        void cleanup() { pearls.forEach(Entity::discard); MobRestraints.release(mob, restraint); }
    }
    private PearlAbility() {}
}
