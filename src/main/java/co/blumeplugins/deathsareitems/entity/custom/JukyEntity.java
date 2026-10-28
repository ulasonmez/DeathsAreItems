package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.einfach.Companions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Vanilla wolf shape is temporary; behavior is entirely owner-scoped. */
public final class JukyEntity extends Wolf {
    private Vec3 guard;
    private int attackTicks;
    public JukyEntity(EntityType<? extends Wolf> type, Level level) { super(type, level); }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); }
    @Override public boolean canAttack(LivingEntity target) { return Companions.hostile(target) && super.canAttack(target); }
    @Override public boolean doHurtTarget(Entity target) { return target instanceof LivingEntity living && Companions.hostile(living) && super.doHurtTarget(target); }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isOwnedBy(player) && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) { guard = guard == null ? position() : null; getNavigation().stop(); setTarget(null); Companions.message(player, guard == null ? "juky_follow" : "juky_guard"); }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void tick() {
        super.tick(); if (level().isClientSide || !isAlive()) return;
        var owner = Companions.owner(this); if (owner == null) return;
        if (attackTicks > 0) attackTicks--;
        if (tickCount % 10 != 0) return;
        Vec3 home = guard == null ? owner.position() : guard;
        var threats = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(12),
                e -> Companions.hostile(e) && e.distanceToSqr(home) < 16 * 16 && hasLineOfSight(e));
        Mob target = threats.stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target);
        if (target != null) {
            getNavigation().moveTo(target, 1.3);
            if (distanceToSqr(target) < 5 && attackTicks == 0) { doHurtTarget(target); attackTicks = 20; }
            var cooldowns = co.blumeplugins.deathsareitems.einfach.EinfachSavedData.get(owner.server);
            var progress = cooldowns.player(owner.getUUID());
            if (Companions.now(owner) >= progress.getLong("JukyHowlReady") && threats.stream().anyMatch(e -> distanceToSqr(e) < 36)) {
                playSound(SoundEvents.WOLF_HOWL, 1.5F, 1);
                for (Mob mob : threats) if (distanceToSqr(mob) < 36 && !Companions.boss(mob))
                    mob.knockback(1.8, getX() - mob.getX(), getZ() - mob.getZ());
                progress.putLong("JukyHowlReady", Companions.now(owner) + 400); cooldowns.setDirty();
            }
        } else if (distanceToSqr(home) > 4) {
            getNavigation().moveTo(home.x, home.y, home.z, 1.2);
            if (guard == null && distanceToSqr(owner) > 24 * 24) {
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                    var pos = owner.blockPosition().offset(x, 0, z);
                    if (level().getBlockState(pos.below()).isSolid() && level().noCollision(this, getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(position())))) {
                        teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5); return;
                    }
                }
            }
        } else getNavigation().stop();
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (guard != null) { tag.putDouble("GuardX", guard.x); tag.putDouble("GuardY", guard.y); tag.putDouble("GuardZ", guard.z); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GuardX")) guard = new Vec3(tag.getDouble("GuardX"), tag.getDouble("GuardY"), tag.getDouble("GuardZ"));
    }
}
