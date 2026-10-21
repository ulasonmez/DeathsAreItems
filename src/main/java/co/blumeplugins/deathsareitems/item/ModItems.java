package co.blumeplugins.deathsareitems.item;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DeathsAreItems.MOD_ID);

    // Example item registration. Add more items below following the same pattern.
    // public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
    //         () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
