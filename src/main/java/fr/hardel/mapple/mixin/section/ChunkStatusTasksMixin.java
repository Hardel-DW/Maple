package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.AirSectionCompaction;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/** The steps that fill sections get writable ones before, and give the uniform air back after. Above the generator, so a mod that replaces the fill is covered too. */
@Mixin(ChunkStatusTasks.class)
public abstract class ChunkStatusTasksMixin {
    @Inject(method = {"generateBiomes", "generateNoise", "generateSurface"}, at = @At("HEAD"), require = 3)
    private static void mapple$writableSections(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            SharedAirSection.writable(sections, index);
        }
    }

    @Inject(method = {"generateBiomes", "generateNoise", "generateSurface"}, at = @At("RETURN"), cancellable = true, require = 3)
    private static void mapple$compactAfterTheStep(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        callback.setReturnValue(callback.getReturnValue().thenApply(ChunkStatusTasksMixin::mapple$compacted));
    }

    private static ChunkAccess mapple$compacted(ChunkAccess chunk) {
        ((AirSectionCompaction) chunk).mapple$compactAirSections();
        return chunk;
    }
}
