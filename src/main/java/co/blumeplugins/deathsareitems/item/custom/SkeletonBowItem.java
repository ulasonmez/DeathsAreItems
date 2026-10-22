package co.blumeplugins.deathsareitems.item.custom;

/** Keeps the existing bow behavior while supplying its composed vanilla skeleton model. */
public final class SkeletonBowItem extends DeathToolItem {
    public SkeletonBowItem() {
        super(Kind.BOW);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        // Item calls this from its constructor, before DeathToolItem.kind is assigned.
        consumer.accept(new co.blumeplugins.deathsareitems.client.ReferenceItemExtensions(
                co.blumeplugins.deathsareitems.client.ReferenceItemRenderer.Kind.SKELETON_BOW));
    }
}
