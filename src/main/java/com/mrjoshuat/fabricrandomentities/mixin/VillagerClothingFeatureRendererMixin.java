package com.mrjoshuat.fabricrandomentities.mixin;

import com.mrjoshuat.fabricrandomentities.ClientMod;
import com.mrjoshuat.fabricrandomentities.RandomEntitiesManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerClothingFeatureRenderer.class)
public class VillagerClothingFeatureRendererMixin {
    @Unique private Entity lastRenderedEntity;

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
        at = @At("HEAD")
    )
    private void renderStoreLastEntity(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, LivingEntity livingEntity,
                        float f, float g, float h, float j, float k, float l, CallbackInfo info) {
        lastRenderedEntity = livingEntity;
    }

    @Inject(
        method = "findTexture(Ljava/lang/String;Lnet/minecraft/util/Identifier;)Lnet/minecraft/util/Identifier;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void findRandomTexture(String keyType, Identifier keyId, CallbackInfoReturnable<Identifier> info) {
        var randomTextureId = RandomEntitiesManager.getRandomTexture(lastRenderedEntity, info.getReturnValue());

        info.setReturnValue(randomTextureId);
        info.cancel();
    }
}
