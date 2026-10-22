package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** The same head-sized geometry is used in the hand and by the armor layer. */
public final class DrowningMaskModel extends HumanoidModel<LivingEntity> {
    public static final ResourceLocation FRAME = new ResourceLocation(DeathsAreItems.MOD_ID,
            "textures/models/armor/drowning_mask.png");
    public static final ResourceLocation GLASS = new ResourceLocation(DeathsAreItems.MOD_ID,
            "textures/models/armor/drowning_mask_visor.png");
    private final ModelPart visor;

    public DrowningMaskModel() {
        this(createRoot());
    }

    private DrowningMaskModel(ModelPart root) {
        super(root);
        visor = root.getChild("visor");
    }

    private static ModelPart createRoot() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder frame = CubeListBuilder.create()
                // Thick brow, narrow side posts, lower frame and projecting respirator.
                .texOffs(0, 0).addBox(-4.8F, -8.8F, -6F, 9.6F, 1.5F, 2F)
                .texOffs(0, 5).addBox(-5F, -7.3F, -5.8F, .65F, 6.5F, 1F)
                .texOffs(4, 5).addBox(4.35F, -7.3F, -5.8F, .65F, 6.5F, 1F)
                .texOffs(0, 14).addBox(-4.8F, -.8F, -6F, 9.6F, 1.3F, 1.7F)
                .texOffs(0, 20).addBox(-2.1F, -1.6F, -6.9F, 4.2F, 2.8F, 2F)
                .texOffs(16, 20).addBox(-1.3F, -.8F, -7.8F, 2.6F, 1.8F, 1F)
                .texOffs(26, 20).addBox(-.85F, -.4F, -8.3F, 1.7F, 1.1F, .6F)
                // Both side straps run around the head and meet at the back.
                .texOffs(0, 28).addBox(-4.8F, -7.5F, -4.9F, .6F, .9F, 9.7F)
                .texOffs(0, 40).addBox(4.2F, -7.5F, -4.9F, .6F, .9F, 9.7F)
                .texOffs(24, 28).addBox(-4.8F, -7.5F, 4.2F, 9.6F, .9F, .6F)
                .texOffs(0, 28).addBox(-4.8F, -1.3F, -4.9F, .6F, .8F, 9.7F)
                .texOffs(0, 40).addBox(4.2F, -1.3F, -4.9F, .6F, .8F, 9.7F)
                .texOffs(24, 31).addBox(-4.8F, -1.3F, 4.2F, 9.6F, .8F, .6F)
                .texOffs(26, 36).addBox(-1F, -7.6F, 4.5F, 2F, 1.2F, .7F)
                .texOffs(34, 36).addBox(-5.5F, -1.9F, -5.1F, 1.1F, 2.6F, 2.2F)
                .texOffs(42, 36).addBox(4.4F, -1.9F, -5.1F, 1.1F, 2.6F, 2.2F);
        root.addOrReplaceChild("head", frame, PartPose.ZERO);
        for (String name : new String[]{"hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        // Zero depth avoids rendering two translucent surfaces on top of one another.
        root.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.35F, -7.3F, -5.55F, 8.7F, 6.5F, 0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    public void renderMask(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FRAME)), light, overlay);
        renderVisor(pose, buffers, light, overlay);
    }

    private void renderVisor(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        head.translateAndRotate(pose);
        // The glass texture is uniformly tinted; its UV coverage cannot change its opacity.
        visor.render(pose, buffers.getBuffer(RenderType.entityTranslucent(GLASS)), light, overlay);
        pose.popPose();
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        pose.pushPose();
        if (young) {
            pose.scale(.75F, .75F, .75F);
            pose.translate(0, 1F, 0);
        }
        head.render(pose, vertices, light, overlay, red, green, blue, alpha);
        renderVisor(pose, Minecraft.getInstance().renderBuffers().bufferSource(), light, overlay);
        pose.popPose();
    }
}
