package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class MobRestraints {
    private static final Map<UUID, Vec3> ANCHORS = new HashMap<>();
    private static final String KEY = "deathsareitems.Restraint";
    public static boolean busy(Mob mob) {
        return mob.getPersistentData().contains(KEY) || mob.getPersistentData().contains("deathsareitems.SwarmAI")
                || BoulderAbility.busy(mob);
    }
    public static void acquire(Mob mob, String owner) {
        acquire(mob, owner, true);
    }
    public static void acquire(Mob mob, String owner, boolean immobilize) {
        var tag = mob.getPersistentData(); tag.putString(KEY, owner);
        tag.putBoolean(KEY + "AI", mob.isNoAi()); tag.putBoolean(KEY + "Gravity", mob.isNoGravity());
        mob.stopRiding(); mob.ejectPassengers();
        if (immobilize) {
            mob.setNoAi(true); mob.setNoGravity(true);
        } else {
            mob.setNoAi(false); mob.setNoGravity(false);
        }
    }
    public static void freeze(Mob mob, Vec3 anchor) {
        ANCHORS.put(mob.getUUID(), anchor);
        mob.getNavigation().stop(); mob.setJumping(false); mob.setDeltaMovement(Vec3.ZERO); mob.setPos(anchor); mob.hurtMarked = true;
    }
    public static void release(Mob mob, String owner) {
        if (!owner.equals(mob.getPersistentData().getString(KEY))) return;
        restore(mob);
    }
    private static void restore(Mob mob) {
        var tag = mob.getPersistentData();
        if (tag.contains(KEY)) { mob.setNoAi(tag.getBoolean(KEY + "AI")); mob.setNoGravity(tag.getBoolean(KEY + "Gravity")); }
        tag.remove(KEY); tag.remove(KEY + "AI"); tag.remove(KEY + "Gravity"); ANCHORS.remove(mob.getUUID());
    }
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent event) {
        Vec3 anchor = ANCHORS.get(event.getEntity().getUUID());
        if (!event.getEntity().level().isClientSide && anchor != null && event.getEntity() instanceof Mob mob) freeze(mob, anchor);
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent event) {
        if (ANCHORS.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.loadedFromDisk() && event.getEntity() instanceof Mob mob) {
            if (mob.getPersistentData().getBoolean("deathsareitems.CramClone")) { event.setCanceled(true); return; }
            restore(mob);
        }
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event) { ANCHORS.clear(); }
    private MobRestraints() {}
}
