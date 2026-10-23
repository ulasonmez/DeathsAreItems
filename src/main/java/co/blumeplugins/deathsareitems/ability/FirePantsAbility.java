package co.blumeplugins.deathsareitems.ability;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class FirePantsAbility {
    private static final UUID SPEED = UUID.fromString("a4eb70e8-933f-44df-97c0-b348dd795180");
    private static final UUID STEP = UUID.fromString("b574acdb-97fa-4a12-9d01-cb0145f3cfe3");
    private static final String RAMP = "deathsareitems.FireRamp";
    public static boolean equipped(Player player) { return player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.FIRE_PANTS.get()); }
    public static int ramp(Player player) { return player.getPersistentData().getInt(RAMP); }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        boolean worn = equipped(player) && player.isAlive() && !player.isSpectator();
        if (player instanceof ServerPlayer server) EquipmentEffects.update(server, "deathsareitems.FirePants", MobEffects.FIRE_RESISTANCE, worn);
        // ServerPlayer can overwrite xo/zo before END; compare positions across whole ticks.
        var data = player.getPersistentData();
        double moved = data.contains("deathsareitems.FireLastX")
                ? Math.pow(player.getX() - data.getDouble("deathsareitems.FireLastX"), 2)
                + Math.pow(player.getZ() - data.getDouble("deathsareitems.FireLastZ"), 2) : 0;
        data.putDouble("deathsareitems.FireLastX", player.getX());
        data.putDouble("deathsareitems.FireLastZ", player.getZ());
        boolean running = worn && player.isSprinting() && !player.getAbilities().flying && !player.isPassenger()
                && (moved > .0004 && moved < 16 || player.getDeltaMovement().horizontalDistanceSqr() > .0004);
        if (running) data.putLong("deathsareitems.FireLastSprint", player.level().getGameTime());
        boolean grace = worn && data.contains("deathsareitems.FireLastSprint")
                && player.level().getGameTime() - data.getLong("deathsareitems.FireLastSprint") <= 8;
        int ramp = !worn ? 0 : running ? Math.min(60, ramp(player) + 1) : grace ? ramp(player) : Math.max(0, ramp(player) - 2);
        data.putInt(RAMP, ramp);
        // The server owns speed modifiers; competing local ramp values caused attribute corrections.
        if (!player.level().isClientSide)
            updateAttribute(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, "Fire Pants sprint", ramp / 60.0 * 1.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
        // Predict step-up immediately on both sides, including while speed is still building.
        updateAttribute(player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()), STEP, "Fire Pants step", worn ? .45 : 0, AttributeModifier.Operation.ADDITION);
        if (!(player instanceof ServerPlayer server) || !worn) return;
        if (running && ramp >= 5 && player.tickCount % 2 == 0) {
            server.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + .15, player.getZ(), 4, .18, .08, .18, .035);
            server.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + .25, player.getZ(), 2, .16, .1, .16, .025);
            server.serverLevel().sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + .5, player.getZ(), 4, .2, .35, .2, .02);
        }
        if (running && ramp >= 5 && player.onGround()) {
            BlockPos support = player.getOnPos();
            if (server.level().getFluidState(support).isEmpty())
                TemporaryTerrain.put(server, support, Blocks.OBSIDIAN.defaultBlockState(), Blocks.LAVA.defaultBlockState(), 30, "molten_trail");
        }
    }
    private static void updateAttribute(AttributeInstance attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && Math.abs(old.getAmount() - amount) < .0001) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }
    private FirePantsAbility() {}
}
