package co.blumeplugins.deathsareitems.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Equipment owns only its own infinite level-I effect; displaced potions resume with elapsed time. */
public final class EquipmentEffects {
    public static void update(ServerPlayer player, String key, MobEffect effect, boolean equipped) {
        CompoundTag data = player.getPersistentData();
        MobEffectInstance current = player.getEffect(effect);
        boolean owned = data.getBoolean(key + ".owned");
        boolean ours = owned && current != null && current.isInfiniteDuration() && current.getAmplifier() == 0
                && current.isAmbient() && !current.isVisible();
        if (!equipped) {
            // Vanilla can hide our infinite effect under a stronger potion. Strip that hidden
            // node too, otherwise it would resume permanently after the potion expires.
            CompoundTag serialized = current == null ? null : current.save(new CompoundTag());
            CompoundTag filtered = owned && serialized != null ? stripOwned(serialized.copy()) : serialized;
            boolean changed = serialized != null && (filtered == null || !serialized.equals(filtered));
            if (changed) {
                player.removeEffect(effect);
                MobEffectInstance backup = data.contains(key + ".backup") ? MobEffectInstance.load(data.getCompound(key + ".backup")) : null;
                long elapsed = Math.max(0, player.level().getGameTime() - data.getLong(key + ".since"));
                if (backup != null && (backup.isInfiniteDuration() || backup.getDuration() > elapsed)) {
                    player.addEffect(new MobEffectInstance(effect, backup.isInfiniteDuration() ? -1 : (int)(backup.getDuration() - elapsed),
                            backup.getAmplifier(), backup.isAmbient(), backup.isVisible(), backup.showIcon()));
                }
                if (filtered != null) {
                    MobEffectInstance external = MobEffectInstance.load(filtered);
                    if (external != null) player.addEffect(external);
                }
            }
            data.remove(key + ".owned"); data.remove(key + ".backup"); data.remove(key + ".since");
            return;
        }
        if (ours || (current != null && (current.getAmplifier() > 0 || current.isInfiniteDuration()))) return;
        if (current != null) {
            data.put(key + ".backup", current.save(new CompoundTag()));
            data.putLong(key + ".since", player.level().getGameTime());
            player.removeEffect(effect);
        }
        player.addEffect(new MobEffectInstance(effect, -1, 0, true, false, true));
        data.putBoolean(key + ".owned", true);
    }
    private static CompoundTag stripOwned(CompoundTag tag) {
        CompoundTag hidden = tag.contains("HiddenEffect") ? stripOwned(tag.getCompound("HiddenEffect")) : null;
        if (tag.getInt("Duration") == -1 && tag.getByte("Amplifier") == 0 && tag.getBoolean("Ambient") && !tag.getBoolean("ShowParticles")) return hidden;
        if (hidden == null) tag.remove("HiddenEffect"); else tag.put("HiddenEffect",hidden);
        return tag;
    }
    private EquipmentEffects() {}
}
