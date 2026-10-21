package co.blumeplugins.deathsareitems.reward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;
import java.util.List;

/** One extension point for each future death type and its three rewards. */
public record RewardDefinition(ResourceLocation id, Predicate<DamageSource> matches,
                               List<RewardOption> options) {
    public RewardDefinition {
        options = List.copyOf(options);
        if (options.size() != 3) throw new IllegalArgumentException("A death type must define exactly three options");
    }

    public ItemStack choice(int slot) {
        return slot >= 0 && slot < options.size() ? options.get(slot).createReward() : ItemStack.EMPTY;
    }
}
