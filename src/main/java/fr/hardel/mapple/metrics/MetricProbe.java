package fr.hardel.mapple.metrics;

import net.minecraft.server.level.ServerLevel;

public interface MetricProbe {
    void collect(MetricSink sink);

    static String scopeOf(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}
