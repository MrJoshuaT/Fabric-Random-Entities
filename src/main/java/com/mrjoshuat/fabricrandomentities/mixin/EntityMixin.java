package com.mrjoshuat.fabricrandomentities.mixin;

import com.mrjoshuat.fabricrandomentities.ClientMod;
import com.mrjoshuat.fabricrandomentities.resources.IEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin implements IEntity {
    @Unique public BlockPos spawnPosition = null;
    @Unique public Identifier spawnBiome = null;

    @Shadow public World world;

    @Inject(
        method = "onSpawnPacket(Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;)V",
        at = @At("TAIL")
    )
    private void onSpawnPacket(EntitySpawnS2CPacket packet, CallbackInfo info) {
        ClientMod.LOGGER.info("[onSpawnPacket] Location {} {} {}", packet.getX(), packet.getY(), packet.getZ());
        spawnPosition = new BlockPos(packet.getX(), packet.getY(), packet.getZ());
        spawnBiome = getBiomeFromPos(spawnPosition);
    }

    @Inject(
        method = "updateTrackedPosition(DDD)V",
        at = @At("TAIL")
    )
    private void updateTrackedPosition(double x, double y, double z, CallbackInfo info) {
        // NOTE: Only set the spawn position one time
        if (spawnPosition != null)
            return;

        ClientMod.LOGGER.info("[updateTrackedPosition] Location {} {} {}", x, y, z);
        spawnPosition = new BlockPos(x, y, z);
        spawnBiome = getBiomeFromPos(spawnPosition);
    }

    private Identifier getBiomeFromPos(BlockPos pos) {
        return world.getRegistryManager().get(Registry.BIOME_KEY).getId(world.getBiome(pos));
    }

    @Override
    public BlockPos spawnPosition() { return this.spawnPosition; }

    @Override
    public Identifier spawnBiome() { return this.spawnBiome; }
}
