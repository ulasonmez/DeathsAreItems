package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.DeathAbilities;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.custom.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class Companions {
    public static boolean isCompanion(Entity e) { return e instanceof JukyEntity || e instanceof VinyEntity; }
    public static boolean hostile(LivingEntity e) {
        return e instanceof Mob && e instanceof Enemy && e.isAlive() && !e.isInvulnerable()
                && !isCompanion(e) && !(e instanceof TamableAnimal tame && tame.isTame())
                && !(e instanceof OwnableEntity owned && owned.getOwnerUUID() != null);
    }
    public static boolean boss(Entity e) {
        return e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || e.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES);
    }
    public static long now(ServerPlayer owner) { return owner.server.overworld().getGameTime(); }
    public static void message(Player owner, String key, Object... args) {
        owner.displayClientMessage(Component.translatable("message.deathsareitems." + key, args), true);
    }
    public static boolean summon(ServerPlayer owner, boolean viny, Vec3 at) {
        if (!owner.isAlive() || owner.isSpectator()) return false;
        var data = EinfachSavedData.get(owner.server); var p = data.player(owner.getUUID());
        String key = viny ? "Viny" : "Juky";
        if (p.hasUUID(key)) { message(owner, "pet_active"); return false; }
        if (now(owner) < p.getLong(key + "Ready")) {
            message(owner, "pet_cooldown", (p.getLong(key + "Ready") - now(owner) + 19) / 20); return false;
        }
        Mob pet = viny ? ModEntities.VINY.get().create(owner.serverLevel()) : ModEntities.JUKY.get().create(owner.serverLevel());
        if (pet == null) return false;
        pet.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        if (!owner.serverLevel().noCollision(pet) || !owner.level().getWorldBorder().isWithinBounds(pet.blockPosition())
                || viny && !owner.level().getFluidState(pet.blockPosition()).is(FluidTags.WATER)) {
            message(owner, viny ? "viny_water" : "pet_space"); return false;
        }
        pet.getPersistentData().putUUID("EinfachOwner", owner.getUUID());
        pet.setPersistenceRequired(); pet.setCustomName(Component.literal(key)); pet.setCustomNameVisible(true);
        if (pet instanceof JukyEntity juky) { juky.setTame(true); juky.setOwnerUUID(owner.getUUID()); juky.setHealth(juky.getMaxHealth()); }
        if (!owner.serverLevel().addFreshEntity(pet)) return false;
        p.putUUID(key, pet.getUUID()); data.setDirty(); message(owner, "pet_summoned", pet.getDisplayName()); return true;
    }
    public static void recall(ServerPlayer owner, boolean viny) {
        String key = viny ? "Viny" : "Juky";
        var data = EinfachSavedData.get(owner.server); var p = data.player(owner.getUUID());
        if (p.hasUUID(key)) {
            UUID id = p.getUUID(key); p.remove(key); data.setDirty();
            for (ServerLevel level : owner.server.getAllLevels()) { Entity pet = level.getEntity(id); if (isCompanion(pet)) pet.discard(); }
            message(owner, "pet_recalled", key);
        }
    }
    public static ServerPlayer owner(Mob pet) {
        if (!(pet.level() instanceof ServerLevel level)) return null;
        var tag = pet.getPersistentData();
        if (!tag.hasUUID("EinfachOwner")) { pet.discard(); return null; }
        UUID ownerId = tag.getUUID("EinfachOwner");
        var data = EinfachSavedData.get(level.getServer()); var p = data.player(ownerId);
        String key = pet instanceof VinyEntity ? "Viny" : "Juky";
        if (!p.hasUUID(key) || !pet.getUUID().equals(p.getUUID(key))) { pet.discard(); return null; }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level) {
            p.remove(key); data.setDirty(); pet.discard(); return null;
        }
        return owner;
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) { recall(owner, true); recall(owner, false); }
        if (!isCompanion(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        var pet = event.getEntity(); var tag = pet.getPersistentData(); if (!tag.hasUUID("EinfachOwner")) return;
        var data = EinfachSavedData.get(level.getServer()); var p = data.player(tag.getUUID("EinfachOwner"));
        String key = pet instanceof VinyEntity ? "Viny" : "Juky";
        if (p.hasUUID(key) && p.getUUID(key).equals(pet.getUUID())) {
            p.remove(key); p.putLong(key + "Ready", level.getServer().overworld().getGameTime() + 600); data.setDirty();
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer p) { recall(p, true); recall(p, false); } }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer p) { recall(p, true); recall(p, false); } }
    /** All death-item damage, including explosions and flying debris, leaves companions alone. */
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void protect(LivingAttackEvent event) {
        if (!isCompanion(event.getEntity())) return;
        var source = event.getSource();
        if (source.typeHolder().unwrapKey().map(k -> k.location().getNamespace().equals(DeathsAreItems.MOD_ID)).orElse(false)
                || source.getEntity() instanceof Player || source.getDirectEntity() instanceof LaunchedBlockEntity)
            event.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        event.getAffectedEntities().removeIf(Companions::isCompanion);
    }
    private Companions() {}
}
