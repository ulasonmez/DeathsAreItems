package co.blumeplugins.deathsareitems.command;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.EndHardcore;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import co.blumeplugins.deathsareitems.reward.RewardManager;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.Collections;

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
                .then(Commands.literal("revive")
                        .executes(c -> revive(Collections.singleton(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> revive(EntityArgument.getPlayers(c, "targets")))))
                .then(Commands.literal("hardcore")
                        .then(Commands.literal("reset")
                                .executes(c -> revive(Collections.singleton(c.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(c -> revive(EntityArgument.getPlayers(c, "targets"))))))
                .then(Commands.literal("reset")
                        .executes(c -> reset(c.getSource(), "all"))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(
                                        java.util.stream.Stream.of("all", "hardcore"),
                                        DeathRewards.ids().stream().map(ResourceLocation::getPath)), b))
                                .executes(c -> reset(c.getSource(), StringArgumentType.getString(c, "type"))))));
    }

    private static int revive(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            EndHardcore.reset(player);
        }
        return players.size();
    }

    private static int reset(CommandSourceStack source, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (type.equals("hardcore")) {
            return revive(Collections.singleton(player));
        }
        RewardSavedData data = RewardSavedData.get(player.server);
        if (type.equals("all")) {
            data.resetDiscoveries(player.getUUID());
            return 1;
        }
        ResourceLocation id = ResourceLocation.tryParse(DeathsAreItems.MOD_ID + ":" + type);
        if (id == null || DeathRewards.get(id) == null) {
            return 0;
        }
        data.resetDiscovery(player.getUUID(), id);
        return 1;
    }

    private static int kill(ServerPlayer p,String type) {
        DamageSource source=switch(type) {
            case "wither" -> p.damageSources().wither();
            case "cram" -> p.damageSources().cramming();
            case "cactus" -> p.damageSources().cactus();
            case "lava" -> p.damageSources().lava();
            case "bed" -> p.damageSources().badRespawnPointExplosion(p.position());
            case "tnt" -> p.damageSources().explosion(new net.minecraft.world.entity.item.PrimedTnt(p.level(),p.getX(),p.getY(),p.getZ(),p),p);
            case "pearl" -> co.blumeplugins.deathsareitems.ability.DeathDamage.source(p,co.blumeplugins.deathsareitems.ability.DeathDamage.PEARL,null);
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
