package com.mrjoshuat.fabricrandomentities;

import com.mrjoshuat.fabricrandomentities.resources.RandomProperties;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class RandomEntitiesManager {
    private static HashMap<Integer, Identifier> cacheTextureMap = new HashMap<>();
    private static HashMap<Integer, Identifier> cacheEmissiveMap = new HashMap<>();

    public static Identifier getRandomTexture(Entity entity, Identifier original) {
        // NOTE: villagers will call this method 3 times, so we cannot return the same texture, shucks, needs looking into
        //if (cacheTextureMap.containsKey(entity.getId()))
        //    return cacheTextureMap.get(entity.getId());

        if (!RandomProperties.randomEntityProperties.containsKey(original))
            return original;

        var randomTexture = RandomProperties.randomEntityProperties.get(original).getTextureIdentifier(original, entity);
        if (randomTexture != null) {
            cacheTextureMap.put(entity.getId(), randomTexture);
            return randomTexture;
        }
        return original;
    }

    public static Identifier getEmissiveTexture(Entity entity, Identifier original) {
        // did we do an initial random cache?
        if (!cacheTextureMap.containsKey(entity.getId()))
            return null;
        // did we already try finding a emissive texture?
        if (cacheEmissiveMap.containsKey(entity.getId()))
            return cacheEmissiveMap.get(entity.getId());
        // do we have a properties file for this original?
        if (!RandomProperties.randomEntityProperties.containsKey(original))
            return null;

        var emissiveExtension = RandomProperties.randomEntityProperties.get(original).getEmissiveExtension();
        var emissiveId = makeEmissiveIdentifier(cacheTextureMap.get(entity.getId()), emissiveExtension);
        if (RandomProperties.resourceManager.containsResource(emissiveId)) {
            cacheEmissiveMap.put(entity.getId(), emissiveId);
            return emissiveId;
        }
        return null;
    }

    public static Identifier getEyesTexture(Entity entity, Identifier original) {
        // did we do an initial random cache?
        if (!cacheTextureMap.containsKey(entity.getId()))
            return null;

        // TODO: cache lookup?

        if (!RandomProperties.randomEntityProperties.containsKey(original))
            return null;

        var randomTexture = RandomProperties.randomEntityProperties.get(original).getTextureIdentifier(original, entity);
        if (randomTexture == null)
            return null;
        return randomTexture;
    }

    private static Identifier makeEmissiveIdentifier(Identifier original, String extension) {
        var path = original.getPath();
        path = path.substring(0, path.length() - 4);
        path = path + extension + ".png";
        return new Identifier(original.getNamespace(), path);
    }
}
