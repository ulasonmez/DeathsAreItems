package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.DeathAbilities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class DeathToolItem extends Item {
    public enum Kind { BOULDER, BOW, STAFF, ANVILS }
    private final Kind kind;
    public DeathToolItem(Kind kind) { super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)); this.kind = kind; }
    public Kind kind() { return kind; }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public int getUseDuration(ItemStack stack) { return kind == Kind.STAFF ? 100 : 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return kind == Kind.STAFF ? UseAnim.BOW : UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.BOW) { player.startUsingItem(hand); return InteractionResultHolder.consume(player.getItemInHand(hand)); }
        return DeathAbilities.useTool(player, hand, kind);
    }
    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (kind == Kind.BOW && remaining % 2 == 0 && user instanceof net.minecraft.server.level.ServerPlayer player) DeathAbilities.shootArrow(player);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (kind == Kind.BOULDER || kind == Kind.ANVILS)
            return DeathAbilities.useBlock(context.getPlayer(), context.getHand(), context.getClickedPos(), kind);
        return super.useOn(context);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return DeathAbilities.useMob(player, target, hand, kind);
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (kind == Kind.BOULDER && attacker instanceof Player player) DeathAbilities.useMob(player, target, InteractionHand.MAIN_HAND, kind);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        co.blumeplugins.deathsareitems.item.TooltipText.append(lines, "tooltip.deathsareitems." + kind.name().toLowerCase(java.util.Locale.ROOT));
    }
}
