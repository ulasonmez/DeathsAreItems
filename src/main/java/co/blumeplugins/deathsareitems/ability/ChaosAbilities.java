package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class ChaosAbilities {
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void immunity(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var source = event.getSource();
        if ((source.is(DamageTypes.CRAMMING) && DeathAbilities.carries(p,ModItems.ENTITY_CRAMMER.get()))
                || (source.is(DamageTypes.CACTUS) && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CACTUS_CHESTPLATE.get()))
                || (source.is(DamageTypes.BAD_RESPAWN_POINT) && DeathAbilities.carries(p,ModItems.BEDROCK.get()))
                || ((source.is(DeathDamage.PEARL) || source.is(DamageTypes.FALL) && PearlAbility.isPearlImpact(p)) && PearlAbility.holdsShield(p))) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (event.getSource().is(DeathDamage.CRAM) || event.getSource().is(DeathDamage.CACTUS) || event.getSource().is(DeathDamage.CHAOS))
            DeathDamage.broadcastMob(event.getEntity(), event.getSource());
    }
    private ChaosAbilities() {}
}
