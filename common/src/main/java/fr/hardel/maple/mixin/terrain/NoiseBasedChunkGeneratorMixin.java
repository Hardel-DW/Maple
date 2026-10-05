package fr.hardel.maple.mixin.terrain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.maple.optimisation.terrain.NoiseFill;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    /** The aquifer debug view marks blocks on the way: vanilla keeps it. */
    @WrapMethod(method = "doFill")
    private void maple$fill(NoiseChunk noiseChunk, ChunkAccess chunk, Operation<Void> original) {
        if (SharedConstants.DEBUG_AQUIFERS) {
            original.call(noiseChunk, chunk);
            return;
        }

        NoiseGeneratorSettings generatorSettings = this.settings.value();
        new NoiseFill(chunk, noiseChunk, generatorSettings.defaultBlock()).run(noiseChunk.cachingSamplers().get(generatorSettings.noiseRouter().finalDensity()));
    }
}
