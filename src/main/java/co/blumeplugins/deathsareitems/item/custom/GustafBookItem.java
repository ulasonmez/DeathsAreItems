package co.blumeplugins.deathsareitems.item.custom;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.einfach.*;
import co.blumeplugins.deathsareitems.item.TooltipText;
import co.blumeplugins.deathsareitems.reward.RewardSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DeathsAreItems.MOD_ID)
public final class GustafBookItem extends EinfachItem {
    private static final List<String> CHAPTERS = List.of("fall", "drown", "anvil", "cactus", "wither");
    public GustafBookItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    public static List<String> unlocked(ServerPlayer player) {
        var rewards = RewardSavedData.get(player.server);
        return CHAPTERS.stream().filter(c -> rewards.hasDiscovered(player.getUUID(), new ResourceLocation(DeathsAreItems.MOD_ID, c))).toList();
    }
    public static ChatFormatting chapterColor(String chapter) {
        return switch (chapter) {
            case "fall" -> ChatFormatting.AQUA;
            case "drown" -> ChatFormatting.BLUE;
            case "anvil" -> ChatFormatting.GOLD;
            case "cactus" -> ChatFormatting.GREEN;
            default -> ChatFormatting.LIGHT_PURPLE;
        };
    }
    private static void message(ServerPlayer owner, String key, ChatFormatting color, Object... args) {
        owner.displayClientMessage(Component.translatable("message.deathsareitems." + key, args).withStyle(color), true);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return use(level, player, hand, null);
    }
    private InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, Mob clicked) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer owner)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        if (!owner.isAlive() || owner.isSpectator()) return InteractionResultHolder.fail(stack);
        List<String> chapters = unlocked(owner);
        if (chapters.isEmpty()) { message(owner, "book_locked", ChatFormatting.RED); return InteractionResultHolder.fail(stack); }
        var data = EinfachSavedData.get(owner.server); var p = data.player(owner.getUUID());
        String chapter = p.getString("Chapter");
        if (!chapters.contains(chapter)) chapter = chapters.get(0);
        // Migrate old saves: the book no longer stores or checks a casting cooldown.
        p.remove("BookReady");
        if (owner.isShiftKeyDown()) {
            chapter = chapters.get((chapters.indexOf(chapter) + 1) % chapters.size());
            p.putString("Chapter", chapter); data.setDirty();
            message(owner, "book_chapter", chapterColor(chapter), Component.translatable("chapter.deathsareitems." + chapter));
            level.playSound(null, owner.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, .7F, 1F + CHAPTERS.indexOf(chapter) * .1F);
            return InteractionResultHolder.success(stack);
        }
        Vec3 start = owner.getEyePosition(), end = start.add(owner.getLookAngle().scale(24));
        Vec3 limit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getLocation();
        Mob target = clicked != null ? clicked : level.getEntitiesOfClass(Mob.class, new AABB(start, limit).inflate(2),
                e -> BookSpells.canTarget(e) && e.distanceToSqr(owner) <= 24 * 24 && owner.hasLineOfSight(e)
                        && e.getBoundingBox().inflate(.35).clip(start, limit).isPresent()).stream()
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().inflate(.35).clip(start, limit).orElse(limit).distanceToSqr(start))).orElse(null);
        if (target == null || !BookSpells.canTarget(target) || target.level() != level
                || target.distanceToSqr(owner) > 24 * 24 || !owner.hasLineOfSight(target)) {
            message(owner, "book_target", ChatFormatting.RED); return InteractionResultHolder.fail(stack);
        }
        if (!BookSpells.cast(owner, target, chapter)) { message(owner, "book_busy", ChatFormatting.RED); return InteractionResultHolder.fail(stack); }
        int verses = BookSpells.inscribe(owner, target, chapter);
        p.putString("Chapter", chapter); data.setDirty();
        if (verses == 3) message(owner, "book_finale", ChatFormatting.LIGHT_PURPLE);
        else message(owner, "book_cast", chapterColor(chapter), Component.translatable("chapter.deathsareitems." + chapter), verses);
        return InteractionResultHolder.success(stack);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event) { interact(event, event.getTarget()); }
    @SubscribeEvent public static void interactAt(PlayerInteractEvent.EntityInteractSpecific event) { interact(event, event.getTarget()); }
    private static void interact(PlayerInteractEvent event, Entity target) {
        Player player = event.getEntity();
        if (!(target instanceof Mob mob) || player.isSpectator() || !player.isAlive()
                || !(event.getItemStack().getItem() instanceof GustafBookItem book)) return;
        // Cast before a villager opens trading or a tamed animal handles its own interaction.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!player.level().isClientSide) book.use(player.level(), player, event.getHand(), mob);
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.tickCount % 20 != 0) return;
        if (!selected && !(owner.getOffhandItem() == stack && !owner.getMainHandItem().is(this))) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(12),
                e -> BookSpells.canTarget(e) && owner.distanceToSqr(e) <= 144))
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        for (String line : TooltipText.wrap(Component.translatable(getDescriptionId() + ".desc").getString()))
            lines.add(Component.literal(line).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
