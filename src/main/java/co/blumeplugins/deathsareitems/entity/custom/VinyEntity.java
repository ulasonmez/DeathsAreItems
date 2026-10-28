package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.ability.DeathAbilities;
import co.blumeplugins.deathsareitems.einfach.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class VinyEntity extends Axolotl {
    public VinyEntity(EntityType<? extends Axolotl> type, Level level) {
        super(type, level);
        setVariant(Variant.BLUE);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(Variant.BLUE);
    }
    @Override protected void customServerAiStep() {} // No vanilla hunting of passive fish or pets.
    @Override public boolean canAttack(LivingEntity target) { return false; }
    @Override public boolean doHurtTarget(Entity target) { return false; }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Override public void tick() {
        super.tick(); if (level().isClientSide || !isAlive()) return;
        var owner = Companions.owner(this); if (owner == null) return;
        if (!level().getFluidState(blockPosition()).is(FluidTags.WATER) || distanceToSqr(owner) > 32 * 32) {
            Companions.recall(owner, true); return;
        }
        if (tickCount % 10 != 0) return;
        if (owner.isInWater() && distanceToSqr(owner) > 4) getNavigation().moveTo(owner, 1.2);
        else getNavigation().stop();
        var cooldowns = EinfachSavedData.get(owner.server); var progress = cooldowns.player(owner.getUUID());
        if (!DeathAbilities.mask(owner) || Companions.now(owner) < progress.getLong("VinyBubbleReady")) return;
        Mob target = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(8),
                e -> Companions.hostile(e) && distanceToSqr(e) <= 64 && hasLineOfSight(e)).stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target != null && BookSpells.bubble(owner, target, 80)) {
            progress.putLong("VinyBubbleReady", Companions.now(owner) + 300); cooldowns.setDirty();
        }
    }
}
