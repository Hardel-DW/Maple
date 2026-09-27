package fr.hardel.mapple.mixin.terrain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.mapple.optimisation.surface.SurfacePass;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemMixin {
    @WrapMethod(method = "buildSurface")
    private void mapple$buildSurface(RandomState randomState, BiomeManager biomeManager, boolean useLegacyRandom, WorldGenerationContext generationContext,
        ChunkAccess chunk, NoiseChunk noiseChunk, SurfaceRules.RuleSource ruleSource, @Nullable Set<Holder<Biome>> possibleBiomes, Operation<Void> original) {
        if (!(chunk instanceof ProtoChunk proto) || !SurfacePass.applies(proto, biomeManager)) {
            original.call(randomState, biomeManager, useLegacyRandom, generationContext, chunk, noiseChunk, ruleSource, possibleBiomes);
            return;
        }

        new SurfacePass((SurfaceSystem) (Object) this, randomState, biomeManager, useLegacyRandom, generationContext, proto, noiseChunk, ruleSource,
            possibleBiomes).run();
    }
}
