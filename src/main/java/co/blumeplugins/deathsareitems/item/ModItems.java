package co.blumeplugins.deathsareitems.item;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.custom.FallDamageBootsItem;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
