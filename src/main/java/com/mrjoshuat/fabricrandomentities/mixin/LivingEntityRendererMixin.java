package com.mrjoshuat.fabricrandomentities.mixin;

import com.mrjoshuat.fabricrandomentities.RandomEntitiesManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private Entity entity;

    @Inject(
        method = "getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
        at = @At("INVOKE")
    )
    private void getRenderLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable info) {
        this.entity = entity;
    }

    @ModifyVariable(
        method = "getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
        at = @At("STORE"),
        ordinal = 0
    )
    protected Identifier getRenderLayerIdentifier(Identifier i) {
        return RandomEntitiesManager.getRandomTexture(entity, i);
    }
}
