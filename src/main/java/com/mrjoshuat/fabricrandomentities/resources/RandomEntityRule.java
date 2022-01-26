package com.mrjoshuat.fabricrandomentities.resources;

import com.mrjoshuat.fabricrandomentities.ClientMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Properties;

public class RandomEntityRule {
    private int index = -1;
    private int[] textures;
    private int[] weights;
    private Identifier[] biomes;
    private int[][] heights;
    private int[][] health;
    private boolean healthPercentage;
    // name, complex
    private VillagerProfessionLevel[] professions;
    private DyeColor[] collarColours;
    private Boolean baby;
    private int[][] moonPhases;
    private int[][] dayTimes;
    private Weather[] weathers;

    private int[] weightsSum;
    private int allWeightsSum;
    private Identifier propsPath;

    public RandomEntityRule(Properties props, String key, int index, Identifier propsPath) {
        try {
            this.propsPath = propsPath;
            this.index = index;
            textures = parseIntList(props.getProperty(key + "." + index)); // Either array of range array
            weights = parseIntList(props.getProperty("weights." + index));
            biomes = parseBiome(props.getProperty("biomes." + index));
            heights = parseRangeListInt(props.getProperty("heights." + index));
            if (heights == null) {
                heights = parseMinMaxHeight(props, index);
            }
            health = parseHealth(props.getProperty("health." + index));
            // name, pattern: ipattern, regex, iregex, hex colours
            professions = parseProfessions(props.getProperty("professions." + index));
            collarColours = parseDyeColours(props.getProperty("collarColors." + index));
            baby = parseBoolean(props.getProperty("baby." + index));
            moonPhases = parseRangeListInt(props.getProperty("moonPhase." + index));
            dayTimes = parseRangeListInt(props.getProperty("dayTime." + index));
            weathers = parseWeather(props.getProperty("weather." + index));

            calculateWeights();
        }
        catch (Exception ex) {
            ClientMod.LOGGER.error("[RandomEntityRule] failed to load rule, reason {}", ex.getMessage());
        }
    }

    private void calculateWeights() {
        if (this.weights == null)
            return;

        this.weightsSum = new int[this.weights.length];
        int sum = 0;
        for (int i = 0; i < this.weights.length; i++) {
            if (this.weights[i] < 0) {
                ClientMod.LOGGER.info("[calculateWeights] weight {} at index {} cannot be below 0", this.weights[i], i);
                this.weightsSum = null;
                this.allWeightsSum = 0;
                return;
            }
            sum += this.weights[i];
            this.weightsSum[i] = sum;
        }
        this.allWeightsSum = sum;
        if (this.allWeightsSum <= 0) {
            this.allWeightsSum = 1;
        }
    }

    public boolean isValid() {
        // TODO: define what is valid?
        return true;
    }

    public boolean isMatch(Entity entity) {
        var ientity = (IEntity)entity;
        var biome = ientity.spawnBiome();
        var pos = ientity.spawnPosition();

        // onSpawnPacket or updateTrackedPosition not called yet, this should not happen ...
        if (pos == null || biome == null)
            return false;

        // NOTE: biome can be null
        if (biomes != null && !isInBiomes(biomes, biome))
            return false;
        if (heights != null && !isPositionInRange(heights, pos))
            return false;
        if (health != null && !isInRange(health, getHealth(entity)))
            return false;
        // check name
        if (professions != null && !isInProfession(professions, entity))
            return false;
        if (collarColours != null && !isInCollarColours(collarColours, entity))
            return false;
        if (baby != null && !isBaby(entity))
            return false;
        if (moonPhases != null && !isInRange(moonPhases, entity.world.getMoonPhase()))
            return false;
        if (dayTimes != null && !isInRange(dayTimes, entity.world.getTimeOfDay()))
            return false;
        if (weathers != null && Arrays.stream(weathers).noneMatch(weather -> weather == getEntityWeather(entity)))
            return false;
        return true;
    }

    public Identifier getTextureIdentifier(Identifier original, int seed) {
        if (textures == null || textures.length == 0)
            return original;
        var index = 0;
        if (weights == null) {
            index = seed % textures.length;
        } else {
            int randWeight = seed % allWeightsSum;
            for (int i = 0; i < weightsSum.length; i++) {
                if (weightsSum[i] > randWeight) {
                    index = i;
                    break;
                }
            }
        }

        // TODO: from texture, make id
        if (index > textures.length) {
            ClientMod.LOGGER.error("[getTextureIdentifier] attempted to get texture out of index, at {} with size {}", index, textures.length);
            return original;
        }
        if (index == 0 || index == 1)
            return original;

        return makeIndexIdentifier(original, index);
    }

