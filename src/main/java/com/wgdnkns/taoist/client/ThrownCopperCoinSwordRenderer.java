package com.wgdnkns.taoist.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

import com.wgdnkns.taoist.entity.ThrownCopperCoinSword;

public class ThrownCopperCoinSwordRenderer extends EntityRenderer<ThrownCopperCoinSword> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");

    public ThrownCopperCoinSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ThrownCopperCoinSword entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 90.0F));
        poseStack.translate(0.0, -0.1, 0.0);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getRenderPickupItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownCopperCoinSword entity) {
        return TEXTURE;
    }
}
