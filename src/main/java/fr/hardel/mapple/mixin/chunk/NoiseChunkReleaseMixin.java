package fr.hardel.mapple.mixin.chunk;

import java.util.concurrent.CompletableFuture;
import net.minecraft.util.StaticCache2D;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A noise chunk clones the whole density function graph for its chunk and vanilla keeps it until the chunk unloads; the carvers are its last reader. */
@Mixin(ChunkStatusTasks.class)
public abstract class NoiseChunkReleaseMixin {
    @Inject(method = "generateCarvers", at = @At("RETURN"))
    private static void mapple$releaseNoiseChunk(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        chunk.noiseChunk = null;
    }
}
