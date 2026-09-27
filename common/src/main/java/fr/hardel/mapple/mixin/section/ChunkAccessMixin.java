package fr.hardel.mapple.mixin.section;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.hardel.mapple.optimisation.section.SharedSectionDataHolder;
import java.util.Arrays;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An imposter reads everything from the chunk it wraps: its own sections only take the writes vanilla throws away, and its sky sources are the wrapped ones.
 * A proto chunk of a level shares its block data from the start; one built for an upgrade or by a mod outside a level has no level to share with.
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Shadow
    protected ChunkSkyLightSources skyLightSources;

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;replaceMissingSections("
                + "Lnet/minecraft/world/level/chunk/PalettedContainerFactory;[Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"
        )
    )
    private void mapple$imposterSections(PalettedContainerFactory containerFactory, LevelChunkSection[] sections, Operation<Void> original,
        @Local(argsOnly = true) LevelHeightAccessor heightAccessor) {
        if ((Object) this instanceof ImposterProtoChunk) {
            Arrays.fill(sections, ((SharedSectionDataHolder) heightAccessor).mapple$sharedSectionData().imposterSection());
            this.skyLightSources = null;
            return;
        }

        original.call(containerFactory, sections);
        if ((Object) this instanceof ProtoChunk && heightAccessor instanceof SharedSectionDataHolder level) {
            level.mapple$sharedSectionData().shareStates(sections);
        }
    }
}
