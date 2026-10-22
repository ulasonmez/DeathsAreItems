package co.blumeplugins.deathsareitems.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;

/** Shared permission checks for delayed abilities: checked again at the time of mutation. */
public final class WorldActions {
    public static boolean allowed(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        return player.isAlive() && player.mayBuild() && !player.isSpectator() && level.hasChunkAt(pos)
                && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && !player.server.isUnderSpawnProtection(level, pos, player);
    }
    public static boolean breakable(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        return allowed(player, pos) && !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && state.getDestroySpeed(player.level(), pos) >= 0 && !state.getCollisionShape(player.level(), pos).isEmpty();
    }
    public static boolean breakEvent(ServerPlayer player, BlockPos pos) {
        return breakable(player, pos) && !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(player.level(), pos, player.level().getBlockState(pos), player));
    }
    public static boolean place(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!allowed(player, pos)) return false;
        var level = player.serverLevel();
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, state, 3)) return false;
        if (ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP)) { snapshot.restore(true, false); return false; }
        return true;
    }
    private WorldActions() {}
}
