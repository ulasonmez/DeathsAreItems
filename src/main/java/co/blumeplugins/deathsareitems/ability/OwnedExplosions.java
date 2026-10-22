package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Scope protection to this synchronous explosion, including its entity knockback and terrain events. */
@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class OwnedExplosions {
    private static Context context;
    public static void explode(ServerPlayer owner, Entity source, Vec3 at, float radius, boolean fire) {
        explode(owner, source, at, radius, fire, false);
    }
    public static void soulBlast(ServerPlayer owner, Vec3 at) {
        explode(owner, owner, at, 4F, false, true);
    }
    private static void explode(ServerPlayer owner, Entity source, Vec3 at, float radius, boolean fire, boolean soul) {
        Context previous = context;
        context = new Context(owner, source);
        try {
            // An indestructible bed must not absorb every explosion ray at its own origin.
            var calculator = new net.minecraft.world.level.ExplosionDamageCalculator() {
                @Override public java.util.Optional<Float> getBlockExplosionResistance(net.minecraft.world.level.Explosion explosion,
                        net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                        net.minecraft.world.level.material.FluidState fluid) {
                    if (soul && (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                            || state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN))) return java.util.Optional.of(3F);
                    return state.is(co.blumeplugins.deathsareitems.block.ModBlocks.BEDROCK.get()) ? java.util.Optional.of(0F)
                            : super.getBlockExplosionResistance(explosion,level,pos,state,fluid);
                }
                @Override public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter level,
                        BlockPos pos, net.minecraft.world.level.block.state.BlockState state, float power) {
                    return !state.is(co.blumeplugins.deathsareitems.block.ModBlocks.BEDROCK.get()) && super.shouldBlockExplode(explosion,level,pos,state,power);
                }
            };
            owner.serverLevel().explode(source, null, calculator, at.x, at.y, at.z, radius, fire, Level.ExplosionInteraction.TNT);
        }
        finally { context = previous; }
    }
    @SubscribeEvent public static void detonate(ExplosionEvent.Detonate event) {
        Context c = context;
        if (c == null || event.getLevel() != c.owner.level() || event.getExplosion().getDirectSourceEntity() != c.source) return;
        // Cannon protection is only for the close blast beneath a rocket-jumping shooter.
        if (!(c.source instanceof co.blumeplugins.deathsareitems.entity.custom.CannonTntEntity)
                || c.source.getY() <= c.owner.getY() + 1 && c.owner.distanceToSqr(c.source) < 36)
            event.getAffectedEntities().remove(c.owner);
        event.getAffectedBlocks().removeIf(pos -> !WorldActions.breakEvent(c.owner, pos));
    }
    private record Context(ServerPlayer owner, Entity source) {}
    private OwnedExplosions() {}
}
