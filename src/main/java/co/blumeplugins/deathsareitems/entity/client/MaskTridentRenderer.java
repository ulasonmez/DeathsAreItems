package co.blumeplugins.deathsareitems.entity.client;

import co.blumeplugins.deathsareitems.entity.custom.MaskTridentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MaskTridentRenderer extends EntityRenderer<MaskTridentEntity> {
    private final TridentModel model;
    public MaskTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new TridentModel(context.bakeLayer(ModelLayers.TRIDENT));
    }
    @Override public void render(MaskTridentEntity trident, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float heading = Mth.rotLerp(partialTick, trident.yRotO, trident.getYRot());
        float pitch = Mth.lerp(partialTick, trident.xRotO, trident.getXRot());
        if (trident.isCharging() && trident.chargeOwner() != null) {
            // Render from the interpolated owner, so network corrections cannot shake the held spear.
            Vec3 anchor = MaskTridentEntity.heldPosition(trident.chargeOwner(), partialTick);
            Vec3 rendered = new Vec3(Mth.lerp(partialTick, trident.xOld, trident.getX()),
                    Mth.lerp(partialTick, trident.yOld, trident.getY()), Mth.lerp(partialTick, trident.zOld, trident.getZ()));
            Vec3 offset = anchor.subtract(rendered);
            pose.translate(offset.x, offset.y, offset.z);
            Vec3 direction = trident.visualAim(partialTick);
            heading = (float)Math.toDegrees(Math.atan2(direction.x, direction.z));
            pitch = (float)Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
        }
        pose.scale(.5F, .5F, .5F);
        pose.mulPose(Axis.YP.rotationDegrees(heading - 90));
        pose.mulPose(Axis.ZP.rotationDegrees(pitch + 90));
        // Roll around the shaft so the three prongs lie horizontally beneath the crosshair.
        pose.mulPose(Axis.YP.rotationDegrees(90));
        var consumer = ItemRenderer.getFoilBufferDirect(buffers, model.renderType(getTextureLocation(trident)), false, trident.isFoil());
        model.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
        super.render(trident, yaw, partialTick, pose, buffers, light);
    }
    @Override public ResourceLocation getTextureLocation(MaskTridentEntity trident) { return ThrownTridentRenderer.TRIDENT_LOCATION; }
}
