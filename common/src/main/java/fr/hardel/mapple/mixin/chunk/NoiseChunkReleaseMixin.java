package fr.hardel.mapple.mixin.chunk;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A noise chunk clones the whole density function graph for its chunk and vanilla keeps it until the chunk unloads.
 * BIOMES and CARVERS are the two steps a chunk can rest on after building it, so each releases it.
 */
@Mixin(ChunkStatusTasks.class)
public abstract class NoiseChunkReleaseMixin {
    @Inject(method = {"generateBiomes", "generateCarvers"}, at = @At("RETURN"), cancellable = true)
    private static void mapple$releaseNoiseChunk(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        callback.setReturnValue(callback.getReturnValue().thenApply(NoiseChunkReleaseMixin::mapple$released));
    }

    @Unique
    private static ChunkAccess mapple$released(ChunkAccess chunk) {
        chunk.noiseChunk = null;
        return chunk;
    }
}
