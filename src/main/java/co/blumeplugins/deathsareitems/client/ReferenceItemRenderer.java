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
    public enum Kind { MASK, SKELETON_BOW, MOB_PICKAXE, ANVILS, CRAMMER }
    public static final ResourceLocation DRAWN_BOW = new ResourceLocation("minecraft", "item/bow_pulling_2");
    public static final ResourceLocation PICKAXE_HANDLE = new ResourceLocation(DeathsAreItems.MOD_ID, "item/mob_pickaxe_handle");
    private static final java.util.Map<ModelLayerLocation, ModelPart> MODELS = new java.util.HashMap<>();
    // Deliberately intersecting, individually tilted anvils; no front/back rows.
    private static final float[][] ANVIL_POSES = {
            {-.40F, -.29F, -.23F, -32, 12, -15},
            {.31F, -.24F, -.31F, 63, -17, 11},
            {-.03F, -.14F, .34F, 128, 18, -9},
            {-.34F, .05F, .17F, 24, -13, 20},
            {.36F, .08F, .12F, -71, 15, -18},
            {.02F, .23F, -.09F, 106, -21, 8}
    };
    private static final int[] CRAMMER_RING_COUNTS = {28, 22, 14, 6};
    private static final double[] CRAMMER_RING_RADII = {.29, .20, .11, .025};
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
        if (left && kind != Kind.MOB_PICKAXE) pose.scale(-1, 1, 1);
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
                    pose.mulPose(Axis.YP.rotationDegrees(45));
                } else if (context.firstPerson()) {
                    pose.scale(.57F, .57F, .57F);
                    pose.translate(.55, -.55, -.2);
                    pose.mulPose(Axis.YP.rotationDegrees(-90));
                } else if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                    // Undo ItemInHandLayer's hand axes, keeping the skeletons upright
                    // and their shared aim along the player's forward direction.
                    pose.mulPose(Axis.YP.rotationDegrees(180));
                    pose.mulPose(Axis.XP.rotationDegrees(-90));
                    pose.mulPose(Axis.YP.rotationDegrees(90));
                    pose.translate(0, -1.2, 0);
                } else {
                    // Largest skeleton keeps its vanilla two-block height when held.
                    pose.translate(.3, -.9, 0);
                    pose.mulPose(Axis.YP.rotationDegrees(-90));
                }
                skeleton(pose, buffers, light, overlay, 0, 0, 1);
                skeleton(pose, buffers, light, overlay, -.76F, .35F, .72F);
                skeleton(pose, buffers, light, overlay, -1.30F, .60F, .50F);
                pose.translate(-1.78, 1.24, -.03);
                // The arrow in bow_pulling_2 runs toward texture upper-left.
                // +45 levels it along the chain; the whole chain then faces forward.
                pose.mulPose(Axis.ZP.rotationDegrees(45));
                pose.scale(.58F, .58F, .58F);
                Minecraft mc = Minecraft.getInstance();
                mc.getItemRenderer().render(new ItemStack(Items.BOW), ItemDisplayContext.NONE, false,
                        pose, buffers, light, overlay, mc.getModelManager().getModel(DRAWN_BOW));
            }
            case MOB_PICKAXE -> {
                // Render the unscaled vanilla grip at its original item coordinates.
                // All hand transforms come from item/handheld, exactly like a normal pickaxe.
                Minecraft mc = Minecraft.getInstance();
                mc.getItemRenderer().render(new ItemStack(Items.WOODEN_PICKAXE), ItemDisplayContext.NONE,
                        false, pose, buffers, light, overlay, mc.getModelManager().getModel(PICKAXE_HANDLE));
                pose.pushPose();
                pose.translate(.25, .25, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(-45));
                pose.scale(1.2F, 1.2F, 1.2F);
                // Lay the mobs along the head, perpendicular to the vanilla shaft.
                // Their chest positions and tangent angles trace a shallow downward arch.
                mob(pose, buffers, light, overlay, ModelLayers.SKELETON, "skeleton/skeleton", -.36F, -.075F, .22F, 115);
                mob(pose, buffers, light, overlay, ModelLayers.ZOMBIE, "zombie/zombie", -.18F, .015F, .23F, 102);
                mob(pose, buffers, light, overlay, ModelLayers.EVOKER, "illager/evoker", 0, .045F, .23F, 90);
                mob(pose, buffers, light, overlay, ModelLayers.PILLAGER, "illager/pillager", .18F, .015F, .22F, 78);
                mob(pose, buffers, light, overlay, ModelLayers.ENDERMAN, "enderman/enderman", .36F, -.075F, .15F, 65);
                pose.popPose();
            }
            case ANVILS -> {
                if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
                    pose.mulPose(Axis.XP.rotationDegrees(30));
                    pose.mulPose(Axis.YP.rotationDegrees(-25));
                    pose.scale(.52F, .52F, .52F);
                } else if (context.firstPerson()) {
                    pose.mulPose(Axis.XP.rotationDegrees(12));
                    pose.mulPose(Axis.YP.rotationDegrees(-15));
                    pose.scale(.60F, .60F, .60F);
                } else {
                    pose.scale(.60F, .60F, .60F);
                }
                for (float[] anvil : ANVIL_POSES) {
                    pose.pushPose();
                    pose.translate(anvil[0], anvil[1], anvil[2]);
                    pose.mulPose(Axis.YP.rotationDegrees(anvil[3]));
                    pose.mulPose(Axis.XP.rotationDegrees(anvil[4]));
                    pose.mulPose(Axis.ZP.rotationDegrees(anvil[5]));
                    pose.scale(.7F, .7F, .7F);
                    pose.translate(-.5, -.5, -.5);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                            net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(), pose, buffers, light, overlay);
                    pose.popPose();
                }
            }
            case CRAMMER -> {
                if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.FIXED) {
                    pose.mulPose(Axis.XP.rotationDegrees(-45));
                    pose.mulPose(Axis.YP.rotationDegrees(-18));
                    pose.mulPose(Axis.ZP.rotationDegrees(-30));
                    pose.scale(.42F, .42F, .42F);
                } else if (context.firstPerson()) {
                    pose.mulPose(Axis.XP.rotationDegrees(-25));
                    pose.mulPose(Axis.YP.rotationDegrees(-12));
                    pose.mulPose(Axis.ZP.rotationDegrees(-12));
                    pose.scale(.56F, .56F, .56F);
                } else {
                    pose.mulPose(Axis.YP.rotationDegrees(35));
                    pose.scale(.62F, .62F, .62F);
                }
                // Seventy overlapping pigs fill the same circular footprint.
                // Fixed height offsets add a few raised pigs without any animation.
                for (int ring = 0; ring < CRAMMER_RING_COUNTS.length; ring++) {
                    int count = CRAMMER_RING_COUNTS[ring];
                    double radius = CRAMMER_RING_RADII[ring];
                    for (int slot = 0; slot < count; slot++) {
                        double angle = Math.PI * 2 * slot / count + ring * .23;
                        pose.pushPose();
                        pose.translate(Math.sin(angle) * radius,
                                -.23 + (slot % 7) * .01 + ring * .03 + slot * .0003,
                                Math.cos(angle) * radius);
                        pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(angle)));
                        pose.scale(.54F, .54F, .54F);
                        ModelPart pig = model(ModelLayers.PIG);
                        renderMob(pig, "pig/pig", pose, buffers, light, overlay);
                        pose.popPose();
                    }
                }
            }
        }
        pose.popPose();
    }

    private static void skeleton(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                 float x, float y, float scale) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, scale);
        // Face down the chain (-X), with both hands reaching forward rather than sideways.
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        ModelPart model = model(ModelLayers.SKELETON);
        model.getChild("right_arm").xRot = -(float) Math.PI / 2;
        model.getChild("left_arm").xRot = -(float) Math.PI / 2;
        model.getChild("right_arm").yRot = -.22F;
        model.getChild("left_arm").yRot = .22F;
        renderMob(model, "skeleton/skeleton", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void mob(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                            ModelLayerLocation layer, String texture, float x, float y, float scale, float angle) {
        pose.pushPose();
        // Intersect volumes without sharing texture planes at the same depth.
        float depth = layer.equals(ModelLayers.SKELETON) ? -.006F :
                layer.equals(ModelLayers.ZOMBIE) ? -.003F :
                layer.equals(ModelLayers.PILLAGER) ? .003F :
                layer.equals(ModelLayers.ENDERMAN) ? .006F : 0;
        pose.translate(x, y, depth);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.scale(scale, scale, scale);
        ModelPart model = model(layer);
        if (layer.equals(ModelLayers.EVOKER) || layer.equals(ModelLayers.PILLAGER)) {
            model.getChild("arms").visible = false;
            model.getChild("head").getChild("hat").visible = false;
        }
        if (layer.equals(ModelLayers.ENDERMAN)) model.getChild("hat").visible = false;
        // Rotate each mob around its chest so adjacent bodies actually intersect.
        pose.translate(0, layer.equals(ModelLayers.ENDERMAN) ? -2 :
                layer.equals(ModelLayers.EVOKER) || layer.equals(ModelLayers.PILLAGER) ? -.875 : -1.125, 0);
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
