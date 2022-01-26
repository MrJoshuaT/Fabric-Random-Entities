package com.mrjoshuat.fabricrandomentities.resources;

import ca.weblite.objc.Client;
import com.mrjoshuat.fabricrandomentities.ClientMod;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class RandomProperties {
    public static final HashMap<Identifier, RandomEntityProperties> randomEntityProperties = new HashMap<>();

    private static final String Optifine = "optifine";
    private static final String OptifineRandomPathPrefix = Optifine + "/random";
    private static final String OptifineMobPathPrefix = Optifine + "/mob";

    private static String PackEmissiveExtension = "";

    public static ResourceManager resourceManager;

    public static void load() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("minecraft", Optifine);
            }

            @Override
            public void reload(ResourceManager manager) {
                RandomProperties.resourceManager = manager;

                Set<String> basePaths = new HashSet();

                manager.streamResourcePacks().forEach(pack -> {
                    for (var namespace : pack.getNamespaces(ResourceType.CLIENT_RESOURCES)) {
                        var ids = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, Optifine,
                            Integer.MAX_VALUE, path -> (path.endsWith(".properties") || path.endsWith(".png")) && !path.endsWith("emissive.properties"));

                        PackEmissiveExtension = getEmissiveExtension(pack, namespace);

                        ids.forEach(id -> {
                            try {
                                // ignore loading of emissive pngs, probs doesn't matter?
                                if (id.getPath().endsWith(PackEmissiveExtension + ".png"))
                                    return;

                                // replace 0-9, png and properties extensions, add back default .png
                                var pathReplaced = id.getPath()
                                    .replaceAll("[0-9]", "")
                                    .replaceAll("png|properties", "") + "png";
                                var path = getVanillaPathBase(pathReplaced);
                                if (path == null) {
                                    // unrecognised path, such as optifine/anim can contain png files etc
                                    return;
                                }
                                if (!basePaths.contains(path)) {
                                    basePaths.add(path);
                                    createProperties(pack, namespace, path);
                                }
                            }
                            catch (Exception ex) {
                                ClientMod.LOGGER.error("[reload] failed to load {}, reason {}", id, ex.getMessage());
                            }
                        });
                    }
                });
            }

        });
    }

    private static void createProperties(ResourcePack pack, String namespace, String path) {
        ClientMod.LOGGER.info("[createProperties] for Pack {} and path {}", pack.getName(), path);

        var randomProperties = getRandomEntityProperties(pack, namespace, OptifineRandomPathPrefix, path);
        if (randomProperties == null) {
            randomProperties = getRandomEntityProperties(pack, namespace, OptifineMobPathPrefix, path);
        }
        if (randomProperties != null) {
            randomEntityProperties.put(new Identifier(namespace, path), randomProperties);
        } else {
            ClientMod.LOGGER.info("[getRandomEntityProperties] no properties or variants found for {}", path);
        }
    }

    private static RandomEntityProperties getRandomEntityProperties(ResourcePack pack, String namespace, String prefix, String path) {
        var properties = getPropertiesLocation (pack, namespace, prefix, path);
        if (properties != null) {
            try {
                return new RandomEntityProperties(pack, properties, PackEmissiveExtension);
            }
            catch (IOException ex) {
                ClientMod.LOGGER.error("[getRandomEntityProperties] properties failed to load for {} with {}", path, ex.getMessage());
            }
        }
        var variants = getVariantsLocations (pack, namespace, prefix, path);
        if (variants != null)
            return new RandomEntityProperties(variants, PackEmissiveExtension);
        return null;
    }

    private static Identifier getPropertiesLocation(ResourcePack pack, String namespace, String prefix, String pngPath) {
        var pathParts = pngPath.split("/");
        var properties = pathParts[pathParts.length - 1].replace(".png", ".properties");
        var resources = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, prefix, Integer.MAX_VALUE,
            path -> path.endsWith(properties));

        if (resources.size() > 1) {
            ClientMod.LOGGER.info("[getPropertiesLocation] multiple resources found for {}, selecting first one {}",
                properties, resources.stream().findFirst().get());
        }

        if (resources.size() >= 1) {
            return resources.stream().findFirst().get();
        }
        return null;
    }

    private static Identifier[] getVariantsLocations(ResourcePack pack, String namespace, String prefix, String pngPath) {
        ClientMod.LOGGER.info("[getVariantsLocations] pack {} in {} finding variations for {}", pack.getName(), prefix, pngPath);
        var pathParts = pngPath.split("/");
        var textureName = pathParts[pathParts.length - 1].replace(".png", "");
        // clean path from "textures/entity/zombie_villager/type/savanna.png" to "/zombie_villager/type/"
        var texturePath = pngPath
            .replace("textures/entity", "")
            .replace(textureName, "").
            replace(".png", "");
        if (texturePath.endsWith("/"))
            texturePath = texturePath.substring(0, texturePath.length() - 1);
        var resources = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, prefix + texturePath, Integer.MAX_VALUE,
            path ->
                // ignore emissive textures, probs okay?
                !path.endsWith(PackEmissiveExtension + ".png") &&
                // find the default or the indexed textures
                (path.endsWith(textureName + ".png") || path.matches(textureName + "[0-9]+.png")));

        if (resources.size() > 0) {
            var identifiers = resources
                .stream().map(RandomProperties::patchIdentifierSlashes)
                .toArray(Identifier[]::new);
            Arrays.sort(identifiers, new StringNumberComparator());

            ClientMod.LOGGER.info("[getVariantsLocations] pack {} in {} found {} variations", pack.getName(), prefix, resources.size());
            return identifiers;
        }
        return null;
    }

    private static Identifier patchIdentifierSlashes(Identifier id) {
        return new Identifier(id.getNamespace(), id.getPath().replaceAll("//", "/"));
    }

    public static String getOptifinePathBase(String pathRandom) {
        if (pathRandom.startsWith("textures/entity"))
            return replaceStart(pathRandom, "textures/entity", OptifineMobPathPrefix + "/");
        if (pathRandom.startsWith("textures"))
            return replaceStart(pathRandom, "textures", OptifineRandomPathPrefix + "/");
        return pathRandom;
    }

    private static String getVanillaPathBase(String pathRandom) {
        if (pathRandom.startsWith(OptifineRandomPathPrefix))
            return replaceStart(pathRandom, OptifineRandomPathPrefix, "textures/");
        if (pathRandom.startsWith(OptifineMobPathPrefix))
            return replaceStart(pathRandom, OptifineMobPathPrefix, "textures/entity/");
        return null;
    }

    private static String replaceStart(String pathRandom, String oldPrefix, String newPrefix) {
        return newPrefix + pathRandom.substring(oldPrefix.length() + 1);
    }

    private static String getEmissiveExtension(ResourcePack pack, String namespace) {
        var id = pack.findResources(ResourceType.CLIENT_RESOURCES, namespace, Optifine,
            Integer.MAX_VALUE, path -> path.endsWith("emissive.properties")).stream().findFirst();
        if (id.isEmpty())
            return null;
        try (var stream = pack.open(ResourceType.CLIENT_RESOURCES, id.get())) {
            var properties = new Properties();
            properties.load(stream);
            return properties.getProperty("suffix.emissive");
        } catch (Exception ex) {
            ClientMod.LOGGER.error("Failed to load emissive.properties from file " + id + " in pack " + pack.getName(), ex);
        }
        return "_e"; // Fallback to default
    }

    static class StringNumberComparator implements Comparator<Identifier>{
        private static final Pattern p = Pattern.compile("([0-9]+)");
        public int compare(Identifier strNumber1, Identifier strNumber2) {
            int number1 = 0;
            int number2 = 0;

            var m1 = p.matcher(strNumber1.getPath());
            if (m1.find())
                number1 = Integer.parseInt(m1.group(1));
            var m2 = p.matcher(strNumber2.getPath());
            if (m2.find())
                number2 = Integer.parseInt(m2.group(1));

            if( number1 > number2 ){
                return 1;
            }else if( number1 < number2 ){
                return -1;
            }else{
                return 0;
            }
        }

    }
}
