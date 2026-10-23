package co.blumeplugins.deathsareitems.reward;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Offers and already selected rewards survive death, logout and server restart. */
public final class RewardSavedData extends SavedData {
    public final Map<UUID, Offer> offers = new LinkedHashMap<>();
    public final List<Grant> grants = new ArrayList<>();
    public final Map<UUID, java.util.Set<ResourceLocation>> discovered = new LinkedHashMap<>();
    public final java.util.Set<UUID> rejected = new java.util.HashSet<>();

    public boolean discover(UUID owner, ResourceLocation death) {
        boolean fresh = discovered.computeIfAbsent(owner, key -> new java.util.HashSet<>()).add(death);
        if (fresh) setDirty();
        return fresh;
    }
    public boolean hasDiscovered(UUID owner, ResourceLocation death) { return discovered.getOrDefault(owner, java.util.Set.of()).contains(death); }

    /** Reopen a death without removing already earned offers or selected rewards. */
    public void resetDiscovery(UUID owner, ResourceLocation death) {
        var deaths = discovered.get(owner);
        if (deaths != null) {
            deaths.remove(death);
            if (deaths.isEmpty()) discovered.remove(owner);
        }
        rejected.remove(owner);
        setDirty();
    }

    public void resetDiscoveries(UUID owner) {
        discovered.remove(owner);
        rejected.remove(owner);
        setDirty();
    }

    public static RewardSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RewardSavedData::load,
                RewardSavedData::new, DeathsAreItems.MOD_ID + "_rewards");
    }

    public static RewardSavedData load(CompoundTag tag) {
        RewardSavedData data = new RewardSavedData();
        for (Tag entry : tag.getList("Discovered", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag)entry;
            if (!value.hasUUID("Owner")) continue;
            for (Tag death : value.getList("Deaths", Tag.TAG_STRING)) {
                ResourceLocation id = ResourceLocation.tryParse(death.getAsString());
                if (id != null) data.discover(value.getUUID("Owner"), id);
            }
        }
        for (Tag entry : tag.getList("Rejected", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag)entry;
            if (value.hasUUID("Owner")) data.rejected.add(value.getUUID("Owner"));
        }
        for (Tag entry : tag.getList("Offers", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) entry;
            ResourceLocation reward = ResourceLocation.tryParse(value.getString("Reward"));
            if (!value.hasUUID("Owner") || reward == null) continue;
            Offer offer = new Offer(value.getUUID("Owner"), reward);
            // Keep the old block markers until login cleans them up; restart the animation afterwards.
            if (value.hasUUID("Generation")) offer.generation = value.getUUID("Generation");
            offer.dimension = ResourceLocation.tryParse(value.getString("Dimension"));
            for (Tag placedEntry : value.getList("PlacedChests", Tag.TAG_COMPOUND)) {
                CompoundTag placed = (CompoundTag) placedEntry;
                offer.placedChests.add(new PlacedChest(NbtUtils.readBlockPos(placed.getCompound("Pos")),
                        NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), placed.getCompound("Original"))));
            }
            data.offers.put(offer.owner, offer);
        }
        for (Tag entry : tag.getList("Grants", Tag.TAG_COMPOUND)) {
            CompoundTag value = (CompoundTag) entry;
            ItemStack stack = ItemStack.of(value.getCompound("Stack"));
            if (value.hasUUID("Owner") && !stack.isEmpty()) {
                data.grants.add(new Grant(value.getUUID("Owner"), stack, value.getLong("Due")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag discoveries = new ListTag(), rejects = new ListTag();
        discovered.forEach((owner, deaths) -> {
            CompoundTag value = new CompoundTag(); value.putUUID("Owner", owner); ListTag ids = new ListTag();
            deaths.forEach(death -> ids.add(net.minecraft.nbt.StringTag.valueOf(death.toString())));
            value.put("Deaths", ids); discoveries.add(value);
        });
        rejected.forEach(owner -> { CompoundTag value = new CompoundTag(); value.putUUID("Owner", owner); rejects.add(value); });
        tag.put("Discovered", discoveries); tag.put("Rejected", rejects);
        ListTag savedOffers = new ListTag();
        offers.values().forEach(offer -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("Owner", offer.owner);
            value.putString("Reward", offer.reward.toString());
            value.putUUID("Generation", offer.generation);
            if (offer.dimension != null) value.putString("Dimension", offer.dimension.toString());
            ListTag placedChests = new ListTag();
            offer.placedChests.forEach(chest -> {
                CompoundTag placed = new CompoundTag();
                placed.put("Pos", NbtUtils.writeBlockPos(chest.pos()));
                placed.put("Original", NbtUtils.writeBlockState(chest.original()));
                placedChests.add(placed);
            });
            value.put("PlacedChests", placedChests);
            savedOffers.add(value);
        });
        ListTag savedGrants = new ListTag();
        grants.forEach(grant -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("Owner", grant.owner());
            value.put("Stack", grant.stack().save(new CompoundTag()));
            value.putLong("Due", grant.due());
            savedGrants.add(value);
        });
        tag.put("Offers", savedOffers);
        tag.put("Grants", savedGrants);
        return tag;
    }

    public static final class Offer {
        public final UUID owner;
        public final ResourceLocation reward;
        public UUID generation = UUID.randomUUID();
        public ResourceLocation dimension;
        public long spawnedAt;
        public int delay = RewardTimings.RESPAWN_DELAY;
        public boolean spawned;
        public boolean announced;
        public final List<UUID> chests = new ArrayList<>();
        public final List<PlacedChest> placedChests = new ArrayList<>();

        public Offer(UUID owner, ResourceLocation reward) {
            this.owner = owner;
            this.reward = reward;
        }
    }

    public record Grant(UUID owner, ItemStack stack, long due) {}
    public record PlacedChest(BlockPos pos, BlockState original) {}
}
