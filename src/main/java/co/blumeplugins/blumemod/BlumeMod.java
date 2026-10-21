package co.blumeplugins.blumemod;

import co.blumeplugins.blumemod.block.ModBlocks;
import co.blumeplugins.blumemod.block.entity.ModBlockEntities;
import co.blumeplugins.blumemod.entity.ModEntities;
import co.blumeplugins.blumemod.item.ModCreativeModTabs;
import co.blumeplugins.blumemod.item.ModItems;
import co.blumeplugins.blumemod.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BlumeMod.MOD_ID)
public class BlumeMod {
    public static final String MOD_ID = "blumemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlumeMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register all DeferredRegisters to the mod event bus
        ModCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            co.blumeplugins.util.internal.GlobalConfigManager.syncProfile(player);
        }
    }
}
