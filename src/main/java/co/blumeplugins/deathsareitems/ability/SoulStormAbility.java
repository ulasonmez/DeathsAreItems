package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import co.blumeplugins.deathsareitems.particle.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class SoulStormAbility {
    public static final String SOULS = "deathsareitems.Souls";
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    private static final List<Soul> SOULS_IN_FLIGHT = new ArrayList<>();
    private static final List<Bolt> BOLTS = new ArrayList<>();
    public static int souls(ItemStack stack) { return stack.hasTag() ? Math.max(0, stack.getTag().getInt(SOULS)) : 0; }
    private static void store(ItemStack stack, int value) { stack.getOrCreateTag().putInt(SOULS, Math.max(0, value)); }
    public static Component counter(ItemStack stack) {
        return Component.translatable("message.deathsareitems.gathered_souls",
                Component.translatable("message.deathsareitems.souls").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(Integer.toString(souls(stack))).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
    }
    private static boolean carries(ServerPlayer owner, ItemStack stack) {
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) if (owner.getInventory().getItem(i) == stack) return true;
        return false;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void immunity(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().is(DamageTypes.WITHER)
                && DeathAbilities.carries(p, ModItems.SOUL_STORM.get())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (event.getSource().is(DeathDamage.SOUL)) DeathDamage.broadcastMob(event.getEntity(), event.getSource());
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        ItemStack held = p.getMainHandItem().is(ModItems.SOUL_STORM.get()) ? p.getMainHandItem() : p.getOffhandItem();
        if (held.is(ModItems.SOUL_STORM.get()) && souls(held) > 0 && p.isAlive() && !p.isSpectator() && p.tickCount % 5 == 0) p.displayClientMessage(counter(held), true);
        boolean channeling = p.isAlive() && !p.isSpectator() && p.isShiftKeyDown() && p.isUsingItem() && p.getUseItem().is(ModItems.SOUL_STORM.get());
        if (!channeling) { CHANNELS.remove(p.getUUID()); return; }
        Channel channel = CHANNELS.computeIfAbsent(p.getUUID(), id -> new Channel(p.getUseItem()));
        if (channel.stack != p.getUseItem()) { channel = new Channel(p.getUseItem()); CHANNELS.put(p.getUUID(), channel); }
        int age = ++channel.age;
        if (age <= 32) spiral(p, age);
        if (age % 32 == 1) p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, .55F, .7F);
        if (age % 4 == 0) harvest(p, channel.stack, age % 12 == 4);
    }
    private static void spiral(ServerPlayer p, int age) {
        ServerLevel level = p.serverLevel();
        for (int strand = 0; strand < 2; strand++) for (int step = 0; step < 5; step++) {
            double t = (age - 1 + step / 5D) / 32D;
            double angle = t * Math.PI * 4 + strand * Math.PI, radius = .85 + t * 1.8;
            Vec3 at = p.position().add(Math.cos(angle) * radius, t * (p.getBbHeight() + 2), Math.sin(angle) * radius);
            level.sendParticles(ModParticles.SOUL_FLAME.get(), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
    private static void harvest(ServerPlayer owner, ItemStack stack, boolean showGlass) {
        var level = owner.serverLevel();
        var targets = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(50),
                mob -> DeathAbilities.eligible(owner, mob, 50) && mob.distanceToSqr(owner) <= 2500);
        targets.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(owner)));
        for (Mob mob : targets.subList(0, Math.min(24, targets.size()))) {
            if (SOULS_IN_FLIGHT.size() >= 256) break;
            Vec3 start = mob.position().add(0, mob.getBbHeight() * .65, 0);
            // Half a point per pulse; do not bypass the target's hurt/death cancellation hooks.
            var harvesters = mob.getPersistentData().getCompound("deathsareitems.SoulHarvesters");
            harvesters.putBoolean(owner.getStringUUID(), true);
            mob.getPersistentData().put("deathsareitems.SoulHarvesters", harvesters);
            mob.invulnerableTime = 0;
            if (!mob.hurt(DeathDamage.source(mob, DeathDamage.SOUL, owner), .5F)) continue;
            if (mob.getTarget() == owner) mob.setTarget(null);
            if (mob.getLastHurtByMob() == owner) mob.setLastHurtByMob(null);
            if (mob instanceof NeutralMob neutral && owner.getUUID().equals(neutral.getPersistentAngerTarget())) {
                neutral.setPersistentAngerTarget(null); neutral.setRemainingPersistentAngerTime(0);
            }
            float next = Math.max(.01F, HungerAbility.scale(mob) - .05F);
            if (showGlass && mob.isAlive()) {
                mob.getPersistentData().putFloat(HungerAbility.SCALE, next); mob.refreshDimensions(); ModNetwork.trackingState(mob, next);
                if (next <= .011F) {
                    // Finish a nearly invisible mob through normal real damage and Forge death events.
                    mob.invulnerableTime = 0;
                    mob.hurt(DeathDamage.source(mob, DeathDamage.SOUL, owner), mob.getMaxHealth() + 1);
                }
            }
            Display.BlockDisplay glass = showGlass
                    ? AbilityVisuals.block(level, start, Blocks.WHITE_STAINED_GLASS.defaultBlockState(), .23F, 24, -1) : null;
            SOULS_IN_FLIGHT.add(new Soul(owner.getUUID(), level, stack, glass, start));
        }
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && event.getEntity().getPersistentData()
                .getCompound("deathsareitems.SoulHarvesters").getBoolean(event.getNewTarget().getStringUUID())) event.setCanceled(true);
    }
    public static boolean fire(ServerPlayer owner, ItemStack stack) {
        if (!owner.isAlive() || owner.isSpectator() || !stack.is(ModItems.SOUL_STORM.get()) || !carries(owner, stack) || BOLTS.size() >= 64) return false;
        int available = souls(stack);
        if (available == 0) return false;
        store(stack, available - 1);
        BOLTS.add(new Bolt(owner.getUUID(), owner.serverLevel(), stack, owner.getEyePosition().add(owner.getLookAngle().scale(.4)), owner.getLookAngle(), Math.min(96D, available * 4D)));
        owner.serverLevel().playSound(null, owner.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, .65F, .85F);
        owner.displayClientMessage(counter(stack), true);
        return true;
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var souls = SOULS_IN_FLIGHT.iterator();
        while (souls.hasNext()) {
            Soul soul = souls.next(); if (soul.level != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(soul.owner);
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level || soul.glass != null && soul.glass.isRemoved()
                    || !carries(owner, soul.stack) || owner.position().distanceToSqr(soul.pos) > 72 * 72) {
                soul.discard(); souls.remove(); continue;
            }
            double t = Math.min(1, ++soul.age / (double)soul.duration);
            Vec3 end = owner.position().add(0, owner.getBbHeight() * .65, 0);
            Vec3 at = soul.start.lerp(end, t).add(0, Math.sin(t * Math.PI) * .7, 0);
            if (soul.glass != null) soul.glass.setPos(at);
            // Fill the swept path so even a soul from 50 blocks away forms a flowing stream.
            Vec3 velocity = end.subtract(at).normalize().scale(.08);
            int samples = Math.min(4, Math.max(1, (int)Math.ceil(soul.pos.distanceTo(at) / .65)));
            for (int sample = 1; sample <= samples; sample++) {
                Vec3 mote = soul.pos.lerp(at, sample / (double)samples);
                level.sendParticles(owner, ModParticles.WHITE_SOUL.get(), true, mote.x, mote.y, mote.z,
                        0, velocity.x, velocity.y, velocity.z, 1);
            }
            soul.pos = at;
            if (t >= 1) {
                store(soul.stack, (int)Math.min(Integer.MAX_VALUE, (long)souls(soul.stack) + 1));
                soul.discard(); souls.remove();
                owner.displayClientMessage(counter(soul.stack), true);
                // One quiet pickup chime for a group arriving on the same tick.
                if (owner.getPersistentData().getLong("deathsareitems.SoulChime") != level.getGameTime()) {
                    level.playSound(null, owner.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .22F, 1.4F);
                    owner.getPersistentData().putLong("deathsareitems.SoulChime", level.getGameTime());
                }
            }
        }
        var bolts = BOLTS.iterator();
        while (bolts.hasNext()) {
            Bolt bolt = bolts.next(); if (bolt.level != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(bolt.owner);
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level || !carries(owner, bolt.stack)) { bolts.remove(); continue; }
            if (advance(owner, bolt)) bolts.remove();
        }
    }
    private static boolean advance(ServerPlayer owner, Bolt bolt) {
        var level = bolt.level;
        // Small swept segments prevent tunnelling and never ask the world to load a distant chunk.
        for (int step = 0; step < 5; step++) {
            if (bolt.travelled >= bolt.range - .001) { impact(owner, bolt, null); return true; }
            int required = 1 + (int)Math.floor((bolt.travelled + .001) / 4);
            if (required > bolt.paid) {
                if (souls(bolt.stack) == 0) { impact(owner, bolt, null); return true; }
                store(bolt.stack, souls(bolt.stack) - 1); bolt.paid++;
            }
            Vec3 end = bolt.pos.add(bolt.direction.scale(.5));
            BlockPos cell = BlockPos.containing(end);
            if (!level.hasChunkAt(cell) || level.isOutsideBuildHeight(cell) || !level.getWorldBorder().isWithinBounds(cell)) return true;
            var block = level.clip(new ClipContext(bolt.pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            var entity = ProjectileUtil.getEntityHitResult(owner, bolt.pos, limit, new AABB(bolt.pos, limit).inflate(.5),
                    e -> e instanceof EndCrystal || e instanceof net.minecraft.world.entity.boss.EnderDragonPart part && part.parentMob.isAlive()
                            || e instanceof Mob mob && mob.isAlive() && !mob.isInvulnerable()
                            && !co.blumeplugins.deathsareitems.einfach.Companions.isCompanion(mob)
                            && !(mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                            && !(mob instanceof TamableAnimal tame && tame.isTame()), bolt.pos.distanceToSqr(limit));
            Vec3 next = entity == null ? limit : entity.getLocation();
            bolt.travelled += bolt.pos.distanceTo(next); bolt.pos = next;
            level.sendParticles(ModParticles.SOUL_SMOKE.get(), next.x, next.y, next.z, 3, .12, .12, .12, .01);
            level.sendParticles(ModParticles.SOUL_FLAME.get(), next.x, next.y, next.z, 1, .06, .06, .06, .003);
            if (++bolt.segments % 5 == 0) level.sendParticles(ModParticles.SOUL_WAVE.get(), next.x, next.y, next.z, 1, 0, 0, 0, 0);
            if (entity != null || block.getType() != HitResult.Type.MISS) { impact(owner, bolt, entity == null ? null : entity.getEntity()); return true; }
        }
        return false;
    }
    private static void impact(ServerPlayer owner, Bolt bolt, Entity target) {
        if (target instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            damageDragon(owner, part.parentMob);
        } else if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            damageDragon(owner, dragon);
        } else if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0; living.hurt(DeathDamage.source(living, DeathDamage.SOUL, owner), 24F);
        } else if (target instanceof EndCrystal crystal) {
            crystal.hurt(owner.damageSources().playerAttack(owner), 24F);
        }
        OwnedExplosions.soulBlast(owner, bolt.pos);
        bolt.level.sendParticles(ModParticles.SOUL_SMOKE.get(), bolt.pos.x, bolt.pos.y, bolt.pos.z, 20, .7, .7, .7, .04);
        bolt.level.sendParticles(ModParticles.SOUL_FLAME.get(), bolt.pos.x, bolt.pos.y, bolt.pos.z, 16, .6, .6, .6, .035);
        owner.displayClientMessage(counter(bolt.stack), true);
    }
    private static void damageDragon(ServerPlayer owner, net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
        dragon.invulnerableTime = 0;
        // Route through a dragon part with player attribution; body hits otherwise quarter the damage.
        dragon.hurt(dragon.head, DeathDamage.source(dragon, DeathDamage.SOUL, owner), 16F);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID(); CHANNELS.remove(id);
        SOULS_IN_FLIGHT.removeIf(s -> { if (!s.owner.equals(id)) return false; s.discard(); return true; });
        BOLTS.removeIf(b -> b.owner.equals(id));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        SOULS_IN_FLIGHT.forEach(s -> s.discard()); SOULS_IN_FLIGHT.clear(); BOLTS.clear(); CHANNELS.clear();
    }
    private static final class Channel {
        final ItemStack stack; int age;
        Channel(ItemStack stack) { this.stack = stack; }
    }
    private static final class Soul {
        final UUID owner; final ServerLevel level; final ItemStack stack; final Display.BlockDisplay glass;
        final Vec3 start; final int duration; Vec3 pos; int age;
        Soul(UUID owner, ServerLevel level, ItemStack stack, Display.BlockDisplay glass, Vec3 start) {
            this.owner = owner; this.level = level; this.stack = stack; this.glass = glass; this.start = start; this.pos = start;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            this.duration = player == null ? 10 : Math.min(16, 10 + (int)(player.position().distanceTo(start) / 8));
        }
        void discard() { if (glass != null) glass.discard(); }
    }
    private static final class Bolt {
        final UUID owner; final ServerLevel level; final ItemStack stack; final Vec3 direction; final double range;
        Vec3 pos; double travelled; int paid = 1, segments;
        Bolt(UUID owner, ServerLevel level, ItemStack stack, Vec3 pos, Vec3 direction, double range) {
            this.owner = owner; this.level = level; this.stack = stack; this.pos = pos; this.direction = direction; this.range = range;
        }
    }
    private SoulStormAbility() {}
}
