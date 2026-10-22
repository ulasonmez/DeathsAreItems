package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ChaosArmorItem extends ArmorItem {
    private final String tooltip;
    public ChaosArmorItem(Type type, String tooltip) {
        super(ArmorMaterials.IRON, type, new Properties().stacksTo(1).fireResistant()); this.tooltip = tooltip;
    }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, "tooltip.deathsareitems." + tooltip);
    }
}
