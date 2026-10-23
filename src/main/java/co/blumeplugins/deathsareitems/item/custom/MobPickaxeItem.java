package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.AbilityMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class MobPickaxeItem extends PickaxeItem {
    public static final String ENABLED = "deathsareitems.Morphing";
    public MobPickaxeItem() { super(Tiers.NETHERITE, 11, -2.4F, new Properties().fireResistant().rarity(Rarity.EPIC)); }
    @Override public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) ? Tiers.IRON.getSpeed() : 1F;
    }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new co.blumeplugins.deathsareitems.client.ReferenceItemExtensions(
                co.blumeplugins.deathsareitems.client.ReferenceItemRenderer.Kind.MOB_PICKAXE));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            boolean enabled = !stack.getOrCreateTag().getBoolean(ENABLED);
            stack.getOrCreateTag().putBoolean(ENABLED, enabled);
            AbilityMessages.show(server, Component.literal("MOB MORPHING TIME " + (enabled ? "ENABLED" : "DISABLED"))
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), 60);
            level.playSound(null, player.blockPosition(), SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, .7F, enabled ? .8F : .65F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return true; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        co.blumeplugins.deathsareitems.item.TooltipText.append(lines, "tooltip.deathsareitems.mob_pickaxe");
    }
}
