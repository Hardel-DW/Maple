package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.DiscardingSection;
import java.util.Arrays;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
}
