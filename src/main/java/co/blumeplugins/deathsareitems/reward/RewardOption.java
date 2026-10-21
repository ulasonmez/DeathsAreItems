package co.blumeplugins.deathsareitems.reward;

import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/** Display titles are independent from availability and the item a choice awards. */
public record RewardOption(String titleKey, boolean selectable, Supplier<ItemStack> item) {
    public static RewardOption reward(String titleKey, Supplier<ItemStack> item) {
        return new RewardOption(titleKey, true, item);
    }

    public static RewardOption preview(String titleKey) {
        return new RewardOption(titleKey, false, () -> ItemStack.EMPTY);
    }

    public ItemStack createReward() {
        return selectable ? item.get() : ItemStack.EMPTY;
    }
}
