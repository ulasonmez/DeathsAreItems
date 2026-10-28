package co.blumeplugins.deathsareitems.mixin;

import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Return the container only when this furnace actually completes a cheese recipe. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class CheeseFurnaceMixin {
    @Redirect(method = "burn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void returnBucket(ItemStack input, int count, RegistryAccess access, Recipe<?> recipe, NonNullList<ItemStack> slots, int maxStack) {
        boolean cheese = input.is(Items.MILK_BUCKET) && recipe != null && recipe.getResultItem(access).is(ModItems.CHEESE_SLICE.get());
        input.shrink(count);
        if (cheese && input.isEmpty()) slots.set(0, new ItemStack(Items.BUCKET));
    }
}
