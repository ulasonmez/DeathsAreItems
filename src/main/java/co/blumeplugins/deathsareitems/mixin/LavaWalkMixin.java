package co.blumeplugins.deathsareitems.mixin;

import co.blumeplugins.deathsareitems.ability.FirePantsAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A wearer collides with the exposed lava surface; the fluid and other entities stay unchanged. */
@Mixin(LiquidBlock.class)
public abstract class LavaWalkMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void deathsareitems$lavaSurface(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context, CallbackInfoReturnable<VoxelShape> result) {
        if (!(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player) || !FirePantsAbility.equipped(player)
                || !player.isAlive() || player.isSpectator() || player.getAbilities().flying || player.isPassenger()
                || !state.getFluidState().is(FluidTags.LAVA) || level.getFluidState(pos.above()).is(FluidTags.LAVA)) return;
        VoxelShape surface = Shapes.box(0, 0, 0, 1, state.getFluidState().getHeight(level, pos), 1);
        if (context.isAbove(surface, pos, true)) result.setReturnValue(surface);
    }
}
