package co.blumeplugins.deathsareitems.mixin;

import co.blumeplugins.deathsareitems.einfach.EinfachHazards;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(BlockBehaviour.class)
public abstract class BirchDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void splinters(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (EinfachHazards.birch(state)) cir.setReturnValue(List.of(new ItemStack(ModItems.BIRCH_SPLINTERS.get())));
    }
}
