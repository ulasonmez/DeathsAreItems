package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.ability.CannonAbility;
import co.blumeplugins.deathsareitems.ability.OwnedExplosions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

public final class CannonTntEntity extends PrimedTnt {
    private UUID shooter;
    public CannonTntEntity(EntityType<? extends PrimedTnt> type, Level level) { super(type, level); }
    public void setShooter(ServerPlayer player) { shooter = player.getUUID(); }
    @Override public LivingEntity getOwner() { return shooter != null && level() instanceof ServerLevel s ? s.getServer().getPlayerList().getPlayer(shooter) : null; }
    @Override public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + .4, getZ(), 3, .08, .08, .08, .025);
            if (onGround()) { discard(); explode(); }
        } else level().addParticle(ParticleTypes.FIREWORK, getX(), getY() + .4, getZ(), 0, .025, 0);
    }
    @Override protected void explode() {
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.level() != level()) return;
        OwnedExplosions.explode(owner, this, position(), 4F, false);
        if (owner.distanceToSqr(this) < 36 && getY() <= owner.getY() + 1) CannonAbility.boost(owner, position());
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); if (shooter != null) tag.putUUID("Shooter", shooter); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); if (tag.hasUUID("Shooter")) shooter = tag.getUUID("Shooter"); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
