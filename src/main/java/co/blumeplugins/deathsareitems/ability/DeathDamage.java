package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;

public final class DeathDamage {
    public static final ResourceKey<DamageType> ARROWS = key("all_the_arrows"), MOBS = key("all_the_mobs"),
            ANVILS = key("lots_of_anvils"), DROWN = key("forced_drown"), SUFFOCATE = key("forced_suffocate"),
            CRAM = key("squished_too_much"), CACTUS = key("pricked_to_death"), PEARL = key("pearl_impact"), CHAOS = key("teleportation_chaos"), SOUL = key("soul_wither");
    private static ResourceKey<DamageType> key(String name) { return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(DeathsAreItems.MOD_ID, name)); }
    public static DamageSource source(LivingEntity target, ResourceKey<DamageType> key, Entity owner) {
        return new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), owner);
    }
    public static void hurt(LivingEntity target, ResourceKey<DamageType> key, Entity owner, float amount) {
        target.hurt(source(target, key, owner), amount);
    }
    public static void broadcastMob(LivingEntity target, DamageSource source) {
        if (!(target instanceof Player) && target.level() instanceof ServerLevel level && level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES))
            level.getServer().getPlayerList().broadcastSystemMessage(source.getLocalizedDeathMessage(target), false);
    }
    private DeathDamage() {}
}
