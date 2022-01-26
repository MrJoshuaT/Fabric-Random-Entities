package com.mrjoshuat.fabricrandomentities.feature;

import com.mrjoshuat.fabricrandomentities.RandomEntitiesManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class EmissiveFeature <T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {
    private FeatureRendererContext<T, M> context;

    public EmissiveFeature(FeatureRendererContext<T, M> context) {
        super(context);
        this.context = context;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        var emissiveTextureId = RandomEntitiesManager.getEmissiveTexture(entity, context.getTexture(entity));
        if (emissiveTextureId == null) {
            return;
        }

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.getEmissiveTextureRenderLayer(emissiveTextureId));
        this.getContextModel().render(matrices, vertexConsumer, 0xF00000, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public RenderLayer getEmissiveTextureRenderLayer(Identifier id) {
        // NOTE: getEyes doesn't always work and can produce odd textures
        return RenderLayer.getBeaconBeam(id, true);
    }
}
