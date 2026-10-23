package co.blumeplugins.deathsareitems.client;

import co.blumeplugins.deathsareitems.DeathsAreItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/** Head-local geometry shared by the armor layer and every item display context. */
public final class DrowningMaskModel extends HumanoidModel<LivingEntity> {
    public static final ResourceLocation FRAME = new ResourceLocation(DeathsAreItems.MOD_ID,
            "textures/models/armor/drowning_mask.png");

    public static final ResourceLocation GLASS = new ResourceLocation(DeathsAreItems.MOD_ID,
            "textures/models/armor/drowning_mask_visor.png");
    private final ModelPart glass;

    public DrowningMaskModel() {
        this(createRoot());
    }

    private DrowningMaskModel(ModelPart root) {
        super(root);
        glass = root.getChild("glass");
    }

    private static ModelPart createRoot() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

        Map<String, float[]> sizes = new HashMap<>();

        // The large dark rectangle in the swimming reference is the TOP of the head.
        // The front is an open window at eye height, with short overhanging brow tabs.
        box(head, sizes, "upper_brow", 48, 72, -4.15F, -8.95F, -5.65F, 8.3F, 1.65F, .9F);
        box(head, sizes, "left_brow_tab", 28, 80, -4.15F, -7.3F, -5.8F, 1.8F, .75F, 1.05F);
        box(head, sizes, "right_brow_tab", 42, 80, 2.35F, -7.3F, -5.8F, 1.8F, .75F, 1.05F);
        // One atlas texel (.5 head units) removed from each front edge. The stepped
        // guards close the cheek/temple gaps without crossing the visible eye area.
        box(head, sizes, "left_face_guard", 32, 45, -4.15F, -6.55F, -5.8F, .65F, 3.55F, 1.05F);
        box(head, sizes, "right_face_guard", 40, 45, 3.5F, -6.55F, -5.8F, .65F, 3.55F, 1.05F);
        box(head, sizes, "left_lower_guard", 98, 62, -4.15F, -3F, -6.1F, 1F, 1.4F, 1.35F);
        box(head, sizes, "right_lower_guard", 110, 62, 3.15F, -3F, -6.1F, 1F, 1.4F, 1.35F);
        box(head, sizes, "left_corner_return", 0, 0, -4.55F, -8.54F, -5.55F, .4F, 9.74F, 1.02F);
        box(head, sizes, "right_corner_return", 8, 0, 4.15F, -8.54F, -5.55F, .4F, 9.74F, 1.02F);
        box(head, sizes, "left_temple_joint", 88, 72, -4.8F, -7.48F, -4.73F, .25F, .93F, 1.73F);
        box(head, sizes, "right_temple_joint", 98, 72, 4.55F, -7.48F, -4.73F, .25F, .93F, 1.73F);

        // Wide lower crosspiece, stepped side cheeks and a deep central respirator.
        // The lower mass continues underneath the mouth projection, as in the held reference.
        box(head, sizes, "lower_housing", 0, 62, -4.15F, -1.6F, -6.55F, 8.3F, 2.15F, 1.8F);
        box(head, sizes, "left_cheek", 0, 72, -4.15F, .55F, -6.35F, 1.25F, 1.35F, 1.59F);
        box(head, sizes, "right_cheek", 14, 72, 2.9F, .55F, -6.35F, 1.25F, 1.35F, 1.59F);
        box(head, sizes, "respirator_body", 48, 45, -2.9F, .55F, -7.25F, 5.8F, 2F, 2.49F);
        box(head, sizes, "lower_keel", 0, 80, -2.3F, 2.55F, -7F, 4.6F, .7F, 2.23F);
        box(head, sizes, "left_filter", 16, 0, -4.8F, -6.55F, -4.74F, .25F, 7.75F, 2.4F);
        box(head, sizes, "right_filter", 30, 0, 4.55F, -6.55F, -4.74F, .25F, 7.75F, 2.4F);
        box(head, sizes, "nose_bridge", 64, 62, -1.25F, -2.5F, -6.75F, 2.5F, 2.3F, 1.2F);

