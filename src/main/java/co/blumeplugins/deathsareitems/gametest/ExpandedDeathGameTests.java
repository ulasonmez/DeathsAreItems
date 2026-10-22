package co.blumeplugins.deathsareitems.gametest;

import co.blumeplugins.deathsareitems.ability.*;
import co.blumeplugins.deathsareitems.block.ModBlocks;
import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("deathsareitems")
@PrefixGameTestTemplate(false)
public final class ExpandedDeathGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"expanded-test"));
        p.setGameMode(GameType.SURVIVAL); p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(5,2,5)))); return p;
    }
    @GameTest(template="test_arena") public static void sevenDeathsHaveOneSelectableReward(GameTestHelper h) {
        h.assertTrue(DeathRewards.ids().size()==7,"Seven death types must be registered");
        Set<Item> items=new HashSet<>();
        for(var id:DeathRewards.ids()) {
            var definition=DeathRewards.get(id);
            h.assertTrue(definition.options().size()==3 && definition.options().stream().filter(RewardOption::selectable).count()==1,"Exactly one of three choices must be selectable");
            var reward=definition.options().stream().filter(RewardOption::selectable).findFirst().orElseThrow().createReward();
            h.assertTrue(!reward.isEmpty() && items.add(reward.getItem()),"Every death awards a distinct real item");
        }
        h.succeed();
    }
    @GameTest(template="test_arena") public static void sourcesDistinguishSkeletonGolemAndAnvil(GameTestHelper h) {
        var p=player(h); var skeleton=EntityType.SKELETON.create(h.getLevel()); var golem=EntityType.IRON_GOLEM.create(h.getLevel());
        Map<DamageSource,net.minecraft.resources.ResourceLocation> expected=new LinkedHashMap<>();
        expected.put(p.damageSources().fall(),DeathRewards.FALL); expected.put(p.damageSources().drown(),DeathRewards.DROWN);
        expected.put(p.damageSources().inWall(),DeathRewards.SUFFOCATE); expected.put(p.damageSources().starve(),DeathRewards.STARVE);
        expected.put(p.damageSources().arrow(EntityType.ARROW.create(h.getLevel()),skeleton),DeathRewards.SKELETON);
        expected.put(p.damageSources().mobAttack(golem),DeathRewards.GOLEM);
        expected.put(new DamageSource(h.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FALLING_ANVIL)),DeathRewards.ANVIL);
        expected.forEach((source,id)->h.assertTrue(DeathRewards.forDeath(source).orElseThrow().id().equals(id),"Damage source must map to its own reward"));
        h.assertTrue(DeathRewards.forDeath(p.damageSources().arrow(EntityType.ARROW.create(h.getLevel()),p)).isEmpty(),"Player arrows are not skeleton arrows");
        h.assertTrue(DeathRewards.forDeath(p.damageSources().mobAttack(skeleton)).isEmpty(),"Skeleton melee does not award the bow"); h.succeed();
    }
    @GameTest(template="test_arena") public static void discoveryHistorySurvivesRestart(GameTestHelper h) {
        var data=new RewardSavedData(); var id=UUID.randomUUID();
        h.assertTrue(data.discover(id,DeathRewards.DROWN),"First discovery is new");
        h.assertTrue(!data.discover(id,DeathRewards.DROWN),"Duplicate discovery is rejected");
        data.discover(id,DeathRewards.STARVE); data.rejected.add(id);
        var reloaded=RewardSavedData.load(data.save(new CompoundTag()));
        h.assertTrue(!reloaded.discover(id,DeathRewards.DROWN) && !reloaded.discover(id,DeathRewards.STARVE),"Save/load must retain both discoveries");
        h.assertTrue(reloaded.discover(id,DeathRewards.ANVIL) && reloaded.rejected.contains(id),"New death stays available and rejection survives save"); h.succeed();
    }
    @GameTest(template="test_arena") public static void maskOnlyProtectsWhenWorn(GameTestHelper h) {
        var p=player(h); p.getInventory().add(new ItemStack(ModItems.DROWNING_MASK.get()));
        var noMask=new LivingHurtEvent(p,p.damageSources().drown(),5); DeathAbilities.immunity(noMask);
        h.assertTrue(!noMask.isCanceled(),"Inventory mask does not grant equipment abilities");
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
        var mask=new LivingHurtEvent(p,p.damageSources().drown(),5); DeathAbilities.immunity(mask);
        h.assertTrue(mask.isCanceled(),"Equipped mask prevents drowning");
        var fire=new LivingHurtEvent(p,p.damageSources().inFire(),5); DeathAbilities.immunity(fire);
        h.assertTrue(!fire.isCanceled(),"Mask does not prevent other damage"); h.succeed();
    }
    @GameTest(template="test_arena") public static void reserveFeedsBeforeNormalHunger(GameTestHelper h) {
        var p=player(h); p.getFoodData().setFoodLevel(18);
        p.getPersistentData().putInt(HungerAbility.RESERVE,5);
        HungerAbility.foodTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getFoodData().getFoodLevel()==20 && p.getPersistentData().getInt(HungerAbility.RESERVE)==3,"Two lost food points consume two reserve points");
        p.getFoodData().setFoodLevel(15); HungerAbility.foodTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.getFoodData().getFoodLevel()==18 && p.getPersistentData().getInt(HungerAbility.RESERVE)==0,"Only available reserve may be restored"); h.succeed();
    }
    @GameTest(template="test_arena") public static void maskBuffsOnlyWorkInWaterAndStopImmediatelyOnLand(GameTestHelper h) {
        var p=player(h); p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
        var effects=List.of(net.minecraft.world.effect.MobEffects.CONDUIT_POWER,net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE,net.minecraft.world.effect.MobEffects.WATER_BREATHING);
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(effects.stream().noneMatch(p::hasEffect),"Wearing a dry mask must not accelerate mining or grant water buffs");
        BlockPos water=p.blockPosition();
        h.getLevel().setBlock(water,Blocks.WATER.defaultBlockState(),3);
        h.getLevel().setBlock(water.above(),Blocks.WATER.defaultBlockState(),3);
        p.baseTick();
        h.assertTrue(p.isInWater(),"Test player is inside actual water");
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(effects.stream().allMatch(effect->p.hasEffect(effect) && p.getEffect(effect).getAmplifier()==2),"Water immediately activates all three level III mask buffs");
        p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(7,2,7)))); p.baseTick();
        h.assertTrue(!p.isInWater(),"Test player has left the water");
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(effects.stream().noneMatch(p::hasEffect),"Mask buffs disappear on the first dry tick instead of speeding up land mining");
        var potion=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING,6000);
        p.addEffect(potion);
        p.setPos(Vec3.atBottomCenterOf(water)); p.baseTick();
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING) && p.getEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING).getDuration()>5000,
                "Taking off the mask preserves independently acquired water breathing");
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.CONDUIT_POWER) && !p.hasEffect(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE),
                "Taking off the mask immediately removes its own buffs even underwater");
        h.succeed();
    }
    @GameTest(template="test_arena") public static void placingLastBlockCannotStartAMaskCharge(GameTestHelper h) {
        var input=new MaskChargeInput();
        input.update(false,false);
        h.assertTrue(!input.update(true,false),"Pressing use while holding a block does not charge");
        for(int i=0;i<10;i++) h.assertTrue(!input.update(true,true),"Consuming the last block while holding use cannot start a charge");
        input.update(false,true);
        h.assertTrue(input.update(true,true),"A fresh press with empty hands starts charging");
        h.assertTrue(input.update(true,true),"Keeping the same valid press held continues charging");
        h.assertTrue(!input.update(true,false) && !input.update(true,true),"Losing eligibility requires release before reactivation");
        input.update(false,true); h.assertTrue(input.update(true,true),"Releasing rearms the next deliberate press");
        input.reset(); h.assertTrue(!input.update(true,true),"Entering a world while holding use never auto-charges");
        h.succeed();
    }
    @GameTest(template="test_arena") public static void shrinkingAlsoChangesCollisionDimensions(GameTestHelper h) {
        var mob=h.spawn(EntityType.COW,new BlockPos(3,2,3)); float width=mob.getBbWidth(),height=mob.getBbHeight();
        mob.getPersistentData().putFloat(HungerAbility.SCALE,.5F); mob.refreshDimensions();
        h.assertTrue(Math.abs(mob.getBbWidth()-width*.5)<.01 && Math.abs(mob.getBbHeight()-height*.5)<.01,"Actual dimensions, not only rendering, must shrink");
        CompoundTag tag=mob.saveWithoutId(new CompoundTag());
        var restored=EntityType.COW.create(h.getLevel()); restored.load(tag);
        h.assertTrue(HungerAbility.scale(restored)==.5F,"Scale must persist in entity NBT"); h.succeed();
    }
    @GameTest(template="test_arena") public static void terrainToolsRespectContainersAndAdventure(GameTestHelper h) {
        var p=player(h); BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
        h.setBlock(3,2,3,Blocks.CHEST);
        h.assertTrue(!BoulderAbility.mark(p,pos) && !AnvilSwarmAbility.start(p,pos,null),"Container contents must survive both terrain abilities");
        h.setBlock(3,2,3,Blocks.BEDROCK);
        h.assertTrue(!BoulderAbility.mark(p,pos) && !AnvilSwarmAbility.start(p,pos,null),"Unbreakable blocks cannot be transformed");
        h.setBlock(3,2,3,Blocks.STONE); p.setGameMode(GameType.ADVENTURE);
        h.assertTrue(!BoulderAbility.mark(p,pos) && !AnvilSwarmAbility.start(p,pos,null),"Adventure players cannot change terrain"); h.succeed();
    }
    private static ServerPlayer online(GameTestHelper h) {
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"ability-test"));
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(new io.netty.channel.ChannelInitializer<io.netty.channel.Channel>() {
            @Override protected void initChannel(io.netty.channel.Channel channel) { channel.pipeline().addLast("packet_handler",connection); }
        });
        connection.setProtocol(net.minecraft.network.ConnectionProtocol.PLAY);
        connection.channel().attr(io.netty.util.AttributeKey.<String>valueOf("fml:netversion")).set(net.minecraftforge.network.NetworkConstants.NETVERSION);
        p.server.getPlayerList().placeNewPlayer(connection,p);
        p.teleportTo(h.getLevel(),h.absolutePos(new BlockPos(7,2,7)).getX()+.5,h.absolutePos(new BlockPos(7,2,7)).getY(),h.absolutePos(new BlockPos(7,2,7)).getZ()+.5,0,0);
        p.setGameMode(GameType.CREATIVE); return p;
    }
    @GameTest(template="test_arena",timeoutTicks=160) public static void hungerStealsExactlyFiveBites(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(true);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.HUNGER_STAFF.get()));
        h.assertTrue(HungerAbility.start(p,mob),"Staff starts a drain");
        h.assertTrue(mob.isCurrentlyGlowing() && mob.getTeam().getColor()==net.minecraft.ChatFormatting.RED,"Target must glow red");
        h.runAfterDelay(125,()->{
            try {
                h.assertTrue(Math.abs(HungerAbility.scale(mob)-.5F)<.01,"Five pulses shrink by ten percent each");
                h.assertTrue(p.getPersistentData().getInt(HungerAbility.RESERVE)==10,"Five arriving bites grant ten stored food points");
                h.assertTrue(!mob.isCurrentlyGlowing() && mob.getTeam()==null,"Temporary glow and team membership restore after five seconds");
                h.succeed();
            } finally { p.server.getPlayerList().remove(p); }
        });
    }
    @GameTest(template="test_arena",timeoutTicks=160) public static void hungerFillsNormalBarBeforeStoringOverflow(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(true);
        p.getFoodData().setFoodLevel(13);
        h.assertTrue(HungerAbility.start(p,mob),"Staff starts while the normal food bar is depleted");
        h.runAfterDelay(65,()->h.assertTrue(p.getFoodData().getFoodLevel()==17
                && p.getPersistentData().getInt(HungerAbility.RESERVE)==0,"First two bites refill normal hunger without creating reserve"));
        h.runAfterDelay(125,()->{
            try {
                h.assertTrue(p.getFoodData().getFoodLevel()==20 && p.getPersistentData().getInt(HungerAbility.RESERVE)==3,
                        "Ten stolen points first fill seven missing food points, then store exactly three overflow points");
                h.succeed();
            } finally { p.server.getPlayerList().remove(p); }
        });
    }
    @GameTest(template="test_arena",timeoutTicks=110) public static void boulderWaitsThreeSecondsAndOneBreakFreesMob(GameTestHelper h) {
        var p=online(h); BlockPos center=h.absolutePos(new BlockPos(3,2,3));
        List<BlockPos> converted=new ArrayList<>();
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,1,1))) { h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3); converted.add(pos.immutable()); }
        h.assertTrue(BoulderAbility.mark(p,center),"A normal stone block can be marked");
        h.runAfterDelay(40,()->h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.STONE),"Mark must not convert the block before three seconds"));
        h.runAfterDelay(62,()->{
            try {
                var state=h.getLevel().getBlockState(center);
                h.assertTrue(state.is(ModBlocks.FRAGILE_SAND.get()) || state.is(ModBlocks.FRAGILE_GRAVEL.get()),"Marked stone turns into fragile sediment");
                h.assertTrue(converted.stream().allMatch(pos->h.getLevel().getBlockState(pos).equals(state)),"Every marked cell changes together to exactly one material");
                var mob=h.spawn(EntityType.COW,new BlockPos(5,2,7)); mob.setNoAi(true);
                h.assertTrue(BoulderAbility.trap(p,mob),"Mob gets a height-matched cage");
                BlockPos bottom=mob.blockPosition();
                h.assertTrue(h.getLevel().getBlockState(bottom).equals(h.getLevel().getBlockState(bottom.above())),"Mob cage also uses one consistent sediment");
                h.getLevel().destroyBlock(bottom,false);
                BoulderAbility.tick(new TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER,TickEvent.Phase.END,h.getLevel(),()->true));
                h.assertTrue(h.getLevel().getBlockState(bottom.above()).isAir(),"Breaking one cage block removes the remaining head block"); h.succeed();
            } finally { p.server.getPlayerList().remove(p); }
        });
    }
    @GameTest(template="test_arena",timeoutTicks=340) public static void anvilSwarmFinishesWithInfinity255Loot(GameTestHelper h) {
        var p=online(h); var center=h.absolutePos(new BlockPos(3,2,3)); h.setBlock(3,2,3,Blocks.DIAMOND_BLOCK);
        h.assertTrue(AnvilSwarmAbility.start(p,center,null),"Swarm starts on an ordinary block");
        h.runAfterDelay(60,()->{
            var flying=h.getLevel().getEntitiesOfClass(Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(center).inflate(12),
                    d->d.getY()>center.getY()+2 && d.saveWithoutId(new CompoundTag()).getCompound("block_state").getString("Name").equals("minecraft:anvil"));
            h.assertTrue(!flying.isEmpty(),"Anvils must start above the target");
            Map<Display.BlockDisplay,Vec3> starts=new HashMap<>();
            Map<Display.BlockDisplay,Tag> rotations=new HashMap<>();
            for(var anvil:flying) {
                starts.put(anvil,anvil.position());
                rotations.put(anvil,anvil.saveWithoutId(new CompoundTag()).getCompound("transformation").get("left_rotation").copy());
            }
            h.runAfterDelay(15,()->{
                for(var anvil:flying) {
                    h.assertTrue(anvil.getY()<starts.get(anvil).y,"Every flying anvil travels downward");
                    h.assertTrue(rotations.get(anvil).equals(anvil.saveWithoutId(new CompoundTag()).getCompound("transformation").get("left_rotation")),
                            "Flight rotation remains unchanged when the anvil embeds");
                }
            });
        });
        h.runAfterDelay(280,()->h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.DIAMOND_BLOCK),"Block survives until the end of the fifteen-second swarm"));
        h.runAfterDelay(295,()->{
            var anvils=h.getLevel().getEntitiesOfClass(Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(center).inflate(1),d->d.saveWithoutId(new CompoundTag()).getCompound("block_state").getString("Name").equals("minecraft:anvil"));
            h.assertTrue(anvils.size()>=150,"The full surface grid must densely cover the block");
            Set<String> angles=new HashSet<>();
            for(var anvil:anvils) {
                var tag=anvil.saveWithoutId(new CompoundTag());
                var transform=com.mojang.math.Transformation.EXTENDED_CODEC.parse(NbtOps.INSTANCE,tag.get("transformation")).result().orElseThrow();
                h.assertTrue(Math.abs(transform.getScale().x()-.48F)<.001,"Both flying and embedded anvils use twice the original scale");
                var up=transform.getLeftRotation().transform(new org.joml.Vector3f(0,1,0));
                h.assertTrue(up.y>.5F,"Embedded anvils preserve a descending approach even on lower faces");
                angles.add(Math.round(up.x*20)+":"+Math.round(up.y*20)+":"+Math.round(up.z*20));
            }
            h.assertTrue(angles.size()>30,"Approach angles vary continuously instead of using six fixed directions");
            for(var face:net.minecraft.core.Direction.values()) {
                var normal=Vec3.atLowerCornerOf(face.getNormal()); var middle=Vec3.atCenterOf(center);
                h.assertTrue(anvils.stream().anyMatch(a->a.position().subtract(middle).dot(normal)>.55),"Anvils must embed on face "+face);
            }
        });
        h.runAfterDelay(310,()->{
            try {
                h.assertTrue(h.getLevel().getBlockState(center).isAir(),"Block is destroyed at the end of the swarm");
                var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(3),e->e.getItem().is(Items.DIAMOND_BLOCK));
                h.assertTrue(!drops.isEmpty(),"Swarm must drop a diamond block; nearby items="+h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(10)).stream().map(e->e.getItem().toString()).toList());
                h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS,drops.get(0).getItem())==255,"Block drop carries Infinity 255; actual="+drops.get(0).getItem().getTag()); h.succeed();
            } finally { p.server.getPlayerList().remove(p); }
        });
    }
    @GameTest(template="test_arena",timeoutTicks=150) public static void maskTridentDrownsTheAimedMob(GameTestHelper h) {
        var p=online(h); p.setYRot(180); p.setXRot(0);
        var target=h.spawn(EntityType.COW,new BlockPos(7,2,3)); target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
        DeathAbilities.input(p,true);
        var tridents=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownTrident.class,p.getBoundingBox().inflate(10));
        h.assertTrue(tridents.size()==1 && tridents.get(0).isCurrentlyGlowing(),"An empty-handed mask wearer charges one glowing trident");
        DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
        DeathAbilities.input(p,false);
        // Embedded clients send no movement packets, which normally drive ServerPlayer's tick.
        h.onEachTick(()->DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p)));
        h.runAfterDelay(8,()->h.assertTrue(target.getHealth()==100,"Trident impact deals no immediate vanilla damage"));
        h.runAfterDelay(125,()->{
            try {
                h.assertTrue(Math.abs(target.getHealth()-75)<.01,"Released trident hits the aimed target and applies drowning pulses; target="+target.position()+" owner="+p.position()+" trident="+tridents.get(0).position()+" removed="+tridents.get(0).isRemoved()+" lineOfSight="+p.hasLineOfSight(target));
                h.assertTrue(tridents.get(0).isRemoved(),"Trident visual disappears on impact"); h.succeed();
            } finally { p.server.getPlayerList().remove(p); }
        });
    }
    @GameTest(template="test_arena") public static void rapidArrowsNeverConsumeAmmunition(GameTestHelper h) {
        var p=player(h); p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.SKELETON_BOW.get()));
        p.getInventory().setItem(8,new ItemStack(Items.ARROW,13));
        for(int i=0;i<5;i++) DeathAbilities.shootArrow(p);
        var arrows=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,p.getBoundingBox().inflate(2),e->e.getOwner()==p);
        h.assertTrue(arrows.size()==5 && p.getInventory().getItem(8).getCount()==13,"Every use tick spawns an arrow without consuming inventory arrows");
        h.assertTrue(arrows.stream().allMatch(a->a.pickup==net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED),"Free arrows cannot be collected");
        arrows.forEach(Entity::discard); h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=20) public static void sedimentSurvivesWalkingAndJumping(GameTestHelper h) {
        var p=player(h); BlockPos support=p.blockPosition().below();
        h.getLevel().setBlock(support,ModBlocks.FRAGILE_GRAVEL.get().defaultBlockState(),3);
        var state=h.getLevel().getBlockState(support);
        state.getBlock().stepOn(h.getLevel(),support,state,p);
        state.getBlock().fallOn(h.getLevel(),state,support,p,2);
        state.getBlock().entityInside(state,h.getLevel(),support,p);
        p.jumpFromGround();
        h.runAfterDelay(5,()->{
            h.assertTrue(h.getLevel().getBlockState(support).is(ModBlocks.FRAGILE_GRAVEL.get()),"Walking, jumping and touching must not crumble sediment"); h.succeed();
        });
    }
    @GameTest(template="test_arena") public static void maskChargesWithoutTargetAndCannotBeCollected(GameTestHelper h) {
        var p=player(h); p.setXRot(-90);
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
        h.assertTrue(DeathAbilities.target(p,32)==null,"Test must aim at empty sky");
        DeathAbilities.input(p,true);
        var tridents=h.getLevel().getEntitiesOfClass(co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.class,p.getBoundingBox().inflate(10),e->e.getOwner()==p);
        h.assertTrue(tridents.size()==1,"Empty sky still charges a mask trident");
        var trident=tridents.get(0); trident.playerTouch(p);
        h.assertTrue(p.getInventory().countItem(Items.TRIDENT)==0 && !trident.isRemoved(),"Even the creative owner cannot pick up this trident");
        h.assertTrue(Math.abs(trident.getBbWidth()-.25F)<.001,"Mask trident has a half-sized collision box");
        DeathAbilities.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p)); h.succeed();
    }
    @GameTest(template="test_arena") public static void stationaryMaskTridentNeverReorientsItself(GameTestHelper h) {
        var p=player(h);
        var trident=new co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity(h.getLevel(),p);
        trident.pointAlong(new Vec3(1,0,0));
        float yaw=trident.getYRot(),pitch=trident.getXRot(); Vec3 position=trident.position();
        for(int i=0;i<10;i++) {
            trident.tick();
            h.assertTrue(Math.abs(trident.getYRot()-yaw)<.001 && Math.abs(trident.getXRot()-pitch)<.001,
                    "Zero velocity must never overwrite the horizontal spear rotation");
            h.assertTrue(trident.position().distanceToSqr(position)<.000001,"Stationary spear must not drift");
        }
        h.assertTrue(pitch==0 && yaw==90,"A horizontal direction keeps zero pitch");
        trident.discard(); h.succeed();
    }
    @GameTest(template="test_arena") public static void chargingSpearAnchorDoesNotLagBehindCameraTurns(GameTestHelper h) {
        var p=player(h); p.setYRot(0); p.setXRot(0);
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.DROWNING_MASK.get()));
        DeathAbilities.input(p,true);
        var trident=h.getLevel().getEntitiesOfClass(co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.class,
                p.getBoundingBox().inflate(10),e->e.getOwner()==p).get(0);
        try {
            p.setYRot(90);
            DeathAbilities.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
            Vec3 offset=trident.position().subtract(p.getEyePosition());
            h.assertTrue(Math.abs(offset.x+1.25)<.001 && Math.abs(offset.z)<.001 && Math.abs(offset.y+.28)<.001,
                    "Held spear stays centered below the crosshair with no right-hand offset");
            h.assertTrue(trident.isCharging() && trident.chargeOwner()==p,"Charge pose synchronizes its owner and held state");
            DeathAbilities.input(p,false);
            h.assertTrue(!trident.isCharging(),"Release immediately switches to the flying pose");
            h.succeed();
        } finally { DeathAbilities.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p)); }
    }
    @GameTest(template="test_arena") public static void spearAimHasLongerLagAndSmoothlyHandlesTurningAround(GameTestHelper h) {
        var first=co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.followAim(new Vec3(0,0,1),new Vec3(1,0,0));
        double degrees=Math.toDegrees(Math.acos(first.z));
        h.assertTrue(degrees>6 && degrees<8,"A ninety-degree camera turn moves the spear only about seven degrees on its first tick");
        Vec3 aim=new Vec3(0,0,1), target=new Vec3(0,0,-1);
        for(int i=0;i<80;i++) {
            Vec3 next=co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.followAim(aim,target);
            h.assertTrue(aim.dot(next)>.96,"A full turnaround never snaps or stalls at an opposite direction"); aim=next;
        }
        h.assertTrue(aim.dot(target)>.999,"Delayed aim eventually converges on the player's new view");
        var p=player(h); p.setYRot(35); p.setXRot(-70);
        Vec3 look=Vec3.directionFromRotation(p.getXRot(),p.getYRot());
        Vec3 right=new Vec3(-Math.cos(Math.toRadians(p.getYRot())),0,-Math.sin(Math.toRadians(p.getYRot())));
        Vec3 offset=co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity.heldPosition(p,1).subtract(p.getEyePosition());
        h.assertTrue(Math.abs(offset.dot(right))<.001 && Math.abs(offset.dot(look)-1.25)<.001
                && Math.abs(offset.dot(look.cross(right))-.28)<.001,"Anchor remains directly below the crosshair even when aiming steeply upward");
        h.succeed();
    }
    @GameTest(template="test_arena") public static void skeletonBowFiresHalfAsOftenAndHitsForThree(GameTestHelper h) {
        var p=player(h); ItemStack bow=new ItemStack(ModItems.SKELETON_BOW.get());
        p.setItemSlot(EquipmentSlot.MAINHAND,bow);
        for(int remaining=72000;remaining>71980;remaining--) bow.getItem().onUseTick(h.getLevel(),p,bow,remaining);
        var arrows=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Arrow.class,p.getBoundingBox().inflate(2),e->e.getOwner()==p);
        h.assertTrue(arrows.size()==10,"Twenty use ticks must emit ten arrows");
        var mob=h.spawn(EntityType.COW,new BlockPos(4,2,4)); mob.setNoAi(true); float health=mob.getHealth();
        DeathAbilities.arrowImpact(new net.minecraftforge.event.entity.ProjectileImpactEvent(arrows.get(0),new net.minecraft.world.phys.EntityHitResult(mob)));
        h.assertTrue(Math.abs(mob.getHealth()-(health-3))<.01,"Each arrow deals three damage, half of the previous six");
        arrows.forEach(Entity::discard); h.succeed();
    }
    @GameTest(template="test_arena") public static void pickaxeTogglesImmediatelyWithoutCooldown(GameTestHelper h) {
        var p=player(h); ItemStack pick=new ItemStack(ModItems.MOB_PICKAXE.get());
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        pick.getItem().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(pick.getOrCreateTag().getBoolean(co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem.ENABLED),"First click enables morphing");
        pick.getItem().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!pick.getOrCreateTag().getBoolean(co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem.ENABLED) && !p.getCooldowns().isOnCooldown(pick.getItem()),"Second click in the same tick disables morphing without cooldown"); h.succeed();
    }
    @GameTest(template="test_arena") public static void pickaxeHitsHardWithoutLosingDamageToHurtImmunity(GameTestHelper h) {
        var p=player(h); ItemStack pick=new ItemStack(ModItems.MOB_PICKAXE.get()); p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        var damage=net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
        double attack=1+pick.getAttributeModifiers(EquipmentSlot.MAINHAND).get(damage).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
        var mob=h.spawn(EntityType.IRON_GOLEM,new BlockPos(5,2,6)); mob.setNoAi(true);
        float before=mob.getHealth();
        h.assertTrue(mob.hurt(p.damageSources().playerAttack(p),(float)attack),"Pickaxe hit succeeds so normal attack feedback and knockback still apply");
        h.assertTrue(Math.abs(before-mob.getHealth()-40)<.01,"A full pickaxe hit deals forty damage through the real attack event pipeline");
        h.assertTrue(mob.getLastDamageSource()!=null && mob.getLastDamageSource().is(DamageTypes.PLAYER_ATTACK),"Only one normal player attack reaches the damage pipeline");
        h.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState())==new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()),
                "Stone mining uses the ordinary iron pickaxe speed");
        mob.discard(); h.succeed();
    }
    @GameTest(template="test_arena",timeoutTicks=30) public static void minedOneBlockTunnelStillSpawnsAMob(GameTestHelper h) {
        var p=player(h); var center=h.absolutePos(new BlockPos(3,3,3));
        ItemStack pick=new ItemStack(ModItems.MOB_PICKAXE.get()); pick.getOrCreateTag().putBoolean(co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem.ENABLED,true);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,-2,-3),center.offset(3,3,3))) h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),center,h.getLevel().getBlockState(center),p);
        DeathAbilities.mining(event);
        h.assertTrue(h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(center)).isEmpty(),"Spawn waits for the block break to finish");
        h.getLevel().setBlock(center,Blocks.AIR.defaultBlockState(),3);
        h.runAfterDelay(5,()->{
            var mobs=h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(center));
            h.assertTrue(mobs.size()==1,"A random mob must spawn in the mined cell even if it does not fit");
            mobs.forEach(Entity::discard); h.succeed();
        });
    }
    @GameTest(template="test_arena",timeoutTicks=30) public static void pickaxeSpawnsEvenWhenMinedCellBecomesSolid(GameTestHelper h) {
        var p=player(h); var center=h.absolutePos(new BlockPos(3,3,3));
        ItemStack pick=new ItemStack(ModItems.MOB_PICKAXE.get());
        pick.getOrCreateTag().putBoolean(co.blumeplugins.deathsareitems.item.custom.MobPickaxeItem.ENABLED,true);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,-2,-3),center.offset(3,3,3)))
            h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        DeathAbilities.mining(new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(),center,h.getLevel().getBlockState(center),p));
        h.getLevel().setBlock(center,Blocks.AIR.defaultBlockState(),3);
        // Another block fills the mined cell before the deferred spawn; collision must not cancel it.
        h.getLevel().setBlock(center,Blocks.STONE.defaultBlockState(),3);
        h.runAfterDelay(5,()->{
            var mobs=h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(center));
            h.assertTrue(mobs.size()==1 && !h.getLevel().noCollision(mobs.get(0)),"A mob must still spawn inside solid blocks and may suffocate normally");
            mobs.forEach(Entity::discard); h.succeed();
        });
    }
    @GameTest(template="test_arena",timeoutTicks=90) public static void anvilVictimCannotMoveAndCancellationRestoresAi(GameTestHelper h) {
        var p=online(h); var mob=h.spawn(EntityType.COW,new BlockPos(4,2,4)); var anchor=mob.position();
        h.assertTrue(!mob.isNoAi() && !mob.isNoGravity(),"Test starts with a normally moving mob");
        h.assertTrue(AnvilSwarmAbility.start(p,null,mob),"Swarm starts on a mob");
        h.runAfterDelay(40,()->{
            mob.push(2,.8,1);
            AnvilSwarmAbility.livingTick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(mob));
            h.assertTrue(mob.position().distanceToSqr(anchor)<.00001 && mob.getDeltaMovement().lengthSqr()==0,"AI, knockback and gravity cannot move the victim");
            h.assertTrue(mob.isNoAi() && mob.isNoGravity(),"Mob stays frozen during the swarm");
            p.server.getPlayerList().remove(p);
        });
        h.runAfterDelay(45,()->{
            h.assertTrue(!mob.isNoAi() && !mob.isNoGravity(),"Canceling the swarm restores the original AI and gravity");
            mob.discard(); h.succeed();
        });
    }
    @GameTest(template="test_arena") public static void translatedTooltipsWrapOnlyAtWholeWords(GameTestHelper h) {
        String text="Moblar yardımcı olmuyor, ama işleri kesinlikle ilginçleştiriyor. Üç dişli de burada.";
        var lines=co.blumeplugins.deathsareitems.item.TooltipText.wrap(text);
        h.assertTrue(String.join(" ",lines).equals(text),"Wrapping preserves every translated word and Turkish character");
        h.assertTrue(lines.size()>1 && lines.stream().allMatch(line->line.codePointCount(0,line.length())<=42),"Tooltip lines stay compact");
        for(Item item:List.of(ModItems.FALL_DAMAGE_BOOTS.get(),ModItems.DROWNING_MASK.get(),ModItems.SUFFOCATION_BOULDER.get(),ModItems.HUNGER_STAFF.get(),ModItems.MOB_PICKAXE.get(),ModItems.SWARM_OF_ANVILS.get(),ModItems.SKELETON_BOW.get())) {
            List<net.minecraft.network.chat.Component> normal=new ArrayList<>(),advanced=new ArrayList<>();
            item.appendHoverText(new ItemStack(item),h.getLevel(),normal,TooltipFlag.NORMAL);
            item.appendHoverText(new ItemStack(item),h.getLevel(),advanced,TooltipFlag.ADVANCED);
            h.assertTrue(!normal.isEmpty() && normal.equals(advanced),"Item tooltip contains only its single wrapped description");
        }
        h.succeed();
    }
    @GameTest(template="test_arena") public static void namedAndToggledRewardsRetainImmunity(GameTestHelper h) {
        var p=player(h); ItemStack pick=new ItemStack(ModItems.MOB_PICKAXE.get());
        pick.setHoverName(net.minecraft.network.chat.Component.literal("My pickaxe")); pick.getOrCreateTag().putBoolean("deathsareitems.Morphing",true);
        p.getInventory().setItem(0,pick);
        var damage=new LivingHurtEvent(p,p.damageSources().mobAttack(EntityType.IRON_GOLEM.create(h.getLevel())),30);
        DeathAbilities.immunity(damage);
        h.assertTrue(damage.isCanceled(),"Custom names and toggle NBT must not disable the item's death immunity"); h.succeed();
    }
}
