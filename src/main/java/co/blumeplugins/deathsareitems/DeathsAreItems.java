package co.blumeplugins.deathsareitems;

import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.block.entity.ModBlockEntities;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.item.ModCreativeModTabs;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.sound.ModSounds;
import co.blumeplugins.deathsareitems.config.ModConfig;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import co.blumeplugins.deathsareitems.particle.ModParticles;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import net.minecraftforge.fml.ModLoadingContext;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DeathsAreItems.MOD_ID)
public class DeathsAreItems {
    public static final String MOD_ID = "deathsareitems";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DeathsAreItems() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register all DeferredRegisters to the mod event bus
        ModCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        ModNetwork.register();
        modEventBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event
                .enqueueWork(DeathRewards::bootstrap));

        MinecraftForge.EVENT_BUS.register(this);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            // co.blumeplugins.util.internal.GlobalConfigManager.syncProfile(player);
        }
    }
}
