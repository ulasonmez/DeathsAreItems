package co.blumeplugins.deathsareitems.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class DrowningMaskItem extends ArmorItem {
    public DrowningMaskItem() { super(ArmorMaterials.TURTLE, Type.HELMET, new Properties().stacksTo(1).fireResistant()); }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        co.blumeplugins.deathsareitems.item.TooltipText.append(lines, "tooltip.deathsareitems.drowning_mask");
    }
}
