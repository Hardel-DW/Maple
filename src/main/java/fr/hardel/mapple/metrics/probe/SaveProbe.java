package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.AutosaveTimer;
import fr.hardel.mapple.metrics.ChunkSaveCounter;
import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

public final class SaveProbe implements MetricProbe {
    private final MinecraftServer server;
    private final AutosaveTimer timer;

    public SaveProbe(MinecraftServer server) {
        this.server = server;
        this.timer = (AutosaveTimer) server;
    }

    @Override
    public void collect(MetricSink sink) {
        sink.put(MetricSink.SERVER, "save.autosaveMillis", this.timer.mapple$autosaveMillis());
        for (ServerLevel level : this.server.getAllLevels()) {
            collect(level, sink);
        }
    }

    private static void collect(ServerLevel level, MetricSink sink) {
        String scope = MetricProbe.scopeOf(level);
        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        sink.put(scope, "save.savedChunksTotal", ((ChunkSaveCounter) chunkMap).mapple$savedChunks());
        sink.put(scope, "save.pendingChunks", pendingWrites(chunkMap));
        sink.put(scope, "save.pendingPoiChunks", pendingWrites(level.getPoiManager().simpleRegionStorage));
        if (level.entityManager.permanentStorage instanceof EntityStorage entityStorage) {
            sink.put(scope, "save.pendingEntityChunks", pendingWrites(entityStorage.simpleRegionStorage));
        }
    }

    private static int pendingWrites(SimpleRegionStorage storage) {
        return storage.worker.pendingWrites.size();
    }
}
