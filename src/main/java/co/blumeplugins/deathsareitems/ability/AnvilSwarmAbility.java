package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class AnvilSwarmAbility {
    private static final List<Swarm> SWARMS = new ArrayList<>();
    public static boolean start(ServerPlayer owner, BlockPos block, LivingEntity mob) {
        if (SWARMS.size() >= 32 || (block != null && !WorldActions.breakable(owner, block))) return false;
        if (SWARMS.stream().anyMatch(s -> s.level == owner.level() && (mob != null ? s.mob == mob : block.equals(s.block)))) return false;
        SWARMS.add(new Swarm(owner, block, mob));
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var iterator = SWARMS.iterator();
        while (iterator.hasNext()) {
            Swarm s = iterator.next(); if (s.level != level) continue;
            var owner = level.getServer().getPlayerList().getPlayer(s.owner);
            if (owner == null || !owner.isAlive() || owner.level() != level || (s.mob != null && (!s.mob.isAlive() || s.mob.level() != level))
                    || (s.block != null && (!level.hasChunkAt(s.block) || !level.getBlockState(s.block).equals(s.state)))) { s.cleanup(); iterator.remove(); continue; }
            s.freeze();
            Vec3 center = s.bounds.getCenter();
            // Spread a complete surface grid across the sequence, rather than repeatedly hitting the top.
            int due = Math.min(s.surfaces.size(), (s.age + 1) * s.surfaces.size() / 280);
            while (s.launched < due) {
                Surface surface = s.surfaces.get(s.launched++);
                Vec3 end = surface.pos.add(surface.normal.scale(.10));
                // Every flight descends from above the target, including hits low on its sides.
                // Vary azimuth and slope independently of the six coverage-grid faces.
                double azimuth = level.random.nextDouble() * Math.PI * 2;
                if (surface.normal.y == 0) azimuth = Math.atan2(surface.normal.z, surface.normal.x)
                        + (level.random.nextDouble() - .5) * 1.4;
                double rise = s.bounds.maxY + 3 + level.random.nextDouble() * 2 - end.y;
                double reach = rise * (.35 + level.random.nextDouble() * .85);
                Vec3 start = end.add(Math.cos(azimuth) * reach, rise, Math.sin(azimuth) * reach);
                var anvil = AbilityVisuals.anvil(level, start, end, 12, 300 - s.age);
                s.visuals.add(anvil);
                s.impacts.add(new Impact(end, s.age + 12));
                level.sendParticles(ParticleTypes.CLOUD, start.x, start.y, start.z, 3, .08, .08, .08, .01);
            }
            s.impacts.removeIf(impact -> {
                if (impact.at != s.age) return false;
                level.sendParticles(ParticleTypes.CRIT, impact.pos.x, impact.pos.y, impact.pos.z, 3, .07, .07, .07, .01);
                if (s.age % 3 == 0) level.playSound(null, BlockPos.containing(center), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, .18F, 1.35F + level.random.nextFloat() * .25F);
                return true;
            });
            if (++s.age < 300) continue;
            iterator.remove();
            s.cleanup();
            if (s.mob != null) {
                s.mob.invulnerableTime = 0;
                DeathDamage.hurt(s.mob, DeathDamage.ANVILS, owner, Float.MAX_VALUE);
            } else if (WorldActions.breakEvent(owner, s.block)) {
                List<ItemStack> drops = Block.getDrops(s.state, level, s.block, null, owner, new ItemStack(Items.NETHERITE_PICKAXE));
                if (level.setBlock(s.block, Blocks.AIR.defaultBlockState(), 3)) {
                    if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) for (ItemStack drop : drops) {
                        enchant255(drop, Enchantments.INFINITY_ARROWS);
                        Block.popResource(level, s.block, drop);
                    }
                    if (level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) level.getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("death.attack.lots_of_anvils", s.state.getBlock().getName()), false);
                }
            }
            level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 35, .6, .6, .6, .08);
            level.playSound(null, BlockPos.containing(center), SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, .8F, .7F);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void drops(LivingDropsEvent event) {
        if (!event.getSource().is(DeathDamage.ANVILS)) return;
        List<Enchantment> pool = BuiltInRegistries.ENCHANTMENT.stream().toList();
        if (pool.isEmpty()) return;
        for (ItemEntity drop : event.getDrops()) enchant255(drop.getItem(), pool.get(event.getEntity().getRandom().nextInt(pool.size())));
    }
    private static void enchant255(ItemStack stack, Enchantment enchantment) {
        // ItemStack.enchant narrows through a byte in 1.20.1; 255 would become -1.
        Map<Enchantment,Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        enchantments.put(enchantment,255);
        EnchantmentHelper.setEnchantments(enchantments,stack);
    }
    @SubscribeEvent public static void livingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) for (Swarm swarm : SWARMS)
            if (swarm.mob == event.getEntity()) swarm.freeze();
    }
    @SubscribeEvent public static void knockback(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
        if (SWARMS.stream().anyMatch(s -> s.mob == event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void join(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.loadedFromDisk() && event.getEntity() instanceof Mob mob) restore(mob);
    }
    private static void restore(Mob mob) {
        var data = mob.getPersistentData();
        if (!data.contains("deathsareitems.SwarmAI")) return;
        mob.setNoAi(data.getBoolean("deathsareitems.SwarmAI"));
        mob.setNoGravity(data.getBoolean("deathsareitems.SwarmGravity"));
        data.remove("deathsareitems.SwarmAI"); data.remove("deathsareitems.SwarmGravity");
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { SWARMS.forEach(Swarm::cleanup); SWARMS.clear(); }
    private record Impact(Vec3 pos, int at) {}
    private record Surface(Vec3 pos, Vec3 normal) {}
    private static List<Surface> surfaces(net.minecraft.world.phys.AABB bounds, long seed) {
        List<Surface> cells = new ArrayList<>();
        Random random = new Random(seed);
        double[] low = {bounds.minX, bounds.minY, bounds.minZ}, high = {bounds.maxX, bounds.maxY, bounds.maxZ};
        for (int axis = 0; axis < 3; axis++) for (int side : new int[]{-1, 1}) {
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            int columns = Math.max(1, (int)Math.ceil((high[u] - low[u]) / .22));
            int rows = Math.max(1, (int)Math.ceil((high[v] - low[v]) / .22));
            for (int col = 0; col < columns; col++) for (int row = 0; row < rows; row++) {
                double[] point = new double[3], normal = new double[3];
                point[axis] = side < 0 ? low[axis] : high[axis]; normal[axis] = side;
                point[u] = low[u] + (col + .2 + random.nextDouble() * .6) * (high[u] - low[u]) / columns;
                point[v] = low[v] + (row + .2 + random.nextDouble() * .6) * (high[v] - low[v]) / rows;
                cells.add(new Surface(new Vec3(point[0], point[1], point[2]), new Vec3(normal[0], normal[1], normal[2])));
            }
        }
        Collections.shuffle(cells, random);
        return cells;
    }
    private static final class Swarm {
        final ServerLevel level; final UUID owner; final BlockPos block; final BlockState state; final LivingEntity mob; final Vec3 anchor;
        final net.minecraft.world.phys.AABB bounds; final List<Surface> surfaces;
        final List<net.minecraft.world.entity.Display.BlockDisplay> visuals = new ArrayList<>();
        final List<Impact> impacts = new ArrayList<>(); int age, launched;
        final float yaw, pitch;
        Swarm(ServerPlayer owner, BlockPos block, LivingEntity mob) {
            this.level=owner.serverLevel(); this.owner=owner.getUUID(); this.block=block == null ? null : block.immutable();
            this.mob=mob; this.state=block == null ? null : level.getBlockState(block); this.anchor=mob == null ? null : mob.position();
            this.yaw=mob == null ? 0 : mob.getYRot(); this.pitch=mob == null ? 0 : mob.getXRot();
            this.bounds=mob == null ? new net.minecraft.world.phys.AABB(block) : mob.getBoundingBox();
            this.surfaces=surfaces(bounds, level.random.nextLong());
            if (mob instanceof Mob target) {
                var data=target.getPersistentData();
                data.putBoolean("deathsareitems.SwarmAI", target.isNoAi());
                data.putBoolean("deathsareitems.SwarmGravity", target.isNoGravity());
                target.stopRiding(); target.ejectPassengers(); target.setNoAi(true); target.setNoGravity(true);
                freeze();
            }
        }
        void freeze() {
            if (mob == null) return;
            mob.setDeltaMovement(Vec3.ZERO); mob.setPos(anchor); mob.setYRot(yaw); mob.setXRot(pitch);
            mob.setYHeadRot(yaw); mob.setYBodyRot(yaw); mob.setJumping(false); mob.hurtMarked=true;
            if (mob instanceof Mob target) target.getNavigation().stop();
        }
        void cleanup() {
            visuals.forEach(net.minecraft.world.entity.Entity::discard);
            if (mob instanceof Mob target) restore(target);
        }
    }
}
