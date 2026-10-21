package co.blumeplugins.blumemod.datagen;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, BlumeMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Example: generate a simple cube_all blockstate + model.
        // blockWithItem(ModBlocks.EXAMPLE_BLOCK);
    }

    // private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
    //     simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    // }
}
