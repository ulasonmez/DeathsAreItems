package co.blumeplugins.deathsareitems.command;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import co.blumeplugins.deathsareitems.reward.RewardManager;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
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
                }).then(Commands.argument("type",StringArgumentType.word()).suggests((c,b) -> SharedSuggestionProvider.suggest(DeathRewards.ids().stream().map(ResourceLocation::getPath),b))
                        .executes(c -> {
                            var definition=DeathRewards.get(new ResourceLocation(DeathsAreItems.MOD_ID,StringArgumentType.getString(c,"type")));
                            if(definition == null) return 0;
                            RewardManager.offer(c.getSource().getPlayerOrException(),definition); return 1;
                        })))
                .then(Commands.literal("die").then(Commands.argument("type",StringArgumentType.word())
                        .suggests((c,b) -> SharedSuggestionProvider.suggest(DeathRewards.ids().stream().map(ResourceLocation::getPath),b))
                        .executes(c -> kill(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"type")))))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p=c.getSource().getPlayerOrException(); var data=RewardSavedData.get(p.server);
                    data.discovered.remove(p.getUUID()); data.rejected.remove(p.getUUID()); data.setDirty();
                    p.sendSystemMessage(Component.literal("Death discoveries reset.")); return 1;
                })));
    }

    private static int kill(ServerPlayer p,String type) {
        DamageSource source=switch(type) {
            case "fall" -> p.damageSources().fall(); case "drown" -> p.damageSources().drown();
            case "suffocate" -> p.damageSources().inWall(); case "starve" -> p.damageSources().starve();
            case "anvil" -> new DamageSource(p.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FALLING_ANVIL));
            case "golem" -> p.damageSources().mobAttack(EntityType.IRON_GOLEM.create(p.level()));
            case "skeleton" -> p.damageSources().arrow(EntityType.ARROW.create(p.level()),EntityType.SKELETON.create(p.level()));
            default -> null;
        };
        if(source == null) return 0;
        p.getCombatTracker().recordDamage(source, p.getHealth()); p.setHealth(0); p.die(source); return 1;
    }
}
