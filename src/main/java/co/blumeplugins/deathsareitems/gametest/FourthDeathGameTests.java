package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import java.util.List;

@GameTestHolder("deathsareitems")
@PrefixGameTestTemplate(false)
public final class FourthDeathGameTests {
    private static ServerPlayer online(GameTestHelper h) { return ExpandedDeathGameTests.online(h); }
    private static ItemStack equip(ServerPlayer p, int souls) {
        ItemStack stack = new ItemStack(ModItems.SOUL_STORM.get()); stack.getOrCreateTag().putInt(SoulStormAbility.SOULS, souls);
        p.setItemSlot(EquipmentSlot.MAINHAND, stack); return p.getMainHandItem();
    }
    private static void pulse(ServerPlayer p) {
        for (int i = 0; i < 4; i++) SoulStormAbility.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
    }
    private static void leave(ServerPlayer p) { p.stopUsingItem(); p.server.getPlayerList().remove(p); }

    @GameTest(template="test_arena") public static void witherEffectHasExactChoicesAndDoesNotMatchSkullsOrPoison(GameTestHelper h) {
        var p = online(h);
        try {
            h.assertTrue(DeathRewards.forDeath(p.damageSources().wither()).orElseThrow().id().equals(DeathRewards.WITHER), "Actual wither effect death unlocks Soul Storm");
            h.assertTrue(DeathRewards.forDeath(p.damageSources().magic()).isEmpty(), "Poison/magic must not unlock the wither reward");
            var definition = DeathRewards.get(DeathRewards.WITHER);
            h.assertTrue(definition.options().stream().map(RewardOption::titleKey).toList().equals(List.of(
                    "reward.deathsareitems.withering_staff", "item.deathsareitems.soul_storm", "reward.deathsareitems.bone_hand")), "Choices keep the PDF order");
            h.assertTrue(definition.choice(0).isEmpty() && definition.choice(1).is(ModItems.SOUL_STORM.get()) && definition.choice(2).isEmpty(), "Only Soul Storm is selectable");
            var data = new RewardSavedData(); data.discover(p.getUUID(), DeathRewards.WITHER);
            h.assertTrue(!RewardSavedData.load(data.save(new CompoundTag())).discover(p.getUUID(), DeathRewards.WITHER), "Wither discovery survives a world save");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void soulsPersistOnTheItemAndImmunityOnlyCoversWither(GameTestHelper h) {
        var p = online(h);
        try {
            var stack = equip(p, 123);
            h.assertTrue(SoulStormAbility.souls(ItemStack.of(stack.save(new CompoundTag()))) == 123, "Item NBT preserves gathered souls");
            var wither = new LivingHurtEvent(p, p.damageSources().wither(), 1); SoulStormAbility.immunity(wither);
            var magic = new LivingHurtEvent(p, p.damageSources().magic(), 1); SoulStormAbility.immunity(magic);
            h.assertTrue(wither.isCanceled() && !magic.isCanceled(), "Matching death immunity does not extend to unrelated magic");
            h.assertTrue(SoulStormAbility.counter(stack).getString().equals("Gathered SOULS : 123"), "Counter uses the reference text and translatable arguments");
            p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            var absent = new LivingHurtEvent(p, p.damageSources().wither(), 1); SoulStormAbility.immunity(absent);
            h.assertTrue(!absent.isCanceled(), "Removing the reward removes its immunity"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",batch="soul-radius") public static void harvestReachesFiftyBlocksAndStopsAtItsBoundary(GameTestHelper h) {
        var p=online(h); p.setPos(p.getX(),h.getLevel().getMaxBuildHeight()-40,p.getZ()); p.setNoGravity(true);
        var inside=EntityType.COW.create(h.getLevel()); var outside=EntityType.COW.create(h.getLevel());
        inside.setPos(p.position().add(45,0,0)); outside.setPos(p.position().add(51,0,0));
        inside.setNoAi(true); outside.setNoAi(true); inside.setNoGravity(true); outside.setNoGravity(true);
        var chunks=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        chunks.add(new net.minecraft.world.level.ChunkPos(inside.blockPosition()));
        chunks.add(new net.minecraft.world.level.ChunkPos(outside.blockPosition()));
        for(var chunk:chunks) h.getLevel().setChunkForced(chunk.x,chunk.z,true);
        h.runAfterDelay(5,()->{try {
            h.assertTrue(h.getLevel().addFreshEntity(inside) && h.getLevel().addFreshEntity(outside),"Test mobs enter loaded chunks");
            var ray=h.getLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),inside.getEyePosition(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            h.assertTrue(DeathAbilities.eligible(p,inside,50),"Distant target is eligible; player="+p.position()+", mob="+inside.position()+", ray="+ray.getType()+" at "+ray.getBlockPos()+", block="+h.getLevel().getBlockState(ray.getBlockPos()));
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(50)).contains(inside),"Distant target is visible to entity queries");
            equip(p,0); p.setShiftKeyDown(true); p.startUsingItem(InteractionHand.MAIN_HAND); pulse(p);
            h.assertTrue(inside.getHealth()==inside.getMaxHealth()-.5F && HungerAbility.scale(inside)<1,"A target 45 blocks away is harvested and shrinks; health="+inside.getHealth());
            h.assertTrue(outside.getHealth()==outside.getMaxHealth(),"A target beyond 50 blocks is excluded"); h.succeed();
        } finally {
            inside.discard(); outside.discard(); leave(p);
            for(var chunk:chunks) h.getLevel().setChunkForced(chunk.x,chunk.z,false);
        }});
    }
    @GameTest(template="test_arena",batch="soul-pulse") public static void harvestDealsHalfPointAndReallyShrinksBelowQuarterSize(GameTestHelper h) {
        var p = online(h); var mob = h.spawn(EntityType.COW, new BlockPos(5,2,7)); mob.setNoAi(true);
        try {
            equip(p, 0); p.setShiftKeyDown(true); p.startUsingItem(InteractionHand.MAIN_HAND);
            float health = mob.getHealth(), width = mob.getBbWidth();
            pulse(p);
            h.assertTrue(mob.getHealth() == health - .5F, "Each harvest pulse deals precisely half a point of real damage");
            h.assertTrue(mob.getBbWidth() < width && HungerAbility.scale(mob) < 1, "Both actual collision and synced scale shrink");
            mob.getPersistentData().putFloat(HungerAbility.SCALE, .26F); mob.refreshDimensions(); pulse(p);
            h.assertTrue(HungerAbility.scale(mob) < .25F && mob.getBbWidth() < width * .25F, "Soul Storm can shrink beyond the Hunger Staff quarter-size floor");
            h.assertTrue(SoulStormAbility.souls(p.getMainHandItem()) == 0, "The counter cannot increment before the flying glass arrives"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=65,batch="soul-flight") public static void holdCollectsNonPickupGlassAndReleaseStopsDamage(GameTestHelper h) {
        var p = online(h); var mob = h.spawn(EntityType.COW, new BlockPos(5,2,7)); mob.setNoAi(true);
        var stack = equip(p, 0); p.setShiftKeyDown(true); p.startUsingItem(InteractionHand.MAIN_HAND);
        p.setNoGravity(true); mob.setNoGravity(true);
        // Embedded test connections do not execute ServerGamePacketListenerImpl.tick/doTick.
        // Drive the actual player tick here, as a real connected client would.
        h.onEachTick(p::doTick);
        h.runAfterDelay(10, () -> {
            h.assertTrue(mob.getHealth() < mob.getMaxHealth(), "Holding sneak and use automatically channels in the real tick loop; using=" + p.isUsingItem() + ", shift=" + p.isShiftKeyDown() + ", health=" + mob.getHealth());
            h.assertTrue(SoulStormAbility.souls(stack) == 0, "Souls are still in flight");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(Display.BlockDisplay.class, p.getBoundingBox().inflate(12),
                    e -> e.saveWithoutId(new CompoundTag()).getCompound("block_state").getString("Name").equals("minecraft:white_stained_glass")).isEmpty(), "Real white glass display travels from the target");
            p.stopUsingItem(); p.setShiftKeyDown(false); mob.getPersistentData().putFloat("test.soulHealth", mob.getHealth());
        });
        h.runAfterDelay(40, () -> {
            try {
                h.assertTrue(SoulStormAbility.souls(stack) >= 1, "Arriving glass increments the soul resource");
                h.assertTrue(mob.getHealth() == mob.getPersistentData().getFloat("test.soulHealth"), "Releasing use immediately stops harvest damage");
                h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(6), e -> e.getItem().is(Blocks.WHITE_STAINED_GLASS.asItem())).isEmpty(), "Soul glass is never a pickup item");
                h.succeed();
            } finally { leave(p); }
        });
    }
    @GameTest(template="test_arena",batch="soul-finish") public static void nearlyInvisibleMobDiesAndDragonAndPetsAreExcluded(GameTestHelper h) {
        var p = online(h); var mob = h.spawn(EntityType.COW, new BlockPos(5,2,7)); mob.setNoAi(true);
        try {
            equip(p, 0); p.setShiftKeyDown(true); p.startUsingItem(InteractionHand.MAIN_HAND);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); mob.setHealth(1000);
            mob.getPersistentData().putFloat(HungerAbility.SCALE, .02F); mob.refreshDimensions(); pulse(p);
            h.assertTrue(!mob.isAlive(), "A nearly zero-sized mob finishes through actual death instead of remaining invisible forever");
            var dragon = EntityType.ENDER_DRAGON.create(h.getLevel()); dragon.setPos(p.position());
            h.assertTrue(!DeathAbilities.eligible(p, dragon), "Ender Dragon can never be harvested");
            var wolf = h.spawn(EntityType.WOLF, new BlockPos(6,2,7)); wolf.setTame(true); wolf.setNoAi(true); pulse(p);
            h.assertTrue(wolf.getHealth() == wolf.getMaxHealth() && HungerAbility.scale(wolf) == 1, "Tamed pets cannot be harvested");
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=35,batch="soul-near") public static void nearBlastCostsOneSoulAndDamagesMob(GameTestHelper h) {
        var p = online(h); var target = h.spawn(EntityType.COW, new BlockPos(7,2,9)); target.setNoAi(true);
        var stack = equip(p, 20); p.setXRot(20); p.setYRot(0);
        h.assertTrue(SoulStormAbility.fire(p, stack), "Collected souls launch a real swept attack");
        h.runAfterDelay(8, () -> {
            try {
                h.assertTrue(!target.isAlive(), "The blast delivers substantial real damage to mobs");
                h.assertTrue(SoulStormAbility.souls(stack) == 19, "A point-blank hit consumes exactly one soul; remaining=" + SoulStormAbility.souls(stack)); h.succeed();
            } finally { leave(p); }
        });
    }
    @GameTest(template="test_arena",timeoutTicks=45,batch="soul-far") public static void fartherBlastConsumesMoreSoulsAndNeverGoesNegative(GameTestHelper h) {
        var p = online(h); var stack = equip(p, 3);
        p.teleportTo(p.getX(), h.getLevel().getMaxBuildHeight()-40, p.getZ()); p.setXRot(0); p.setYRot(0);
        var chunks=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for(int i=0;i<=16;i+=4) chunks.add(new net.minecraft.world.level.ChunkPos(p.blockPosition().south(i)));
        for(var chunk:chunks) h.getLevel().setChunkForced(chunk.x,chunk.z,true);
        h.runAfterDelay(5,()->{
            var ray=h.getLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(12)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            h.assertTrue(ray.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"Shot corridor is clear; player="+p.position()+", look="+p.getLookAngle()+", ray="+ray.getType()+" at "+ray.getBlockPos()+", block="+h.getLevel().getBlockState(ray.getBlockPos()));
            h.assertTrue(SoulStormAbility.fire(p, stack), "A funded distant shot starts");
        });
        h.runAfterDelay(17, () -> {
            try {
                h.assertTrue(SoulStormAbility.souls(stack) == 0, "Twelve blocks of flight consume three souls; remaining=" + SoulStormAbility.souls(stack));
                h.assertTrue(!SoulStormAbility.fire(p, stack) && SoulStormAbility.souls(stack) == 0, "An empty resource cannot fire or become negative"); h.succeed();
            } finally {
                leave(p); for(var chunk:chunks) h.getLevel().setChunkForced(chunk.x,chunk.z,false);
            }
        });
    }
    @GameTest(template="test_arena",batch="soul-tower") public static void soulBlastBreaksObsidianButProtectsOwnerAndContainers(GameTestHelper h) {
        var p = online(h); p.setGameMode(GameType.SURVIVAL); p.setHealth(20);
        try {
            BlockPos center = h.absolutePos(new BlockPos(4,3,4));
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) h.getLevel().setBlock(center.offset(x,y,z), Blocks.OBSIDIAN.defaultBlockState(), 3);
            BlockPos chest = center.east(2); h.getLevel().setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
            p.setPos(Vec3.atCenterOf(center).add(0,3,0)); Vec3 motion = p.getDeltaMovement();
            OwnedExplosions.soulBlast(p, Vec3.atCenterOf(center));
            h.assertTrue(h.getLevel().getBlockState(center).isAir(), "Soul explosions really excavate obsidian End towers");
            h.assertTrue(h.getLevel().getBlockState(chest).is(Blocks.CHEST), "Container protection survives the custom resistance calculator");
            h.assertTrue(p.getHealth() == 20 && p.getDeltaMovement().equals(motion), "The caster takes neither blast damage nor knockback"); h.succeed();
        } finally { leave(p); }
    }
}
