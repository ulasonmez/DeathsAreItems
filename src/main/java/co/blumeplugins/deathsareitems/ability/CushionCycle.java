package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The next landing belongs to this individual pair of boots and is saved with its NBT. */
public final class CushionCycle {
    private static final String NEXT = "NextCushion";
    private static final Block[] ORDER = {Blocks.WATER, Blocks.SLIME_BLOCK, Blocks.HAY_BLOCK};

    private CushionCycle() {}

    public static int index(ItemStack boots) {
        CompoundTag data = boots.getTagElement(DeathsAreItems.MOD_ID);
        return data == null ? 0 : Math.floorMod(data.getInt(NEXT), ORDER.length);
    }

    public static Block next(ItemStack boots) { return ORDER[index(boots)]; }

    public static void advance(ItemStack boots) {
        boots.getOrCreateTagElement(DeathsAreItems.MOD_ID).putInt(NEXT, (index(boots) + 1) % ORDER.length);
    }
}
