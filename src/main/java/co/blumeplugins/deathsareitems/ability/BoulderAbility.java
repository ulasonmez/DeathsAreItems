package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class BoulderAbility {
    private static final List<Mark> MARKS = new ArrayList<>();
    private static final Map<UUID, Trap> TRAPS = new HashMap<>();
    @SubscribeEvent public static void suffocation(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (TRAPS.containsKey(event.getEntity().getUUID()) && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) event.setCanceled(true);
    }
    public static boolean mark(ServerPlayer owner, BlockPos center) {
        if (!WorldActions.breakable(owner, center) || MARKS.stream().anyMatch(m -> m.level == owner.level() && m.center.equals(center))) return false;
        List<Cell> cells = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2)))
            if (pos.distSqr(center) <= 6 && WorldActions.breakable(owner, pos)) cells.add(new Cell(pos.immutable(), owner.level().getBlockState(pos)));
        Collections.shuffle(cells, new Random(owner.getRandom().nextLong()));
        MARKS.add(new Mark(owner.serverLevel(), owner.getUUID(), center.immutable(), cells, owner.level().getGameTime()));
        AbilityVisuals.block(owner.serverLevel(), Vec3.atCenterOf(center), owner.level().getBlockState(center), 1.002F, 60, 0xFFFFFF);
        owner.level().playSound(null, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, .6F, .7F);
        return true;
    }
    public static boolean trap(ServerPlayer owner, LivingEntity target) {
        if (!(target instanceof Mob mob) || TRAPS.containsKey(target.getUUID())) return false;
        List<Cell> cells = new ArrayList<>();
        BlockState sediment = sediment(owner.serverLevel());
        var box = target.getBoundingBox().deflate(.02);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var old = owner.level().getBlockState(pos);
            if (!old.isAir() || !WorldActions.allowed(owner, pos)) continue;
            if (WorldActions.place(owner, pos, sediment)) cells.add(new Cell(pos.immutable(), sediment));
        }
        if (cells.isEmpty()) return false;
        TRAPS.put(target.getUUID(), new Trap(mob, owner.getUUID(), target.position(), cells));
        mob.getNavigation().stop();
        return true;
    }
    private static BlockState sediment(ServerLevel level) {
        return (level.random.nextBoolean() ? ModBlocks.FRAGILE_SAND : ModBlocks.FRAGILE_GRAVEL).get().defaultBlockState();
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var marks = MARKS.iterator();
        while (marks.hasNext()) {
            Mark mark = marks.next(); if (mark.level != level) continue;
            long age = level.getGameTime() - mark.start;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(mark.owner);
            if (owner == null || !owner.isAlive() || owner.level() != level || age > 160) { marks.remove(); continue; }
            if (age < 60) { if (age % 5 == 0) level.sendParticles(ParticleTypes.END_ROD, mark.center.getX() + .5, mark.center.getY() + .5, mark.center.getZ() + .5, 5, .6, .6, .6, .01); continue; }
            if (age == 60) level.playSound(null, mark.center, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, .4F, .8F);
            // Every cell changes in the same server tick after the three-second glow.
            while (mark.index < mark.cells.size()) {
                Cell cell = mark.cells.get(mark.index++);
                if (!level.getBlockState(cell.pos).equals(cell.state) || !WorldActions.breakEvent(owner, cell.pos)) continue;
                if (WorldActions.place(owner, cell.pos, mark.sediment)) {
                    level.sendParticles(ParticleTypes.POOF, cell.pos.getX() + .5, cell.pos.getY() + .5, cell.pos.getZ() + .5, 5, .25, .25, .25, .02);
                }
            }
            if (mark.index >= mark.cells.size()) marks.remove();
        }
        var traps = TRAPS.values().iterator();
        while (traps.hasNext()) {
            Trap trap = traps.next(); if (trap.mob.level() != level) continue;
            var owner = level.getServer().getPlayerList().getPlayer(trap.owner);
            if (!trap.mob.isAlive() || owner == null || owner.level() != level || ++trap.age > 200
                    || trap.cells.stream().anyMatch(cell -> !level.getBlockState(cell.pos).equals(cell.state))) {
                // Remove the rest of this cage when any cell breaks; a remaining head block must not keep suffocating the mob.
                if (owner != null && owner.level() == level) for (Cell cell : trap.cells)
                    if (level.getBlockState(cell.pos).equals(cell.state) && WorldActions.breakEvent(owner, cell.pos)) level.destroyBlock(cell.pos, false);
                traps.remove(); continue;
            }
            trap.mob.getNavigation().stop(); trap.mob.setDeltaMovement(Vec3.ZERO); trap.mob.setPos(trap.anchor);
            if (trap.age % 20 == 0) {
                trap.mob.invulnerableTime = 0;
                DeathDamage.hurt(trap.mob, DeathDamage.SUFFOCATE, owner, 5F);
                level.sendParticles(ParticleTypes.POOF, trap.mob.getX(), trap.mob.getEyeY(), trap.mob.getZ(), 8, .2, .2, .2, .02);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { MARKS.clear(); TRAPS.clear(); }
    private record Cell(BlockPos pos, BlockState state) {}
    private static final class Mark {
        final ServerLevel level; final UUID owner; final BlockPos center; final List<Cell> cells; final long start; final BlockState sediment; int index;
        Mark(ServerLevel level, UUID owner, BlockPos center, List<Cell> cells, long start) { this.level=level; this.owner=owner; this.center=center; this.cells=cells; this.start=start; this.sediment=sediment(level); }
    }
    private static final class Trap {
        final Mob mob; final UUID owner; final Vec3 anchor; final List<Cell> cells; int age;
        Trap(Mob mob, UUID owner, Vec3 anchor, List<Cell> cells) { this.mob=mob; this.owner=owner; this.anchor=anchor; this.cells=cells; }
    }
}
