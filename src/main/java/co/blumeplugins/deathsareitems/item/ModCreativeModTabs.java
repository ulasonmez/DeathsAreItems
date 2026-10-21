package co.blumeplugins.deathsareitems.item;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DeathsAreItems.MOD_ID);

    // Example creative tab. Add new items/blocks inside the displayItems consumer.
    public static final RegistryObject<CreativeModeTab> DEATHSAREITEMS_TAB = CREATIVE_MODE_TABS.register("deathsareitems_tab",
            () -> CreativeModeTab.builder()
                    // .icon(() -> ModItems.EXAMPLE_ITEM.get().getDefaultInstance())
                    .icon(() -> net.minecraft.world.item.Items.APPLE.getDefaultInstance())
                    .title(Component.translatable("creativetab.deathsareitems_tab"))
                    .displayItems((parameters, output) -> {
                        // output.accept(ModItems.EXAMPLE_ITEM.get());
                        // output.accept(ModBlocks.EXAMPLE_BLOCK.get().asItem());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
