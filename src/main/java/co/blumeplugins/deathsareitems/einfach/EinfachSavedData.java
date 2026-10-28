package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** World-owned progression and cooldowns survive respawns, copies and reconnects. */
public final class EinfachSavedData extends SavedData {
    private final Map<UUID, CompoundTag> players = new HashMap<>();
    public final CompoundTag buttons = new CompoundTag();
    public CompoundTag player(UUID owner) { return players.computeIfAbsent(owner, id -> new CompoundTag()); }
    public static EinfachSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EinfachSavedData::load,
                EinfachSavedData::new, DeathsAreItems.MOD_ID + "_einfach");
    }
    public static EinfachSavedData load(CompoundTag tag) {
        var data = new EinfachSavedData();
        for (Tag entry : tag.getList("Players", Tag.TAG_COMPOUND)) {
            var p = (CompoundTag)entry;
            if (p.hasUUID("Owner")) data.players.put(p.getUUID("Owner"), p.getCompound("Data"));
        }
        data.buttons.merge(tag.getCompound("Buttons"));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        players.forEach((id, value) -> {
            var p = new CompoundTag(); p.putUUID("Owner", id); p.put("Data", value.copy()); list.add(p);
        });
        tag.put("Players", list); tag.put("Buttons", buttons.copy()); return tag;
    }
}