    private Identifier makeIndexIdentifier(Identifier original, int index) {
        var path = original.getPath();
        path = path.substring(0, path.length() - 4);
        path = path + index + ".png";
        path = RandomProperties.getOptifinePathBase(path);
        return new Identifier(original.getNamespace(), path);
    }

    private boolean isPositionInRange(int[][] range, BlockPos pos) {
        if (pos == null)
            return false;
        return isInRange(range, pos.getY());
    }

    private boolean isInRange(int[][] range, long val) {
        for (var ints : range) {
            if (val < ints[0] || val > ints[1])
                return false;
        }
        return true;
    }

    private boolean isInBiomes(Identifier[] ids, Identifier entityBiomeId) {
        if (entityBiomeId == null)
            return false;
        return Arrays.stream(ids).anyMatch(biome -> biome == entityBiomeId);
    }

    private boolean isInProfession(VillagerProfessionLevel[] levels, Entity entity) {
        // just need to match one
        if (!(entity instanceof VillagerEntity villagerEntity))
            return false;

        for (var professions : levels) {
            if (professions.isMatch(villagerEntity))
                return true;
        }
        return false;
    }

    private boolean isInCollarColours(DyeColor[] colours, Entity entity) {
        DyeColor collarColour = null;
        if (entity instanceof CatEntity catEntity) {
            collarColour = catEntity.getCollarColor();
        } else if (entity instanceof WolfEntity wolfEntity) {
            collarColour = wolfEntity.getCollarColor();
        }
        if (collarColour == null)
            return false;

        var id = collarColour.getId();
        if (Arrays.stream(colours).anyMatch(colour -> id == colour.getId()))
            return true;
        return false;
    }

