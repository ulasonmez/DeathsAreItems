package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.EndHardcore;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import co.blumeplugins.deathsareitems.reward.RewardManager;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(DeathsAreItems.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResetCommandGameTests {
    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "reset-test"));
    }

    private static int execute(ServerPlayer player, String command) throws CommandSyntaxException {
        return player.server.getCommands().getDispatcher().execute(command,
                player.createCommandSourceStack().withPermission(2).withSuppressedOutput());
    }

    private static void cleanup(ServerPlayer player) {
        RewardManager.clearOffer(player);
        RewardSavedData data = RewardSavedData.get(player.server);
        data.resetDiscoveries(player.getUUID());
        data.grants.removeIf(grant -> grant.owner().equals(player.getUUID()));
    }

    @GameTest(template = "test_arena")
    public static void specificResetReopensOnlyTheRequestedDeath(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = player(helper), other = player(helper);
        RewardSavedData data = RewardSavedData.get(player.server);
        try {
            for (var type : DeathRewards.ids()) {
                DeathRewards.ids().forEach(id -> data.discover(player.getUUID(), id));
                data.discover(other.getUUID(), type);
                data.rejected.add(player.getUUID());
                helper.assertTrue(execute(player, "deathsareitems reset " + type.getPath()) == 1, "Every death key must be accepted");
                helper.assertTrue(!data.hasDiscovered(player.getUUID(), type), "Requested death must be available again");
                helper.assertTrue(DeathRewards.ids().stream().filter(id -> !id.equals(type))
                        .allMatch(id -> data.hasDiscovered(player.getUUID(), id)), "Other discoveries must remain");
                helper.assertTrue(data.hasDiscovered(other.getUUID(), type), "Other players must keep their discoveries");
                helper.assertTrue(!data.rejected.contains(player.getUUID()), "Stale rejection must be removed");
                var reloaded = RewardSavedData.load(data.save(new CompoundTag()));
                helper.assertTrue(!reloaded.hasDiscovered(player.getUUID(), type), "Reset must survive saving");
            }
            RewardManager.onDeath(new LivingDeathEvent(player, player.damageSources().fall()));
            helper.assertTrue(!data.offers.containsKey(player.getUUID()), "Already discovered fall must be rejected");
            execute(player, "deathsareitems reset fall");
            RewardManager.onDeath(new LivingDeathEvent(player, player.damageSources().fall()));
            helper.assertTrue(data.offers.get(player.getUUID()).reward.equals(DeathRewards.FALL), "Fall after reset must offer boots again");
            RewardManager.onDeath(new LivingDeathEvent(player, player.damageSources().fall()));
            helper.assertTrue(!data.offers.containsKey(player.getUUID()), "A new discovery must still be available only once");
            helper.succeed();
        } finally {
            cleanup(player);
            cleanup(other);
        }
    }

    @GameTest(template = "test_arena")
    public static void fullResetAliasesPreserveEarnedRewardsAndEndRule(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = player(helper), other = player(helper);
        RewardSavedData data = RewardSavedData.get(player.server);
        try {
            EndHardcore.enter(player);
            data.discover(other.getUUID(), DeathRewards.FALL);
            RewardManager.offer(player, DeathRewards.get(DeathRewards.FALL));
            var offer = data.offers.get(player.getUUID());
            var grant = new RewardSavedData.Grant(player.getUUID(), new ItemStack(ModItems.DROWNING_MASK.get()), Long.MAX_VALUE);
            data.grants.add(grant);
            for (String command : List.of("deathsareitems reset all", "deathsareitems reset")) {
                DeathRewards.ids().forEach(id -> data.discover(player.getUUID(), id));
                data.rejected.add(player.getUUID());
                helper.assertTrue(execute(player, command) == 1, "Both full reset forms must succeed");
                helper.assertTrue(DeathRewards.ids().stream().noneMatch(id -> data.hasDiscovered(player.getUUID(), id)), "All own discoveries must reset");
                helper.assertTrue(!data.rejected.contains(player.getUUID()), "Rejection must reset");
                helper.assertTrue(data.hasDiscovered(other.getUUID(), DeathRewards.FALL), "Full reset applies only to the caller");
                helper.assertTrue(data.offers.get(player.getUUID()) == offer && data.grants.contains(grant), "Earned offers and grants must survive");
                helper.assertTrue(EndHardcore.active(player), "Discovery reset must preserve the End final-life rule");
                var reloaded = RewardSavedData.load(data.save(new CompoundTag()));
                helper.assertTrue(DeathRewards.ids().stream().noneMatch(id -> reloaded.hasDiscovered(player.getUUID(), id)), "Full reset must survive saving");
            }
            helper.succeed();
        } finally {
            cleanup(player);
            cleanup(other);
        }
    }

    @GameTest(template = "test_arena")
    public static void invalidAndUnauthorizedResetsKeepDiscoveries(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = player(helper);
        RewardSavedData data = RewardSavedData.get(player.server);
        try {
            data.discover(player.getUUID(), DeathRewards.FALL);
            data.rejected.add(player.getUUID());
            for (String type : List.of("unknown", "FALL")) {
                helper.assertTrue(execute(player, "deathsareitems reset " + type) == 0, "Invalid keys must fail cleanly");
                helper.assertTrue(data.hasDiscovered(player.getUUID(), DeathRewards.FALL) && data.rejected.contains(player.getUUID()), "Invalid reset must not change state");
            }
            boolean denied = false;
            try {
                player.server.getCommands().getDispatcher().execute("deathsareitems reset all",
                        player.createCommandSourceStack().withPermission(0).withSuppressedOutput());
            } catch (CommandSyntaxException expected) {
                denied = true;
            }
            helper.assertTrue(denied && data.hasDiscovered(player.getUUID(), DeathRewards.FALL), "Non-operators must not reset discoveries");
            helper.succeed();
        } finally {
            cleanup(player);
        }
    }
}
