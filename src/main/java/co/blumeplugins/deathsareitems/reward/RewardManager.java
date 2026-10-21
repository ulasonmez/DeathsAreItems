package co.blumeplugins.deathsareitems.reward;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import co.blumeplugins.deathsareitems.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class RewardManager {
    private static final int CHEST_DISTANCE = 3;
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1F, .91F, .12F), 1.6F);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(.22F, 1F, .08F), 1.6F);

    private RewardManager() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeathRewards.forDeath(event.getSource()).ifPresentOrElse(definition -> offer(player, definition), () -> {
                RewardSavedData data = RewardSavedData.get(player.server);
                RewardSavedData.Offer previous = data.offers.remove(player.getUUID());
                if (previous != null) {
                    removeChests(player.server, previous);
                    data.setDirty();
                }
            });
        }
    }

    public static void offer(ServerPlayer player, RewardDefinition definition) {
        RewardSavedData data = RewardSavedData.get(player.server);
        RewardSavedData.Offer old = data.offers.remove(player.getUUID());
        if (old != null) removeChests(player.server, old);
        data.offers.put(player.getUUID(), new RewardSavedData.Offer(player.getUUID(), definition.id()));
        data.setDirty();
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) resetPresentation(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetPresentation(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetPresentation(player);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resetPresentation(player);
    }

    private static void resetPresentation(ServerPlayer player) {
        RewardSavedData data = RewardSavedData.get(player.server);
        RewardSavedData.Offer offer = data.offers.get(player.getUUID());
        if (offer != null) {
            removeChests(player.server, offer);
            offer.generation = UUID.randomUUID();
            offer.spawned = false;
            offer.delay = RewardTimings.RESPAWN_DELAY;
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        RewardSavedData data = RewardSavedData.get(server);
        for (RewardSavedData.Offer offer : new ArrayList<>(data.offers.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(offer.owner);
            if (player == null || !player.isAlive() || player.isSpectator()) continue;
            if (!offer.spawned && --offer.delay <= 0) spawnPresentation(player, offer);
            else if (offer.spawned && server.overworld().getGameTime() % 40 == 0) {
                Entity chest = offer.chests.isEmpty() ? null : player.serverLevel().getEntity(offer.chests.get(0));
                if (!player.level().dimension().location().equals(offer.dimension)
                        || chest == null || chest.distanceToSqr(player) > 24 * 24) resetPresentation(player);
            }
        }
        Iterator<RewardSavedData.Grant> iterator = data.grants.iterator();
        while (iterator.hasNext()) {
            RewardSavedData.Grant grant = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(grant.owner());
            if (player == null || !player.isAlive() || server.overworld().getGameTime() < grant.due()) continue;
            ItemStack stack = grant.stack().copy();
            if (!player.getInventory().add(stack)) {
                net.minecraft.world.entity.item.ItemEntity dropped = player.drop(stack, false);
                if (dropped != null) {
                    dropped.setTarget(player.getUUID());
                    dropped.setNoPickUpDelay();
                    dropped.setUnlimitedLifetime();
                }
            }
            player.inventoryMenu.broadcastChanges();
            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .8F, 1.15F);
            iterator.remove();
            data.setDirty();
        }
    }

    private static void spawnPresentation(ServerPlayer player, RewardSavedData.Offer offer) {
        ServerLevel level = player.serverLevel();
        List<Vec3> positions = findPositions(player);
        if (positions.size() != 3) {
            offer.delay = 40; // Retry in a tight room without replacing terrain.
            player.displayClientMessage(Component.translatable("message.deathsareitems.need_space"), true);
            return;
        }
        removeChests(player.server, offer);
        offer.dimension = level.dimension().location();
        offer.spawnedAt = level.getGameTime();
        offer.generation = UUID.randomUUID();
        offer.chests.clear();
        RewardDefinition definition = DeathRewards.get(offer.reward);
        if (definition == null) {
            removeChests(player.server, offer);
            RewardSavedData.get(player.server).offers.remove(player.getUUID());
            RewardSavedData.get(player.server).setDirty();
            return;
        }
        double centerX = Math.floor(player.getX()) + .5;
        double centerZ = Math.floor(player.getZ()) + .5;
        AABB centeredPlayer = player.getBoundingBox().move(centerX - player.getX(), 0, centerZ - player.getZ());
        if (level.noCollision(player, centeredPlayer)) {
            player.connection.teleport(centerX, player.getY(), centerZ, player.getYRot(), player.getXRot());
        }
        for (int i = 0; i < 3; i++) {
            RewardChestEntity chest = ModEntities.REWARD_CHEST.get().create(level);
            if (chest == null) continue;
            Vec3 pos = positions.get(i);
            chest.setPos(pos);
            Direction facing = Direction.getNearest(centerX - pos.x, 0, centerZ - pos.z);
            chest.setYRot(-facing.toYRot());
            chest.configure(player.getUUID(), offer.generation, i, offer.spawnedAt, definition.options().get(i).titleKey());
            level.addFreshEntity(chest);
            offer.chests.add(chest.getUUID());
        }
        offer.spawned = true;
        RewardSavedData.get(player.server).setDirty();
        level.sendParticles(ModParticles.CONFETTI.get(), player.getX(), player.getY() + 1, player.getZ(), 130, .65, .5, .65, .3);
        ModNetwork.celebrate(player);
    }

    /** Three block-aligned positions, three blocks from the centered respawn anchor. */
    public static List<Vec3> findPositions(ServerPlayer player) {
        List<Vec3> result = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        BlockPos anchor = player.blockPosition();
        Direction forward = player.getDirection();
        Direction[] directions = {forward, forward.getCounterClockWise(), forward.getClockWise()};
        for (int slot = 0; slot < 3; slot++) {
            Vec3 selected = null;
            for (int dy : new int[]{0, -1, 1, -2, 2}) {
                BlockPos feet = anchor.relative(directions[slot], CHEST_DISTANCE).offset(0, dy, 0);
                if (!level.hasChunkAt(feet) || !level.getWorldBorder().isWithinBounds(feet)) continue;
                if (!SettledRewardChests.canPlace(level, feet)) continue;
                if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
                AABB box = new AABB(feet).deflate(.0625).expandTowards(0, .35, 0);
                if (!level.noCollision(box)) continue;
                selected = Vec3.atBottomCenterOf(feet);
                break;
            }
            if (selected == null) return List.of();
            result.add(selected);
        }
        return result;
    }

    public static void tickChest(RewardChestEntity chest) {
        if (!(chest.level() instanceof ServerLevel level)) return;
        UUID owner = chest.owner().orElse(null);
        RewardSavedData.Offer offer = RewardSavedData.get(level.getServer()).offers.get(owner);
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (offer == null || player == null || !offer.spawned || !offer.generation.equals(chest.generation().orElse(null))) {
            chest.discard();
            return;
        }
        int age = (int) chest.animationAge(0);
        int settle = RewardTimings.settlesAt(chest.slot());
        if (age >= settle && !chest.isSettled() && !SettledRewardChests.place(chest, offer)) {
            resetPresentation(player);
            player.displayClientMessage(Component.translatable("message.deathsareitems.need_space"), true);
            return;
        }
        if (chest.isSettled() && SettledRewardChests.read(level, chest.blockPosition())
                .filter(marker -> marker.matches(offer) && marker.slot() == chest.slot()).isEmpty()) {
            resetPresentation(player);
            return;
        }
        if (age < RewardTimings.RISE && age % 4 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), chest.getX(), chest.getY() + .08, chest.getZ(), 8, .5, .05, .5, .12);
            level.playSound(null, chest.blockPosition(), SoundEvents.GRAVEL_STEP, SoundSource.BLOCKS, .38F, .7F + age * .012F);
        }
        if ((age < settle && age % 3 == 0) || (age >= settle && age % 25 == 0)) magicParticles(level, chest.position(), age < settle ? 4 : 1);
        if (age == settle) {
            magicParticles(level, chest.position(), 22);
            level.playSound(null, chest.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, .8F, .85F + chest.slot() * .13F);
            level.playSound(null, chest.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .45F, 1F + chest.slot() * .18F);
        }
    }

    private static void magicParticles(ServerLevel level, Vec3 pos, int count) {
        level.sendParticles(GOLD, pos.x, pos.y + .45, pos.z, count, .6, .5, .6, .015);
        level.sendParticles(GREEN, pos.x, pos.y + .3, pos.z, count, .55, .35, .55, .015);
        if (count > 1) level.sendParticles(ParticleTypes.ENTITY_EFFECT, pos.x, pos.y + .1, pos.z, 0, .6, .9, .05, 1);
    }

    public static boolean select(Player candidate, RewardChestEntity chest) {
        if (!chest.isSettled()) return false;
        return selectBlock(candidate, chest.blockPosition());
    }

    public static boolean selectBlock(Player candidate, BlockPos pos) {
        if (!(candidate instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return false;
        SettledRewardChests.Marker marker = SettledRewardChests.read(player.level(), pos).orElse(null);
        if (marker == null) return false;
        if (!marker.owner().equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.deathsareitems.not_yours"), true);
            return false;
        }
        RewardSavedData data = RewardSavedData.get(player.server);
        RewardSavedData.Offer offer = data.offers.get(player.getUUID());
        if (offer == null || !offer.spawned || !marker.matches(offer)
                || !player.level().dimension().location().equals(offer.dimension)
                || offer.placedChests.stream().noneMatch(chest -> chest.pos().equals(pos))
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36 || marker.slot() < 0 || marker.slot() > 2) return false;
        if (player.level().getGameTime() - offer.spawnedAt < RewardTimings.READY) return false;
        RewardDefinition definition = DeathRewards.get(offer.reward);
        if (definition == null) return false;
        RewardOption option = definition.options().get(marker.slot());
        if (!option.selectable()) {
            player.displayClientMessage(Component.translatable("message.deathsareitems.preview_only", Component.translatable(option.titleKey())), true);
            return false;
        }
        ItemStack reward = option.createReward().copy();
        if (reward.isEmpty()) return false;
        // Remove the offer before sending effects; subsequent clicks cannot claim it again.
        data.offers.remove(player.getUUID());
        data.grants.add(new RewardSavedData.Grant(player.getUUID(), reward, player.server.overworld().getGameTime() + RewardTimings.REVEAL));
        data.setDirty();
        removeChests(player.server, offer);
        ModNetwork.reveal(player, reward);
        player.serverLevel().sendParticles(ModParticles.CONFETTI.get(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 65, .4, .4, .4, .18);
        return true;
    }

    private static void removeChests(MinecraftServer server, RewardSavedData.Offer offer) {
        SettledRewardChests.restore(server, offer);
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : offer.chests) {
                Entity entity = level.getEntity(id);
                if (entity instanceof RewardChestEntity) entity.discard();
            }
        }
        offer.chests.clear();
    }
}
