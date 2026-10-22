package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class CannonAbility {
    public static boolean fire(ServerPlayer owner) {
        if (!owner.isAlive() || owner.isSpectator()) return false;
        var tnt = ModEntities.CANNON_TNT.get().create(owner.serverLevel()); if (tnt == null) return false;
        tnt.setShooter(owner); tnt.setFuse(60);
        Vec3 look = owner.getLookAngle(); tnt.setPos(owner.getEyePosition().add(look.scale(.8)));
        tnt.setDeltaMovement(look.scale(1.45).add(owner.getDeltaMovement().scale(.3)));
        if (!owner.level().addFreshEntity(tnt)) return false;
        owner.level().playSound(null, owner.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1F, .7F);
        if (look.y < -.55) {
            owner.setDeltaMovement(owner.getDeltaMovement().add(-look.x * .15, .65, -look.z * .15)); owner.hurtMarked = true;
        }
        return true;
    }
    public static void boost(ServerPlayer owner, Vec3 blast) {
        Vec3 away = owner.position().subtract(blast).multiply(1,0,1).normalize().scale(.35);
        owner.setDeltaMovement(away.x, Math.max(1.5, owner.getDeltaMovement().y + 1.15), away.z);
        owner.hurtMarked = true;
    }
    private CannonAbility() {}
}
