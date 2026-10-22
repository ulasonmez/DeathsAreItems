package co.blumeplugins.deathsareitems.datagen;

import co.blumeplugins.deathsareitems.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

public class ModLootTableProvider {
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootTables::new, net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK)
        ));
    }

    // Example block loot subprovider — drops the block itself.
    public static class ModBlockLootTables extends BlockLootSubProvider {
        protected ModBlockLootTables() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            add(ModBlocks.BEDROCK.get(), createSinglePropConditionTable(ModBlocks.BEDROCK.get(),
                    net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(e -> (Block) e.get())::iterator;
        }
    }
}
