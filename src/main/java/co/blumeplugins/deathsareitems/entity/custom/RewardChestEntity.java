package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.reward.RewardManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/** Animated chest during the reveal; a stationary label above its vanilla block afterwards. */
public final class RewardChestEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> GENERATION = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> LABEL = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(RewardChestEntity.class, EntityDataSerializers.BOOLEAN);

    public RewardChestEntity(EntityType<? extends RewardChestEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(UUID owner, UUID generation, int slot, long start, String label) {
        entityData.set(OWNER, Optional.of(owner));
        entityData.set(GENERATION, Optional.of(generation));
        entityData.set(SLOT, slot);
        entityData.set(START, start);
        entityData.set(LABEL, label);
    }

    public Optional<UUID> owner() { return entityData.get(OWNER); }
    public Optional<UUID> generation() { return entityData.get(GENERATION); }
    public int slot() { return entityData.get(SLOT); }
    public String label() { return entityData.get(LABEL); }
    public boolean isSettled() { return entityData.get(SETTLED); }
    public void setSettled(boolean settled) { entityData.set(SETTLED, settled); }
    public float animationAge(float partialTick) { return Math.max(0, level().getGameTime() - entityData.get(START) + partialTick); }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, Optional.empty());
        entityData.define(GENERATION, Optional.empty());
        entityData.define(SLOT, 0);
        entityData.define(START, 0L);
        entityData.define(LABEL, "item.deathsareitems.fall_damage_boots");
        entityData.define(SETTLED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) RewardManager.tickChest(this);
    }

    @Override
    public boolean isPickable() { return !isRemoved() && !isSettled(); }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return true; }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) RewardManager.select(player, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        if (tag.hasUUID("Generation")) entityData.set(GENERATION, Optional.of(tag.getUUID("Generation")));
        entityData.set(SLOT, tag.getInt("Slot"));
        entityData.set(START, tag.getLong("Start"));
        entityData.set(LABEL, tag.getString("Label"));
        entityData.set(SETTLED, tag.getBoolean("Settled"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        owner().ifPresent(id -> tag.putUUID("Owner", id));
        generation().ifPresent(id -> tag.putUUID("Generation", id));
        tag.putInt("Slot", slot());
        tag.putLong("Start", entityData.get(START));
        tag.putString("Label", label());
        tag.putBoolean("Settled", isSettled());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
