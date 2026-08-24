package fr.hardel.mapple.mixin;

import fr.hardel.mapple.metrics.ChunkSaveCounter;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkSaveCounter {
    @Unique
    private long mapple$savedChunks;

    @Inject(method = "saveChunkIfNeeded", at = @At("RETURN"))
    private void mapple$countSavedChunk(ChunkHolder chunk, long now, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ()) {
            this.mapple$savedChunks++;
        }
    }

    @Override
    public long mapple$savedChunks() {
        return this.mapple$savedChunks;
    }
}
