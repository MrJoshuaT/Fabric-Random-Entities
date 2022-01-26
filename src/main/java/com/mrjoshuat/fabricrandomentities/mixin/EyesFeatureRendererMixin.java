package com.mrjoshuat.fabricrandomentities.mixin;

import com.mrjoshuat.fabricrandomentities.ClientMod;
import com.mrjoshuat.fabricrandomentities.RandomEntitiesManager;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EyesFeatureRenderer.class)
abstract class EyesFeatureRendererMixin<T extends Entity, M extends EntityModel<T>>
    extends FeatureRenderer<T, M> {
    public EyesFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Shadow public abstract RenderLayer getEyesTexture();

    @Inject(
        at = @At("INVOKE"),
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V",
        cancellable = true
    )
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Entity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch,
                       CallbackInfo info) {
        // Legit ewwwwww noises
        var layer = (RenderLayer.MultiPhase)getEyesTexture();
        var phases = layer.getPhases();
        var phaseAccessor = ((MultiPhaseParametersAccessor)(Object)phases);
        var id = phaseAccessor.getTexture().getId();

        if (id.isEmpty())
            return;

        var randomTextureId = RandomEntitiesManager.getEyesTexture(entity, id.get());
        if (randomTextureId == null) {
            return;
        }

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEyes(randomTextureId));
        this.getContextModel().render(matrices, vertexConsumer, 0xF00000, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

        info.cancel();
    }
}
