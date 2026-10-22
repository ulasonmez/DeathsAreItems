package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Composes Minecraft's original mob meshes and textures, without spawning entities. */
public final class ReferenceItemRenderer extends BlockEntityWithoutLevelRenderer {
    public enum Kind { MASK, SKELETON_BOW, MOB_PICKAXE }
    public static final ResourceLocation DRAWN_BOW = new ResourceLocation("minecraft", "item/bow_pulling_2");
    public static final ResourceLocation PICKAXE_HANDLE = new ResourceLocation(DeathsAreItems.MOD_ID, "item/mob_pickaxe_handle");
    private static final java.util.Map<ModelLayerLocation, ModelPart> MODELS = new java.util.HashMap<>();
    private final Kind kind;

    public static void clearModels() {
        MODELS.clear();
    }

    private static ModelPart model(ModelLayerLocation layer) {
        ModelPart model = MODELS.computeIfAbsent(layer, key -> Minecraft.getInstance().getEntityModels().bakeLayer(key));
        model.getAllParts().forEach(part -> { part.resetPose(); part.visible = true; });
        return model;
    }

    public ReferenceItemRenderer(Kind kind) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.kind = kind;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, .5, .5);
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        if (left) pose.scale(-1, 1, 1);
        switch (kind) {
            case MASK -> {
                if (context == ItemDisplayContext.GUI) {
                    pose.mulPose(Axis.XP.rotationDegrees(12));
                    pose.mulPose(Axis.YP.rotationDegrees(-25));
                    pose.scale(.94F, .94F, .94F);
                } else if (context.firstPerson()) {
                    pose.mulPose(Axis.YP.rotationDegrees(-20));
                    pose.translate(0, -.05, 0);
                    pose.scale(.8F, .8F, .8F);
                } else pose.scale(.75F, .75F, .75F);
                pose.translate(0, -.23, 0);
                pose.scale(1, -1, -1);
                MaskHolder.MODEL.head.resetPose();
                MaskHolder.MODEL.renderMask(pose, buffers, light, overlay);
            }
            case SKELETON_BOW -> {
                if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
                    pose.scale(.38F, .38F, .38F);
                    pose.translate(.7, -1, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(-12));
                } else if (context.firstPerson()) {
                    pose.scale(.57F, .57F, .57F);
                    pose.translate(.55, -.55, -.2);
                    pose.mulPose(Axis.YP.rotationDegrees(22));
                } else {
                    // Largest skeleton keeps its vanilla two-block height when held.
                    pose.translate(.3, -.9, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(15));
                }
                skeleton(pose, buffers, light, overlay, 0, 0, 1);
                skeleton(pose, buffers, light, overlay, -.76F, .35F, .72F);
                skeleton(pose, buffers, light, overlay, -1.30F, .60F, .50F);
                pose.translate(-1.93, 1.24, -.03);
                pose.mulPose(Axis.ZP.rotationDegrees(-45));
                pose.scale(.58F, .58F, .58F);
                Minecraft mc = Minecraft.getInstance();
                mc.getItemRenderer().render(new ItemStack(Items.BOW), ItemDisplayContext.NONE, false,
                        pose, buffers, light, overlay, mc.getModelManager().getModel(DRAWN_BOW));
            }
            case MOB_PICKAXE -> {
                if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
                    pose.scale(.66F, .66F, .66F);
                    pose.translate(-.1, -.52, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(-8));
                    pose.mulPose(Axis.ZP.rotationDegrees(-25));
                } else if (context.firstPerson()) {
                    pose.scale(.78F, .78F, .78F);
                    pose.translate(-.07, -.27, 0);
                    pose.mulPose(Axis.ZP.rotationDegrees(24));
                    pose.mulPose(Axis.YP.rotationDegrees(-12));
                } else {
                    pose.scale(.7F, .7F, .7F);
                    pose.translate(0, -.3, 0);
                    pose.mulPose(Axis.ZP.rotationDegrees(-10));
                }
                pose.pushPose();
                pose.translate(.075, .4, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(45));
                pose.scale(1.7F, 1.7F, 1.7F);
                Minecraft mc = Minecraft.getInstance();
                mc.getItemRenderer().render(new ItemStack(Items.WOODEN_PICKAXE), ItemDisplayContext.NONE,
                        false, pose, buffers, light, overlay, mc.getModelManager().getModel(PICKAXE_HANDLE));
                pose.popPose();
                mob(pose, buffers, light, overlay, ModelLayers.SKELETON, "skeleton/skeleton", -.67F, .68F, .32F, 155);
                mob(pose, buffers, light, overlay, ModelLayers.ZOMBIE, "zombie/zombie", -.56F, .55F, .32F, -18);
                mob(pose, buffers, light, overlay, ModelLayers.EVOKER, "illager/evoker", -.38F, .55F, .40F, -35);
                mob(pose, buffers, light, overlay, ModelLayers.PILLAGER, "illager/pillager", -.16F, .72F, .32F, -62);
                mob(pose, buffers, light, overlay, ModelLayers.ENDERMAN, "enderman/enderman", .04F, .80F, .26F, -70);
            }
        }
        pose.popPose();
    }

    private static void skeleton(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                 float x, float y, float scale) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, scale);
        ModelPart model = model(ModelLayers.SKELETON);
        // Both hands reach toward the next, smaller skeleton.
        model.getChild("right_arm").zRot = (float) Math.PI / 2;
        model.getChild("left_arm").zRot = (float) Math.PI / 2;
        model.getChild("left_arm").yRot = -.18F;
        renderMob(model, "skeleton/skeleton", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void mob(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                            ModelLayerLocation layer, String texture, float x, float y, float scale, float angle) {
        pose.pushPose();
        pose.translate(x, y, .02);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.scale(scale, scale, scale);
        ModelPart model = model(layer);
        if (layer.equals(ModelLayers.EVOKER) || layer.equals(ModelLayers.PILLAGER)) {
            model.getChild("arms").visible = false;
            model.getChild("head").getChild("hat").visible = false;
        }
        if (layer.equals(ModelLayers.ENDERMAN)) model.getChild("hat").visible = false;
        renderMob(model, texture, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderMob(ModelPart model, String texture, PoseStack pose,
                                  MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.scale(1, -1, -1);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation(
                "minecraft", "textures/entity/" + texture + ".png"))), light, overlay);
        pose.popPose();
    }

    private static final class MaskHolder {
        private static final DrowningMaskModel MODEL = new DrowningMaskModel();
    }

}
