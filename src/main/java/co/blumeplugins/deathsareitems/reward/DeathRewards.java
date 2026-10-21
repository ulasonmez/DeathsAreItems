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

    private DeathRewards() {}

    public static void bootstrap() {
        register(new RewardDefinition(FALL, source -> source.is(DamageTypes.FALL), List.of(
                RewardOption.reward("item.deathsareitems.fall_damage_boots", () -> new ItemStack(ModItems.FALL_DAMAGE_BOOTS.get())),
                RewardOption.preview("reward.deathsareitems.long_trip"),
                RewardOption.preview("reward.deathsareitems.slippery_banana_peel"))));
    }

    public static void register(RewardDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate death reward " + definition.id());
        }
    }

    public static Optional<RewardDefinition> forDeath(DamageSource source) {
        return DEFINITIONS.values().stream().filter(type -> type.matches().test(source))
                .findFirst();
    }

    public static RewardDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }
}
