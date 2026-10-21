package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

/** Debris has its own lifecycle and never places a falling block on impact. */
public final class LaunchedBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> STATE = SynchedEntityData.defineId(LaunchedBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Long> BIRTH = SynchedEntityData.defineId(LaunchedBlockEntity.class, EntityDataSerializers.LONG);

    public LaunchedBlockEntity(EntityType<? extends LaunchedBlockEntity> type, Level level) {
        super(type, level);
    }

    public static LaunchedBlockEntity create(ServerLevel level, BlockPos pos, BlockState state, double vx, double vy, double vz) {
        LaunchedBlockEntity entity = ModEntities.LAUNCHED_BLOCK.get().create(level);
        if (entity == null) throw new IllegalStateException("Could not create launched block");
        entity.setPos(pos.getX() + .5, pos.getY() + .05, pos.getZ() + .5);
        entity.entityData.set(STATE, state);
        entity.entityData.set(BIRTH, level.getGameTime());
        entity.setDeltaMovement(vx, vy, vz);
        return entity;
    }

    public BlockState blockState() { return entityData.get(STATE); }
    public float age(float partialTick) { return level().getGameTime() - entityData.get(BIRTH) + partialTick; }

    @Override
    protected void defineSynchedData() {
        entityData.define(STATE, Blocks.DIRT.defaultBlockState());
        entityData.define(BIRTH, 0L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isNoGravity()) setDeltaMovement(getDeltaMovement().add(0, -.04, 0));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(.985));
        if (!level().isClientSide && (tickCount > 120 || getY() < level().getMinBuildHeight() - 8)) {
            discard();
        } else if (!level().isClientSide && onGround() && tickCount > 5) {
            ServerLevel level = (ServerLevel) level();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState()), getX(), getY() + .35, getZ(), 18, .3, .3, .3, .06);
            level.playSound(null, blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.BLOCKS, .65F, .85F + random.nextFloat() * .25F);
            if (level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("death.fell.accident.generic", blockState().getBlock().getName()), false);
            }
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(STATE, NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState")));
        entityData.set(BIRTH, tag.getLong("Birth"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(blockState()));
        tag.putLong("Birth", entityData.get(BIRTH));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
