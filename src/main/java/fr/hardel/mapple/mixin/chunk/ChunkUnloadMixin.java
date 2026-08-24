package fr.hardel.mapple.mixin.chunk;

import fr.hardel.mapple.optimisation.chunk.ChunkIndex;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkUnloadMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private PoiManager poiManager;

    @Shadow
    @Final
    private Long2ByteMap chunkTypeCache;

    @Inject(method = "lambda$scheduleUnload$0", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2LongMap;remove(J)J"))
    private void mapple$purgeIndexes(ChunkHolder holder, CompletableFuture<?> saveSyncFuture, long chunkKey, CallbackInfo callback) {
        this.chunkTypeCache.remove(chunkKey);
        ((ChunkIndex) this.poiManager).mapple$unloadChunk(chunkKey);
        ((ChunkIndex) this.level.structureManager().structureCheck).mapple$unloadChunk(chunkKey);
    }
}
