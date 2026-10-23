package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.SoulStormAbility;
import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class SoulStormItem extends Item {
    public SoulStormItem() { super(new Properties().stacksTo(1).fireResistant()); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.COMMON; }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator() || !player.isAlive() || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        if (!SoulStormAbility.fire((ServerPlayer)player, stack)) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, 12);
        return InteractionResultHolder.consume(stack);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return use(player.level(), player, hand).getResult();
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!entity.isShiftKeyDown()) entity.stopUsingItem();
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, "tooltip.deathsareitems.soul_storm");
    }
}
