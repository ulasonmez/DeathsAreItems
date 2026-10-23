package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

/** Persistent scheduled block transitions: unloading/restarting cannot strand cages or cancel molten trails. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class TemporaryTerrain extends SavedData {
    private final List<Cell> cells = new ArrayList<>();
    public static TemporaryTerrain get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TemporaryTerrain::load, TemporaryTerrain::new, "deathsareitems_terrain");
    }
    public static boolean put(ServerPlayer owner, BlockPos pos, BlockState state, BlockState after, int delay, String group) {
        var level = owner.serverLevel(); var data = get(owner.server);
        if (data.cells.size() >= 8192 || !WorldActions.allowed(owner, pos)) return false;
        BlockState old = level.getBlockState(pos);
        for (int i = 0; i < data.cells.size(); i++) {
            Cell c = data.cells.get(i);
            if (!c.dimension.equals(level.dimension().location()) || !c.pos.equals(pos)) continue;
            if (group.equals("molten_trail") && c.group.equals("lava_walk") && old.equals(state)) {
                data.cells.set(i, new Cell(c.dimension,c.pos,state,after,c.before,owner.getUUID(),level.getGameTime()+delay,group));
                data.setDirty(); return true;
            }
            return false;
        }
        if (old.equals(state)) {
            if (!group.equals("molten_trail") || !WorldActions.breakEvent(owner,pos)) return false;
            data.cells.add(new Cell(level.dimension().location(),pos.immutable(),state,after,old,owner.getUUID(),level.getGameTime()+delay,group));
            data.setDirty(); return true;
        }
        if (old.hasBlockEntity() || old.getDestroySpeed(level, pos) < 0) return false;
        if (!old.isAir() && old.getFluidState().isEmpty() && !WorldActions.breakEvent(owner, pos)) return false;
        if (!WorldActions.place(owner, pos, state)) return false;
        data.cells.add(new Cell(level.dimension().location(), pos.immutable(), state, after, old, owner.getUUID(), level.getGameTime() + delay, group));
        data.setDirty(); return true;
    }
    public static void forget(ServerLevel level, Collection<BlockPos> positions) {
        var data = get(level.getServer());
        if (data.cells.removeIf(c -> c.dimension.equals(level.dimension().location()) && positions.contains(c.pos))) data.setDirty();
    }
    public static void renewWalk(ServerPlayer owner, BlockPos pos, int delay) {
        var data = get(owner.server);
        for (int i = 0; i < data.cells.size(); i++) {
            Cell c = data.cells.get(i);
            if (c.dimension.equals(owner.level().dimension().location()) && c.pos.equals(pos) && c.group.equals("lava_walk")) {
                data.cells.set(i, new Cell(c.dimension,c.pos,c.state,c.after,c.before,c.owner,owner.level().getGameTime()+delay,c.group)); data.setDirty(); return;
            }
        }
    }
    public static void shatter(ServerLevel level, String group) {
        var data = get(level.getServer());
        for (int i = 0; i < data.cells.size(); i++) {
            Cell c = data.cells.get(i);
            if (!c.dimension.equals(level.dimension().location()) || !c.group.equals(group) || !level.hasChunkAt(c.pos)) continue;
            if (matches(c,level.getBlockState(c.pos))) {
                level.setBlock(c.pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                // Leave a real opening during the ten-tick launch; then restore displaced terrain.
                data.cells.set(i,new Cell(c.dimension,c.pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),c.after,c.before,c.owner,
                        level.getGameTime()+12,"shattered."+group)); data.setDirty();
            }
        }
    }
    public static void finish(ServerLevel level, String group) {
        var data = get(level.getServer());
        var it = data.cells.iterator();
        while (it.hasNext()) {
            Cell cell = it.next();
            if (!cell.dimension.equals(level.dimension().location()) || !cell.group.equals(group)) continue;
            if (!level.hasChunkAt(cell.pos)) continue;
            if (matches(cell,level.getBlockState(cell.pos))) level.setBlock(cell.pos, cell.after, 3);
            it.remove(); data.setDirty();
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer(); if (server == null) return;
        TemporaryTerrain data = get(server);
        var iterator = data.cells.iterator();
        while (iterator.hasNext()) {
            Cell cell = iterator.next();
            ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, cell.dimension));
            if (level == null || level.getGameTime() < cell.due || !level.hasChunkAt(cell.pos)) continue;
            if (matches(cell,level.getBlockState(cell.pos))) {
                ServerPlayer owner = cell.owner == null ? null : server.getPlayerList().getPlayer(cell.owner);
                if (cell.group.equals("molten_trail")) {
                    if (owner == null || owner.level() != level || !WorldActions.breakEvent(owner,cell.pos)
                            || !WorldActions.place(owner,cell.pos,cell.after)) level.setBlock(cell.pos,cell.before,3);
                } else level.setBlock(cell.pos, cell.after, 3);
            }
            iterator.remove(); data.setDirty();
        }
    }
    private static boolean matches(Cell cell, BlockState current) {
        // Pane connections are recomputed as neighboring glass is restored.
        return cell.group.startsWith("crammer.") ? current.is(cell.state.getBlock()) : current.equals(cell.state);
    }
    public static TemporaryTerrain load(CompoundTag tag) {
        var data = new TemporaryTerrain();
        for (Tag entry : tag.getList("Cells", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)entry;
            ResourceLocation dimension = ResourceLocation.tryParse(t.getString("Dimension")); if (dimension == null) continue;
            data.cells.add(new Cell(dimension, BlockPos.of(t.getLong("Pos")),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), t.getCompound("State")),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), t.getCompound("After")), NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), t.getCompound("Before")), t.hasUUID("Owner") ? t.getUUID("Owner") : null, t.getString("Group").startsWith("crammer.") ? 0 : t.getLong("Due"), t.getString("Group")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Cell c : cells) {
            CompoundTag t = new CompoundTag(); t.putString("Dimension", c.dimension.toString()); t.putLong("Pos", c.pos.asLong());
            t.put("State", NbtUtils.writeBlockState(c.state)); t.put("After", NbtUtils.writeBlockState(c.after)); t.put("Before", NbtUtils.writeBlockState(c.before)); if (c.owner != null) t.putUUID("Owner",c.owner);
            t.putLong("Due", c.due); t.putString("Group", c.group); list.add(t);
        }
        tag.put("Cells", list); return tag;
    }
    private record Cell(ResourceLocation dimension, BlockPos pos, BlockState state, BlockState after, BlockState before, UUID owner, long due, String group) {}
}
