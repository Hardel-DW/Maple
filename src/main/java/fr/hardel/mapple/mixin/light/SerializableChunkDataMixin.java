package fr.hardel.mapple.mixin.light;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SerializableChunkData.class)
public abstract class SerializableChunkDataMixin {
    @Inject(method = "read", at = @At("RETURN"))
    private void mapple$readAsSaved(CallbackInfoReturnable<ChunkAccess> callback) {
        ChunkAccess chunk = callback.getReturnValue();
        (chunk instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : chunk).tryMarkSaved();
    }
}
