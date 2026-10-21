package co.blumeplugins.deathsareitems.entity.client;

import co.blumeplugins.deathsareitems.entity.custom.RewardChestEntity;
import co.blumeplugins.deathsareitems.reward.RewardTimings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RewardChestRenderer extends EntityRenderer<RewardChestEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/chest/normal.png");
    private final ModelPart model;

    public RewardChestRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = context.bakeLayer(ModelLayers.CHEST);
        shadowRadius = .45F;
    }

    @Override
    public void render(RewardChestEntity chest, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        shadowRadius = chest.isSettled() ? 0 : .45F;
        float age = chest.animationAge(partialTick);
        float rise = smooth(Mth.clamp(age / RewardTimings.RISE, 0, 1));
        float settleStart = RewardTimings.SETTLE_START + chest.slot() * RewardTimings.SLOT_DELAY;
        float settle = Mth.clamp((age - settleStart) / RewardTimings.SETTLE_DURATION, 0, 1);
        float originalAge = RewardTimings.originalAge(age);
        float rotation = age < settleStart ? originalAge * 12 : RewardTimings.originalAge(settleStart) * 12 + (1 - (1 - settle) * (1 - settle)) * 90;
        // Finish at the exact facing angle, with no discontinuity at the end.
        rotation = Mth.lerp(smooth(settle), rotation, 1080);
        if (!chest.isSettled()) {
            pose.pushPose();
            float hover = age >= RewardTimings.RISE && settle < 1 ? Mth.sin(originalAge * .15F) * .07F : 0;
            float bounce = Mth.sin(settle * Mth.PI) * .16F;
            pose.translate(0, -1.15F * (1 - rise) + hover + bounce, 0);
            pose.mulPose(Axis.YP.rotationDegrees(chest.getYRot() + rotation));
            if (settle > 0 && settle < 1) {
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(originalAge * 2.5F) * 9 * (1 - settle)));
                pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(originalAge * 2.1F) * 5 * (1 - settle)));
            }
            pose.translate(-.5, 0, -.5);
            float lidAngle = -.35F * Mth.sin(settle * Mth.PI);
            model.getChild("lid").xRot = lidAngle;
            model.getChild("lock").xRot = lidAngle;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
            model.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

        if (age >= RewardTimings.settlesAt(chest.slot())) {
            pose.pushPose();
            pose.translate(0, 1.17, 0);
            // A world-fixed plane faces the original center; the camera never changes its yaw.
            pose.mulPose(Axis.YP.rotationDegrees(chest.getYRot()));
            // Move along the chest's local front, above the lid edge and latch.
            pose.translate(0, 0, .46);
            pose.scale(.022F, -.022F, .022F);
            Component label = Component.translatable(chest.label());
            Font font = getFont();
            float x = -font.width(label) / 2F;
            int alpha = Math.max(8, (int) (Mth.clamp((age - RewardTimings.settlesAt(chest.slot())) / 10, 0, 1) * 255));
            // Vanilla's sign outline separates the glyph depth from its outline. A nametag
            // background plus drop shadow can occlude parts of a fixed, angled text plane.
            font.drawInBatch8xOutline(label.getVisualOrderText(), x, 0, (alpha << 24) | 0xFFFF55,
                    (alpha << 24) | 0x242400, pose.last().pose(), buffers, 0xF000F0);
            pose.popPose();
        }
    }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }
    @Override public ResourceLocation getTextureLocation(RewardChestEntity entity) { return TEXTURE; }
}
