package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.FallBootsAbility;
import co.blumeplugins.deathsareitems.ability.CushionCycle;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.custom.LaunchedBlockEntity;
import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.DeathRewards;
import co.blumeplugins.deathsareitems.reward.RewardManager;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import co.blumeplugins.deathsareitems.reward.RewardTimings;
import co.blumeplugins.deathsareitems.reward.SettledRewardChests;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@GameTestHolder(DeathsAreItems.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeathRewardsGameTests {
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "reward-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 2, 5))));
        return player;
    }

    @GameTest(template = "test_arena")
    public static void bootsPreventRealFallDamage(GameTestHelper helper) {
        Zombie protectedMob = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        protectedMob.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get()));
        float health = protectedMob.getHealth();
        protectedMob.causeFallDamage(30, 1, protectedMob.damageSources().fall());
        helper.assertTrue(protectedMob.getHealth() == health, "Equipped boots must cancel landing damage");
        protectedMob.hurt(protectedMob.damageSources().fall(), 12);
        helper.assertTrue(protectedMob.getHealth() == health, "Direct fall damage must also be cancelled");
        Zombie control = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 3));
        control.causeFallDamage(30, 1, control.damageSources().fall());
        helper.assertTrue(control.getHealth() < health, "Control mob must take fall damage");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void cushionsDoNotOverwriteExistingBlocks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get()));
        helper.setBlock(5, 2, 5, Blocks.DIAMOND_BLOCK);
        helper.setBlock(4, 2, 5, Blocks.CHEST);
        int placed = FallBootsAbility.placeCushion(player, helper.absolutePos(new BlockPos(5, 2, 5)));
        helper.assertTrue(placed == 7, "Only the seven air cells may receive cushions");
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(5, 2, 5));
        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(4, 2, 5));
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void terrainLaunchProtectsContainersAndBedrock(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(3, 2, 3, Blocks.BEDROCK);
        helper.setBlock(4, 2, 3, Blocks.BARREL);
        helper.setBlock(5, 2, 3, Blocks.STONE);
        helper.assertTrue(!FallBootsAbility.canLaunch(player, helper.absolutePos(new BlockPos(3, 2, 3))), "Bedrock cannot launch");
        helper.assertTrue(!FallBootsAbility.canLaunch(player, helper.absolutePos(new BlockPos(4, 2, 3))), "Container contents must survive");
        helper.assertTrue(FallBootsAbility.canLaunch(player, helper.absolutePos(new BlockPos(5, 2, 3))), "Ordinary stone can launch");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void pendingRewardsSurviveSerialization(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        RewardSavedData before = new RewardSavedData();
        RewardSavedData.Offer offer = new RewardSavedData.Offer(owner, DeathRewards.FALL);
        offer.dimension = helper.getLevel().dimension().location();
        offer.placedChests.add(new RewardSavedData.PlacedChest(new BlockPos(3, 2, 3), Blocks.GRASS.defaultBlockState()));
        before.offers.put(owner, offer);
        before.grants.add(new RewardSavedData.Grant(owner, new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get()), 12345));
        RewardSavedData after = RewardSavedData.load(before.save(new CompoundTag()));
        helper.assertTrue(after.offers.containsKey(owner), "Pending presentation must survive restart");
        helper.assertTrue(!after.offers.get(owner).spawned, "Presentation must receive a fresh generation on restart");
        helper.assertTrue(after.offers.get(owner).generation.equals(offer.generation)
                        && after.offers.get(owner).placedChests.get(0).original().is(Blocks.GRASS),
                "Old chest markers and original terrain must survive until cleanup");
        helper.assertTrue(after.grants.size() == 1 && after.grants.get(0).owner().equals(owner)
                && after.grants.get(0).stack().is(ModItems.FALL_DAMAGE_BOOTS.get()) && after.grants.get(0).due() == 12345,
                "Chosen item and reveal deadline must survive restart");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void chestClaimsValidateOwnershipTimingAndDuplicates(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer stranger = player(helper);
        RewardSavedData data = RewardSavedData.get(helper.getLevel().getServer());
        try (Presentation presentation = presentation(helper, owner)) {
            BlockPos boots = presentation.chests.get(0).blockPosition();
            helper.assertTrue(!RewardManager.selectBlock(stranger, boots), "Another player must not claim the reward");
            helper.assertTrue(!RewardManager.selectBlock(owner, boots), "Animation must finish before selection");
            presentation.offer.spawnedAt -= RewardTimings.READY;
            for (int i = 1; i < 3; i++) {
                helper.assertTrue(!RewardManager.selectBlock(owner, presentation.chests.get(i).blockPosition()), "Preview choices must never award an item");
                helper.assertTrue(data.offers.containsKey(owner.getUUID()) && presentation.offer.placedChests.size() == 3,
                        "Preview interaction must leave all three choices intact");
            }
            helper.assertTrue(RewardManager.selectBlock(owner, boots), "Owner can select the boots chest");
            helper.assertTrue(!RewardManager.selectBlock(owner, boots), "Repeated clicks must not duplicate the reward");
            List<RewardSavedData.Grant> grants = data.grants.stream().filter(g -> g.owner().equals(owner.getUUID())).toList();
            helper.assertTrue(grants.size() == 1 && grants.get(0).due() > helper.getLevel().getGameTime(),
                    "Exactly one item must be queued after the totem animation");
            for (RewardChestEntity chest : presentation.chests) {
                helper.assertTrue(chest.isRemoved() && !helper.getLevel().getBlockState(chest.blockPosition()).is(Blocks.CHEST),
                        "All three vanilla chests and their labels must disappear after selecting");
            }
            helper.succeed();
        }
    }

    @GameTest(template = "test_arena")
    public static void threeChestsFitAroundPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<Vec3> positions = RewardManager.findPositions(player);
        helper.assertTrue(positions.size() == 3, "Open terrain must fit all three chests");
        for (Vec3 position : positions) helper.assertTrue(Math.abs(position.distanceTo(player.position()) - 3) < .1,
                "Chests should start three blocks from the respawned player");
        try (Presentation presentation = presentation(helper, player)) {
            for (RewardChestEntity chest : presentation.chests) {
                var state = helper.getLevel().getBlockState(chest.blockPosition());
                Direction expectedFacing = Direction.getNearest(player.getX() - chest.getX(), 0, player.getZ() - chest.getZ());
                helper.assertTrue(state.is(Blocks.CHEST) && state.getValue(ChestBlock.TYPE) == ChestType.SINGLE,
                        "Settled chests must be actual single vanilla chest blocks");
                helper.assertTrue(state.getValue(ChestBlock.FACING) == expectedFacing, "Each chest must face the center");
                helper.assertTrue(chest.isSettled() && !chest.isPickable(), "Settled labels must not intercept block clicks");
            }
            helper.assertTrue(presentation.chests.stream().map(RewardChestEntity::label).distinct().count() == 3,
                    "The three reward labels must be different");
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void unsupportedAndRepeatedDeathsDoNotCreateOffers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        RewardSavedData data = RewardSavedData.get(helper.getLevel().getServer());
        try {
            for (DamageSource source : List.of(player.damageSources().generic(), player.damageSources().lava(),
                    player.damageSources().inFire(), player.damageSources().flyIntoWall(),
                    player.damageSources().magic(), player.damageSources().stalagmite())) {
                RewardManager.offer(player, DeathRewards.get(DeathRewards.FALL));
                helper.assertTrue(DeathRewards.forDeath(source).isEmpty(), "Other damage must not fall back to boots");
                RewardManager.onDeath(new LivingDeathEvent(player, source));
                helper.assertTrue(!data.offers.containsKey(player.getUUID()), "Other deaths must clear any stale offer");
            }
            data.discovered.remove(player.getUUID());
            RewardManager.onDeath(new LivingDeathEvent(player, player.damageSources().fall()));
            helper.assertTrue(data.offers.containsKey(player.getUUID()), "First fall death creates an offer");
            RewardManager.onDeath(new LivingDeathEvent(player, player.damageSources().fall()));
            helper.assertTrue(!data.offers.containsKey(player.getUUID()) && data.rejected.contains(player.getUUID()), "Repeated death creates only a rejection notice");
            helper.succeed();
        } finally {
            data.offers.remove(player.getUUID());
            data.discovered.remove(player.getUUID()); data.rejected.remove(player.getUUID());
            data.setDirty();
        }
    }

    @GameTest(template = "test_arena")
    public static void cushionsCycleOnTheBootsAndSurviveSaving(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos center = helper.absolutePos(new BlockPos(5, 2, 5));
        player.setPos(Vec3.atBottomCenterOf(center.above(3)));
        ItemStack boots = new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get());
        player.setItemSlot(EquipmentSlot.FEET, boots);
        Block[] expected = {Blocks.WATER, Blocks.SLIME_BLOCK, Blocks.HAY_BLOCK, Blocks.WATER};
        for (int step = 0; step < expected.length; step++) {
            helper.assertTrue(CushionCycle.next(boots) == expected[step], "The cycle must be water, slime, hay, water");
            helper.assertTrue(FallBootsAbility.placeCushion(player, center) == 9, "Each empty 3x3 landing must receive nine cushions");
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                helper.assertTrue(helper.getLevel().getBlockState(center.offset(x, 0, z)).is(expected[step]), "All nine cells must use the same cycle step");
                helper.getLevel().setBlock(center.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 3);
            }
            boots = ItemStack.of(boots.save(new CompoundTag()));
            player.setItemSlot(EquipmentSlot.FEET, boots);
        }
        helper.assertTrue(CushionCycle.next(new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get())) == Blocks.WATER,
                "Another pair of boots must have an independent sequence");
        helper.assertTrue(CushionCycle.index(boots) == 1, "Saved boots must retain the next slime step");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void landingSoundsUseTheActualBlock(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(5, 1, 5));
        for (Block block : List.of(Blocks.HAY_BLOCK, Blocks.SLIME_BLOCK, Blocks.STONE)) {
            var state = block.defaultBlockState();
            helper.assertTrue(FallBootsAbility.landingSound(state, helper.getLevel(), pos, player)
                            == state.getSoundType(helper.getLevel(), pos, player).getFallSound(),
                    "Landing audio must come from the landed block's sound type");
        }
        helper.assertTrue(FallBootsAbility.landingSound(Blocks.HAY_BLOCK.defaultBlockState(), helper.getLevel(), pos, player) != SoundEvents.SLIME_BLOCK_FALL,
                "Hay must never play the slime fall sound");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void savedVanillaChestsCanBeCleanedUpAfterRestart(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try (Presentation presentation = presentation(helper, player)) {
            RewardSavedData data = RewardSavedData.get(helper.getLevel().getServer());
            RewardSavedData reloaded = RewardSavedData.load(data.save(new CompoundTag()));
            RewardSavedData.Offer savedOffer = reloaded.offers.get(player.getUUID());
            helper.assertTrue(savedOffer.placedChests.size() == 3, "All placed block positions must survive saving");
            SettledRewardChests.restore(helper.getLevel().getServer(), savedOffer);
            for (RewardChestEntity chest : presentation.chests) {
                helper.assertTrue(helper.getLevel().getBlockState(chest.blockPosition()).isAir(), "Restart cleanup must restore the original terrain");
            }
        }
        helper.succeed();
    }

    private static Presentation presentation(GameTestHelper helper, ServerPlayer player) {
        RewardSavedData data = RewardSavedData.get(helper.getLevel().getServer());
        RewardSavedData.Offer offer = new RewardSavedData.Offer(player.getUUID(), DeathRewards.FALL);
        offer.dimension = helper.getLevel().dimension().location();
        offer.spawnedAt = helper.getLevel().getGameTime();
        offer.spawned = true;
        data.offers.put(player.getUUID(), offer);
        Presentation presentation = new Presentation(helper, player, offer, new ArrayList<>());
        try {
            List<Vec3> positions = RewardManager.findPositions(player);
            helper.assertTrue(positions.size() == 3, "Test arena must fit three chest blocks");
            for (int i = 0; i < 3; i++) {
                RewardChestEntity chest = ModEntities.REWARD_CHEST.get().create(helper.getLevel());
                chest.setPos(positions.get(i));
                chest.setYRot(-Direction.getNearest(player.getX() - chest.getX(), 0, player.getZ() - chest.getZ()).toYRot());
                chest.configure(player.getUUID(), offer.generation, i, offer.spawnedAt, DeathRewards.get(DeathRewards.FALL).options().get(i).titleKey());
                helper.getLevel().addFreshEntity(chest);
                presentation.chests.add(chest);
                offer.chests.add(chest.getUUID());
                helper.assertTrue(SettledRewardChests.place(chest, offer), "Animated chest must settle into a vanilla block");
            }
            return presentation;
        } catch (RuntimeException | Error failure) {
            presentation.close();
            throw failure;
        }
    }

    private record Presentation(GameTestHelper helper, ServerPlayer player, RewardSavedData.Offer offer,
                                List<RewardChestEntity> chests) implements AutoCloseable {
        @Override public void close() {
            SettledRewardChests.restore(helper.getLevel().getServer(), offer);
            chests.forEach(RewardChestEntity::discard);
            RewardSavedData data = RewardSavedData.get(helper.getLevel().getServer());
            data.offers.remove(player.getUUID());
            data.grants.removeIf(grant -> grant.owner().equals(player.getUUID()));
            data.setDirty();
        }
    }

    @GameTest(template = "test_arena", timeoutTicks = 140)
    public static void launchedBlocksDieWithoutPlacingTerrain(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 3, 5));
        LaunchedBlockEntity debris = LaunchedBlockEntity.create(helper.getLevel(), pos, Blocks.DIRT.defaultBlockState(), 0, .6, 0);
        helper.getLevel().addFreshEntity(debris);
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(debris.isRemoved(), "Debris must die on landing");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 2, 5));
            helper.succeed();
        });
    }
}
