package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.CushionCycle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FallDamageBootsItem extends ArmorItem {
    public FallDamageBootsItem() {
        super(ArmorMaterials.GOLD, Type.BOOTS, new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.deathsareitems.fall_damage_boots").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.deathsareitems.fall_damage_boots.cushion").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.deathsareitems.fall_damage_boots.next_cushion", CushionCycle.next(stack).getName()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.deathsareitems.fall_damage_boots.sneak").withStyle(ChatFormatting.GOLD));
    }
}
