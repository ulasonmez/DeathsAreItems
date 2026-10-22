package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class BedrockItem extends BedItem {
    public BedrockItem(Block block) { super(block, new Properties().stacksTo(1).fireResistant()); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, "tooltip.deathsareitems.bedrock");
    }
}
