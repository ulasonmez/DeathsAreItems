package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.entity.custom.ChaosPearlEntity;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("deathsareitems")
@PrefixGameTestTemplate(false)
public final class GameplayRevisionGameTests {
    private static ServerPlayer online(GameTestHelper h) { return ExpandedDeathGameTests.online(h); }
    private static TickEvent.PlayerTickEvent tick(ServerPlayer p) { return new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p); }
    private static void leave(ServerPlayer p) {
        p.stopUsingItem(); RewardManager.clearOffer(p); p.server.getPlayerList().remove(p);
    }

    @GameTest(template="test_arena", timeoutTicks=30) public static void rewardChestsDoNotRepositionThePlayer(GameTestHelper h) {
        var p = online(h);
        Vec3 at = p.position().add(.21, 0, -.17); p.setPos(at); p.setNoGravity(true);
        RewardManager.offer(p, DeathRewards.get(DeathRewards.FALL));
        RewardSavedData.get(p.server).offers.get(p.getUUID()).delay = 1;
        h.runAfterDelay(4, () -> {
            try {
                h.assertTrue(RewardSavedData.get(p.server).offers.get(p.getUUID()).spawned, "Three chests start on valid open ground");
                h.assertTrue(p.position().distanceToSqr(at) < .000001, "Chest presentation preserves the player's precise coordinates");
                h.succeed();
            } finally { leave(p); }
        });
    }
    @GameTest(template="test_arena") public static void bowInInventoryDoesNotCancelSkeletonArrowDamage(GameTestHelper h) {
        var p = online(h);
        try {
            for (int i = 0; i < 65; i++) p.tick();
            p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.invulnerableTime = 0;
            p.getInventory().setItem(15, new ItemStack(ModItems.SKELETON_BOW.get()));
            var skeleton = EntityType.SKELETON.create(h.getLevel());
            h.assertTrue(p.hurt(p.damageSources().arrow(EntityType.ARROW.create(h.getLevel()), skeleton), 4F)
                    && p.getHealth() < 20, "Carrying Skeleton Bow must allow real skeleton arrow damage");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena", batch="revision-starvation") public static void easyAndNormalStarvationCanKillEvenWithStaff(GameTestHelper h) {
        var p = online(h); var previous = h.getLevel().getDifficulty();
        try {
            for (int i = 0; i < 65; i++) p.tick();
            p.setGameMode(GameType.SURVIVAL); p.getFoodData().setFoodLevel(0); p.getFoodData().setSaturation(0);
            p.getInventory().add(new ItemStack(ModItems.HUNGER_STAFF.get()));
            for (Difficulty difficulty : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL}) {
                p.server.setDifficulty(difficulty, true); p.setHealth(1); p.invulnerableTime = 0;
                for (int i = 0; i < 80; i++) HungerAbility.starvation(tick(p));
                h.assertTrue(!p.isAlive(), difficulty + " starvation must finish the last half-heart");
            }
            h.succeed();
        } finally { p.server.setDifficulty(previous, true); leave(p); }
    }
    @GameTest(template="test_arena") public static void idleHungerIconsExpireOneAtATime(GameTestHelper h) {
        var p = online(h);
        try {
            p.getFoodData().setFoodLevel(20); p.getPersistentData().putInt(HungerAbility.RESERVE, 6);
            for (int i = 0; i < 199; i++) HungerAbility.foodTick(tick(p));
            h.assertTrue(p.getPersistentData().getInt(HungerAbility.RESERVE) == 6, "The first icon lasts ten seconds");
            HungerAbility.foodTick(tick(p));
            h.assertTrue(p.getPersistentData().getInt(HungerAbility.RESERVE) == 4, "Exactly one full icon expires after ten seconds");
            p.getFoodData().setFoodLevel(19); HungerAbility.foodTick(tick(p));
            h.assertTrue(p.getFoodData().getFoodLevel() == 20 && p.getPersistentData().getInt(HungerAbility.RESERVE) == 3,
                    "Useful reserves still replenish actual hunger before expiring");
            for (int i = 0; i < 400; i++) HungerAbility.foodTick(tick(p));
            h.assertTrue(p.getPersistentData().getInt(HungerAbility.RESERVE) == 0, "The remaining icons eventually expire");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void solidCushionsReplaceGroundAndProtectContainers(GameTestHelper h) {
        var p = online(h);
        try {
            var boots = new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get()); p.setItemSlot(EquipmentSlot.FEET, boots);
            CushionCycle.advance(boots);
            BlockPos center = h.absolutePos(new BlockPos(3, 1, 3));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1,0,-1), center.offset(1,0,1))) h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            h.getLevel().setBlock(center.east(), Blocks.CHEST.defaultBlockState(), 3);
            h.getLevel().setBlock(center.west(), Blocks.BEDROCK.defaultBlockState(), 3);
            h.assertTrue(FallBootsAbility.placeCushion(p, center) == 7, "Slime replaces ordinary solid landing blocks");
            h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.SLIME_BLOCK), "The landing surface itself becomes slime");
            h.assertTrue(h.getLevel().getBlockState(center.east()).is(Blocks.CHEST)
                    && h.getLevel().getBlockState(center.west()).is(Blocks.BEDROCK), "Containers and bedrock remain protected");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void boulderSedimentDropsVanillaItems(GameTestHelper h) {
        BlockPos sand = h.absolutePos(new BlockPos(3,2,3)), gravel = sand.east();
        h.getLevel().setBlock(sand, ModBlocks.FRAGILE_SAND.get().defaultBlockState(), 3);
        h.getLevel().setBlock(gravel, ModBlocks.FRAGILE_GRAVEL.get().defaultBlockState(), 3);
        h.getLevel().destroyBlock(sand, true); h.getLevel().destroyBlock(gravel, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(sand).inflate(3));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.SAND))
                && drops.stream().anyMatch(e -> e.getItem().is(Items.GRAVEL)), "Instantly mined sediment drops sand and gravel");
        h.succeed();
    }
    @GameTest(template="test_arena", timeoutTicks=30) public static void crammerClearsSnowPlantsAndContainersBeforePlacingGlass(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(true);
        BlockPos base=mob.blockPosition(), snow=base.west(), flower=base.north(), chest=base.south();
        h.getLevel().setBlock(base.below(),Blocks.BEDROCK.defaultBlockState(),3);
        h.getLevel().setBlock(snow,Blocks.SNOW.defaultBlockState(),3);
        h.getLevel().setBlock(flower,Blocks.DANDELION.defaultBlockState(),3);
        h.getLevel().setBlock(base,Blocks.SNOW.defaultBlockState(),3);
        h.getLevel().setBlock(chest,Blocks.CHEST.defaultBlockState(),3);
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,3));
        h.assertTrue(CrammerAbility.start(p,mob),"Snow, plants, container blocks and a bedrock floor do not prevent a cage");
        h.assertTrue(h.getLevel().getBlockState(snow).is(Blocks.GLASS_PANE)
                && h.getLevel().getBlockState(flower).is(Blocks.GLASS_PANE)
                && h.getLevel().getBlockState(chest).is(Blocks.GLASS_PANE),"Every obstructed wall position receives glass");
        h.assertTrue(h.getLevel().getBlockState(base).isAir(),"Snow inside the cage is cleared too");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(4));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==3,
                "Breaking an obstructing container drops its contents exactly once");
        leave(p);
        h.runAfterDelay(3,()->{
            h.assertTrue(h.getLevel().getBlockState(chest).isAir(),"Cleanup does not restore a broken chest and duplicate its drops");
            h.assertTrue(h.getLevel().getBlockState(base.below()).is(Blocks.BEDROCK),"The existing unbreakable floor remains usable");
            mob.discard(); h.succeed();
        });
    }
    @GameTest(template="test_arena", timeoutTicks=25, batch="crammer-impact") public static void crammerLaunchEndsEarlyOnARealWallCollision(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7));
        h.assertTrue(CrammerAbility.start(p,mob),"The cage starts before testing its cramped surroundings");
        BlockPos base=mob.blockPosition();
        for (int y=0;y<=3;y++) for (int z=-2;z<=2;z++) h.getLevel().setBlock(base.offset(2,y,z),Blocks.STONE.defaultBlockState(),3);
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) h.getLevel().setBlock(base.offset(x,3,z),Blocks.STONE.defaultBlockState(),3);
        CrammerAbility.damage(new net.minecraftforge.event.entity.living.LivingHurtEvent(mob,mob.damageSources().cramming(),1));
        h.runAfterDelay(1,()->h.assertTrue(mob.isAlive(),"Breaking the glass still gives a brief visible launch"));
        h.runAfterDelay(7,()->{
            try {
                h.assertTrue(!mob.isAlive(),"An actual wall or ceiling impact finishes the mob before the ten-tick timeout");
                h.assertTrue(mob.getLastDamageSource()!=null && mob.getLastDamageSource().is(DeathDamage.CRAM),"Impact keeps the cramming death source and normal loot pipeline");
                h.succeed();
            } finally { mob.discard(); leave(p); }
        });
    }
    @GameTest(template="test_arena", timeoutTicks=25, batch="crammer-open") public static void crammerOpenLaunchKeepsItsHalfSecondLifetime(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7));
        Vec3 start=mob.position();
        h.assertTrue(CrammerAbility.start(p,mob),"The open-area cage starts");
        CrammerAbility.damage(new net.minecraftforge.event.entity.living.LivingHurtEvent(mob,mob.damageSources().cramming(),1));
        h.runAfterDelay(5,()->{
            h.assertTrue(mob.isAlive(),"Without an obstacle, the mob stays alive halfway through the existing launch duration");
            h.assertTrue(mob.position().distanceToSqr(start)>.1,"The mob really flies away from the shattered cage");
        });
        h.runAfterDelay(12,()->{
            try {
                h.assertTrue(!mob.isAlive(),"An unobstructed flight still finishes after half a second"); h.succeed();
            } finally { mob.discard(); leave(p); }
        });
    }
    @GameTest(template="test_arena") public static void cannonFiresTwiceWithoutCooldown(GameTestHelper h) {
        var p = online(h);
        try {
            p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TNT_CANNON.get()));
            var item = ModItems.TNT_CANNON.get();
            h.assertTrue(item.use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "First cannon shot fires");
            h.assertTrue(!p.getCooldowns().isOnCooldown(item), "Firing adds no cannon cooldown");
            h.assertTrue(item.use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Second shot fires in the same tick");
            h.getLevel().getEntitiesOfClass(co.blumeplugins.deathsareitems.entity.custom.CannonTntEntity.class,
                    p.getBoundingBox().inflate(4)).forEach(Entity::discard);
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena", timeoutTicks=25) public static void heldShieldBlocksAndRetaliatesAgainstTamedMobs(GameTestHelper h) {
        var p = online(h); for (int i = 0; i < 65; i++) p.tick();
        p.setGameMode(GameType.SURVIVAL); p.setHealth(20); p.setYRot(0);
        p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.ENDER_PEARL_SHIELD.get()));
        var wolf = h.spawn(EntityType.WOLF, new BlockPos(7,2,9)); wolf.setTame(true); wolf.setNoAi(true);
        var item = ModItems.ENDER_PEARL_SHIELD.get(); item.use(h.getLevel(), p, InteractionHand.OFF_HAND);
        h.onEachTick(p::doTick);
        h.runAfterDelay(8, () -> {
            try {
                h.assertTrue(p.isBlocking(), "After throwing the first pearl, holding use enters real blocking");
                p.invulnerableTime = 0; p.hurt(p.damageSources().mobAttack(wolf), 3);
                h.assertTrue(p.getHealth() == 20, "The raised shield actually blocks a frontal mob hit");
                var pearls = h.getLevel().getEntitiesOfClass(ChaosPearlEntity.class, wolf.getBoundingBox().inflate(3));
                h.assertTrue(pearls.size() >= 60, "Even a tamed neutral attacker triggers teleport retaliation on a blocked hit");
                pearls.forEach(Entity::discard); wolf.discard(); h.succeed();
            } finally { leave(p); }
        });
    }
    @GameTest(template="test_arena") public static void harvestingShrinksOnlyOnGlassPulsesAndPreventsAggro(GameTestHelper h) {
        var p = online(h); var mob = h.spawn(EntityType.ZOMBIE, new BlockPos(7,2,9)); mob.setNoAi(true);
        try {
            p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SOUL_STORM.get()));
            p.setShiftKeyDown(true); p.startUsingItem(InteractionHand.MAIN_HAND);
            for (int i = 0; i < 4; i++) SoulStormAbility.playerTick(tick(p));
            float first = HungerAbility.scale(mob);
            h.assertTrue(first < 1 && first > .9F, "The first glass pulse shrinks only slightly");
            for (int i = 0; i < 8; i++) SoulStormAbility.playerTick(tick(p));
            h.assertTrue(HungerAbility.scale(mob) == first, "Intervening damage pulses do not shrink the mob");
            mob.setTarget(p); h.assertTrue(mob.getTarget() == null, "Harvested mobs cannot acquire their harvester as a target");
            for (int i = 0; i < 4; i++) SoulStormAbility.playerTick(tick(p));
            h.assertTrue(HungerAbility.scale(mob) < first, "The next glass pulse shrinks again");
            h.succeed();
        } finally { mob.discard(); leave(p); }
    }
    @GameTest(template="test_arena", timeoutTicks=40, batch="revision-dragon") public static void soulShotDealsSubstantialRealDragonDamage(GameTestHelper h) {
        var p = online(h); p.setPos(p.getX(), h.getLevel().getMaxBuildHeight()-40, p.getZ()); p.setNoGravity(true);
        var dragon = EntityType.ENDER_DRAGON.create(h.getLevel()); dragon.moveTo(p.getX(), p.getY(), p.getZ()+12, 0, 0);
        dragon.setNoAi(true); dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING); h.getLevel().addFreshEntity(dragon);
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SOUL_STORM.get()));
        p.getMainHandItem().getOrCreateTag().putInt(SoulStormAbility.SOULS, 20); p.setYRot(0); p.setXRot(0);
        h.assertTrue(SoulStormAbility.fire(p, p.getMainHandItem()), "The funded shot fires toward the dragon");
        h.runAfterDelay(12, () -> {
            try {
                h.assertTrue(Math.abs(dragon.getHealth()-(dragon.getMaxHealth()-16)) < .01F, "The shot removes sixteen real dragon health after the sixty-percent reduction; health=" + dragon.getHealth());
                h.succeed();
            } finally { dragon.discard(); leave(p); }
        });
    }
}
