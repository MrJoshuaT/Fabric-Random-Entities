package com.mrjoshuat.fabricrandomentities.resources;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public interface IEntity {
    BlockPos spawnPosition();
    Identifier spawnBiome();
}
