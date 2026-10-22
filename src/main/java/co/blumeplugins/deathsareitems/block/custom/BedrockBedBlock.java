package co.blumeplugins.deathsareitems.block.custom;

import co.blumeplugins.deathsareitems.ability.OwnedExplosions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;

/** Retains vanilla two-cell placement and breaking, but never attempts to sleep or removes itself. */
public final class BedrockBedBlock extends BedBlock {
    public BedrockBedBlock() { super(DyeColor.RED, BlockBehaviour.Properties.copy(Blocks.RED_BED).strength(3F, 3600000F).noOcclusion()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos head = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        if (!level.getBlockState(head).is(this) || !(player instanceof ServerPlayer owner)) return InteractionResult.FAIL;
        String key = "deathsareitems.BedCooldown";
        if (owner.getPersistentData().getLong(key) > level.getGameTime()) return InteractionResult.CONSUME;
        owner.getPersistentData().putLong(key, level.getGameTime() + 30);
        OwnedExplosions.explode(owner, null, Vec3.atCenterOf(head), 8F, true);
        return InteractionResult.CONSUME;
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
}
