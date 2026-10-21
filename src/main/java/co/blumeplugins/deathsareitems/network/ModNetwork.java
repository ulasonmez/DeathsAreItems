package co.blumeplugins.deathsareitems.network;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import co.blumeplugins.deathsareitems.client.ClientEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DeathsAreItems.MOD_ID, "effects"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(EffectPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> { buffer.writeBoolean(packet.celebration); buffer.writeItem(packet.stack); })
                .decoder(buffer -> new EffectPacket(buffer.readBoolean(), buffer.readItem()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEffects.handle(packet.celebration, packet.stack));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void celebrate(ServerPlayer player) { send(player, new EffectPacket(true, ItemStack.EMPTY)); }
    public static void reveal(ServerPlayer player, ItemStack stack) { send(player, new EffectPacket(false, stack)); }
    private static void send(ServerPlayer player, EffectPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    private record EffectPacket(boolean celebration, ItemStack stack) {}
}
