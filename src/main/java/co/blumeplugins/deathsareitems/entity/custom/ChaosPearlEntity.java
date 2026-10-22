package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.ability.PearlAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

public final class ChaosPearlEntity extends ThrownEnderpearl {
    private UUID target;
    private UUID burst;
    public ChaosPearlEntity(EntityType<? extends ThrownEnderpearl> type, Level level) { super(type, level); }
    public void retaliation(UUID target, UUID burst) { this.target = target; this.burst = burst; }
    public UUID burst() { return burst; }
    @Override protected boolean canHitEntity(Entity entity) { return (target == null || !target.equals(entity.getUUID())) && super.canHitEntity(entity); }
    @Override protected void onHit(HitResult hit) {
        if (level() instanceof ServerLevel) { PearlAbility.land(this, target, burst, hit.getLocation()); discard(); }
    }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && !isRemoved() && tickCount % 2 == 0)
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 2, .045, .045, .045, .02);
        if (!level().isClientSide && tickCount > (target == null ? 160 : 80)) {
            PearlAbility.land(this, target, burst, position()); discard();
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); if (target != null) tag.putUUID("ChaosTarget", target); if (burst != null) tag.putUUID("ChaosBurst", burst);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); if (tag.hasUUID("ChaosTarget")) target = tag.getUUID("ChaosTarget"); if (tag.hasUUID("ChaosBurst")) burst = tag.getUUID("ChaosBurst");
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
