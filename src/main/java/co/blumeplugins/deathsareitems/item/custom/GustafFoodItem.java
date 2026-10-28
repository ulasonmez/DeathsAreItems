package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.ability.HungerAbility;
import co.blumeplugins.deathsareitems.einfach.EinfachSavedData;
import co.blumeplugins.deathsareitems.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class GustafFoodItem extends EinfachItem {
    private final boolean hamburger;
    public GustafFoodItem(boolean hamburger) {
        super(new Properties().food(new FoodProperties.Builder().nutrition(0).saturationMod(0).alwaysEat().build()));
        this.hamburger = hamburger;
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        ItemStack result = super.finishUsingItem(stack, level, eater);
        if (eater instanceof ServerPlayer player) {
            int amount = hamburger ? 12 : 4;
            int food = player.getFoodData().getFoodLevel(), restored = Math.min(amount, 20 - food);
            player.getFoodData().setFoodLevel(food + restored);
            int reserve = Math.min(100, player.getPersistentData().getInt(HungerAbility.RESERVE) + amount - restored);
            player.getPersistentData().putInt(HungerAbility.RESERVE, reserve);
            player.getPersistentData().putInt("deathsareitems.FoodReserveIdle", 0);
            ModNetwork.state(player, player.getId(), 1, reserve);
            if (hamburger) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            else {
                var data = EinfachSavedData.get(player.server); var p = data.player(player.getUUID());
                long now = player.server.overworld().getGameTime();
                if (now >= p.getLong("CheeseHealAt")) {
                    player.heal(2); p.putLong("CheeseHealAt", now + 200); data.setDirty();
                }
            }
        }
        return result;
    }
}
