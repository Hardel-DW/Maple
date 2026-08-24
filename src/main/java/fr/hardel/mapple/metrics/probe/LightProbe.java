package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;

public final class LightProbe implements MetricProbe {
    private final MinecraftServer server;

    public LightProbe(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void collect(MetricSink sink) {
        for (ServerLevel level : this.server.getAllLevels()) {
            String scope = MetricProbe.scopeOf(level);
            LevelLightEngine engine = level.getLightEngine();
            collect(sink, scope, "light.block", engine.blockEngine);
            collect(sink, scope, "light.sky", engine.skyEngine);
        }
    }

    private static void collect(MetricSink sink, String scope, String prefix, @Nullable LightEngine<?, ?> engine) {
        if (engine == null) {
            return;
        }

        LayerLightSectionStorage<?> storage = engine.storage;
        long compact = 0L;
        long materialized = 0L;
        long uniform = 0L;
        for (DataLayer layer : storage.visibleSectionData.map.values()) {
            byte[] data = layer.data;
            if (data == null) {
                compact++;
            } else {
                materialized++;
                uniform += isUniform(data) ? 1L : 0L;
            }
        }

        sink.put(scope, prefix + ".compact", compact);
        sink.put(scope, prefix + ".materialized", materialized);
        sink.put(scope, prefix + ".uniform", uniform);
        sink.put(scope, prefix + ".queuedSections", storage.queuedSections.size());
        sink.put(scope, prefix + ".sectionStates", storage.sectionStates.size());
    }

    private static boolean isUniform(byte[] data) {
        byte first = data[0];
        for (byte value : data) {
            if (value != first) {
                return false;
            }
        }

        return true;
    }
}