        // Mottled receiver on a substantial underside, followed by the narrower banded muzzle.
        box(head, sizes, "socket", 44, 62, -1.5F, -.5F, -7.8F, 3F, 2.3F, 1.25F);
        box(head, sizes, "receiver", 84, 45, -1.15F, 0F, -10.4F, 2.3F, 1.9F, 2.6F);
        box(head, sizes, "receiver_underside", 28, 72, -1.35F, 1.9F, -10F, 2.7F, .95F, 2.21F);
        box(head, sizes, "collar", 56, 80, -1.2F, .35F, -10.9F, 2.4F, 1.35F, .5F);
        box(head, sizes, "barrel", 80, 62, -.85F, .45F, -13.2F, 1.7F, 1.1F, 2.3F);
        box(head, sizes, "muzzle", 70, 80, -.9F, .4F, -13.65F, 1.8F, 1.2F, .45F);

        // Rear cap/bands retain .05 units of clearance around the outer skin layer.
        box(head, sizes, "crown", 0, 23, -4.55F, -8.95F, -4.74F, 9.1F, .4F, 9.29F);
        box(head, sizes, "left_upper_strap", 44, 0, -4.8F, -8.54F, -4.73F, .25F, 1.05F, 9.28F);
        box(head, sizes, "right_upper_strap", 86, 0, 4.55F, -8.54F, -4.73F, .25F, 1.05F, 9.28F);
        box(head, sizes, "back_upper_strap", 82, 80, -4.8F, -8.54F, 4.55F, 9.6F, 1.05F, .25F);
        box(head, sizes, "left_lower_strap", 76, 23, -4.8F, -1.8F, -2.33F, .25F, .65F, 6.88F);
        box(head, sizes, "right_lower_strap", 0, 45, 4.55F, -1.8F, -2.33F, .25F, .65F, 6.88F);
        box(head, sizes, "back_lower_strap", 12, 87, -4.8F, -1.8F, 4.55F, 9.6F, .65F, .25F);
        box(head, sizes, "back_buckle", 0, 87, -1F, -2F, 4.81F, 2F, 1.05F, .35F);

        // Exactly ONE two-sided translucent quad. A zero-depth six-face cube would
        // blend two coincident glass faces and make the empty visor too dark.
        root.addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.97F, -7.3F, -5.35F, 7.94F, 5.7F, 0F,
                        EnumSet.of(Direction.NORTH)), PartPose.ZERO);

        for (String name : new String[]{"hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        ModelPart baked = LayerDefinition.create(mesh, 128, 128).bakeRoot();
        sizes.forEach((name, size) -> {
            ModelPart part = baked.getChild("head").getChild(name);
            part.xScale = size[0];
            part.yScale = size[1];
            part.zScale = size[2];
        });
        return baked;
    }

    /** Integer UV dimensions avoid fractional texel seams on thin straps and stepped parts. */
    private static void box(PartDefinition parent, Map<String, float[]> sizes,
                            String name, int u, int v,
                            float x, float y, float z, float width, float height, float depth) {
        int tw = Math.max(1, Math.round(width * 2));
        int th = Math.max(1, Math.round(height * 2));
        int td = Math.max(1, Math.round(depth * 2));
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(0, 0, 0, tw, th, td), PartPose.offset(x, y, z));
        sizes.put(name, new float[]{width / tw, height / th, depth / td});
    }

    public void renderMask(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(FRAME)), light, overlay);
        renderGlass(pose, buffers, light, overlay);
    }

    private void renderGlass(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!head.visible) return;
        pose.pushPose();
        head.translateAndRotate(pose);
        glass.render(pose, buffers.getBuffer(RenderType.entityTranslucent(GLASS)), light, overlay);
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
        renderGlass(pose, Minecraft.getInstance().renderBuffers().bufferSource(), light, overlay);
        pose.popPose();
    }
}
