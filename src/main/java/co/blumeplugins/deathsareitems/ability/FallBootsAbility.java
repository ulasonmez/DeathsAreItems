package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.config.ModConfig;
import co.blumeplugins.deathsareitems.entity.custom.LaunchedBlockEntity;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class FallBootsAbility {
    private static final Map<UUID, Landing> LANDINGS = new HashMap<>();
    private static final Map<UUID, Long> LAST_IMPACT = new HashMap<>();
    private static final List<Launch> LAUNCHES = new ArrayList<>();

    private FallBootsAbility() {}

    public static boolean equipped(net.minecraft.world.entity.LivingEntity player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.FALL_DAMAGE_BOOTS.get());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFall(LivingFallEvent event) {
        if (!equipped(event.getEntity())) return;
        event.setDamageMultiplier(0);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && event.getDistance() >= 3) impact(player, event.getDistance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (equipped(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_FALL)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (!equipped(player) || !player.isAlive() || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()) {
            LANDINGS.remove(id);
            return;
        }
        Landing landing = LANDINGS.get(id);
        if (player.fallDistance >= 2.5F && player.getDeltaMovement().y < -.1) {
            if (landing == null) {
                landing = new Landing();
                LANDINGS.put(id, landing);
            }
            landing.distance = Math.max(landing.distance, player.fallDistance);
            if (!landing.cushion && !player.isShiftKeyDown() && ModConfig.CUSHIONS.get()) {
                Vec3 from = player.position().add(0, .1, 0);
                Vec3 to = from.add(0, -Math.max(1.6, -player.getDeltaMovement().y + .9), 0);
                BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP) {
                    placeCushion(player, hit.getBlockPos().above());
                    landing.cushion = true;
                }
            }
        } else if (landing != null && (player.onGround() || player.isInWaterOrBubble() || player.getDeltaMovement().y >= 0)) {
            if (landing.distance >= 3) impact(player, landing.distance);
            LANDINGS.remove(id);
        }
    }

    /** Never covers existing blocks, fluids or block entities; honours Forge placement cancellation. */
    public static int placeCushion(ServerPlayer player, BlockPos center) {
        ServerLevel level = player.serverLevel();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ModItems.FALL_DAMAGE_BOOTS.get())) return 0;
        BlockState cushion = CushionCycle.next(boots).defaultBlockState();
        if (cushion.is(Blocks.WATER) && level.dimensionType().ultraWarm()) {
            level.sendParticles(ParticleTypes.CLOUD, center.getX() + .5, center.getY() + .2, center.getZ() + .5, 15, .4, .1, .4, .03);
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, .7F, 1.1F);
            CushionCycle.advance(boots);
            player.getInventory().setChanged();
            return 0;
        }
        int placed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (!canModify(player, pos) || !level.getBlockState(pos).isAir()
                        || !level.getFluidState(pos).isEmpty()) continue;
                // A fast fall or another nearby mob must not be enclosed by a late solid cushion.
                if (!cushion.is(Blocks.WATER) && (new AABB(pos).intersects(player.getBoundingBox())
                        || !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty())) continue;
                BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
                if (level.setBlock(pos, cushion, 3)) {
                    if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) snapshot.restore(true, false);
                    else placed++;
                }
            }
        }
        if (placed > 0) {
            level.playSound(null, center, cushion.is(Blocks.WATER) ? SoundEvents.BUCKET_EMPTY : cushion.getSoundType(level, center, player).getPlaceSound(),
                    SoundSource.PLAYERS, .7F, 1.1F);
            CushionCycle.advance(boots);
            player.getInventory().setChanged();
        }
        return placed;
    }

    private static boolean canModify(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        return !player.isSpectator() && level.hasChunkAt(pos)
                && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.mayBuild() && !player.server.isUnderSpawnProtection(level, pos, player);
    }

    private static void impact(ServerPlayer player, float distance) {
        long now = player.serverLevel().getGameTime();
        if (now - LAST_IMPACT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2) < 10) return;
        LAST_IMPACT.put(player.getUUID(), now);
        LANDINGS.remove(player.getUUID());
        BlockPos support = player.getOnPos();
        BlockState landedOn = player.level().getBlockState(support);
        player.serverLevel().playSound(null, player.blockPosition(), landingSound(landedOn, player.serverLevel(), support, player),
                SoundSource.PLAYERS, .7F, landedOn.getSoundType(player.level(), support, player).getPitch());
        if (!player.isShiftKeyDown()) {
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .05, player.getZ(), 15, .55, .05, .55, .05);
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + .15, player.getZ(), 3, .45, .15, .45, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + .1, player.getZ(), 60, .9, .15, .9, .13);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .8F, .6F);
        if (!ModConfig.TERRAIN_LAUNCH.get() || !player.mayBuild()) return;
        BlockPos center = player.getOnPos();
        List<BlockPos> blocks = new ArrayList<>();
        for (int dy = 0; dy >= -1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) blocks.add(center.offset(dx, dy, dz));
            }
        }
        blocks.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        int count = 0;
        for (BlockPos pos : blocks) {
            if (count >= ModConfig.MAX_DEBRIS.get()) break;
            if (!canLaunch(player, pos)) continue;
            LAUNCHES.add(new Launch(player.getUUID(), level, pos.immutable(), level.getBlockState(pos), now + count * 2L, center));
            count++;
        }
    }

    public static SoundEvent landingSound(BlockState state, ServerLevel level, BlockPos pos, LivingEntity entity) {
        return entity.isInWaterOrBubble() || state.is(Blocks.WATER) ? SoundEvents.PLAYER_SPLASH
                : state.getSoundType(level, pos, entity).getFallSound();
    }

    public static boolean canLaunch(ServerPlayer player, BlockPos pos) {
        if (!canModify(player, pos)) return false;
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_PORTAL)
                && !state.getCollisionShape(level, pos).isEmpty() && state.getDestroySpeed(level, pos) >= 0;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Iterator<Launch> iterator = LAUNCHES.iterator();
        while (iterator.hasNext()) {
            Launch launch = iterator.next();
            if (launch.level != level || level.getGameTime() < launch.due) continue;
            iterator.remove();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(launch.owner);
            if (player == null || player.level() != level || !canLaunch(player, launch.pos)
                    || !level.getBlockState(launch.pos).equals(launch.state)) continue;
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, launch.pos, launch.state, player))) continue;
            double dx = launch.pos.getX() - launch.center.getX();
            double dz = launch.pos.getZ() - launch.center.getZ();
            LaunchedBlockEntity debris = LaunchedBlockEntity.create(level, launch.pos, launch.state,
                    dx * .13 + (level.random.nextDouble() - .5) * .08,
                    .68 + level.random.nextDouble() * .3, dz * .13 + (level.random.nextDouble() - .5) * .08);
            // A visual block must correspond to exactly one successfully removed world block.
            if (!level.setBlock(launch.pos, Blocks.AIR.defaultBlockState(), 3)) continue;
            if (!level.addFreshEntity(debris)) level.setBlock(launch.pos, launch.state, 3);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LANDINGS.remove(event.getEntity().getUUID());
        LAST_IMPACT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        LANDINGS.clear();
        LAST_IMPACT.clear();
        LAUNCHES.clear();
    }

    private static final class Landing {
        float distance;
        boolean cushion;
    }
    private record Launch(UUID owner, ServerLevel level, BlockPos pos, BlockState state, long due, BlockPos center) {}
}
