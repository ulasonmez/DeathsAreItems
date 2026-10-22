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
    public static final RegistryObject<Item> SKELETON_BOW = ITEMS.register("skeleton_bow", () -> new DeathToolItem(DeathToolItem.Kind.BOW));
    public static final RegistryObject<Item> MOB_PICKAXE = ITEMS.register("mob_pickaxe", MobPickaxeItem::new);
    public static final RegistryObject<Item> HUNGER_STAFF = ITEMS.register("hunger_staff", () -> new DeathToolItem(DeathToolItem.Kind.STAFF));
    public static final RegistryObject<Item> SWARM_OF_ANVILS = ITEMS.register("swarm_of_anvils", () -> new DeathToolItem(DeathToolItem.Kind.ANVILS));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
