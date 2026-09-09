package fr.hardel.mapple.mixin.cold;

import fr.hardel.mapple.optimisation.cold.ColdChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The section array leaves the chunk by copy (LevelChunk from ProtoChunk), so it only leaves hot. */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Inject(method = "getSections", at = @At("HEAD"))
    private void mapple$thawBeforeHandingTheSections(CallbackInfoReturnable<LevelChunkSection[]> callback) {
        if ((Object) this instanceof ColdChunk cold) {
            cold.mapple$thaw();
        }
    }
}
