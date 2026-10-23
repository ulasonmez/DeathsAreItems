package co.blumeplugins.deathsareitems.datagen;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, DeathsAreItems.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (String item : new String[]{"fall_damage_boots", "drowning_mask", "suffocation_boulder", "skeleton_bow", "mob_pickaxe",
                "hunger_staff", "swarm_of_anvils", "entity_crammer", "cactus_chestplate", "tnt_cannon",
                "fire_pants", "bedrock", "ender_pearl_shield", "soul_storm"}) {
            withExistingParent(item, modLoc("item/" + item + "_base"));
        }
    }

    // private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
    //     return withExistingParent(item.getId().getPath(),
    //             new ResourceLocation("item/generated"))
    //             .texture("layer0", new ResourceLocation(DeathsAreItems.MOD_ID, "item/" + item.getId().getPath()));
    // }
}
