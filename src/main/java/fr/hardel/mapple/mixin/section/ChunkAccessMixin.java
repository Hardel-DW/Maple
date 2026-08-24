package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.DiscardingSection;
import fr.hardel.mapple.optimisation.section.SharedAirSection;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Shadow
    private static void replaceMissingSections(PalettedContainerFactory containerFactory, LevelChunkSection[] sections) {
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;replaceMissingSections(Lnet/minecraft/world/level/chunk/PalettedContainerFactory;[Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"))
    private void mapple$shareImposterSections(PalettedContainerFactory containerFactory, LevelChunkSection[] sections) {
        if ((Object) this instanceof ImposterProtoChunk) {
            Arrays.fill(sections, DiscardingSection.INSTANCE);
        } else {
            replaceMissingSections(containerFactory, sections);
        }
    }

    @Inject(method = "collectBiomesInPalette", at = @At("HEAD"), cancellable = true)
    private void mapple$collectWrappedBiomes(Set<Holder<Biome>> output, CallbackInfo callback) {
        if ((Object) this instanceof ImposterProtoChunk imposter) {
            imposter.getWrapped().collectBiomesInPalette(output);
            callback.cancel();
        }
    }

    @Redirect(method = "fillBiomesFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;"))
    private LevelChunkSection mapple$writableSection(ChunkAccess chunk, int index) {
        return SharedAirSection.writable(chunk.getSections(), index);
    }
}
