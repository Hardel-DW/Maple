package fr.hardel.mapple.mixin.section;

import fr.hardel.mapple.optimisation.section.SharedAirSection;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Ore placement takes sections through here and writes into them directly. */
@Mixin(BulkSectionAccess.class)
public abstract class BulkSectionAccessMixin {
    @Redirect(
        method = "lambda$getSection$0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection mapple$writableSection(ChunkAccess chunk, int index) {
        return SharedAirSection.writable(chunk.getSections(), index);
    }
}
