package fr.hardel.mapple.mixin.light;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin {
    @Redirect(method = "lightChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;setLightCorrect(Z)V"))
    private void mapple$keepLightedChunksCorrect(ChunkAccess chunk, boolean lightCorrect, ChunkAccess centerChunk, boolean lighted) {
        if (!lighted) {
            chunk.setLightCorrect(false);
        }
    }
}
