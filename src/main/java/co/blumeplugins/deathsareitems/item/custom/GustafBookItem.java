package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.einfach.*;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.*;

public final class GustafBookItem extends EinfachItem {
    private static final List<String> CHAPTERS = List.of("fall", "drown", "anvil", "cactus", "wither");
    public GustafBookItem() { super(new Properties().stacksTo(1)); }
    public static List<String> unlocked(ServerPlayer player) {
        var rewards = RewardSavedData.get(player.server);
        return CHAPTERS.stream().filter(c -> rewards.hasDiscovered(player.getUUID(), new ResourceLocation(DeathsAreItems.MOD_ID, c))).toList();
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer owner)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        if (!owner.isAlive() || owner.isSpectator()) return InteractionResultHolder.fail(stack);
        List<String> chapters = unlocked(owner);
        if (chapters.isEmpty()) { Companions.message(owner, "book_locked"); return InteractionResultHolder.fail(stack); }
        var data = EinfachSavedData.get(owner.server); var p = data.player(owner.getUUID());
        String chapter = p.getString("Chapter");
        if (!chapters.contains(chapter)) chapter = chapters.get(0);
        if (owner.isShiftKeyDown()) {
            chapter = chapters.get((chapters.indexOf(chapter) + 1) % chapters.size());
            p.putString("Chapter", chapter); data.setDirty(); Companions.message(owner, "book_chapter", Component.translatable("chapter.deathsareitems." + chapter));
            return InteractionResultHolder.success(stack);
        }
        long now = Companions.now(owner);
        if (now < p.getLong("BookReady")) { Companions.message(owner, "book_cooldown", (p.getLong("BookReady") - now + 19) / 20); return InteractionResultHolder.fail(stack); }
        Vec3 start = owner.getEyePosition(), end = start.add(owner.getLookAngle().scale(24));
        Vec3 limit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getLocation();
        Mob target = level.getEntitiesOfClass(Mob.class, new AABB(start, limit).inflate(2),
                e -> Companions.hostile(e) && e.distanceToSqr(owner) <= 24 * 24 && owner.hasLineOfSight(e)
                        && e.getBoundingBox().inflate(.35).clip(start, limit).isPresent()).stream()
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().inflate(.35).clip(start, limit).orElse(limit).distanceToSqr(start))).orElse(null);
        if (target == null) { Companions.message(owner, "book_target"); return InteractionResultHolder.fail(stack); }
        if (!BookSpells.cast(owner, target, chapter)) { Companions.message(owner, "book_busy"); return InteractionResultHolder.fail(stack); }
        p.putString("Chapter", chapter); p.putLong("BookReady", now + 300); data.setDirty();
        Companions.message(owner, "book_cast", Component.translatable("chapter.deathsareitems." + chapter));
        return InteractionResultHolder.success(stack);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        for (String chapter : CHAPTERS) lines.add(Component.translatable("chapter.deathsareitems." + chapter).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
