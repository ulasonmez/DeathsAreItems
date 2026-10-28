package co.blumeplugins.deathsareitems.item;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.custom.FallDamageBootsItem;
import co.blumeplugins.deathsareitems.item.custom.DrowningMaskItem;
import co.blumeplugins.deathsareitems.item.custom.DeathToolItem;
import co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DeathsAreItems.MOD_ID);

    public static final RegistryObject<Item> FALL_DAMAGE_BOOTS = ITEMS.register("fall_damage_boots",
            FallDamageBootsItem::new);
    public static final RegistryObject<Item> DROWNING_MASK = ITEMS.register("drowning_mask", DrowningMaskItem::new);
    public static final RegistryObject<Item> SUFFOCATION_BOULDER = ITEMS.register("suffocation_boulder", () -> new DeathToolItem(DeathToolItem.Kind.BOULDER));
    public static final RegistryObject<Item> SKELETON_BOW = ITEMS.register("skeleton_bow", co.blumeplugins.deathsareitems.item.custom.SkeletonBowItem::new);
    public static final RegistryObject<Item> MOB_PICKAXE = ITEMS.register("mob_pickaxe", MobPickaxeItem::new);
    public static final RegistryObject<Item> HUNGER_STAFF = ITEMS.register("hunger_staff", () -> new DeathToolItem(DeathToolItem.Kind.STAFF));
    public static final RegistryObject<Item> SWARM_OF_ANVILS = ITEMS.register("swarm_of_anvils", co.blumeplugins.deathsareitems.item.custom.SwarmOfAnvilsItem::new);
    public static final RegistryObject<Item> SOUL_STORM = ITEMS.register("soul_storm", co.blumeplugins.deathsareitems.item.custom.SoulStormItem::new);

    public static final RegistryObject<Item> ENTITY_CRAMMER = ITEMS.register("entity_crammer",
            co.blumeplugins.deathsareitems.item.custom.EntityCrammerItem::new);
    public static final RegistryObject<Item> TNT_CANNON = ITEMS.register("tnt_cannon", () ->
            new co.blumeplugins.deathsareitems.item.custom.ChaosToolItem(co.blumeplugins.deathsareitems.item.custom.ChaosToolItem.Kind.CANNON));
    public static final RegistryObject<Item> ENDER_PEARL_SHIELD = ITEMS.register("ender_pearl_shield", () ->
            new co.blumeplugins.deathsareitems.item.custom.ChaosToolItem(co.blumeplugins.deathsareitems.item.custom.ChaosToolItem.Kind.SHIELD));
    public static final RegistryObject<Item> CACTUS_CHESTPLATE = ITEMS.register("cactus_chestplate", () ->
            new co.blumeplugins.deathsareitems.item.custom.ChaosArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, "cactus_chestplate"));
    public static final RegistryObject<Item> FIRE_PANTS = ITEMS.register("fire_pants", () ->
            new co.blumeplugins.deathsareitems.item.custom.ChaosArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, "fire_pants"));
    public static final RegistryObject<Item> BEDROCK = ITEMS.register("bedrock", () ->
            new co.blumeplugins.deathsareitems.item.custom.BedrockItem(co.blumeplugins.deathsareitems.block.ModBlocks.BEDROCK.get()));

    public static final RegistryObject<Item> BIRCH_SPLINTERS = ITEMS.register("birch_splinters", () -> new co.blumeplugins.deathsareitems.item.custom.EinfachItem(new Item.Properties()));
    public static final RegistryObject<Item> VINYS_BUCKET = ITEMS.register("vinys_bucket", () -> new co.blumeplugins.deathsareitems.item.custom.CompanionItem(true));
    public static final RegistryObject<Item> JUKYS_WHISTLE = ITEMS.register("jukys_whistle", () -> new co.blumeplugins.deathsareitems.item.custom.CompanionItem(false));
    public static final RegistryObject<Item> CHEESE_SLICE = ITEMS.register("cheese_slice", () -> new co.blumeplugins.deathsareitems.item.custom.GustafFoodItem(false));
    public static final RegistryObject<Item> HAMBURGER = ITEMS.register("hamburger", () -> new co.blumeplugins.deathsareitems.item.custom.GustafFoodItem(true));
    public static final RegistryObject<Item> GUSTAFS_JUKEBOX = ITEMS.register("gustafs_jukebox", co.blumeplugins.deathsareitems.item.custom.GustafJukeboxItem::new);
    public static final RegistryObject<Item> GUSTAFS_NEW_BOOK = ITEMS.register("gustafs_new_book", co.blumeplugins.deathsareitems.item.custom.GustafBookItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
