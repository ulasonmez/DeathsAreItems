package co.blumeplugins.deathsareitems.block.entity.custom;

import co.blumeplugins.deathsareitems.block.entity.ModBlockEntities;
import co.blumeplugins.deathsareitems.einfach.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.UUID;

public final class GustafJukeboxBlockEntity extends JukeboxBlockEntity {
    private UUID owner;
    public GustafJukeboxBlockEntity(BlockPos pos, BlockState state) { super(pos, state); }
    @Override public BlockEntityType<?> getType() { return ModBlockEntities.GUSTAFS_JUKEBOX.get(); }
    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    @Override public void load(CompoundTag tag) { super.load(tag); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (owner != null) tag.putUUID("Owner", owner); }
    public static void tick(Level world, BlockPos pos, BlockState state, GustafJukeboxBlockEntity box) {
        JukeboxBlockEntity.playRecordTick(world, pos, state, box);
        if (!(world instanceof ServerLevel level) || box.owner == null || !box.isRecordPlaying()) return;
        var owner = level.getServer().getPlayerList().getPlayer(box.owner);
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level) return;
        long now = level.getServer().overworld().getGameTime(); Vec3 center = Vec3.atCenterOf(pos);
        // Two-tick effects expire immediately after music stops or the owner leaves the radius.
        if (owner.distanceToSqr(center) <= 256) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, 1, true, false));
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 1, true, false));
        }
        if (now % 100 != 0) return;
        pulse(level, pos, owner);
    }
    public static void pulse(ServerLevel level, BlockPos pos, net.minecraft.server.level.ServerPlayer owner) {
        long now = level.getServer().overworld().getGameTime(); Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.NOTE, center.x, center.y + 1, center.z, 30, 3, .3, 3, .1);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(16), e -> Companions.hostile(e) && e.distanceToSqr(center) <= 256)) {
            var tag = mob.getPersistentData();
            if (tag.getLong("deathsareitems.RemixPulse") > now) continue;
            tag.putLong("deathsareitems.RemixPulse", now + 100);
            BookSpells.damage(owner, mob, "wither", 16);
            if (!Companions.boss(mob)) mob.knockback(1.3, center.x - mob.getX(), center.z - mob.getZ());
        }
    }
}
