package co.blumeplugins.deathsareitems.item.custom;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.item.TooltipText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
public final class GustafJukeboxItem extends BlockItem {
    public GustafJukeboxItem() { super(ModBlocks.GUSTAFS_JUKEBOX.get(), new Properties()); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        TooltipText.append(lines, "tooltip.deathsareitems.gustafs_jukebox");
    }
}
