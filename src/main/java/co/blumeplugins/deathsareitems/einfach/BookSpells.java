package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Independent spells: never call an equipped reward's ability. Cages are particle effects. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class BookSpells {
    private static final List<Spell> ACTIVE = new ArrayList<>();
    private static final String LOCK = "einfach_spell";
    public static boolean bubble(ServerPlayer owner, Mob target, int duration) { return timed(owner, target, "drown", duration); }
    public static boolean cast(ServerPlayer owner, Mob target, String chapter) {
        if (chapter.equals("fall") || chapter.equals("wither")) {
            if (!Companions.boss(target) && chapter.equals("fall")) {
                target.setDeltaMovement(0, -2.5, 0); target.hurtMarked = true;
            }
            damage(owner, target, chapter, chapter.equals("fall") ? 24 : 32);
            owner.serverLevel().sendParticles(chapter.equals("fall") ? ParticleTypes.CLOUD : ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + .5, target.getZ(), 40, .8, .5, .8, .15);
            return true;
        }
        return timed(owner, target, chapter, 80);
    }
    private static boolean timed(ServerPlayer owner, Mob target, String chapter, int duration) {
        if (Companions.boss(target)) { damage(owner, target, chapter, chapter.equals("anvil") ? 48 : 20); return true; }
        if (MobRestraints.busy(target) || ACTIVE.size() >= 128) return false;
        MobRestraints.acquire(target, LOCK); MobRestraints.freeze(target, target.position());
        ACTIVE.add(new Spell(owner.getUUID(), target, target.position(), chapter, duration)); return true;
    }
    public static void damage(ServerPlayer owner, Mob target, String chapter, float amount) {
        var type = switch (chapter) { case "drown" -> DeathDamage.DROWN; case "anvil" -> DeathDamage.ANVILS; case "cactus" -> DeathDamage.CACTUS; default -> DeathDamage.SOUL; };
        var source = DeathDamage.source(target, type, owner);
        Vec3 velocity = target.getDeltaMovement();
        if (target instanceof EnderDragon dragon) dragon.hurt(dragon.head, source, amount);
        else target.hurt(source, amount);
        if (Companions.boss(target)) target.setDeltaMovement(velocity);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            var spell = iterator.next(); var mob = spell.mob;
            if (mob.level() != level) continue;
            var owner = level.getServer().getPlayerList().getPlayer(spell.owner);
            if (!mob.isAlive() || mob.isRemoved() || owner == null || !owner.isAlive() || owner.level() != level || ++spell.age > spell.duration) {
                MobRestraints.release(mob, LOCK); iterator.remove(); continue;
            }
            MobRestraints.freeze(mob, spell.anchor);
            if (spell.age % 5 == 0) {
                double radius = mob.getBbWidth() / 2 + .4;
                if (!spell.chapter.equals("anvil")) for (int point = 0; point < 16; point++) {
                    double angle = point * Math.PI / 8;
                    var particle = spell.chapter.equals("cactus")
                            ? new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.CACTUS.defaultBlockState())
                            : ParticleTypes.BUBBLE_POP;
                    level.sendParticles(particle, mob.getX() + Math.cos(angle) * radius,
                            mob.getY() + (spell.chapter.equals("cactus") ? .25 : mob.getBbHeight() * (point % 4) / 3),
                            mob.getZ() + Math.sin(angle) * radius, 2, .05, .05, .05, .01);
                }
                if (spell.chapter.equals("anvil")) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                        net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState()), mob.getX(), mob.getY() + 2, mob.getZ(), 12, .3, .4, .3, .2);
            }
            if (spell.age % (spell.chapter.equals("drown") ? 20 : 10) == 0) damage(owner, mob, spell.chapter, spell.chapter.equals("anvil") ? 8 : 4);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        ACTIVE.forEach(s -> MobRestraints.release(s.mob, LOCK)); ACTIVE.clear();
    }
    private static final class Spell {
        final UUID owner; final Mob mob; final Vec3 anchor; final String chapter; final int duration; int age;
        Spell(UUID owner, Mob mob, Vec3 anchor, String chapter, int duration) { this.owner=owner; this.mob=mob; this.anchor=anchor; this.chapter=chapter; this.duration=duration; }
    }
}
