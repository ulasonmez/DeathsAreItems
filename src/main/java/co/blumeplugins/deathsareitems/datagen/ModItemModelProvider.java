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
        for (String item : new String[]{"fall_damage_boots", "drowning_mask", "suffocation_boulder", "skeleton_bow", "mob_pickaxe"}) {
            withExistingParent(item, modLoc("item/" + item + "_base"));
        }
        placeholder("hunger_staff", "blaze_rod", true);
        withExistingParent("swarm_of_anvils", mcLoc("block/anvil"));
        placeholder("entity_crammer", "slime_ball", false);
        placeholder("cactus_chestplate", "iron_chestplate", false);
        placeholder("fire_pants", "iron_leggings", false);
        placeholder("tnt_cannon", "crossbow_standby", true);
        placeholder("ender_pearl_shield", "ender_pearl", false);
        withExistingParent("bedrock", mcLoc("item/red_bed"));
        withExistingParent("soul_storm", modLoc("item/soul_storm_base"));
    }

    private void placeholder(String name, String texture, boolean handheld) {
        withExistingParent(name, mcLoc(handheld ? "item/handheld" : "item/generated")).texture("layer0", mcLoc("item/" + texture));
    }

    // private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
    //     return withExistingParent(item.getId().getPath(),
    //             new ResourceLocation("item/generated"))
    //             .texture("layer0", new ResourceLocation(DeathsAreItems.MOD_ID, "item/" + item.getId().getPath()));
    // }
}
