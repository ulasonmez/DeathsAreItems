package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.block.entity.custom.GustafJukeboxBlockEntity;
import co.blumeplugins.deathsareitems.einfach.*;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.item.custom.GustafBookItem;
import co.blumeplugins.deathsareitems.reward.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("deathsareitems")
@PrefixGameTestTemplate(false)
public final class EinfachGameTests {
    private static ServerPlayer online(GameTestHelper h) { return ExpandedDeathGameTests.online(h); }
    private static void leave(ServerPlayer p) { RewardManager.clearOffer(p); p.server.getPlayerList().remove(p); }
    private static long count(ServerPlayer p, Item item) { return RewardSavedData.get(p.server).grants.stream().filter(g -> g.owner().equals(p.getUUID()) && g.stack().is(item)).mapToInt(g -> g.stack().getCount()).sum(); }

    @GameTest(template="test_arena") public static void bonusesAreDistinctPersistentAndDoNotReplaceBaseRewards(GameTestHelper h) {
        var p = online(h);
        try {
            for (var death : DeathRewards.ids()) EinfachRewards.claim(p, death, Long.MAX_VALUE);
            h.assertTrue(count(p, ModItems.VINYS_BUCKET.get()) == 1 && count(p, ModItems.JUKYS_WHISTLE.get()) == 1, "Each companion has one bonus");
            h.assertTrue(count(p, ModItems.CHEESE_SLICE.get()) == 8 && count(p, ModItems.HAMBURGER.get()) == 2, "Starvation grants food quantities");
            h.assertTrue(count(p, ModItems.GUSTAFS_JUKEBOX.get()) == 1 && count(p, Items.MUSIC_DISC_CAT) == 1 && count(p, ModItems.GUSTAFS_NEW_BOOK.get()) == 1, "Milestones grant jukebox, disc and book once");
            int before = RewardSavedData.get(p.server).grants.size();
            for (var death : DeathRewards.ids()) EinfachRewards.claim(p, death, Long.MAX_VALUE);
            h.assertTrue(before == RewardSavedData.get(p.server).grants.size(), "Repeated claims cannot farm bonuses");
            var saved = EinfachSavedData.get(p.server).save(new CompoundTag());
            var loaded = EinfachSavedData.load(saved).player(p.getUUID());
            h.assertTrue(loaded.getCompound("Claimed").getAllKeys().size() == 14 && loaded.getBoolean("BookGranted"), "Claim history survives save and load");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void previewOffersNeverBecomeNaturalAndResetsKeepBonusHistory(GameTestHelper h) {
        var p = online(h);
        try {
            RewardManager.offer(p, DeathRewards.get(DeathRewards.FALL));
            var rewards = RewardSavedData.get(p.server);
            h.assertTrue(!rewards.offers.get(p.getUUID()).natural, "Preview-created offers never count");
            RewardManager.clearOffer(p);
            RewardManager.onDeath(new LivingDeathEvent(p, p.damageSources().fall()));
            h.assertTrue(rewards.offers.get(p.getUUID()).natural, "An actual newly discovered death creates an eligible offer");
            h.assertTrue(RewardSavedData.load(rewards.save(new CompoundTag())).offers.get(p.getUUID()).natural, "Offer eligibility survives a restart");
            EinfachRewards.claim(p, DeathRewards.FALL, Long.MAX_VALUE); rewards.resetDiscoveries(p.getUUID());
            h.assertTrue(EinfachSavedData.get(p.server).player(p.getUUID()).getCompound("Claimed").getBoolean("fall"), "Discovery resets cannot farm milestone rewards");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void cheeseAndHamburgersUseCappedReserveAndSharedHealing(GameTestHelper h) {
        var p = online(h);
        try {
            p.setHealth(10); p.getFoodData().setFoodLevel(19); p.getPersistentData().putInt(HungerAbility.RESERVE, 98);
            ModItems.CHEESE_SLICE.get().finishUsingItem(new ItemStack(ModItems.CHEESE_SLICE.get()), p.level(), p);
            h.assertTrue(p.getFoodData().getFoodLevel() == 20 && p.getPersistentData().getInt(HungerAbility.RESERVE) == 100, "Cheese fills hunger first and caps overflow");
            h.assertTrue(p.getHealth() == 12, "Cheese heals one heart");
            ModItems.CHEESE_SLICE.get().finishUsingItem(new ItemStack(ModItems.CHEESE_SLICE.get()), p.level(), p);
            h.assertTrue(p.getHealth() == 12, "A second stack cannot bypass the shared heal cooldown");
            p.getPersistentData().putInt(HungerAbility.RESERVE, 0);
            ModItems.HAMBURGER.get().finishUsingItem(new ItemStack(ModItems.HAMBURGER.get()), p.level(), p);
            var effect = p.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            h.assertTrue(p.getPersistentData().getInt(HungerAbility.RESERVE) == 12 && effect != null && effect.getAmplifier() == 1 && effect.getDuration() == 100, "Burger supplies reserve and five seconds of regeneration II");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena", timeoutTicks=240) public static void furnaceMakesFourCheeseAndReturnsBucket(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlock(pos, Blocks.FURNACE.defaultBlockState(), 3);
        var furnace = (FurnaceBlockEntity)h.getLevel().getBlockEntity(pos);
        furnace.setItem(0, new ItemStack(Items.MILK_BUCKET)); furnace.setItem(1, new ItemStack(Items.COAL));
        h.runAfterDelay(210, () -> {
            h.assertTrue(furnace.getItem(2).is(ModItems.CHEESE_SLICE.get()) && furnace.getItem(2).getCount() == 4, "One milk bucket cooks into four slices");
            h.assertTrue(furnace.getItem(0).is(Items.BUCKET), "The milk container is returned to the input slot"); h.succeed();
        });
    }
    @GameTest(template="test_arena") public static void birchOnlyDropsSplintersAndCannotUnlockCactus(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2,2,2));
        for (var block : List.of(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)) {
            var drops = Block.getDrops(block.defaultBlockState(), h.getLevel(), pos, null);
            h.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.BIRCH_SPLINTERS.get()), "All birch logs drop only splinters");
        }
        h.assertTrue(Block.getDrops(Blocks.OAK_LOG.defaultBlockState(), h.getLevel(), pos, null).get(0).is(Items.OAK_LOG), "Other logs retain normal loot");
        var p = online(h);
        try {
            h.assertTrue(DeathRewards.forDeath(DeathDamage.source(p, DeathDamage.SPLINTER, null)).isEmpty(), "Splinter deaths cannot unlock an unrelated death reward"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena", timeoutTicks=90) public static void buttonTntIsUniqueAndPreservesTerrain(GameTestHelper h) {
        var p = online(h); var pos = h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(pos, Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR), 3);
        var click = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, pos, new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
        EinfachHazards.button(click); EinfachHazards.button(click);
        var tnts = h.getLevel().getEntitiesOfClass(co.blumeplugins.deathsareitems.entity.custom.ButtonTntEntity.class, new AABB(pos).inflate(2));
        h.assertTrue(tnts.size() == 1 && tnts.get(0).getFuse() == 60, "Double interaction spawns one TNT with a three-second fuse");
        h.assertTrue(DeathRewards.forDeath(p.damageSources().explosion(tnts.get(0), null)).orElseThrow().id().equals(DeathRewards.TNT), "Button TNT unlocks the existing TNT reward");
        p.setPos(Vec3.atCenterOf(pos).add(15,0,0));
        h.runAfterDelay(65, () -> {
            try {
                h.assertTrue(h.getLevel().getBlockState(pos.below()).is(Blocks.STONE) && h.getLevel().getBlockState(pos).is(Blocks.STONE_BUTTON), "Explosion preserves terrain and button");
                h.assertTrue(tnts.get(0).isRemoved(), "TNT explodes and is removed");
                String key = h.getLevel().dimension().location() + ":" + pos.asLong();
                h.assertTrue(!EinfachSavedData.get(p.server).buttons.hasUUID(key), "Explosion releases the button lock"); h.succeed();
            } finally { leave(p); }
        });
    }
    @GameTest(template="test_arena") public static void companionsHaveOneSummonAndRecallDoesNotDuplicate(GameTestHelper h) {
        var p = online(h); var pos = h.absolutePos(new BlockPos(4,2,4));
        try {
            h.assertTrue(Companions.summon(p, false, Vec3.atBottomCenterOf(pos)), "Juky summons in clear space");
            h.assertTrue(!Companions.summon(p, false, Vec3.atBottomCenterOf(pos.east())), "Extra whistles cannot summon a second Juky");
            var data = EinfachSavedData.get(p.server).player(p.getUUID());
            var juky = (Mob)h.getLevel().getEntity(data.getUUID("Juky"));
            var cow = h.spawn(EntityType.COW, new BlockPos(6,2,4));
            h.assertTrue(!juky.canAttack(p) && !juky.canAttack(cow), "Juky cannot attack players or passive animals");
            h.assertTrue(!DeathAbilities.eligible(p, juky), "Reward items exclude companions from targeting");
            float health = juky.getHealth(); juky.hurt(DeathDamage.source(juky, DeathDamage.SOUL, p), 100);
            h.assertTrue(juky.getHealth() == health, "Death-item area damage leaves companions unharmed");
            Companions.recall(p, false); h.assertTrue(juky.isRemoved() && !data.hasUUID("Juky"), "Recall removes the entity and active marker");
            h.assertTrue(Companions.summon(p, false, Vec3.atBottomCenterOf(pos)), "Manual recall allows immediate resummoning");
            juky = (Mob)h.getLevel().getEntity(data.getUUID("Juky")); juky.hurt(juky.damageSources().genericKill(), Float.MAX_VALUE);
            h.assertTrue(!Companions.summon(p, false, Vec3.atBottomCenterOf(pos)), "Defeat enforces a shared thirty-second cooldown"); h.succeed();
        } finally { Companions.recall(p, false); leave(p); }
    }
    @GameTest(template="test_arena") public static void vinyRequiresWaterAndReturnsFromDryLand(GameTestHelper h) {
        var p = online(h); var pos = h.absolutePos(new BlockPos(4,2,4));
        try {
            h.assertTrue(!Companions.summon(p, true, Vec3.atCenterOf(pos)), "Viny refuses a dry summon");
            h.getLevel().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            h.assertTrue(Companions.summon(p, true, Vec3.atCenterOf(pos)), "Viny can summon into water");
            var data = EinfachSavedData.get(p.server).player(p.getUUID());
            var viny = (Mob)h.getLevel().getEntity(data.getUUID("Viny"));
            viny.setPos(Vec3.atBottomCenterOf(pos.above())); viny.tick();
            h.assertTrue(viny.isRemoved() && !data.hasUUID("Viny"), "Viny returns safely on dry land"); h.succeed();
        } finally { Companions.recall(p, true); leave(p); }
    }
    @GameTest(template="test_arena") public static void bookRestraintsNeverImmobilizeBosses(GameTestHelper h) {
        var p = online(h);
        try {
            var data = RewardSavedData.get(p.server); data.discover(p.getUUID(), DeathRewards.FALL);
            h.assertTrue(GustafBookItem.unlocked(p).equals(List.of("fall")), "Only matching discoveries unlock chapters");
            var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(5,2,5));
            h.assertTrue(BookSpells.bubble(p, zombie, 80) && MobRestraints.busy(zombie), "Normal enemies are held in a water bubble");
            var boss = h.spawn(EntityType.WITHER, new BlockPos(8,2,8)); boss.setNoAi(true); float hp = boss.getHealth();
            h.assertTrue(BookSpells.bubble(p, boss, 80) && !MobRestraints.busy(boss) && boss.getHealth() < hp, "Bosses take water damage without cages");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void jukeboxPersistsOwnerAndPulsesDoNotStack(GameTestHelper h) {
        var p = online(h); var pos = h.absolutePos(new BlockPos(4,2,4));
        try {
            p.setPos(Vec3.atBottomCenterOf(pos.east(3)));
            h.getLevel().setBlock(pos, ModBlocks.GUSTAFS_JUKEBOX.get().defaultBlockState(), 3);
            var box = (GustafJukeboxBlockEntity)h.getLevel().getBlockEntity(pos); box.setOwner(p.getUUID());
            var loaded = BlockEntity.loadStatic(pos, box.getBlockState(), box.saveWithFullMetadata());
            h.assertTrue(loaded instanceof GustafJukeboxBlockEntity && loaded.saveWithFullMetadata().getUUID("Owner").equals(p.getUUID()), "Custom jukebox type and owner survive reload");
            box.setFirstItem(new ItemStack(Items.MUSIC_DISC_CAT));
            var mob = h.spawn(EntityType.HUSK, new BlockPos(6,2,4)); mob.setNoAi(true); float hp = mob.getHealth();

            GustafJukeboxBlockEntity.tick(h.getLevel(), pos, box.getBlockState(), box);
            GustafJukeboxBlockEntity.pulse(h.getLevel(), pos, p);
            float after = mob.getHealth(); mob.invulnerableTime = 0;
            GustafJukeboxBlockEntity.pulse(h.getLevel(), pos, p);

            h.assertTrue(after < hp && mob.getHealth() == after, "Repeated overlapping pulses cannot multiply damage");
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST) && p.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST).getAmplifier() == 1, "Owner receives Strength II while in range");
            box.popOutRecord(); h.assertTrue(!box.isRecordPlaying(), "Removing the disc stops remix playback"); h.succeed();
        } finally { leave(p); }
    }

