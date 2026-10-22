package co.blumeplugins.deathsareitems.item.custom;

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
        co.blumeplugins.deathsareitems.item.TooltipText.append(lines, "tooltip.deathsareitems.fall_damage_boots");
    }
}
