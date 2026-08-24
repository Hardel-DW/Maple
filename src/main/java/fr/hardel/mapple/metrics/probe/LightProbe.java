package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import fr.hardel.mapple.optimisation.light.LayerMapAccess;
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
        LayerCensus census = new LayerCensus();
        ((LayerMapAccess) storage.visibleSectionData).mapple$layers().forEach(census::accept);
        sink.put(scope, prefix + ".compact", census.compact);
        sink.put(scope, prefix + ".materialized", census.materialized);
        sink.put(scope, prefix + ".uniform", census.uniform);
        sink.put(scope, prefix + ".queuedSections", storage.queuedSections.size());
        sink.put(scope, prefix + ".sectionStates", storage.sectionStates.size());
    }

    private static final class LayerCensus {
        private long compact;
        private long materialized;
        private long uniform;

        private void accept(DataLayer layer) {
            byte[] data = layer.data;
            if (data == null) {
                this.compact++;
                return;
            }

            this.materialized++;
            this.uniform += isUniform(data) ? 1L : 0L;
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
}
