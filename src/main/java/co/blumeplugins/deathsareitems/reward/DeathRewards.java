package co.blumeplugins.deathsareitems.reward;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public final class DeathRewards {
    private static final Map<ResourceLocation, RewardDefinition> DEFINITIONS = new LinkedHashMap<>();
    public static final ResourceLocation FALL = new ResourceLocation(DeathsAreItems.MOD_ID, "fall");
    public static final ResourceLocation DROWN = id("drown"), SUFFOCATE = id("suffocate"), SKELETON = id("skeleton"),
            GOLEM = id("golem"), STARVE = id("starve"), ANVIL = id("anvil"),
            CRAM = id("cram"), CACTUS = id("cactus"), TNT = id("tnt"), LAVA = id("lava"), BED = id("bed"), PEARL = id("pearl"), WITHER = id("wither");

    private static ResourceLocation id(String path) { return new ResourceLocation(DeathsAreItems.MOD_ID, path); }
    public static List<ResourceLocation> ids() { return List.copyOf(DEFINITIONS.keySet()); }

    private DeathRewards() {}

    public static void bootstrap() {
        register(new RewardDefinition(WITHER, source -> source.is(DamageTypes.WITHER), List.of(
                preview("withering_staff"), reward(ModItems.SOUL_STORM), preview("bone_hand"))));
        register(new RewardDefinition(PEARL, source -> source.is(co.blumeplugins.deathsareitems.ability.DeathDamage.PEARL), List.of(
                preview("ender_pearl_necklace"), preview("ender_eyeballs"), reward(ModItems.ENDER_PEARL_SHIELD))));
        register(new RewardDefinition(CRAM, source -> source.is(DamageTypes.CRAMMING), List.of(
                preview("crammer_error"), reward(ModItems.ENTITY_CRAMMER), preview("squish_squash"))));
        register(new RewardDefinition(CACTUS, source -> source.is(DamageTypes.CACTUS), List.of(
                reward(ModItems.CACTUS_CHESTPLATE), preview("thorn_in_your_side"), preview("rusty_cactus"))));
        register(new RewardDefinition(TNT, source -> (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION))
                && source.getDirectEntity() instanceof net.minecraft.world.entity.item.PrimedTnt, List.of(
                reward(ModItems.TNT_CANNON), preview("tasty_tnt"), preview("blast_from_the_past"))));
        register(new RewardDefinition(LAVA, source -> source.is(DamageTypes.LAVA), List.of(
                preview("firewall"), reward(ModItems.FIRE_PANTS), preview("molten_socks"))));
        register(new RewardDefinition(BED, source -> source.is(DamageTypes.BAD_RESPAWN_POINT), List.of(
                preview("fluffy_pillow"), reward(ModItems.BEDROCK), preview("explosive_blankie"))));
        register(new RewardDefinition(FALL, source -> source.is(DamageTypes.FALL), List.of(
                RewardOption.reward("item.deathsareitems.fall_damage_boots", () -> new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get())),
                RewardOption.preview("reward.deathsareitems.long_trip"),
                RewardOption.preview("reward.deathsareitems.slippery_banana_peel"))));
        register(new RewardDefinition(DROWN, source -> source.is(DamageTypes.DROWN), List.of(
                reward(ModItems.DROWNING_MASK), preview("empty_scuba_tank"), preview("tiny_gills"))));
        register(new RewardDefinition(SUFFOCATE, source -> source.is(DamageTypes.IN_WALL), List.of(
                preview("bigger_lung_capacity"), reward(ModItems.SUFFOCATION_BOULDER), preview("cave_diver_toolkit"))));
        register(new RewardDefinition(SKELETON, source -> source.is(DamageTypes.ARROW)
                && source.getEntity() instanceof net.minecraft.world.entity.monster.Skeleton, List.of(
                preview("bowbowbow"), reward(ModItems.SKELETON_BOW), preview("arrowarrowarrow"))));
        register(new RewardDefinition(GOLEM, source -> source.getEntity() instanceof net.minecraft.world.entity.animal.IronGolem, List.of(
                preview("mob_on_a_stick"), reward(ModItems.MOB_PICKAXE), preview("iron_fist"))));
        register(new RewardDefinition(STARVE, source -> source.is(DamageTypes.STARVE), List.of(
                preview("hunger_employee"), preview("hunger_manager"), reward(ModItems.HUNGER_STAFF))));
        register(new RewardDefinition(ANVIL, source -> source.is(DamageTypes.FALLING_ANVIL), List.of(
                reward(ModItems.SWARM_OF_ANVILS), preview("rusty_sharpened_anvil"), preview("heavier_anvil"))));
    }

    private static RewardOption reward(net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item) {
        return RewardOption.reward("item.deathsareitems." + item.getId().getPath(), () -> new ItemStack(item.get()));
    }
    private static RewardOption preview(String name) { return RewardOption.preview("reward.deathsareitems." + name); }

    public static void register(RewardDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate death reward " + definition.id());
        }
    }

    public static Optional<RewardDefinition> forDeath(net.minecraft.world.entity.LivingEntity victim, DamageSource source) {
        if (source.is(DamageTypes.FALL) && co.blumeplugins.deathsareitems.ability.PearlAbility.isPearlImpact(victim))
            return Optional.ofNullable(get(PEARL));
        return forDeath(source);
    }

    public static Optional<RewardDefinition> forDeath(DamageSource source) {
        return DEFINITIONS.values().stream().filter(type -> type.matches().test(source))
                .findFirst();
    }

    public static RewardDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }
}
