package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;

public final class IndexProbe implements MetricProbe {
    private final MinecraftServer server;

    public IndexProbe(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void collect(MetricSink sink) {
        for (ServerLevel level : this.server.getAllLevels()) {
            String scope = MetricProbe.scopeOf(level);
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            sink.put(scope, "index.chunkTypeCache", chunkMap.chunkTypeCache.size());
            sink.put(scope, "index.nextChunkSaveTime", chunkMap.nextChunkSaveTime.size());
            sink.put(scope, "index.structureCheck", level.structureManager().structureCheck.loadedChunks.size());
            sink.put(scope, "index.poiSections", level.getPoiManager().storage.size());
            sink.put(scope, "index.savedData", level.getDataStorage().cache.size());
        }
    }
}