    private static void readyChest(GameTestHelper h, ServerPlayer p, RewardSavedData.Offer offer, int slot, BlockPos pos) {
        offer.dimension = h.getLevel().dimension().location(); offer.spawned = true;
        offer.spawnedAt = h.getLevel().getGameTime() - RewardTimings.READY;
        var chest = ModEntities.REWARD_CHEST.get().create(h.getLevel()); chest.setPos(Vec3.atBottomCenterOf(pos));
        chest.configure(p.getUUID(), offer.generation, slot, offer.spawnedAt, DeathRewards.get(offer.reward).options().get(slot).titleKey());
        h.getLevel().addFreshEntity(chest); offer.chests.add(chest.getUUID());
        h.assertTrue(SettledRewardChests.place(chest, offer), "Reward chest must settle");
    }
    @GameTest(template="test_arena") public static void actualChestClaimAddsBonusButPreviewDoesNot(GameTestHelper h) {
        var p = online(h); var rewards = RewardSavedData.get(p.server); var pos = h.absolutePos(new BlockPos(5,2,5));
        try {
            RewardManager.onDeath(new LivingDeathEvent(p, p.damageSources().drown()));
            readyChest(h, p, rewards.offers.get(p.getUUID()), 0, pos);
            h.assertTrue(RewardManager.selectBlock(p, pos), "A natural drowning chest can be claimed");
            h.assertTrue(count(p, ModItems.DROWNING_MASK.get()) == 1 && count(p, ModItems.VINYS_BUCKET.get()) == 1, "Claim gives both the original mask and its companion bonus");
            RewardManager.offer(p, DeathRewards.get(DeathRewards.GOLEM));
            readyChest(h, p, rewards.offers.get(p.getUUID()), 1, pos);
            h.assertTrue(RewardManager.selectBlock(p, pos) && count(p, ModItems.MOB_PICKAXE.get()) == 1, "Preview chest still gives its original item");
            h.assertTrue(count(p, ModItems.JUKYS_WHISTLE.get()) == 0
                    && EinfachSavedData.get(p.server).player(p.getUUID()).getCompound("Claimed").getAllKeys().size() == 1, "Preview claims cannot add bonuses or milestone progress"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void bookCastsWithoutCooldownAndRespectsChapterLocks(GameTestHelper h) {
        var p = online(h);
        try {
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(7,2,7))));
            var target = h.spawn(EntityType.COW, new BlockPos(7,2,3)); target.setNoAi(true);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            var book = ModItems.GUSTAFS_NEW_BOOK.get(); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(book));
            book.use(p.level(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(target.getHealth() == 200, "A given book cannot cast undiscovered chapters");
            RewardSavedData.get(p.server).discover(p.getUUID(), DeathRewards.FALL);
            EinfachSavedData.get(p.server).player(p.getUUID()).putLong("BookReady", Long.MAX_VALUE);
            book.use(p.level(), p, InteractionHand.MAIN_HAND); float after = target.getHealth();
            h.assertTrue(after < 200, "Unlocked chapter casts through actual right-click input");
            target.invulnerableTime = 0; p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(book));
            book.use(p.level(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(target.getHealth() < after, "Another copy can cast immediately without a cooldown");
            var saved = EinfachSavedData.load(EinfachSavedData.get(p.server).save(new CompoundTag())).player(p.getUUID());
            h.assertTrue(!saved.contains("BookReady"), "Book cooldown is not saved");
            RewardSavedData.get(p.server).discover(p.getUUID(), DeathRewards.DROWN);
            p.setShiftKeyDown(true); book.use(p.level(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(EinfachSavedData.get(p.server).player(p.getUUID()).getString("Chapter").equals("drown"), "Sneaking cycles only unlocked chapters"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void whistleSoundsForAirAndBlockCallsAndRecalls(GameTestHelper h) {
        var p = online(h);
        List<net.minecraft.sounds.SoundEvent> heard = new ArrayList<>();
        java.util.function.Consumer<net.minecraftforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == h.getLevel() && event.getSound() != null) heard.add(event.getSound().value());
        };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            var whistle = ModItems.JUKYS_WHISTLE.get();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(whistle));
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,7))));
            whistle.use(p.level(), p, InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(true); whistle.use(p.level(), p, InteractionHand.MAIN_HAND);
            var pos = h.absolutePos(new BlockPos(5,1,5));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            p.setShiftKeyDown(false); whistle.useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND, hit));
            p.setShiftKeyDown(true); whistle.useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND, hit));
            var summon = co.blumeplugins.deathsareitems.sound.ModSounds.JUKY_SUMMON.get();
            var recall = co.blumeplugins.deathsareitems.sound.ModSounds.JUKY_RECALL.get();
            h.assertTrue(heard.equals(List.of(summon, recall, summon, recall)), "Air and block inputs each play exactly one correct whistle sound");
            h.succeed();
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
            Companions.recall(p, false); leave(p);
        }
    }
    @GameTest(template="test_arena") public static void bookCastsBeforeTradingOrSittingInteractions(GameTestHelper h) {
        var p = online(h);
        try {
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,7))));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUSTAFS_NEW_BOOK.get()));
            var rewards = RewardSavedData.get(p.server);
            rewards.discover(p.getUUID(), DeathRewards.FALL); rewards.discover(p.getUUID(), DeathRewards.DROWN);
            var villager = h.spawn(EntityType.VILLAGER, new BlockPos(5,2,5)); villager.setNoAi(true);
            villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); villager.setHealth(200);
            h.assertTrue(p.interactOn(villager, InteractionHand.MAIN_HAND).consumesAction() && villager.getHealth() < 200,
                    "Clicking a villager casts instead of trading");
            h.assertTrue(p.containerMenu == p.inventoryMenu, "Book interactions never open the trading screen");
            villager.invulnerableTime = 0; float hp = villager.getHealth();
            var specific = net.minecraftforge.common.ForgeHooks.onInteractEntityAt(p, villager,
                    new EntityHitResult(villager, villager.getEyePosition()), InteractionHand.MAIN_HAND);
            h.assertTrue(specific != null && specific.consumesAction() && villager.getHealth() < hp, "The precise entity-click path also casts");
            var wolf = h.spawn(EntityType.WOLF, new BlockPos(6,2,5)); wolf.setTame(true); wolf.setOwnerUUID(p.getUUID());
            wolf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); wolf.setHealth(200);
            p.interactOn(wolf, InteractionHand.MAIN_HAND);
            h.assertTrue(wolf.getHealth() < 200 && !wolf.isOrderedToSit(), "Clicking a tamed wolf casts instead of ordering it to sit");
            hp = wolf.getHealth(); p.setShiftKeyDown(true); p.interactOn(wolf, InteractionHand.MAIN_HAND);
            h.assertTrue(wolf.getHealth() == hp && !wolf.isOrderedToSit()
                    && EinfachSavedData.get(p.server).player(p.getUUID()).getString("Chapter").equals("drown"),
                    "Sneaking still changes chapters when pointing at a mob");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void bookChaptersAffectPassiveNeutralTamedAndCompanionMobs(GameTestHelper h) {
        var p = online(h);
        try {
            var pos = new BlockPos(5,2,5);
            var wolf = h.spawn(EntityType.WOLF, pos); wolf.setTame(true); wolf.setOwnerUUID(p.getUUID());
            List<Mob> targets = List.of(h.spawn(EntityType.COW, pos), h.spawn(EntityType.VILLAGER, pos),
                    h.spawn(EntityType.IRON_GOLEM, pos), wolf, h.spawn(ModEntities.JUKY.get(), pos), h.spawn(ModEntities.VINY.get(), pos));
            h.assertTrue(!BookSpells.canTarget(p), "Players are not mob targets");
            for (Mob target : targets) {
                target.setNoAi(true);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
                for (String chapter : List.of("fall", "drown", "anvil", "cactus", "wither")) {
                    float hp = target.getHealth(); target.invulnerableTime = 0;
                    h.assertTrue(BookSpells.cast(p, target, chapter), chapter + " accepts " + target.getType());
                    h.assertTrue(target.getHealth() < hp, chapter + " damages " + target.getType());
                }
                if (Companions.isCompanion(target)) {
                    float hp = target.getHealth(); target.invulnerableTime = 0;
                    target.hurt(DeathDamage.source(target, DeathDamage.SOUL, p), 10);
                    h.assertTrue(target.getHealth() == hp, "Companions still resist other death items");
                }
            }
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void bookPagesReplaceOwnSpellButRespectOtherReadersAndViny(GameTestHelper h) {
        var p = online(h); var other = online(h);
        try {
            var target = h.spawn(EntityType.HUSK, new BlockPos(5,2,5)); target.setNoAi(true);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            h.assertTrue(BookSpells.cast(p, target, "drown"), "Water chapter can start a book spell");
            h.assertTrue(!BookSpells.cast(other, target, "anvil"), "Another reader cannot steal a restraint");
            h.assertTrue(BookSpells.cast(p, target, "anvil"), "Same reader can change chapters immediately");
            h.assertTrue(BookSpells.cast(p, target, "fall") && !MobRestraints.busy(target), "Instant chapter releases the reader's old cage");
            h.assertTrue(BookSpells.bubble(p, target, 80), "Viny can create a separate bubble");
            h.assertTrue(!BookSpells.cast(p, target, "cactus") && MobRestraints.busy(target), "The book leaves Viny's active bubble alone");
            h.succeed();
        } finally { leave(p); leave(other); }
    }
    @GameTest(template="test_arena") public static void bookFinaleRequiresDistinctVersesAndAffectsAllMobs(GameTestHelper h) {
        var p = online(h);
        try {
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,7))));
            var target = h.spawn(EntityType.HUSK, new BlockPos(5,2,4)); target.setNoAi(true);
            var nearby = h.spawn(EntityType.HUSK, new BlockPos(7,2,4)); nearby.setNoAi(true);
            var cow = h.spawn(EntityType.COW, new BlockPos(6,2,4)); cow.setNoAi(true);
            float hp = nearby.getHealth(), cowHp = cow.getHealth();
            h.assertTrue(BookSpells.inscribe(p, target, "fall") == 1, "First verse starts the sequence");
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "Ground Slam grants its blessing");
            h.assertTrue(BookSpells.inscribe(p, target, "fall") == 1, "Repeating a chapter cannot fill the finale");
            h.assertTrue(BookSpells.inscribe(p, target, "drown") == 2, "A different chapter adds a verse");
            h.assertTrue(BookSpells.inscribe(p, target, "anvil") == 3, "Three different chapters trigger a finale");
            h.assertTrue(nearby.getHealth() < hp && cow.getHealth() < cowHp && p.getHealth() == p.getMaxHealth(), "Finale hits hostile and passive mobs while sparing the reader");
            h.assertTrue(nearby.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Finale slows surviving normal enemies");
            h.assertTrue(BookSpells.inscribe(p, target, "cactus") == 1, "A finale consumes the verses");
            var progress = EinfachSavedData.get(p.server).player(p.getUUID());
            progress.putLong("BookVerseUntil", Companions.now(p) - 1);
            h.assertTrue(BookSpells.inscribe(p, target, "wither") == 1, "Expired verses restart the sequence");
            h.assertTrue(BookSpells.inscribe(p, nearby, "drown") == 1, "Switching targets starts a new sequence");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void bookSightWorksInEitherHandAndRevealsAllMobs(GameTestHelper h) {
        var p = online(h);
        try {
            p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,5))));
            var foe = h.spawn(EntityType.HUSK, new BlockPos(6,2,5)); foe.setNoAi(true);
            var cow = h.spawn(EntityType.COW, new BlockPos(4,2,5)); cow.setNoAi(true);
            var book = ModItems.GUSTAFS_NEW_BOOK.get(); var stack = new ItemStack(book);
            p.tickCount = 20;
            book.inventoryTick(stack, p.level(), p, 0, false);
            h.assertTrue(!foe.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "Stored books do not reveal enemies");
            book.inventoryTick(stack, p.level(), p, 0, true);
            h.assertTrue(foe.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "Main-hand sight reveals hostile and passive mobs");
            foe.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
            p.setItemInHand(InteractionHand.OFF_HAND, stack);
            book.inventoryTick(stack, p.level(), p, 40, false);
            h.assertTrue(foe.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "Off-hand sight works too");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void vinyBubblesOnlyWithMaskAndCannotResetCooldownByRecall(GameTestHelper h) {
        var p = online(h); var pos = h.absolutePos(new BlockPos(4,2,4));
        try {
            h.getLevel().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            h.assertTrue(Companions.summon(p, true, Vec3.atCenterOf(pos)), "Viny appears in water");
            var progress = EinfachSavedData.get(p.server).player(p.getUUID());
            var viny = (Mob)h.getLevel().getEntity(progress.getUUID("Viny"));
            var first = h.spawn(EntityType.HUSK, new BlockPos(5,2,4)); first.setNoAi(true);
            viny.tickCount = 10; viny.tick();
            h.assertTrue(!MobRestraints.busy(first), "Viny does not cast without the mask");
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.DROWNING_MASK.get()));
            viny.tickCount = 20; viny.tick();
            h.assertTrue(MobRestraints.busy(first), "The mask enables Viny's bubble");
            long ready = progress.getLong("VinyBubbleReady");
            Companions.recall(p, true); Companions.summon(p, true, Vec3.atCenterOf(pos));
            viny = (Mob)h.getLevel().getEntity(progress.getUUID("Viny"));
            var second = h.spawn(EntityType.HUSK, new BlockPos(3,2,4)); second.setNoAi(true);
            viny.tickCount = 10; viny.tick();
            h.assertTrue(!MobRestraints.busy(second) && progress.getLong("VinyBubbleReady") == ready, "Recall and extra buckets cannot reset the fifteen-second cooldown"); h.succeed();
        } finally { Companions.recall(p, true); leave(p); }
    }
}
