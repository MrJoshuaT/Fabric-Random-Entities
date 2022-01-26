package com.mrjoshuat.fabricrandomentities;

import com.mrjoshuat.fabricrandomentities.feature.EmissiveFeature;
import net.fabricmc.api.ClientModInitializer;
import com.mrjoshuat.fabricrandomentities.resources.RandomProperties;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.model.EntityModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("random-entities");

    @Override
    public void onInitializeClient() {
        RandomProperties.load();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer.getModel() instanceof EntityModel) {
                registrationHelper.register(new EmissiveFeature<>(entityRenderer));
            }
        });
    }
}
