package co.blumeplugins.blumemod.datagen;

import co.blumeplugins.blumemod.BlumeMod;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, BlumeMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Example: generate a simple item/generated model for the example item.
        // simpleItem(ModItems.EXAMPLE_ITEM);
    }

    // private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
    //     return withExistingParent(item.getId().getPath(),
    //             new ResourceLocation("item/generated"))
    //             .texture("layer0", new ResourceLocation(TemplateForgeMod.MOD_ID, "item/" + item.getId().getPath()));
    // }
}
