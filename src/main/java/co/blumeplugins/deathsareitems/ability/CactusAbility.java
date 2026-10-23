package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class CactusAbility {
    private static final List<Pin> PINS = new ArrayList<>();
    @SubscribeEvent public static void retaliate(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || event.getAmount() <= 0
                || !owner.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CACTUS_CHESTPLATE.get()) || PearlAbility.holdsShield(owner)) return;
        if (event.getSource().getEntity() instanceof Mob mob) start(owner, mob);
    }
    public static boolean start(ServerPlayer owner, Mob mob) {
        if (!DeathAbilities.eligible(owner, mob) || MobRestraints.busy(mob) || PINS.size() >= 64) return false;
        Pin pin = new Pin(owner, mob); PINS.add(pin); MobRestraints.acquire(mob, pin.id); MobRestraints.freeze(mob, pin.anchor);
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI * 2 / 3 + owner.getRandom().nextDouble() * .4;
            Vec3 body = pin.anchor.add(0, mob.getBbHeight() * (.35 + i * .12), 0);
            Vec3 start = body.add(Math.cos(angle) * 1.8, 1.6 + i * .15, Math.sin(angle) * 1.8);
            Vec3 direction = body.subtract(start).normalize();
            // Continue through the body to the actual collision surface, including slabs and slopes.
            var ground = owner.serverLevel().clip(new ClipContext(body, body.add(direction.scale(32)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
            Vec3 end = ground.getType() == HitResult.Type.BLOCK
                    ? ground.getLocation().add(direction.scale(.025))
                    : body.add(direction.scale(mob.getBbHeight() / -direction.y));
            pin.visuals.add(AbilityVisuals.spike(owner.serverLevel(), start, end, 4, 220));
        }
        owner.level().playSound(null, mob.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, .8F, .65F);
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var it = PINS.iterator();
        while (it.hasNext()) {
            Pin p = it.next(); if (p.mob.level() != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(p.owner);
            if (owner == null || !owner.isAlive() || owner.level() != level || !p.mob.isAlive() || !level.hasChunkAt(p.mob.blockPosition())) {
                p.cleanup(); it.remove(); continue;
            }
            MobRestraints.freeze(p.mob, p.anchor);
            if (++p.age >= 10 && p.age % 10 == 0) {
                p.mob.invulnerableTime = 0; DeathDamage.hurt(p.mob, DeathDamage.CACTUS, owner, p.age >= 200 ? Float.MAX_VALUE : 4F);
                level.sendParticles(ParticleTypes.CRIT, p.mob.getX(), p.mob.getY() + .8, p.mob.getZ(), 8, .25, .5, .25, .03);
                level.playSound(null, p.mob.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, .35F, .75F);
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { PINS.forEach(Pin::cleanup); PINS.clear(); }
    private static final class Pin {
        final Mob mob; final UUID owner; final String id = "cactus." + UUID.randomUUID(); final Vec3 anchor;
        final List<Entity> visuals = new ArrayList<>(); int age;
        Pin(ServerPlayer owner, Mob mob) { this.owner = owner.getUUID(); this.mob = mob; this.anchor = mob.position(); }
        void cleanup() { visuals.forEach(Entity::discard); MobRestraints.release(mob, id); }
    }
}
