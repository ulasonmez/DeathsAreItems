package co.blumeplugins.deathsareitems.command;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import co.blumeplugins.deathsareitems.reward.RewardManager;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class ModCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(DeathsAreItems.MOD_ID).requires(source -> source.hasPermission(2))
                .then(Commands.literal("preview").executes(context -> {
                    RewardManager.offer(context.getSource().getPlayerOrException(), DeathRewards.get(DeathRewards.FALL));
                    return 1;
                })));
    }
}
