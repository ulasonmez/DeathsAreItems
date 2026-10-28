package co.blumeplugins.deathsareitems.datagen;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagGenerator extends BlockTagsProvider {
    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, DeathsAreItems.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE).add(co.blumeplugins.deathsareitems.block.ModBlocks.GUSTAFS_JUKEBOX.get());
        // Example: tag a block as needing a pickaxe.
        // tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.EXAMPLE_BLOCK.get());
    }
}
