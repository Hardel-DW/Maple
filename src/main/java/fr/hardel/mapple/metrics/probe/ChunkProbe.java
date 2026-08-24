package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;

public final class ChunkProbe implements MetricProbe {
    private final MinecraftServer server;

    public ChunkProbe(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void collect(MetricSink sink) {
        for (ServerLevel level : this.server.getAllLevels()) {
            collect(level, sink);
        }
    }

    private static void collect(ServerLevel level, MetricSink sink) {
        String scope = MetricProbe.scopeOf(level);
        long holders = 0L;
        long full = 0L;
        long proto = 0L;
        long sections = 0L;
        long airSections = 0L;
        long blockEntities = 0L;
        for (ChunkHolder holder : level.getChunkSource().chunkMap.visibleChunkMap.values()) {
            holders++;
            ChunkAccess chunk = holder.getLatestChunk();
            if (chunk instanceof LevelChunk loaded) {
                full++;
                blockEntities += loaded.blockEntities.size();
                for (LevelChunkSection section : loaded.getSections()) {
                    sections++;
                    airSections += section.hasOnlyAir() ? 1L : 0L;
                }
            } else if (chunk instanceof ProtoChunk) {
                proto++;
            }
        }

        sink.put(scope, "chunk.holders", holders);
        sink.put(scope, "chunk.full", full);
        sink.put(scope, "chunk.proto", proto);
        sink.put(scope, "chunk.sections", sections);
        sink.put(scope, "chunk.airSections", airSections);
        sink.put(scope, "chunk.blockEntities", blockEntities);
    }
}
