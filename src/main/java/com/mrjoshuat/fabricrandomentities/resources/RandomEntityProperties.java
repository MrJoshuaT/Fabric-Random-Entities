package com.mrjoshuat.fabricrandomentities.resources;

import com.mrjoshuat.fabricrandomentities.ClientMod;
import net.minecraft.entity.Entity;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

public class RandomEntityProperties {
    private RandomEntityRule[] rules = null;
    private Identifier[] variations = null;
    private String emissiveExtension;

    public RandomEntityProperties(ResourcePack pack, Identifier propertiesFile, String emissiveExtension) throws IOException {
        this.emissiveExtension = emissiveExtension;

        var file = pack.open(ResourceType.CLIENT_RESOURCES, propertiesFile);
        var propertiesOrdered = new Properties();
        propertiesOrdered.load(file);
        file.close();

        parseRules(propertiesOrdered, propertiesFile);

        // TODO: check is valid?
    }

    public RandomEntityProperties(Identifier[] ids, String emissiveExtension) {
        this.emissiveExtension = emissiveExtension;
        variations = ids;
    }

    public String getEmissiveExtension() {
        return this.emissiveExtension;
    }

    private int getEntityId(Entity entity) {
        // NOTE: taken straight from Optifine for compatibility
        return (int)((entity).getUuid().getLeastSignificantBits() & 0x7FFFFFFFL);
    }

    public Identifier getTextureIdentifier(Identifier original, Entity entity) {
        var entityId = getEntityId(entity);
        if (rules != null) {
            for (var rule : rules) {
                if (rule.isMatch(entity))
                    return rule.getTextureIdentifier(original, entityId);
            }
        }
        if (variations != null) {
            int index = entityId % variations.length;
            if (index > variations.length) {
                ClientMod.LOGGER.error("[getTextureIdentifier] tried to get an index out of range, well this is a bug. Length {}, index {}",
                    variations.length, index);
                return original;
            }
            return variations[index];
        }
        return original;
    }

    private void parseRules(Properties props, Identifier propsPath) {
        // NOTE: either skins.{num} or textures.{num}
        var key = getKey(props);

        var rules = new LinkedList<RandomEntityRule>();

        // NOTE: props size is all props, which is far too large anyway, but this allows skipping of missing textures
        for (var i = 1; i < props.size(); i++) {
            if (props.get(key + "." + i) != null) {
                var rule = new RandomEntityRule(props, key, i, propsPath);
                if (rule.isValid()) {
                    rules.add(rule);
                }
            }
        }

        this.rules = rules.toArray(new RandomEntityRule[0]);
    }

    private String getKey(Properties props) {
        var key = "textures";
        if (props.get(key + ".1") == null) {
            key = "skins";
        }
        return key;
    }
}
