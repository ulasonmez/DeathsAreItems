package co.blumeplugins.deathsareitems.item.custom;

public final class SwarmOfAnvilsItem extends DeathToolItem {
    public SwarmOfAnvilsItem() {
        super(Kind.ANVILS);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new co.blumeplugins.deathsareitems.client.ReferenceItemExtensions(
                co.blumeplugins.deathsareitems.client.ReferenceItemRenderer.Kind.ANVILS));
    }
}
