package co.blumeplugins.deathsareitems.reward;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.UUID;

/** Vanilla chest blocks after the animation, with server-side ownership markers. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class SettledRewardChests {
    private static final String MARKER = DeathsAreItems.MOD_ID + ":reward";

    private SettledRewardChests() {}

    public static boolean place(RewardChestEntity entity, RewardSavedData.Offer offer) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (!offer.generation.equals(entity.generation().orElse(null)) || !offer.owner.equals(entity.owner().orElse(null))) return false;
        BlockPos pos = entity.blockPosition();
        if (read(level, pos).filter(marker -> marker.matches(offer) && marker.slot == entity.slot()).isPresent()) {
            entity.setSettled(true);
            return true;
        }
        BlockState original = level.getBlockState(pos);
        if (!canPlace(level, pos)) return false;
        Direction facing = Direction.fromYRot(-entity.getYRot());
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing)
                .setValue(ChestBlock.TYPE, ChestType.SINGLE);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, chest, 3)) return false;
        if (ForgeEventFactory.onBlockPlace(level.getServer().getPlayerList().getPlayer(offer.owner), snapshot, Direction.UP)) {
            snapshot.restore(true, false);
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            snapshot.restore(true, false);
            return false;
        }
        CompoundTag marker = new CompoundTag();
        marker.putUUID("Owner", offer.owner);
        marker.putUUID("Generation", offer.generation);
        marker.putInt("Slot", entity.slot());
        blockEntity.getPersistentData().put(MARKER, marker);
        blockEntity.setChanged();
        offer.placedChests.add(new RewardSavedData.PlacedChest(pos.immutable(), original));
        RewardSavedData.get(level.getServer()).setDirty();
        entity.setSettled(true);
        return true;
    }

    public static void restore(MinecraftServer server, RewardSavedData.Offer offer) {
        if (offer.dimension == null) return;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, offer.dimension));
        if (level == null) return;
        for (RewardSavedData.PlacedChest chest : offer.placedChests) {
            if (read(level, chest.pos()).filter(marker -> marker.matches(offer)).isPresent()) {
                // Vanilla drops any items inserted by a hopper before replacing the empty reward chest.
                level.setBlock(chest.pos(), chest.original(), 3);
            }
        }
        offer.placedChests.clear();
    }

    public static Optional<Marker> read(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.CHEST)) return Optional.empty();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return Optional.empty();
        CompoundTag marker = blockEntity.getPersistentData().getCompound(MARKER);
        if (!marker.hasUUID("Owner") || !marker.hasUUID("Generation")) return Optional.empty();
        return Optional.of(new Marker(marker.getUUID("Owner"), marker.getUUID("Generation"), marker.getInt("Slot")));
    }

    public static boolean canPlace(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.CHEST)) return false;
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || read(event.getLevel(), event.getPos()).isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) RewardManager.selectBlock(event.getEntity(), event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && read(level, event.getPos()).isPresent()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> read(event.getLevel(), pos).isPresent());
    }

    public record Marker(UUID owner, UUID generation, int slot) {
        public boolean matches(RewardSavedData.Offer offer) {
            return owner.equals(offer.owner) && generation.equals(offer.generation);
        }
    }
}
