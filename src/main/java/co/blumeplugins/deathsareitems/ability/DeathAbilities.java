package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.item.custom.DeathToolItem.Kind;
import co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class DeathAbilities {
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();
    private static final Map<UUID, Drowning> DROWNING = new HashMap<>();
    private static final List<Morph> MORPH_QUEUE = new ArrayList<>();
    private static final List<Arrow> ARROWS = new ArrayList<>();
    private static final String MASK_EFFECTS = "deathsareitems.MaskEffects";
    private static final List<MobEffect> WATER_EFFECTS = List.of(MobEffects.CONDUIT_POWER, MobEffects.DOLPHINS_GRACE, MobEffects.WATER_BREATHING);
    private static final List<EntityType<? extends Mob>> MORPHS = List.of(
            EntityType.ALLAY, EntityType.AXOLOTL, EntityType.BAT, EntityType.BEE, EntityType.BLAZE,
            EntityType.CAMEL, EntityType.CAT, EntityType.CAVE_SPIDER, EntityType.CHICKEN, EntityType.COD,
            EntityType.COW, EntityType.CREEPER, EntityType.DOLPHIN, EntityType.DONKEY, EntityType.DROWNED,
            EntityType.ELDER_GUARDIAN, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER,
            EntityType.FOX, EntityType.FROG, EntityType.GHAST, EntityType.GLOW_SQUID, EntityType.GOAT,
            EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HORSE, EntityType.HUSK, EntityType.IRON_GOLEM,
            EntityType.LLAMA, EntityType.MAGMA_CUBE, EntityType.MOOSHROOM, EntityType.MULE, EntityType.OCELOT,
            EntityType.PANDA, EntityType.PARROT, EntityType.PHANTOM, EntityType.PIG, EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.POLAR_BEAR, EntityType.PUFFERFISH,
            EntityType.RABBIT, EntityType.RAVAGER, EntityType.SALMON, EntityType.SHEEP, EntityType.SHULKER,
            EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SKELETON_HORSE, EntityType.SLIME,
            EntityType.SNIFFER, EntityType.SNOW_GOLEM, EntityType.SPIDER, EntityType.SQUID, EntityType.STRAY,
            EntityType.STRIDER, EntityType.TADPOLE, EntityType.TRADER_LLAMA, EntityType.TROPICAL_FISH,
            EntityType.TURTLE, EntityType.VEX, EntityType.VILLAGER, EntityType.VINDICATOR,
            EntityType.WANDERING_TRADER, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.WOLF,
            EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_HORSE, EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIFIED_PIGLIN);
    public static boolean mask(LivingEntity entity) { return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DROWNING_MASK.get()); }
    public static boolean carries(Player player, Item item) {
        for (int slot=0;slot<player.getInventory().getContainerSize();slot++) if (player.getInventory().getItem(slot).is(item)) return true;
        return false;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void immunity(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var s = event.getSource();
        if ((mask(p) && s.is(DamageTypes.DROWN))
                || (s.is(DamageTypes.IN_WALL) && carries(p, ModItems.SUFFOCATION_BOULDER.get()))
                || (s.is(DamageTypes.STARVE) && carries(p, ModItems.HUNGER_STAFF.get()))
                || (s.is(DamageTypes.FALLING_ANVIL) && carries(p, ModItems.SWARM_OF_ANVILS.get()))
                || (s.is(DamageTypes.ARROW) && s.getEntity() instanceof net.minecraft.world.entity.monster.Skeleton && carries(p, ModItems.SKELETON_BOW.get()))
                || (s.getEntity() instanceof net.minecraft.world.entity.animal.IronGolem && carries(p, ModItems.MOB_PICKAXE.get()))) event.setCanceled(true);
    }
    public static InteractionResultHolder<ItemStack> useTool(Player player, InteractionHand hand, Kind kind) {
        if (kind == Kind.STAFF) {
            LivingEntity target = target(player, 24);
            if (target != null) return new InteractionResultHolder<>(useMob(player, target, hand, kind), player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    public static InteractionResult useBlock(Player player, InteractionHand hand, BlockPos pos, Kind kind) {
        if (player == null || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer server = (ServerPlayer)player;
        if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) return InteractionResult.FAIL;
        boolean ok = kind == Kind.BOULDER ? BoulderAbility.mark(server, pos) : AnvilSwarmAbility.start(server, pos, null);
        if (ok) player.getCooldowns().addCooldown(player.getItemInHand(hand).getItem(), 20);
        return ok ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
    public static InteractionResult useMob(Player player, LivingEntity target, InteractionHand hand, Kind kind) {
        if (hand != InteractionHand.MAIN_HAND || !eligible(player, target)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer server = (ServerPlayer)player;
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResult.FAIL;
        boolean ok = switch (kind) {
            case BOULDER -> BoulderAbility.trap(server, target);
            case ANVILS -> AnvilSwarmAbility.start(server, null, target);
            case STAFF -> HungerAbility.start(server, target);
            default -> false;
        };
        if (ok) {
            player.getCooldowns().addCooldown(stack.getItem(), kind == Kind.STAFF ? 110 : 20);
            if (kind == Kind.STAFF) player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    public static boolean eligible(Player owner, LivingEntity target) { return eligible(owner, target, 32); }
    public static boolean eligible(Player owner, LivingEntity target, double range) {
        return target instanceof Mob && target.isAlive() && !target.isInvulnerable() && target.level() == owner.level()
                && target.distanceToSqr(owner) <= range * range && owner.hasLineOfSight(target)
                && !(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                && !(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                && !(target instanceof TamableAnimal tame && tame.isTame());
    }
    /** Server ray cast clips entities at the first opaque block. */
    public static LivingEntity target(Player player, double range) {
        Vec3 start = player.getEyePosition(), end = start.add(player.getLookAngle().scale(range));
        var block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().expandTowards(limit.subtract(start)).inflate(1),
                        e -> eligible(player, e)).stream()
                .filter(e -> e.getBoundingBox().inflate(.25).clip(start, limit).isPresent())
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }
    public static void shootArrow(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        Arrow arrow = new Arrow(player.level(), player);
        arrow.setBaseDamage(2.5); arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.getPersistentData().putBoolean("deathsareitems.Arrow", true);
        arrow.getPersistentData().putBoolean("deathsareitems.TemporaryVisual", true);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.1F, 1F);
        player.level().addFreshEntity(arrow);
        ARROWS.add(arrow);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, .24F, .85F + player.getRandom().nextFloat() * .3F);
    }
    @SubscribeEvent public static void arrowImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow) || !arrow.getPersistentData().getBoolean("deathsareitems.Arrow")) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            event.setCanceled(true);
            if (arrow.getOwner() instanceof ServerPlayer owner && eligible(owner, target)) {
                target.invulnerableTime = 0;
                DeathDamage.hurt(target, DeathDamage.ARROWS, owner, 3F);
            }
            arrow.discard();
        } else if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) { event.setCanceled(true); arrow.discard(); }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void mining(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.MOB_PICKAXE.get()) || !stack.getOrCreateTag().getBoolean(MobPickaxeItem.ENABLED)) return;
        // Server.execute may run inline, while the mined block is still solid. Wait for the next level tick.
        MORPH_QUEUE.add(new Morph(player, player.serverLevel(), event.getPos().immutable(), event, player.level().getGameTime()));
    }
    private static boolean spawnMorph(Morph request) {
        ServerLevel level = request.level;
        List<EntityType<? extends Mob>> types = new ArrayList<>(MORPHS);
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) types.removeIf(type -> type.getCategory() == MobCategory.MONSTER);
        Collections.shuffle(types, new Random(level.random.nextLong()));
        BlockPos pos = request.pos;
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) return true;
        for (EntityType<? extends Mob> type : types) {
            Mob mob = type.create(level);
            if (mob == null) continue;
            // Morph in the mined cell even if the mob overlaps walls, other mobs or fluids.
            mob.moveTo(pos.getX() + .5, pos.getY() + .02, pos.getZ() + .5, level.random.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
            if (!level.addFreshEntity(mob)) { mob.discard(); return true; }
            level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + .3, mob.getZ(), 12, .3, .3, .3, .02);
            level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, .5F, 1.2F);
            return true;
        }
        return false;
    }
    /** Only held-state is supplied by the client; equipment, targets and flight are server-owned. */
    public static void input(ServerPlayer player, boolean held) {
        if (!held) { release(player); return; }
        if (!mask(player) || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()
                || !player.isAlive() || player.isSpectator()) { cancel(player.getUUID()); return; }
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) {
            if (player.getPersistentData().getLong("deathsareitems.TridentCooldown") > player.level().getGameTime()) return;
            ThrownTrident trident = new co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity(player.level(), player);
            trident.setNoGravity(true); trident.setNoPhysics(true); trident.setGlowingTag(true);
            trident.pickup = AbstractArrow.Pickup.DISALLOWED;
            trident.getPersistentData().putBoolean("deathsareitems.TemporaryVisual", true);
            player.level().addFreshEntity(trident);
            charge = new Charge(trident, player.getLookAngle()); CHARGES.put(player.getUUID(), charge);
            player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, .6F, 1.3F);
        }
        charge.lastInput = player.level().getGameTime();
    }
    private static void release(ServerPlayer player) {
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null || charge.flying) return;
        charge.flying = true; charge.life = 0;
        ((co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity)charge.trident).launch();
        player.getPersistentData().putLong("deathsareitems.TridentCooldown", player.level().getGameTime() + 30);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, .8F, .85F);
    }
    private static void cancel(UUID id) { Charge c = CHARGES.remove(id); if (c != null) c.trident.discard(); }
    private static void waterEffects(ServerPlayer player) {
        boolean worn = mask(player) && player.isAlive() && !player.isSpectator();
        // Remove short level-III effects left by the previous version before migrating.
        int legacy = player.getPersistentData().getInt(MASK_EFFECTS);
        for (int i = 0; i < WATER_EFFECTS.size(); i++) {
            MobEffect effect = WATER_EFFECTS.get(i);
            MobEffectInstance current = player.getEffect(effect);
            if ((legacy & (1 << i)) != 0 && current != null && current.getAmplifier() == 2
                    && current.getDuration() <= 15 && current.isAmbient() && !current.isVisible()) player.removeEffect(effect);
            EquipmentEffects.update(player, "deathsareitems.Mask." + i, effect, worn);
        }
        player.getPersistentData().remove(MASK_EFFECTS);
        if (worn) player.setAirSupply(player.getMaxAirSupply());
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        waterEffects(player);
        Charge c = CHARGES.get(player.getUUID());
        if (c == null) return;
        if (!player.isAlive() || !mask(player) || player.isSpectator() || player.level() != c.trident.level()
                || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty() || ++c.life > 400) { cancel(player.getUUID()); return; }
        if (!c.flying) {
            if (player.level().getGameTime() - c.lastInput > 12) { cancel(player.getUUID()); return; }
            c.direction = co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.followAim(c.direction, player.getLookAngle());
            c.trident.setPos(co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.heldPosition(player, 1));
        } else {
            Vec3 start = c.trident.position(), end = start.add(c.direction.scale(4.0));
            var block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            LivingEntity hit = player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, limit).inflate(.7), e -> eligible(player, e)).stream()
                    .filter(e -> e.getBoundingBox().inflate(.4).clip(start, limit).isPresent()).min(Comparator.comparingDouble(e -> e.distanceToSqr(start))).orElse(null);
            if (hit != null) {
                DROWNING.put(hit.getUUID(), new Drowning(hit, player.getUUID(), 0));
                player.serverLevel().sendParticles(ParticleTypes.SPLASH, hit.getX(), hit.getY() + .7, hit.getZ(), 30, .3, .5, .3, .1);
                player.level().playSound(null, hit.blockPosition(), SoundEvents.PLAYER_HURT_DROWN, SoundSource.HOSTILE, .7F, 1F);
                cancel(player.getUUID()); return;
            }
            c.trident.setPos(end);
            if (block.getType() != HitResult.Type.MISS || c.life > 25) { cancel(player.getUUID()); return; }
        }
        c.trident.setDeltaMovement(Vec3.ZERO);
        c.trident.setYRot((float)(Math.atan2(c.direction.x, c.direction.z) * 180 / Math.PI));
        c.trident.setXRot((float)(Math.atan2(c.direction.y, c.direction.horizontalDistance()) * 180 / Math.PI));
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var morphs = MORPH_QUEUE.iterator();
        while (morphs.hasNext()) {
            Morph request = morphs.next();
            if (request.level != level || level.getGameTime() <= request.created) continue;
            if (request.event.isCanceled() || !request.player.isAlive() || request.player.level() != level
                    || !level.hasChunkAt(request.pos) || level.getGameTime() - request.created > 100) { morphs.remove(); continue; }
            if (spawnMorph(request)) morphs.remove();
        }
        ARROWS.removeIf(arrow -> {
            if (arrow.isRemoved()) return true;
            if (arrow.level() == level && arrow.tickCount > 100) { arrow.discard(); return true; }
            return false;
        });
        var iterator = DROWNING.values().iterator();
        while (iterator.hasNext()) {
            Drowning d = iterator.next(); if (d.mob.level() != level) continue;
            if (!d.mob.isAlive() || ++d.age > 100) { iterator.remove(); continue; }
            var owner = level.getServer().getPlayerList().getPlayer(d.owner);
            if (owner == null || owner.level() != level) { iterator.remove(); continue; }
            if (d.age % 3 == 0) level.sendParticles(co.blumeplugins.deathsareitems.particle.ModParticles.DROWNING_BUBBLE.get(), d.mob.getX(), d.mob.getY() + d.mob.getBbHeight() * .6, d.mob.getZ(), 8, .4, .6, .4, .02);
            if (d.age % 20 == 0) {
                d.mob.invulnerableTime = 0;
                DeathDamage.hurt(d.mob, DeathDamage.DROWN, owner, 5);
                level.playSound(null, d.mob.blockPosition(), SoundEvents.PLAYER_HURT_DROWN, SoundSource.HOSTILE, .6F, 1);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void mobDeath(LivingDeathEvent event) {
        // Let pickaxe attacks complete normally, including cooldown, criticals and knockback.
        // Only the death announcement needs the custom source; nested hurt calls lose damage to immunity.
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getEntity() instanceof ServerPlayer owner
                && owner.getMainHandItem().is(ModItems.MOB_PICKAXE.get())) {
            DeathDamage.broadcastMob(event.getEntity(), DeathDamage.source(event.getEntity(), DeathDamage.MOBS, owner));
            return;
        }
        if (event.getSource().is(DeathDamage.ARROWS) || event.getSource().is(DeathDamage.MOBS) || event.getSource().is(DeathDamage.DROWN)
                || event.getSource().is(DeathDamage.SUFFOCATE) || event.getSource().is(DeathDamage.ANVILS)) DeathDamage.broadcastMob(event.getEntity(), event.getSource());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { cancel(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { CHARGES.clear(); DROWNING.clear(); ARROWS.clear(); MORPH_QUEUE.clear(); }
    private record Morph(ServerPlayer player, ServerLevel level, BlockPos pos, BlockEvent.BreakEvent event, long created) {}
    private static final class Charge {
        final ThrownTrident trident; Vec3 direction; long lastInput; int life; boolean flying;
        Charge(ThrownTrident trident, Vec3 direction) { this.trident = trident; this.direction = direction; }
    }
    private static final class Drowning {
        final LivingEntity mob; final UUID owner; int age;
        Drowning(LivingEntity mob, UUID owner, int age) { this.mob = mob; this.owner = owner; this.age = age; }
    }
}
