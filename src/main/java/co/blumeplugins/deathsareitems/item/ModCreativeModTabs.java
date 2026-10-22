package co.blumeplugins.deathsareitems.item;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeathsAreItems.MOD_ID);

    public static final RegistryObject<CreativeModeTab> DEATHSAREITEMS_TAB = CREATIVE_MODE_TABS.register("deathsareitems_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.FALL_DAMAGE_BOOTS.get().getDefaultInstance())
                    .title(Component.translatable("creativetab.deathsareitems_tab"))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
