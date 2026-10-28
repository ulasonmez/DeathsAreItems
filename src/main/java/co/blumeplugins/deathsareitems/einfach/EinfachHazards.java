package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.ability.WorldActions;
import co.blumeplugins.deathsareitems.entity.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class EinfachHazards {
    public static boolean birch(BlockState state) {
        return state.is(Blocks.BIRCH_LOG) || state.is(Blocks.STRIPPED_BIRCH_LOG)
                || state.is(Blocks.BIRCH_WOOD) || state.is(Blocks.STRIPPED_BIRCH_WOOD);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void breakBirch(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.isCreative() && birch(event.getState())) {
            co.blumeplugins.deathsareitems.ability.DeathDamage.hurt(player, co.blumeplugins.deathsareitems.ability.DeathDamage.SPLINTER, null, 2);
            Companions.message(player, "birch_splinters");
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void button(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getUseBlock() == Event.Result.DENY
                || !WorldActions.allowed(player, event.getPos())) return;
        var level = player.serverLevel(); var pos = event.getPos(); var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ButtonBlock) || state.getValue(ButtonBlock.POWERED)) return;
        // Sneaking with an item bypasses the block's use, just as in vanilla.
        if (player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) return;
        String key = level.dimension().location() + ":" + pos.asLong();
        var data = EinfachSavedData.get(player.server);
        if (data.buttons.hasUUID(key)) return;
        var tnt = ModEntities.BUTTON_TNT.get().create(level); if (tnt == null) return;
        tnt.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); tnt.configure(key);
        if (!level.addFreshEntity(tnt)) return;
        data.buttons.putUUID(key, tnt.getUUID()); data.setDirty();
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 2, .7F);
        Companions.message(player, "button_warning");
    }
}
