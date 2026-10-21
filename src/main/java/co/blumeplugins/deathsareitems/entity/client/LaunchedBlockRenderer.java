package co.blumeplugins.deathsareitems.entity.client;

import co.blumeplugins.deathsareitems.entity.custom.LaunchedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public final class LaunchedBlockRenderer extends EntityRenderer<LaunchedBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public LaunchedBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = .4F;
    }

    @Override
    public void render(LaunchedBlockEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, .4, 0);
        float direction = (entity.getId() & 1) == 0 ? 1 : -1;
        pose.mulPose(Axis.YP.rotationDegrees(entity.age(partialTick) * 13 * direction));
        pose.mulPose(Axis.XP.rotationDegrees(entity.age(partialTick) * 8));
        pose.scale(.82F, .82F, .82F);
        pose.translate(-.5, -.5, -.5);
        blocks.renderSingleBlock(entity.blockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override public ResourceLocation getTextureLocation(LaunchedBlockEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