    private boolean isBaby(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity))
            return false;
        return baby && livingEntity.isBaby();
    }

    private int getHealth(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity))
            return 0;

        var health = (int)livingEntity.getHealth();
        if (this.healthPercentage) {
            var healthMax = livingEntity.getMaxHealth();
            if (healthMax > 0)
                return (int) ((health * 100) / healthMax);
        }
        return health;
    }

    private Weather getEntityWeather(Entity entity) {
        if (entity.world.isThundering())
            return Weather.Thunder;
        if (entity.world.isRaining())
            return Weather.Rain;
        return Weather.Clear;
    }

    private int[] parseIntList(String input) {
        if (input == null)
            return null;

        var arr = new LinkedList<Integer>();
        // split by both space and comma
        Arrays.stream(input.split("[ ,]")).forEach(str -> {
            str = str.trim();
            if (str.equals("")) {
                return;
            }

            if (str.contains("-")) {
                var range = str.split("-");
                if (range.length != 2) {
                    ClientMod.LOGGER.warn("[parseIntList] a range can only have 2 numbers, got {}", range.length);
                    return;
                }
                var min = Integer.parseInt(range[0]);
                var max = Integer.parseInt(range[1]);
                for (var i = min; i != max; i++) {
                    arr.add(i);
                }
            } else {
                arr.add(Integer.parseInt(str.trim()));
            }
        });
        return arr.stream().mapToInt(i -> i).toArray();
    }

    private Identifier[] parseBiome(String input) {
        if (input == null)
            return null;

        var arr = new LinkedList<Identifier>();

        // e.g. FrozenOcean DeepFrozenOcean -> frozenocean deepfrozenocean
        var split = Arrays.stream(input.split(" ")).map(String::toLowerCase).toList();
        BuiltinRegistries.BIOME.getIds().forEach(id -> {
            // id = frozen_ocean deep_frozen_ocean -> frozenocean deepfrozenocean
            var optifineId = id.getPath().replace("_", "").toLowerCase();
            if (split.contains(optifineId)) {
                arr.add(id);
            }
        });

        // TODO: warn about missing biomes
        // TODO: add mappings for older biome names in newer versions?

        return arr.toArray(new Identifier[0]);
    }

    private int[][] parseRangeListInt(String input) {
        if (input == null)
            return null;

        var arr = new LinkedList<int[]>();
        Arrays.stream(input.split("[ ,]")).forEach(str -> {
            str = str.trim();
            if (str.equals("")) {
                return;
            }
            var range = parseRangeInt(str);
            if (range != null)
                arr.add(range);
        });
        return arr.toArray(new int[0][]);
    }

    private int[] parseRangeInt(String input) {
        if (input == null)
            return null;

        // e.g. 1-3
        if (!input.contains("-")) {
            return parseIntList(input);
        }
        /* If optifine wasn't so jank we could just do -64--20, 1-2, 10--2 - ([-]?[0-9]+)-([-]?[0-9]+) which works */
        /* But negative values need to be in () - so add \(? \)? to each side of the numbers and ignore the random format */
        var splitInput = input.split("\\(?([-]?[0-9]+)\\)?-\\(?([-]?[0-9]+)\\)?");
        if (splitInput.length != 2) {
            ClientMod.LOGGER.warn("[parseRangeIntArray] expected 2 numbers but got {}", splitInput.length);
            return null;
        }
        var start = Integer.parseInt(splitInput[0]);
        var end = Integer.parseInt(splitInput[1]);
        if (start > end) {
            ClientMod.LOGGER.warn("[parseRangeIntArray] cannot have min ({}) higher than max ({})", start, end);
            return null;
        }
        return new int[] { start, end };
    }

    private int[][] parseMinMaxHeight(Properties props, int index) {
        var minHeightStr = props.getProperty("minHeight." + index);
        var maxHeightStr = props.getProperty("maxHeight." + index);

        if (minHeightStr == null || maxHeightStr == null)
            return null;

        // TODO: can we get these from a world instance instead?
        var minHeight = -64;
        var maxHeight = 320;

        try {
            minHeight = Integer.parseInt(minHeightStr);
        }
        catch (NumberFormatException ex) {
            ClientMod.LOGGER.warn("[parseMinMaxHeight] invalid min height value {}", maxHeightStr);
            return null;
        }

        try {
            maxHeight = Integer.parseInt(maxHeightStr);
        }
        catch (NumberFormatException ex) {
            ClientMod.LOGGER.warn("[parseMinMaxHeight] invalid max height value {}", maxHeightStr);
            return null;
        }

        return new int[][] { new int[] { minHeight, maxHeight } };
    }

    private int[][] parseHealth(String input) {
        if (input == null)
            return null;

        healthPercentage = input.contains("%");
        if (healthPercentage) {
            input = input.replace("%", "");
        }
        return parseRangeListInt(input);
    }

    private Boolean parseBoolean(String input) {
        if (input == null)
            return null;
        return Boolean.parseBoolean(input);
    }

    private Weather[] parseWeather(String input) {
        if (input == null)
            return null;
        return Arrays.stream(input.split(" "))
            .map(Weather::get)
            .filter(Objects::nonNull)
            .toArray(Weather[]::new);
    }

    private DyeColor[] parseDyeColours(String input) {
        if (input == null)
            return null;
        return Arrays.stream(input.split(" "))
            .map(dye -> DyeColor.byName(dye, null))
            .filter(Objects::nonNull)
            .toArray(DyeColor[]::new);
    }

    private VillagerProfessionLevel[] parseProfessions(String input) {
        if (input == null)
            return null;
        // e.g. farmer librarian:1,3-4
        return Arrays.stream(input.split(" "))
            .map(this::parseProfession)
            .filter(Objects::nonNull)
            .toArray(VillagerProfessionLevel[]::new);
    }

    private VillagerProfessionLevel parseProfession(String input) {
        if (input == null)
            return null;

        var split = input.split(":");

        var profession = split[0];
        var level = split.length > 1 ? Integer.parseInt(split[1]) : -1;

        var parsedProfession = Registry.VILLAGER_PROFESSION.get(Identifier.tryParse(profession));
        return new VillagerProfessionLevel(parsedProfession, level);
    }

    private enum Weather {
        Clear("clear"),
        Rain("rain"),
        Thunder("thunder");

        private String format;
        Weather(String format) {
            this.format = format;
        }

        public static Weather get(String input) {
            var trimmedInput = input.trim();
            return Arrays.stream(values()).filter(w -> Objects.equals(w.format, trimmedInput)).findFirst().orElse(null);
        }
    }

    private record VillagerProfessionLevel(VillagerProfession profession, int level) {
        public boolean isMatch(VillagerEntity villager) {
            var villagerData = villager.getVillagerData();
            var profession = villagerData.getProfession();
            var level = villagerData.getLevel();
            if (this.profession.equals(profession)) {
                return this.level == -1 || this.level == level;
            }
            return false;
        }
    }
}
