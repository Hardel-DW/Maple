package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.AirSectionCompaction;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The noise fill writes straight into sections; the ones it leaves as air go back to the shared section of their biome. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Redirect(
        method = "doFill",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection mapple$writableSection(ChunkAccess chunk, int index) {
        return SharedAirSection.writable(chunk.getSections(), index);
    }

    @Inject(method = "doFill", at = @At("RETURN"))
    private void mapple$compactAirAboveTerrain(CallbackInfoReturnable<ChunkAccess> callback) {
        ((AirSectionCompaction) callback.getReturnValue()).mapple$compactAirSections();
    }
}
