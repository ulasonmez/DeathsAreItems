package co.blumeplugins.deathsareitems.block.custom;

import co.blumeplugins.deathsareitems.block.entity.ModBlockEntities;
import co.blumeplugins.deathsareitems.block.entity.custom.GustafJukeboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class GustafJukeboxBlock extends JukeboxBlock {
    public GustafJukeboxBlock() { super(BlockBehaviour.Properties.copy(Blocks.JUKEBOX)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GustafJukeboxBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player && level.getBlockEntity(pos) instanceof GustafJukeboxBlockEntity box) box.setOwner(placer.getUUID());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(HAS_RECORD) && player.getItemInHand(hand).getItem() instanceof RecordItem
                && level.getBlockEntity(pos) instanceof GustafJukeboxBlockEntity box) {
            if (!level.isClientSide) {
                box.setFirstItem(player.getItemInHand(hand).copyWithCount(1));
                if (!player.isCreative()) player.getItemInHand(hand).shrink(1);
                player.awardStat(net.minecraft.stats.Stats.PLAY_RECORD);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GUSTAFS_JUKEBOX.get(), GustafJukeboxBlockEntity::tick);
    }
}
