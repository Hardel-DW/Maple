package fr.hardel.mapple.mixin.terrain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.mapple.optimisation.terrain.NoiseFill;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    /** The aquifer debug view and the debug void terrain change blocks on the way: vanilla keeps them. */
    @WrapMethod(method = "doFill")
    private ChunkAccess mapple$fill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess centerChunk, int cellMinY,
        int cellCountY, Operation<ChunkAccess> original) {
        if (SharedConstants.DEBUG_AQUIFERS || SharedConstants.debugVoidTerrain(centerChunk.getPos())) {
            return original.call(blender, structureManager, randomState, centerChunk, cellMinY, cellCountY);
        }

        NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(
            chunk -> ((NoiseBasedChunkGenerator) (Object) this).createNoiseChunk(chunk, structureManager, blender, randomState));
        new NoiseFill(centerChunk, noiseChunk, this.settings.value().defaultBlock()).run(cellMinY, cellCountY);
        return centerChunk;
    }
}
