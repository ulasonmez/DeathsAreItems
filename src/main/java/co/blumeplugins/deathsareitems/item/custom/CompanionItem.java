package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.einfach.Companions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

public final class CompanionItem extends EinfachItem {
    private final boolean viny;
    public CompanionItem(boolean viny) { super(new Properties().stacksTo(1)); this.viny = viny; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer owner) {
            if (owner.isShiftKeyDown()) Companions.recall(owner, viny);
            else if (viny) {
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
                if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
                    Companions.summon(owner, true, Vec3.atCenterOf(hit.getBlockPos()));
                else Companions.message(owner, "viny_water");
            } else Companions.summon(owner, false, owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(2)));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer(); if (player == null) return InteractionResult.PASS;
        if (player instanceof ServerPlayer owner) {
            if (owner.isShiftKeyDown()) Companions.recall(owner, viny);
            else {
                BlockPos pos = context.getClickedPos();
                if (viny && !context.getLevel().getFluidState(pos).is(FluidTags.WATER)) pos = pos.relative(context.getClickedFace());
                else if (!viny) pos = pos.relative(context.getClickedFace());
                Companions.summon(owner, viny, viny ? Vec3.atCenterOf(pos) : Vec3.atBottomCenterOf(pos));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
