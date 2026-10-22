package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import co.blumeplugins.deathsareitems.entity.custom.*;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("deathsareitems")
@PrefixGameTestTemplate(false)
public final class ThirdDeathGameTests {
    private static ServerPlayer online(GameTestHelper h) { return ExpandedDeathGameTests.online(h); }
    private static void leave(ServerPlayer p) { p.server.getPlayerList().remove(p); }
    private static void fireTick(ServerPlayer p) { FirePantsAbility.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p)); }
    @GameTest(template="test_arena") public static void allNewDeathsHaveExactChoicesAndDistinctSources(GameTestHelper h) {
        var p=online(h);
        try {
            Map<DamageSource,net.minecraft.resources.ResourceLocation> sources=new LinkedHashMap<>();
            sources.put(p.damageSources().cramming(),DeathRewards.CRAM); sources.put(p.damageSources().cactus(),DeathRewards.CACTUS);
            sources.put(p.damageSources().lava(),DeathRewards.LAVA); sources.put(p.damageSources().badRespawnPointExplosion(p.position()),DeathRewards.BED);
            sources.put(p.damageSources().explosion(new net.minecraft.world.entity.item.PrimedTnt(p.level(),p.getX(),p.getY(),p.getZ(),p),p),DeathRewards.TNT);
            sources.put(DeathDamage.source(p,DeathDamage.PEARL,null),DeathRewards.PEARL);
            sources.forEach((s,id)->h.assertTrue(DeathRewards.forDeath(p,s).orElseThrow().id().equals(id),"Every source must select its intended new reward"));
            h.assertTrue(DeathRewards.forDeath(p,p.damageSources().explosion(EntityType.CREEPER.create(p.level()),null)).isEmpty(),"A creeper explosion must not unlock the TNT reward");
            Map<net.minecraft.resources.ResourceLocation,List<String>> expected=Map.of(
                    DeathRewards.CRAM,List.of("reward.deathsareitems.crammer_error","item.deathsareitems.entity_crammer","reward.deathsareitems.squish_squash"),
                    DeathRewards.CACTUS,List.of("item.deathsareitems.cactus_chestplate","reward.deathsareitems.thorn_in_your_side","reward.deathsareitems.rusty_cactus"),
                    DeathRewards.TNT,List.of("item.deathsareitems.tnt_cannon","reward.deathsareitems.tasty_tnt","reward.deathsareitems.blast_from_the_past"),
                    DeathRewards.LAVA,List.of("reward.deathsareitems.firewall","item.deathsareitems.fire_pants","reward.deathsareitems.molten_socks"),
                    DeathRewards.BED,List.of("reward.deathsareitems.fluffy_pillow","item.deathsareitems.bedrock","reward.deathsareitems.explosive_blankie"),
                    DeathRewards.PEARL,List.of("reward.deathsareitems.ender_pearl_necklace","reward.deathsareitems.ender_eyeballs","item.deathsareitems.ender_pearl_shield"));
            expected.forEach((id,titles)->h.assertTrue(DeathRewards.get(id).options().stream().map(RewardOption::titleKey).toList().equals(titles),"Choice order must match the reference"));
            h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void ordinaryFallAndVanillaPearlDeathAreSeparate(GameTestHelper h) {
        var p=online(h);
        try {
            h.assertTrue(DeathRewards.forDeath(p,p.damageSources().fall()).orElseThrow().id().equals(DeathRewards.FALL),"Ordinary fall unlocks boots");
            var pearl=EntityType.ENDER_PEARL.create(p.level()); pearl.setOwner(p);
            PearlAbility.vanillaPearl(new EntityTeleportEvent.EnderPearl(p,p.getX(),p.getY(),p.getZ(),pearl,5F,null));
            h.assertTrue(DeathRewards.forDeath(p,p.damageSources().fall()).orElseThrow().id().equals(DeathRewards.PEARL),"Vanilla pearl fall damage unlocks the shield");
            p.getPersistentData().putLong("deathsareitems.PearlImpact",p.level().getGameTime()-1);
            h.assertTrue(DeathRewards.forDeath(p,p.damageSources().fall()).orElseThrow().id().equals(DeathRewards.FALL),"A stale pearl landing cannot reclassify later falls"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void newRewardDiscoveryAndOffersSurviveSave(GameTestHelper h) {
        var data=new RewardSavedData(); var owner=UUID.randomUUID();
        for (var id:List.of(DeathRewards.CRAM,DeathRewards.CACTUS,DeathRewards.TNT,DeathRewards.LAVA,DeathRewards.BED,DeathRewards.PEARL)) data.discover(owner,id);
        data.offers.put(owner,new RewardSavedData.Offer(owner,DeathRewards.PEARL));
        var reload=RewardSavedData.load(data.save(new CompoundTag()));
        h.assertTrue(reload.discovered.get(owner).size()==6 && reload.offers.get(owner).reward.equals(DeathRewards.PEARL),"All six new discoveries and pending shield survive save/load"); h.succeed();
    }
    @GameTest(template="test_arena") public static void crammerInterceptsVillagerTrading(GameTestHelper h) {
        var p=online(h); var villager=h.spawn(EntityType.VILLAGER,new BlockPos(5,2,7)); villager.setNoAi(true);
        try {
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.ENTITY_CRAMMER.get()));
            var click=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,villager);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(click);
            h.assertTrue(click.isCanceled() && click.getCancellationResult().consumesAction(),"Crammer consumes the click before villager trading");
            h.assertTrue(villager.isNoGravity() && h.getLevel().getBlockState(villager.blockPosition().west()).is(Blocks.GLASS_PANE),"Villager really enters the cage");
            h.assertTrue(villager.getTradingPlayer()==null,"Cramming never opens a villager trade"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=45) public static void groundedFirePantsRampAndMeltWithoutJumping(GameTestHelper h) {
        var p=online(h); BlockPos support=h.absolutePos(new BlockPos(5,1,5));
        p.setPos(Vec3.atBottomCenterOf(support.above()));
        p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.FIRE_PANTS.get()));
        p.setSprinting(true); p.setOnGround(true); p.setDeltaMovement(Vec3.ZERO); fireTick(p);
        for(int i=0;i<60;i++) {
            p.setPos(p.getX()+.025,p.getY(),p.getZ()); p.xo=p.getX(); p.zo=p.getZ();
            h.getLevel().setBlock(p.getOnPos(),Blocks.STONE.defaultBlockState(),3);
            fireTick(p);
        }
        BlockPos trail=p.getOnPos();
        h.assertTrue(FirePantsAbility.ramp(p)==60 && p.onGround(),"Ground position packets accelerate even when xo and delta movement are reset");
        h.assertTrue(h.getLevel().getBlockState(trail).is(Blocks.OBSIDIAN),"Full speed lays obsidian while grounded");
        p.setSprinting(false); p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY); fireTick(p);
        h.runAfterDelay(33,()->{try {
            h.assertTrue(h.getLevel().getBlockState(trail).is(Blocks.LAVA),"Ground sprint obsidian turns into lava"); h.succeed();
        } finally { leave(p); }});
    }
    @GameTest(template="test_arena") public static void cannonDoesNotStartUseOrProtectUnrelatedTnt(GameTestHelper h) {
        var p=online(h);
        try {
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.TNT_CANNON.get()));
            var result=p.getMainHandItem().getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(result.getResult().consumesAction() && !result.getResult().shouldSwing() && !p.isUsingItem(),"Cannon fires without swing or held use animation");
            var tnt=new net.minecraft.world.entity.item.PrimedTnt(p.level(),p.getX(),p.getY(),p.getZ(),p);
            var hit=new LivingHurtEvent(p,p.damageSources().explosion(tnt,p),5); ChaosAbilities.immunity(hit);
            h.assertTrue(!hit.isCanceled(),"Carrying cannon does not cancel unrelated TNT damage"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void armorSlotsAndInfiniteFireResistanceAreReal(GameTestHelper h) {
        var p=online(h);
        try {
            h.assertTrue(((ArmorItem)ModItems.CACTUS_CHESTPLATE.get()).getDefense()==6 && ((ArmorItem)ModItems.FIRE_PANTS.get()).getDefense()==5,"Reference armor stats must come from actual armor");
            p.getInventory().add(new ItemStack(ModItems.FIRE_PANTS.get())); fireTick(p);
            h.assertTrue(!p.hasEffect(MobEffects.FIRE_RESISTANCE),"Inventory pants grant no equipment effects");
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,1200));
            p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.FIRE_PANTS.get())); fireTick(p);
            h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration() && p.getEffect(MobEffects.FIRE_RESISTANCE).getAmplifier()==0,"Worn pants grant infinite level-I resistance");
            p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY); fireTick(p);
            h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration()==1200,"Removing pants restores the independent potion");
            h.assertTrue(!new ItemStack(ModItems.FIRE_PANTS.get()).isDamageableItem() && ModItems.BEDROCK.get().isFireResistant(),"Rewards must remain durable and Bedrock must survive fire"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void removingMaskStripsItsHiddenEffectUnderStrongerPotions(GameTestHelper h) {
        var p=online(h);
        try {
            p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
            DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,100,2));
            h.assertTrue(p.getEffect(MobEffects.WATER_BREATHING).save(new CompoundTag()).contains("HiddenEffect"),"Vanilla hides the infinite equipment effect beneath a stronger potion");
            p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
            var effect=p.getEffect(MobEffects.WATER_BREATHING);
            h.assertTrue(effect!=null && effect.getAmplifier()==2 && effect.getDuration()==100,"Stronger external potion is preserved");
            h.assertTrue(!effect.save(new CompoundTag()).contains("HiddenEffect"),"The removed mask cannot reappear as a permanent hidden effect"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void referenceTooltipsAreExactAndArmorIsEquipmentOnly(GameTestHelper h) {
        Map<Item,String> expected=Map.of(ModItems.ENTITY_CRAMMER.get(),"Things are about to get extremely crowded in here.",
                ModItems.CACTUS_CHESTPLATE.get(),"This armor has one simple rule: hit me, regret it.",
                ModItems.FIRE_PANTS.get(),"Liar, liar, pants on fire—now with actual consequences.",
                ModItems.ENDER_PEARL_SHIELD.get(),"Turns defense into complete teleportation chaos.",
                ModItems.SWARM_OF_ANVILS.get(),"This might be the loudest way to solve a problem.");
        expected.forEach((item,text)->{
            List<net.minecraft.network.chat.Component> lines=new ArrayList<>();
            item.appendHoverText(new ItemStack(item),h.getLevel(),lines,TooltipFlag.NORMAL);
            h.assertTrue(String.join(" ",lines.stream().map(net.minecraft.network.chat.Component::getString).toList()).equals(text),"Tooltip must exactly retain reference punctuation and wording");
        });
        var p=online(h);
        try {
            p.getInventory().add(new ItemStack(ModItems.CACTUS_CHESTPLATE.get()));
            var inventory=new LivingHurtEvent(p,p.damageSources().cactus(),2); ChaosAbilities.immunity(inventory);
            h.assertTrue(!inventory.isCanceled(),"Cactus chestplate must be worn to protect its owner");
            p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.CACTUS_CHESTPLATE.get()));
            var worn=new LivingHurtEvent(p,p.damageSources().cactus(),2); ChaosAbilities.immunity(worn);
            h.assertTrue(worn.isCanceled(),"Worn cactus armor grants its matching immunity"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void firePantsRampCapAndStepReset(GameTestHelper h) {
        var p=online(h);
        try {
            p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.FIRE_PANTS.get()));
            p.setSprinting(true); p.setDeltaMovement(.1, 0, 0); p.horizontalCollision=false; p.setOnGround(true);
            double base=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            for (int i=0;i<30;i++) fireTick(p);
            double halfway=p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            for (int i=0;i<50;i++) fireTick(p);
            h.assertTrue(FirePantsAbility.ramp(p)==60 && halfway>base && p.getAttributeValue(Attributes.MOVEMENT_SPEED)>halfway,"Sprint accelerates gradually then caps");
            h.assertTrue(p.getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get())>=.4,"Only full speed enables one-block stepping");
            p.setSprinting(false); fireTick(p);
            h.assertTrue(FirePantsAbility.ramp(p)==0 && p.getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get())==0,"Stopping removes both ramp and step ability");
            h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED)<halfway,"Stopping removes the speed bonus"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=65) public static void moltenTrailWaitsThirtyTicksAndPersists(GameTestHelper h) {
        var p=online(h); BlockPos pos=h.absolutePos(new BlockPos(3,2,3)); h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(TemporaryTerrain.put(p,pos,Blocks.OBSIDIAN.defaultBlockState(),Blocks.LAVA.defaultBlockState(),30,"molten_trail"),"Stone becomes obsidian immediately");
        var saved=TemporaryTerrain.get(p.server).save(new CompoundTag());
        h.assertTrue(TemporaryTerrain.load(saved).save(new CompoundTag()).getList("Cells",10).size()==saved.getList("Cells",10).size(),"Scheduled transition survives save/load");
        h.runAfterDelay(20,()->h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.OBSIDIAN),"Trail stays obsidian for the first second"));
        h.runAfterDelay(33,()->{try { h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.LAVA),"Trail becomes real lava after 1.5 seconds"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena",timeoutTicks=50) public static void lavaWalkingCreatesCollisionAndRestoresLava(GameTestHelper h) {
        var p=online(h); BlockPos lava=h.absolutePos(new BlockPos(5,2,5)); h.getLevel().setBlock(lava,Blocks.LAVA.defaultBlockState(),3);
        p.setPos(lava.getX()+.5,lava.getY()+1,lava.getZ()+.5); p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ModItems.FIRE_PANTS.get())); fireTick(p);
        h.assertTrue(h.getLevel().getBlockState(lava).is(Blocks.OBSIDIAN),"Lava walking has actual solid collision beneath the wearer");
        p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY); p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(7,2,7))));
        h.runAfterDelay(12,()->{try { h.assertTrue(h.getLevel().getBlockState(lava).is(Blocks.LAVA),"Leaving returns the surface to lava"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena") public static void terrainChangesRespectContainersAndSurviveCageCleanup(GameTestHelper h) {
        var p=online(h); BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
        try {
            h.setBlock(3,2,3,Blocks.CHEST);
            h.assertTrue(!TemporaryTerrain.put(p,pos,Blocks.OBSIDIAN.defaultBlockState(),Blocks.LAVA.defaultBlockState(),30,"molten_trail"),"Containers cannot be melted");
            h.setBlock(3,2,3,Blocks.STONE);
            h.assertTrue(TemporaryTerrain.put(p,pos,Blocks.GLASS_PANE.defaultBlockState(),Blocks.STONE.defaultBlockState(),100,"crammer.test"),"Cage can replace solid terrain");
            TemporaryTerrain.finish(h.getLevel(),"crammer.test");
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE),"Cage cleanup restores replaced terrain");
            p.setGameMode(GameType.ADVENTURE);
            h.assertTrue(!TemporaryTerrain.put(p,pos,Blocks.OBSIDIAN.defaultBlockState(),Blocks.LAVA.defaultBlockState(),30,"molten_trail"),"Adventure mode cannot change terrain"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=540) public static void crammerCopiesEquipmentThenBurstsAndKillsAfterHalfSecond(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.ZOMBIE,new BlockPos(5,2,7)); mob.setNoAi(true);
        ItemStack helmet=new ItemStack(Items.DIAMOND_HELMET); helmet.setHoverName(net.minecraft.network.chat.Component.literal("Original helmet"));
        mob.setItemSlot(EquipmentSlot.HEAD,helmet); mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        BlockPos wall=mob.blockPosition().west(); h.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(CrammerAbility.start(p,mob),"Crammer must build even when its glass replaces existing stone");
        h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.GLASS_PANE),"Thin glass forms the cage wall");
        var pane = h.getLevel().getBlockState(wall);
        h.assertTrue(pane.getValue(IronBarsBlock.NORTH) && pane.getValue(IronBarsBlock.SOUTH)
                && !pane.getValue(IronBarsBlock.EAST), "Wall panes connect along their neighbors without an inward cross");
        h.assertTrue(h.getLevel().getBlockState(mob.blockPosition().above(3)).isAir(), "Cage has no full-block roof");
        h.runAfterDelay(25,()->{
            var clones=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,mob.getBoundingBox().inflate(1),m->m!=mob);
            h.assertTrue(clones.size() >= 4,"Fourfold spawn rate produces at least four copies in 25 ticks");
            h.assertTrue(clones.stream().anyMatch(m->ItemStack.matches(m.getItemBySlot(EquipmentSlot.HEAD),helmet)
                    && m.getMainHandItem().is(Items.IRON_SWORD)),"Clones retain equipment and custom item data");
        });
        h.runAfterDelay(123,()->{
            h.assertTrue(h.getLevel().getBlockState(wall).isAir(),"Crowding shatters glass and leaves an opening for the launch: " + h.getLevel().getBlockState(wall) + ", original alive=" + mob.isAlive());
            h.assertTrue(mob.isAlive() && mob.getDeltaMovement().lengthSqr()>.01,"Burst launches living mobs before their delayed death");
        });
        h.runAfterDelay(156,()->{try { h.assertTrue(!mob.isAlive(),"Original dies half a second after glass bursts");
            h.assertTrue(h.getLevel().getBlockState(wall).is(Blocks.STONE),"Replaced terrain returns after the launch has cleared the opening");
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class,new AABB(wall).inflate(30),m->m.getPersistentData().getBoolean("deathsareitems.CramClone") && m.isAlive()).isEmpty(),"All copies also die"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena",timeoutTicks=55) public static void cactusPinsRetaliateAndRestoreOriginalFlags(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(false); mob.setHealth(5);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.CACTUS_CHESTPLATE.get()));
        CactusAbility.retaliate(new LivingHurtEvent(p,p.damageSources().mobAttack(mob),2));
        h.assertTrue(mob.isNoAi() && mob.isNoGravity(),"Chestplate immobilizes the attacking mob");
        var visuals=h.getLevel().getEntitiesOfClass(Display.BlockDisplay.class,mob.getBoundingBox().inflate(8),e->e.saveWithoutId(new CompoundTag()).getCompound("block_state").getString("Name").equals("deathsareitems:cactus_spike"));
        h.assertTrue(visuals.size()==3,"Three angled cactus spikes launch from overhead");
        h.runAfterDelay(35,()->{try { h.assertTrue(!mob.isAlive(),"Impaled mob takes repeated real cactus damage");
            h.assertTrue(!mob.isNoAi() && !mob.isNoGravity(),"Cleanup restores original AI and gravity"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena",timeoutTicks=60,batch="deaths3-cannon") public static void cannonUsesGravityGroundDetonationAndProtectsShooter(GameTestHelper h) {
        var p=online(h); for (int i=0;i<65;i++) p.tick(); p.setGameMode(GameType.SURVIVAL); p.setHealth(20);
        BlockPos floor=h.absolutePos(new BlockPos(5,1,5)); h.getLevel().setBlock(floor,Blocks.STONE.defaultBlockState(),3);
        p.setPos(floor.getX()+.5,floor.getY()+1,floor.getZ()+.5); p.setXRot(85); p.setYRot(0);
        h.assertTrue(CannonAbility.fire(p),"Cannon fires a real TNT projectile");
        var tnts=h.getLevel().getEntitiesOfClass(CannonTntEntity.class,p.getBoundingBox().inflate(5));
        h.assertTrue(tnts.size()==1 && tnts.get(0).getFuse()==60 && !tnts.get(0).isNoGravity(),"Projectile has gravity and a three-second fuse");
        h.runAfterDelay(15,()->{try { h.assertTrue(!tnts.get(0).isAlive(),"Ground collision detonates before three seconds");
            h.assertTrue(p.getHealth()==20 && p.getDeltaMovement().y>0,"Downward shot launches its shooter without blast damage");
            p.fallDistance = 9;
            CannonAbility.boost(p, p.position().add(0, -1, 0));
            h.assertTrue(p.fallDistance == 9, "Cannon boost must preserve accumulated fall distance");
            float health = p.getHealth(); p.invulnerableTime = 0;
            p.hurt(p.damageSources().fall(), 3);
            h.assertTrue(p.getHealth() < health, "Cannon jumps still take real fall damage"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena",batch="deaths3-bed") public static void bedrockPlacesTwoHalvesAndSurvivesItsOwnExplosion(GameTestHelper h) {
        var p=online(h); p.setGameMode(GameType.SURVIVAL);
        try {
            BlockPos foot=h.absolutePos(new BlockPos(4,2,4)); var block=(co.blumeplugins.deathsareitems.block.custom.BedrockBedBlock)ModBlocks.BEDROCK.get();
            var state=block.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,BedPart.FOOT);
            h.getLevel().setBlock(foot,state,3); block.setPlacedBy(h.getLevel(),foot,state,p,new ItemStack(ModItems.BEDROCK.get()));
            BlockPos head=foot.south(); h.assertTrue(h.getLevel().getBlockState(head).getValue(BedBlock.PART)==BedPart.HEAD,"Bed uses vanilla two-block placement");
            BlockPos nearby=head.east(2); h.getLevel().setBlock(nearby,Blocks.DIRT.defaultBlockState(),3);
            p.setPos(head.getX()+.5,head.getY()+.6,head.getZ()+.5); p.setHealth(20); Vec3 before=p.getDeltaMovement();
            block.use(state,h.getLevel(),foot,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(foot),Direction.UP,foot,false));
            h.assertTrue(h.getLevel().getBlockState(foot).is(block) && h.getLevel().getBlockState(head).is(block),"Both halves survive their large explosion");
            h.assertTrue(p.getHealth()==20 && p.getDeltaMovement().equals(before),"Bed activator takes neither blast damage nor knockback");
            h.assertTrue(!h.getLevel().getBlockState(nearby).is(Blocks.DIRT),"The indestructible bed must not absorb its own blast; surrounding terrain really explodes"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena") public static void shieldThrowsFromEitherHandWithoutVanillaDamage(GameTestHelper h) {
        var p=online(h);
        try {
            p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModItems.ENDER_PEARL_SHIELD.get()));
            h.assertTrue(PearlAbility.holdsShield(p) && PearlAbility.throwPearl(p),"Offhand shield launches a real pearl");
            var pearls=h.getLevel().getEntitiesOfClass(ChaosPearlEntity.class,p.getBoundingBox().inflate(2));
            h.assertTrue(pearls.size()==1 && pearls.get(0).getOwner()==p,"Pearl belongs to its shooter");
            p.setHealth(20); Vec3 target=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3)));
            PearlAbility.land(pearls.get(0),null,null,target);
            h.assertTrue(p.getHealth()==20 && p.distanceToSqr(target)<1,"Shield teleport applies no pearl impact damage");
            pearls.get(0).discard();
            var vanilla=EntityType.ENDER_PEARL.create(p.level()); var event=new EntityTeleportEvent.EnderPearl(p,target.x,target.y,target.z,vanilla,5,null);
            PearlAbility.vanillaPearl(event); h.assertTrue(event.getAttackDamage()==0,"Held shield also protects vanilla pearl damage"); h.succeed();
        } finally { leave(p); }
    }
    @GameTest(template="test_arena",timeoutTicks=235) public static void shieldEmitsSixtyRealPearlsAndQueuesDistinctTeleports(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(true); float health=mob.getHealth();
        p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ModItems.ENDER_PEARL_SHIELD.get()));
        PearlAbility.defend(new LivingHurtEvent(p,p.damageSources().mobAttack(mob),2));
        var pearls=h.getLevel().getEntitiesOfClass(ChaosPearlEntity.class,mob.getBoundingBox().inflate(2));
        h.assertTrue(pearls.size()==60,"Retaliation launches sixty actual pearls from the attacker");
        h.assertTrue(pearls.stream().map(e->e.getDeltaMovement().toString()).distinct().count()==60,"Every pearl has a distinct flight direction");
        Vec3 first=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,3))),second=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(7,2,3)));
        PearlAbility.land(pearls.get(0),mob.getUUID(),pearls.get(0).burst(),first); pearls.get(0).discard();
        PearlAbility.land(pearls.get(1),mob.getUUID(),pearls.get(1).burst(),second); pearls.get(1).discard();
        h.runAfterDelay(5,()->h.assertTrue(mob.getHealth()<health,"Queued landings apply real damage on separate ticks"));
        h.runAfterDelay(225,()->{try { h.assertTrue(!mob.isAlive(),"Sixty impacts finish the attacking mob"); h.succeed(); } finally { leave(p); }});
    }
    @GameTest(template="test_arena") public static void endEntryPersistsHardcoreAndNextDeathSuppressesRewards(GameTestHelper h) {
        var p=online(h);
        try {
            EndHardcore.dimension(new PlayerEvent.PlayerChangedDimensionEvent(p,Level.OVERWORLD,Level.END));
            h.assertTrue(EndHardcore.active(p),"End entry marks this player permanently");
            var death=new net.minecraftforge.event.entity.living.LivingDeathEvent(p,p.damageSources().cactus());
            EndHardcore.death(death); RewardManager.onDeath(death);
            h.assertTrue(!RewardSavedData.get(p.server).offers.containsKey(p.getUUID()),"No reward revives a hardcore death");
            EndHardcore.respawn(new PlayerEvent.PlayerRespawnEvent(p,false));
            h.assertTrue(p.isSpectator(),"The next death leads to spectator mode");
            p.setGameMode(GameType.SURVIVAL); EndHardcore.login(new PlayerEvent.PlayerLoggedInEvent(p));
            h.assertTrue(p.isSpectator(),"Reconnecting cannot escape permanent death"); h.succeed();
        } finally { leave(p); }
    }
}
