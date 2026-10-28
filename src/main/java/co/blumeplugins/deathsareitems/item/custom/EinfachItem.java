package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public class EinfachItem extends Item {
    public EinfachItem(Properties properties) { super(properties); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, getDescriptionId() + ".desc");
    }
}
