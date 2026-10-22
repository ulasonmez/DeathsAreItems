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
    private static final String VERSION = "3";
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
        CHANNEL.messageBuilder(InputPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.held))
                .decoder(buffer -> new InputPacket(buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) co.blumeplugins.deathsareitems.ability.DeathAbilities.input(player, packet.held);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(StatePacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> { buffer.writeVarInt(packet.entity); buffer.writeFloat(packet.scale); buffer.writeVarInt(packet.reserve); })
                .decoder(buffer -> new StatePacket(buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> co.blumeplugins.deathsareitems.client.AbilityClient.state(packet.entity, packet.scale, packet.reserve));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(FoodPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> { buffer.writeDouble(packet.x); buffer.writeDouble(packet.y); buffer.writeDouble(packet.z); buffer.writeVarInt(packet.recipient); })
                .decoder(buffer -> new FoodPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> co.blumeplugins.deathsareitems.client.AbilityClient.food(packet.x, packet.y, packet.z, packet.recipient));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void food(net.minecraft.world.entity.LivingEntity source, net.minecraft.world.phys.Vec3 start, ServerPlayer recipient) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> source), new FoodPacket(start.x, start.y, start.z, recipient.getId()));
    }

    public static void input(boolean held) { CHANNEL.sendToServer(new InputPacket(held)); }
    public static void state(ServerPlayer player, int entity, float scale, int reserve) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StatePacket(entity, scale, reserve));
    }
    public static void trackingState(net.minecraft.world.entity.LivingEntity entity, float scale) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new StatePacket(entity.getId(), scale, -1));
    }

    public static void celebrate(ServerPlayer player, ItemStack icon) { send(player, new EffectPacket(true, icon)); }
    public static void reveal(ServerPlayer player, ItemStack stack) { send(player, new EffectPacket(false, stack)); }
    private static void send(ServerPlayer player, EffectPacket packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    private record EffectPacket(boolean celebration, ItemStack stack) {}
    private record InputPacket(boolean held) {}
    private record StatePacket(int entity, float scale, int reserve) {}
    private record FoodPacket(double x, double y, double z, int recipient) {}
}
