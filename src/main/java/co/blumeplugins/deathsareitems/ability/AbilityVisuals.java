package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Vanilla display entities provide temporary visuals without adding final item models. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class AbilityVisuals {
    private static final List<Visual> ACTIVE = new ArrayList<>();
    public static Display.BlockDisplay block(ServerLevel level, Vec3 pos, BlockState state, float scale, int life, int color) {
        Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        CompoundTag tag = base(scale, color);
        if (color >= 0) {
            CompoundTag brightness = new CompoundTag(); brightness.putInt("block", 15); brightness.putInt("sky", 15);
            tag.put("brightness", brightness);
        }
        tag.put("block_state", NbtUtils.writeBlockState(state));
        display.load(tag);
        spawn(level, display, pos, life);
        return display;
    }
    public static Display.BlockDisplay anvil(ServerLevel level, Vec3 start, Vec3 impact, int flight, int life) {
        Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        CompoundTag tag = base(.48F, -1);
        tag.put("block_state", NbtUtils.writeBlockState(net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState()));
        Vec3 outward = start.subtract(impact).normalize();
        org.joml.Quaternionf rotation = new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0, 1, 0),
                new org.joml.Vector3f((float)outward.x, (float)outward.y, (float)outward.z));
        CompoundTag transform = tag.getCompound("transformation");
        ListTag quaternion = new ListTag();
        for (float f : new float[]{rotation.x, rotation.y, rotation.z, rotation.w}) quaternion.add(FloatTag.valueOf(f));
        transform.put("left_rotation", quaternion);
        org.joml.Vector3f centered = rotation.transform(new org.joml.Vector3f(-.24F, -.24F, -.24F));
        ListTag translation = new ListTag();
        for (float f : new float[]{centered.x, centered.y, centered.z}) translation.add(FloatTag.valueOf(f));
        transform.put("translation", translation);
        CompoundTag arrival = transform.copy();
        ListTag departure = new ListTag();
        for (float f : new float[]{centered.x + (float)(start.x-impact.x),
                centered.y + (float)(start.y-impact.y), centered.z + (float)(start.z-impact.z)})
            departure.add(FloatTag.valueOf(f));
        transform.put("translation", departure);
        tag.putInt("interpolation_duration", flight);
        display.load(tag);
        spawn(level, display, impact, life);
        Visual visual = ACTIVE.get(ACTIVE.size() - 1);
        visual.arrivalTransform = arrival; visual.flight = flight;
        return display;
    }
    public static Display.BlockDisplay spike(ServerLevel level, Vec3 start, Vec3 end, int flight, int life, float length) {
        Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        CompoundTag tag = base(1F, -1);
        // The tip's light probe ends inside the floor; keep the sand blades' original color.
        CompoundTag brightness = new CompoundTag();
        brightness.putInt("block", 15); brightness.putInt("sky", 15);
        tag.put("brightness", brightness);
        tag.put("block_state", NbtUtils.writeBlockState(co.blumeplugins.deathsareitems.block.ModBlocks.CACTUS_SPIKE.get().defaultBlockState()));
        org.joml.Quaternionf q = new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0, 1, 0),
                new org.joml.Vector3f((float)(end.x-start.x), (float)(end.y-start.y), (float)(end.z-start.z)).normalize());
        CompoundTag transform = tag.getCompound("transformation");
        ListTag rotation = new ListTag(); for (float f : new float[]{q.x,q.y,q.z,q.w}) rotation.add(FloatTag.valueOf(f));
        transform.put("left_rotation", rotation);
        // The two flat blades run from local Y=0 to Y=2. Anchor their tapered tip at end.
        ListTag scale = new ListTag();
        for (float f : new float[]{1F, length / 2F, 1F}) scale.add(FloatTag.valueOf(f));
        transform.put("scale", scale);
        org.joml.Vector3f offset = q.transform(new org.joml.Vector3f(-.5F, -length, -.5F));
        ListTag translation = new ListTag(); for (float f : new float[]{offset.x,offset.y,offset.z}) translation.add(FloatTag.valueOf(f));
        transform.put("translation", translation);
        CompoundTag arrival = transform.copy();
        // Animate the transform rather than teleporting the display every server tick.
        // The client interpolates this translation at its own frame rate.
        translation = new ListTag();
        for (float f : new float[]{offset.x + (float)(start.x-end.x),
                offset.y + (float)(start.y-end.y), offset.z + (float)(start.z-end.z)})
            translation.add(FloatTag.valueOf(f));
        transform.put("translation", translation);
        tag.putInt("interpolation_duration", flight);
        display.load(tag); spawn(level, display, end, life);
        Visual visual = ACTIVE.get(ACTIVE.size() - 1);
        visual.arrivalTransform = arrival; visual.flight = flight;
        return display;
    }
    private static CompoundTag base(float scale, int color) {
        CompoundTag tag = new CompoundTag();
        CompoundTag transform = new CompoundTag();
        ListTag size = new ListTag(), translation = new ListTag();
        for (int i = 0; i < 3; i++) { size.add(FloatTag.valueOf(scale)); translation.add(FloatTag.valueOf(-scale / 2)); }
        ListTag rotation = new ListTag();
        for (float value : new float[]{0,0,0,1}) rotation.add(FloatTag.valueOf(value));
        transform.put("left_rotation", rotation); transform.put("right_rotation", rotation.copy());
        transform.put("scale", size); transform.put("translation", translation); tag.put("transformation", transform);
        tag.putInt("interpolation_duration", 2); tag.putFloat("view_range", 1F);
        tag.putBoolean("Glowing", color >= 0); tag.putInt("glow_color_override", color);
        return tag;
    }
    private static void spawn(ServerLevel level, Entity entity, Vec3 pos, int life) {
        entity.setPos(pos); entity.setInvulnerable(true); entity.setNoGravity(true);
        entity.getPersistentData().putBoolean("deathsareitems.TemporaryVisual", true);
        level.addFreshEntity(entity); ACTIVE.add(new Visual(entity, pos, life));
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        var iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Visual v = iterator.next();
            if (v.entity.level() != event.level) continue;
            if (v.entity.isRemoved() || ++v.age >= v.life) { v.entity.discard(); iterator.remove(); continue; }
            if (v.arrivalTransform != null && v.age >= 2) {
                // Allow the initial spawn state to reach clients before starting the flight.
                CompoundTag tag = v.entity.saveWithoutId(new CompoundTag());
                tag.put("transformation", v.arrivalTransform);
                tag.putInt("interpolation_duration", v.flight);
                tag.putInt("start_interpolation", 0);
                v.entity.load(tag);
                v.arrivalTransform = null;
            }
            if (v.destination != null) {
                double t = Math.min(1, (double)v.age / v.flight);
                v.entity.setPos(v.start.lerp(v.destination, t));
            }
        }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().getPersistentData().getBoolean("deathsareitems.TemporaryVisual")) event.setCanceled(true);
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ACTIVE.clear(); }
    private static final class Visual {
        final Entity entity; final Vec3 start; final int life; int age; int flight; Vec3 destination;
        CompoundTag arrivalTransform;
        Visual(Entity entity, Vec3 start, int life) { this.entity = entity; this.start = start; this.life = life; this.flight = life; }
    }
}
