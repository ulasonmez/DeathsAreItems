package co.blumeplugins.deathsareitems.einfach;

import co.blumeplugins.deathsareitems.item.ModItems;
import co.blumeplugins.deathsareitems.reward.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

public final class EinfachRewards {
    /** Called only by a successful claim of an offer created by an actual death. */
    public static void claim(ServerPlayer player, ResourceLocation death, long due) {
        var data = EinfachSavedData.get(player.server);
        CompoundTag progress = data.player(player.getUUID());
        var claimed = progress.getCompound("Claimed");
        if (claimed.getBoolean(death.getPath())) return;
        claimed.putBoolean(death.getPath(), true); progress.put("Claimed", claimed);
        if (death.equals(DeathRewards.DROWN)) grant(player, new ItemStack(ModItems.VINYS_BUCKET.get()), due);
        if (death.equals(DeathRewards.GOLEM)) grant(player, new ItemStack(ModItems.JUKYS_WHISTLE.get()), due);
        if (death.equals(DeathRewards.STARVE)) {
            grant(player, new ItemStack(ModItems.CHEESE_SLICE.get(), 8), due);
            grant(player, new ItemStack(ModItems.HAMBURGER.get(), 2), due);
            player.awardRecipesByKey(new ResourceLocation[]{id("cheese_from_milk"), id("hamburger")});
        }
        int count = claimed.getAllKeys().size();
        if (count >= 5 && !progress.getBoolean("JukeboxGranted")) {
            progress.putBoolean("JukeboxGranted", true);
            grant(player, new ItemStack(ModItems.GUSTAFS_JUKEBOX.get()), due);
            grant(player, new ItemStack(Items.MUSIC_DISC_CAT), due);
        }
        if (count >= 10 && !progress.getBoolean("BookGranted")) {
            progress.putBoolean("BookGranted", true);
            grant(player, new ItemStack(ModItems.GUSTAFS_NEW_BOOK.get()), due);
        }
        data.setDirty();
    }
    private static ResourceLocation id(String path) { return new ResourceLocation(co.blumeplugins.deathsareitems.DeathsAreItems.MOD_ID, path); }
    private static void grant(ServerPlayer player, ItemStack stack, long due) {
        var rewards = RewardSavedData.get(player.server);
        rewards.grants.add(new RewardSavedData.Grant(player.getUUID(), stack, due)); rewards.setDirty();
    }
    private EinfachRewards() {}
}
