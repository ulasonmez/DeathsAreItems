package co.blumeplugins.deathsareitems.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Constructed only from Forge's client-side item extension callback. */
public final class ReferenceItemExtensions implements IClientItemExtensions {
    private final ReferenceItemRenderer.Kind kind;
    private ReferenceItemRenderer renderer;
    private DrowningMaskModel armor;

    public ReferenceItemExtensions(ReferenceItemRenderer.Kind kind) {
        this.kind = kind;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) renderer = new ReferenceItemRenderer(kind);
        return renderer;
    }

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                  EquipmentSlot slot, HumanoidModel<?> original) {
        if (kind != ReferenceItemRenderer.Kind.MASK || slot != EquipmentSlot.HEAD) return original;
        if (armor == null) armor = new DrowningMaskModel();
        return armor;
    }
}
