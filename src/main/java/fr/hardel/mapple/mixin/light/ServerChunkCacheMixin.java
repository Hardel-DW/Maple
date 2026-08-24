package fr.hardel.mapple.mixin.light;

import fr.hardel.mapple.optimisation.light.LightDataListener;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements LightDataListener {
    @Shadow
    @Final
    private ServerChunkCache.MainThreadExecutor mainThreadProcessor;

    @Shadow
    protected abstract @Nullable ChunkHolder getVisibleChunkIfPresent(long key);

    @Override
    public void mapple$onLightDataChanged(LongSet chunkKeys) {
        this.mainThreadProcessor.execute(() -> chunkKeys.forEach(this::mapple$markUnsaved));
    }

    @Unique
    private void mapple$markUnsaved(long chunkKey) {
        ChunkHolder holder = this.getVisibleChunkIfPresent(chunkKey);
        ChunkAccess chunk = holder == null ? null : holder.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
        if (chunk != null) {
            chunk.markUnsaved();
        }
    }
}
