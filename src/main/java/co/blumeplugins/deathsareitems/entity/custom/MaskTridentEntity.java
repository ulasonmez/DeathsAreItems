package co.blumeplugins.deathsareitems.entity.custom;

import co.blumeplugins.deathsareitems.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

/** Flight and drowning are controlled by the mask, never vanilla trident damage or pickup. */
public final class MaskTridentEntity extends ThrownTrident {
    private static final EntityDataAccessor<Integer> CHARGE_OWNER = SynchedEntityData.defineId(MaskTridentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(MaskTridentEntity.class, EntityDataSerializers.BOOLEAN);
    private Vec3 visualAim, previousVisualAim;
    public MaskTridentEntity(EntityType<? extends MaskTridentEntity> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
        setNoPhysics(true);
    }
    public MaskTridentEntity(Level level, Player owner) {
        this(ModEntities.MASK_TRIDENT.get(), level);
        setOwner(owner);
        entityData.set(CHARGE_OWNER, owner.getId());
        setPos(heldPosition(owner, 1));
        pointAlong(owner.getLookAngle());
        yRotO = getYRot(); xRotO = getXRot();
        setGlowingTag(true);
        getPersistentData().putBoolean("deathsareitems.TemporaryVisual", true);
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGE_OWNER, -1);
        entityData.define(CHARGING, true);
    }
    public boolean isCharging() { return entityData.get(CHARGING); }
    public void launch() { entityData.set(CHARGING, false); }
    public Player chargeOwner() {
        var owner = level().getEntity(entityData.get(CHARGE_OWNER));
        return owner instanceof Player player ? player : getOwner() instanceof Player player ? player : null;
    }
    /** Anchor follows the view immediately; only the spear's aim has a deliberate delay. */
    public static Vec3 heldPosition(Player owner, float partialTick) {
        Vec3 look = viewDirection(owner, partialTick);
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, owner.yRotO, owner.getYRot()));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 down = look.cross(right).normalize();
        return owner.getEyePosition(partialTick).add(look.scale(1.25)).add(down.scale(.28));
    }
    private static Vec3 viewDirection(Player owner, float partialTick) {
        // LivingEntity.getViewVector uses the separately synchronized head rotation, which can lag.
        return Vec3.directionFromRotation(Mth.lerp(partialTick, owner.xRotO, owner.getXRot()),
                Mth.rotLerp(partialTick, owner.yRotO, owner.getYRot()));
    }
    public static Vec3 followAim(Vec3 previous, Vec3 target) {
        float yaw = (float)Math.toDegrees(Math.atan2(-previous.x, previous.z));
        float targetYaw = (float)Math.toDegrees(Math.atan2(-target.x, target.z));
        float pitch = (float)Math.toDegrees(Math.atan2(-previous.y, previous.horizontalDistance()));
        float targetPitch = (float)Math.toDegrees(Math.atan2(-target.y, target.horizontalDistance()));
        // Angle interpolation also turns smoothly through a full 180-degree camera reversal.
        return Vec3.directionFromRotation(Mth.lerp(.35F, pitch, targetPitch), Mth.rotLerp(.35F, yaw, targetYaw));
    }
    public void pointAlong(Vec3 direction) {
        setYRot((float)Math.toDegrees(Math.atan2(direction.x, direction.z)));
        setXRot((float)Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
    }
    public Vec3 visualAim(float partialTick) {
        Player owner = chargeOwner();
        if (visualAim == null) return owner == null ? new Vec3(0, 0, 1) : viewDirection(owner, partialTick);
        return previousVisualAim.lerp(visualAim, partialTick).normalize();
    }
    @Override public void tick() {
        // The ability owns flight. AbstractArrow.tick would derive a new rotation from zero velocity
        // (and reverse yaw for noPhysics), fighting server rotation packets every client tick.
        baseTick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide && isCharging()) {
            Player owner = chargeOwner();
            if (owner != null) {
                if (visualAim == null) visualAim = viewDirection(owner, 1);
                previousVisualAim = visualAim;
                visualAim = followAim(visualAim, viewDirection(owner, 1));
                setPos(heldPosition(owner, 1));
            }
        }
    }
    @Override public void playerTouch(Player player) {}
    @Override protected boolean tryPickup(Player player) { return false; }
    @Override protected void onHitEntity(EntityHitResult hit) {}
    @Override protected void onHitBlock(BlockHitResult hit) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
