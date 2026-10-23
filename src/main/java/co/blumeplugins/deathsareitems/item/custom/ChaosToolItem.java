package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public class ChaosToolItem extends Item {
    public enum Kind { CRAMMER, CANNON, SHIELD }
    private final Kind kind;
    public ChaosToolItem(Kind kind) { super(new Properties().stacksTo(1).fireResistant()); this.kind = kind; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return kind == Kind.SHIELD ? UseAnim.BLOCK : UseAnim.NONE; }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return kind == Kind.CANNON ? slotChanged || !ItemStack.isSameItemSameTags(oldStack, newStack)
                : super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }
    @Override public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction action) {
        return kind == Kind.SHIELD && action == net.minecraftforge.common.ToolActions.SHIELD_BLOCK;
    }
    @Override public int getUseDuration(ItemStack stack) { return kind == Kind.SHIELD ? 72000 : 0; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.CANNON) player.getCooldowns().removeCooldown(this);
        if (kind == Kind.SHIELD) {
            player.startUsingItem(hand);
            if (!level.isClientSide && !player.getCooldowns().isOnCooldown(this) && PearlAbility.throwPearl((ServerPlayer)player))
                player.getCooldowns().addCooldown(this, 16);
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        ServerPlayer owner = (ServerPlayer)player;
        boolean started = switch (kind) {
            case CRAMMER -> { LivingEntity target = DeathAbilities.target(owner, 24); yield target != null && CrammerAbility.start(owner, target); }
            case CANNON -> CannonAbility.fire(owner);
            case SHIELD -> PearlAbility.throwPearl(owner);
        };
        if (!started) return InteractionResultHolder.fail(stack);
        if (kind == Kind.CRAMMER) player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (kind != Kind.CRAMMER) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (CrammerAbility.start((ServerPlayer)player, target)) {
            player.getCooldowns().addCooldown(this, 20); return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, "tooltip.deathsareitems." + switch (kind) { case CRAMMER -> "entity_crammer"; case CANNON -> "tnt_cannon"; case SHIELD -> "ender_pearl_shield"; });
    }
}
