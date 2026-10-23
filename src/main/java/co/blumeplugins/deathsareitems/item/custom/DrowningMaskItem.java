package co.blumeplugins.deathsareitems.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class DrowningMaskItem extends ArmorItem {
    public DrowningMaskItem() { super(ArmorMaterials.TURTLE, Type.HELMET, new Properties().stacksTo(1).fireResistant()); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.COMMON; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
                                           net.minecraft.world.entity.EquipmentSlot slot, String type) {
        return co.blumeplugins.deathsareitems.DeathsAreItems.MOD_ID + ":textures/models/armor/drowning_mask.png";
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new co.blumeplugins.deathsareitems.client.ReferenceItemExtensions(
                co.blumeplugins.deathsareitems.client.ReferenceItemRenderer.Kind.MASK));
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        co.blumeplugins.deathsareitems.item.TooltipText.append(lines, "tooltip.deathsareitems.drowning_mask");
    }
}
