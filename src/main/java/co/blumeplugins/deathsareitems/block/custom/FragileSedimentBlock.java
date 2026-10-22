package co.blumeplugins.deathsareitems.block.custom;

import net.minecraft.world.level.block.Block;

/** Stable ability sediment; only deliberate mining breaks the cage. */
public final class FragileSedimentBlock extends Block {
    public FragileSedimentBlock(Properties properties) { super(properties.strength(0F).noLootTable()); }
}
