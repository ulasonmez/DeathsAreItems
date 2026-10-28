package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
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
    public static boolean canTarget(LivingEntity target) {
        return target instanceof Mob && target.isAlive() && !target.isRemoved() && !target.isInvulnerable();
    }
    public static boolean isBookDamage(DamageSource source) { return source instanceof BookDamageSource; }
    public static boolean bubble(ServerPlayer owner, Mob target, int duration) { return timed(owner, target, "drown", duration, false); }
    public static boolean cast(ServerPlayer owner, Mob target, String chapter) {
        if (!List.of("fall", "drown", "anvil", "cactus", "wither").contains(chapter)
                || owner.level() != target.level() || !canTarget(target)) return false;
        // A new page replaces this reader's own spell, never another reader's or Viny's bubble.
        Spell previous = ACTIVE.stream().filter(s -> s.mob == target && s.book && s.owner.equals(owner.getUUID())).findFirst().orElse(null);
        if (MobRestraints.busy(target) && previous == null) return false;
        if (!chapter.equals("fall") && !chapter.equals("wither") && !Companions.boss(target)
                && previous == null && ACTIVE.size() >= 128) return false;
        if (previous != null) { MobRestraints.release(target, LOCK); ACTIVE.remove(previous); }
        if (chapter.equals("fall") || chapter.equals("wither")) {
            if (!Companions.boss(target) && chapter.equals("fall")) {
                target.setDeltaMovement(0, -2.5, 0); target.hurtMarked = true;
            }
            damage(owner, target, chapter, chapter.equals("fall") ? 24 : 32);
            owner.serverLevel().sendParticles(chapter.equals("fall") ? ParticleTypes.CLOUD : ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + .5, target.getZ(), 40, .8, .5, .8, .15);
            return true;
        }
        return timed(owner, target, chapter, 80, true);
    }
    private static boolean timed(ServerPlayer owner, Mob target, String chapter, int duration, boolean book) {
        if (Companions.boss(target)) { damage(owner, target, chapter, chapter.equals("anvil") ? 48 : 20, book); return true; }
        if (MobRestraints.busy(target) || ACTIVE.size() >= 128) return false;
        MobRestraints.acquire(target, LOCK); MobRestraints.freeze(target, target.position());
        ACTIVE.add(new Spell(owner.getUUID(), target, target.position(), chapter, duration, book));
        if (book) damage(owner, target, chapter, chapter.equals("anvil") ? 8 : 4);
        return true;
    }
    /** Living pages grant a blessing; three distinct verses on one foe release a silent finale. */
    public static int inscribe(ServerPlayer owner, Mob target, String chapter) {
        var blessing = switch (chapter) {
            case "fall" -> MobEffects.SLOW_FALLING;
            case "drown" -> MobEffects.WATER_BREATHING;
            case "anvil" -> MobEffects.DAMAGE_RESISTANCE;
            case "cactus" -> MobEffects.MOVEMENT_SPEED;
            default -> MobEffects.REGENERATION;
        };
        owner.addEffect(new MobEffectInstance(blessing, 160, 0, false, true));
        var data = EinfachSavedData.get(owner.server); var p = data.player(owner.getUUID());
        long now = Companions.now(owner);
        Set<String> verses = new LinkedHashSet<>();
        boolean continuing = p.hasUUID("BookTarget") && p.getUUID("BookTarget").equals(target.getUUID())
                && now <= p.getLong("BookVerseUntil");
        if (continuing) {
            for (String verse : p.getString("BookVerses").split(",")) if (!verse.isEmpty()) verses.add(verse);
        }
        verses.add(chapter);
        p.putUUID("BookTarget", target.getUUID());
        if (!continuing) p.putLong("BookVerseUntil", now + 160);
        p.putString("BookVerses", String.join(",", verses)); data.setDirty();
        var level = owner.serverLevel();
        level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, .5F, .8F + verses.size() * .2F);
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1, target.getZ(), 16, .5, .6, .5, .2);
        if (verses.size() < 3) return verses.size();
        p.remove("BookTarget"); p.remove("BookVerses"); p.remove("BookVerseUntil");
        for (Mob enemy : level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(6),
                e -> canTarget(e) && e.distanceToSqr(target) <= 36 && owner.hasLineOfSight(e))) {
            enemy.invulnerableTime = 0;
            damage(owner, enemy, "wither", 16);
            if (!Companions.boss(enemy)) enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
        }
        // The struck foe may have died to the third verse; the finale still blooms at that spot.
        for (int point = 0; point < 48; point++) {
            double angle = point * Math.PI / 24;
            level.sendParticles(ParticleTypes.END_ROD, target.getX() + Math.cos(angle) * 3,
                    target.getY() + .5, target.getZ() + Math.sin(angle) * 3, 1, 0, .2, 0, .02);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, .6F);
        return 3;
    }
    public static void damage(ServerPlayer owner, Mob target, String chapter, float amount) {
        damage(owner, target, chapter, amount, true);
    }
    private static void damage(ServerPlayer owner, Mob target, String chapter, float amount, boolean book) {
        var type = switch (chapter) { case "drown" -> DeathDamage.DROWN; case "anvil" -> DeathDamage.ANVILS; case "cactus" -> DeathDamage.CACTUS; default -> DeathDamage.SOUL; };
        DamageSource source = DeathDamage.source(target, type, owner);
        if (book) source = new BookDamageSource(source, owner);
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
            if (spell.age % (spell.chapter.equals("drown") ? 20 : 10) == 0) damage(owner, mob, spell.chapter, spell.chapter.equals("anvil") ? 8 : 4, spell.book);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        ACTIVE.forEach(s -> MobRestraints.release(s.mob, LOCK)); ACTIVE.clear();
    }
    /** Identifies book hits while preserving the chapter's damage type and player attribution. */
    private static final class BookDamageSource extends DamageSource {
        BookDamageSource(DamageSource source, ServerPlayer owner) { super(source.typeHolder(), owner); }
    }
    private static final class Spell {
        final UUID owner; final Mob mob; final Vec3 anchor; final String chapter; final int duration; final boolean book; int age;
        Spell(UUID owner, Mob mob, Vec3 anchor, String chapter, int duration, boolean book) { this.owner=owner; this.mob=mob; this.anchor=anchor; this.chapter=chapter; this.duration=duration; this.book=book; }
    }
}
