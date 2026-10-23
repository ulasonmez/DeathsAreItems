package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class CrammerAbility {
    private static final List<Cage> CAGES = new ArrayList<>();
    public static boolean start(ServerPlayer owner, LivingEntity victim) {
        if (!DeathAbilities.eligible(owner, victim) || !(victim instanceof Mob mob) || CAGES.size() >= 12
                || MobRestraints.busy(mob)) return false;
        BlockPos base = victim.blockPosition(); List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y < 3; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (x != 0 || z != 0) cells.add(base.offset(x, y, z));
        // An unbreakable solid floor already supports the cage; do not reject bedrock terrain.
        if (owner.level().getBlockState(base.below()).getDestroySpeed(owner.level(), base.below()) >= 0)
            cells.add(base.below());
        List<BlockPos> clearance = new ArrayList<>(cells);
        for (int y = 0; y < Math.max(3, (int)Math.ceil(mob.getBbHeight())); y++) clearance.add(base.above(y));
        // Snow layers and plants may have no collision shape. Check actual break permission,
        // rather than the solid-block target filter shared by the other abilities.
        for (BlockPos pos : clearance) {
            BlockState old = owner.level().getBlockState(pos);
            if (!WorldActions.allowed(owner, pos) || old.getDestroySpeed(owner.level(), pos) < 0) return false;
            if (!old.isAir() && net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new net.minecraftforge.event.level.BlockEvent.BreakEvent(owner.level(), pos, old, owner))) return false;
        }
        for (BlockPos pos : clearance) {
            if (!owner.level().getBlockState(pos).isAir() && !owner.serverLevel().destroyBlock(pos, true, owner)) return false;
        }
        // Broken blocks stay broken, with their normal drops; restoring them would duplicate loot.
        TemporaryTerrain.forget(owner.serverLevel(), clearance);
        String group = "crammer." + UUID.randomUUID(); List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : cells) {
            BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
            // Connect only along the wall, including corners, rather than crossing every pane.
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(direction);
                BlockState adjacent = owner.level().getBlockState(neighbor);
                boolean connects = cells.contains(neighbor) || adjacent.getBlock() instanceof IronBarsBlock
                        || adjacent.is(Blocks.GLASS) || adjacent.isFaceSturdy(owner.level(), neighbor, direction.getOpposite());
                pane = pane.setValue(switch (direction) {
                    case NORTH -> IronBarsBlock.NORTH; case SOUTH -> IronBarsBlock.SOUTH;
                    case EAST -> IronBarsBlock.EAST; case WEST -> IronBarsBlock.WEST;
                    default -> throw new IllegalArgumentException("Horizontal pane connection required");
                }, connects);
            }
            if (pos.getY() < base.getY()) pane = Blocks.GLASS.defaultBlockState();
            if (!TemporaryTerrain.put(owner, pos, pane, Blocks.AIR.defaultBlockState(), 720, group)) {
                TemporaryTerrain.finish(owner.serverLevel(), group); return false;
            }
            placed.add(pos.immutable());
        }
        var cage = new Cage(owner, mob, Vec3.atBottomCenterOf(base), placed, group);
        CAGES.add(cage); cage.add(mob); return true;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(co.blumeplugins.deathsareitems.item.ModItems.ENTITY_CRAMMER.get())
                || !(event.getTarget() instanceof LivingEntity target)) return;
        event.setCanceled(true);
        event.setCancellationResult(event.getItemStack().interactLivingEntity(event.getEntity(), target, event.getHand()));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void damage(LivingHurtEvent event) {
        for (Cage cage : CAGES) if (cage.burstAt < 0 && cage.mobs.contains(event.getEntity())) {
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.CRAMMING)) {
                event.setCanceled(true); burst(cage);
            } else if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) event.setCanceled(true);
            return;
        }
    }
    private static void burst(Cage cage) {
        if (cage.burstAt >= 0) return;
        cage.burstAt = cage.age;
        for (BlockPos pos : cage.cells) cage.level.levelEvent(2001, pos, Block.getId(Blocks.GLASS.defaultBlockState()));
        TemporaryTerrain.shatter(cage.level, cage.group);
        cage.level.playSound(null, BlockPos.containing(cage.anchor), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1F, .65F);
        for (int i = 0; i < cage.mobs.size(); i++) {
            Mob mob = cage.mobs.get(i); MobRestraints.release(mob, cage.group);
            cage.launchOrigins.put(mob.getUUID(), mob.position());
            double angle = i * Math.PI * (3 - Math.sqrt(5));
            mob.setDeltaMovement(Math.cos(angle) * (.325 + cage.level.random.nextDouble() * .25), .3 + cage.level.random.nextDouble() * .25,
                    Math.sin(angle) * (.325 + cage.level.random.nextDouble() * .25)); mob.hurtMarked = true;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var it = CAGES.iterator();
        while (it.hasNext()) {
            Cage c = it.next(); if (c.level != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(c.owner);
            if (owner == null || !owner.isAlive() || owner.level() != level || !level.hasChunkAt(BlockPos.containing(c.anchor))) {
                c.cleanup(); for (Mob m : c.mobs) if (m != c.original) m.discard(); it.remove(); continue;
            }
            c.age++;
            if (c.burstAt >= 0) {
                int flightAge = c.age - c.burstAt;
                // Allow a brief launch before using collision flags, which may still describe
                // the cage floor on the burst tick. Open flights keep the half-second lifetime.
                for (Mob mob : List.copyOf(c.mobs)) if (mob.isAlive()) {
                    Vec3 origin = c.launchOrigins.get(mob.getUUID());
                    boolean launched = origin != null && mob.position().distanceToSqr(origin) > .0001;
                    boolean impact = flightAge >= 2 && launched && (mob.horizontalCollision || mob.verticalCollision);
                    if (flightAge < 10 && !impact) continue;
                    mob.invulnerableTime = 0; DeathDamage.hurt(mob, DeathDamage.CRAM, owner, Float.MAX_VALUE);
                    level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + .5, mob.getZ(), 18, .4, .5, .4, .08);
                }
                if (flightAge >= 10 || c.mobs.stream().noneMatch(Mob::isAlive)) { c.cleanup(); it.remove(); }
                continue;
            }
            if (c.cells.stream().anyMatch(pos -> !level.getBlockState(pos).is(Blocks.GLASS_PANE) && !level.getBlockState(pos).is(Blocks.GLASS))
                    || c.mobs.stream().anyMatch(m -> !m.isAlive() || m.level() != level)) { burst(c); continue; }
            for (Mob mob : c.mobs) contain(mob, c.anchor);
            if (c.age % 5 == 0) {
                CompoundTag copy = c.template.copy();
                Mob clone = c.original.getType().create(level) instanceof Mob m ? m : null;
                if (clone == null) { burst(c); continue; }
                copy.remove("UUID"); copy.remove("Passengers"); copy.remove("Leash"); copy.remove("DeathTime"); copy.remove("HurtTime");
                copy.remove("NoAI");
                clone.load(copy); clone.setUUID(UUID.randomUUID()); clone.setHealth(clone.getMaxHealth());
                clone.moveTo(c.anchor.x + (level.random.nextDouble() - .5) * .2, c.anchor.y, c.anchor.z + (level.random.nextDouble() - .5) * .2, level.random.nextFloat() * 360F, 0);
                clone.setPersistenceRequired(); clone.getPersistentData().putBoolean("deathsareitems.CramClone", true);
                if (level.addFreshEntity(clone)) { c.add(clone); clone.playAmbientSound(); } else { clone.discard(); burst(c); continue; }
                level.sendParticles(ParticleTypes.POOF, c.anchor.x, c.anchor.y + 1, c.anchor.z, 8, .2, .4, .2, .02);
                int limit = level.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
                // Still resolves when cramming is disabled or set to an excessive value.
                if (c.mobs.size() > Math.min(24, limit <= 0 ? 24 : limit)) burst(c);
            }
        }
    }
    private static void contain(Mob mob, Vec3 anchor) {
        double maxRadius = 0.45;
        double dx = mob.getX() - anchor.x;
        double dz = mob.getZ() - anchor.z;
        double clampedX = mob.getX();
        double clampedZ = mob.getZ();
        double vx = mob.getDeltaMovement().x;
        double vz = mob.getDeltaMovement().z;

        if (Math.abs(dx) > maxRadius) {
            clampedX = anchor.x + Math.signum(dx) * maxRadius;
            if (Math.signum(dx) == Math.signum(vx)) vx = -vx * 0.5;
        }
        if (Math.abs(dz) > maxRadius) {
            clampedZ = anchor.z + Math.signum(dz) * maxRadius;
            if (Math.signum(dz) == Math.signum(vz)) vz = -vz * 0.5;
        }

        double minY = anchor.y;
        double maxY = anchor.y + 1.2;
        double clampedY = Math.max(minY, Math.min(maxY, mob.getY()));
        double vy = mob.getDeltaMovement().y;
        if (mob.getY() > maxY && vy > 0) vy = 0;

        if (clampedX != mob.getX() || clampedY != mob.getY() || clampedZ != mob.getZ()) {
            mob.setPos(clampedX, clampedY, clampedZ);
        }
        mob.setDeltaMovement(vx, vy, vz);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { CAGES.forEach(Cage::cleanup); CAGES.clear(); }
    private static final class Cage {
        final ServerLevel level; final UUID owner; final Mob original; final CompoundTag template;
        final Vec3 anchor; final List<BlockPos> cells; final String group; final List<Mob> mobs = new ArrayList<>();
        final Map<UUID, Vec3> launchOrigins = new HashMap<>();
        int age, burstAt = -1;
        Cage(ServerPlayer owner, Mob original, Vec3 anchor, List<BlockPos> cells, String group) {
            this.level = owner.serverLevel(); this.owner = owner.getUUID(); this.original = original;
            this.anchor = anchor; this.cells = cells; this.group = group;
            this.template = original.saveWithoutId(new CompoundTag());
        }
        void add(Mob mob) { mobs.add(mob); MobRestraints.acquire(mob, group, false); }
        void cleanup() { TemporaryTerrain.finish(level, group); mobs.forEach(m -> MobRestraints.release(m, group)); }
    }
}
