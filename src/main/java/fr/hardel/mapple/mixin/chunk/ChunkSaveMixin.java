package fr.hardel.mapple.mixin.chunk;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkSaveMixin {
    @Shadow
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Shadow
    @Final
    private Long2LongMap nextChunkSaveTime;

    @Shadow
    @Final
    private LongSet chunksToEagerlySave;

    @Inject(method = "saveAllChunks", at = @At("HEAD"), cancellable = true)
    private void mapple$queueAutosave(boolean flushStorage, CallbackInfo callback) {
        if (flushStorage) {
            return;
        }

        this.nextChunkSaveTime.clear();
        for (ChunkHolder holder : this.visibleChunkMap.values()) {
            ChunkAccess chunk = holder.getLatestChunk();
            if (chunk != null && chunk.isUnsaved()) {
                this.chunksToEagerlySave.add(holder.getPos().pack());
            }
        }

        callback.cancel();
    }
}
