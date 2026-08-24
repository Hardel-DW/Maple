package fr.hardel.mapple.metrics;

import fr.hardel.mapple.metrics.probe.ChunkProbe;
import fr.hardel.mapple.metrics.probe.EntityProbe;
import fr.hardel.mapple.metrics.probe.IndexProbe;
import fr.hardel.mapple.metrics.probe.JvmProbe;
import fr.hardel.mapple.metrics.probe.LightProbe;
import fr.hardel.mapple.metrics.probe.SaveProbe;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public final class MetricsService {
    private static volatile MetricsService active;

    private final MinecraftServer server;
    private final List<MetricProbe> probes;
    private @Nullable MeasurementRun run;

    private MetricsService(MinecraftServer server) {
        this.server = server;
        this.probes = List.of(new JvmProbe(), new ChunkProbe(server), new LightProbe(server), new IndexProbe(server), new EntityProbe(server), new SaveProbe(server));
    }

    public static void start(MinecraftServer server) {
        active = new MetricsService(server);
    }

    public static void stop() {
        MetricsService service = active;
        active = null;
        service.stopRun();
    }

    public static void tick() {
        active.tickRun();
    }

    public static MetricsService active() {
        return active;
    }

    public void collect(MetricSink sink) {
        for (MetricProbe probe : this.probes) {
            probe.collect(sink);
        }
    }

    public boolean startRun(String name, int periodTicks, boolean record) {
        if (this.run != null)
            return false;

        this.run = MeasurementRun.open(this.server, name, periodTicks, record);
        return true;
    }

    public @Nullable Path stopRun() {
        MeasurementRun closing = this.run;
        this.run = null;
        return closing == null ? null : closing.close();
    }

    public Path heapDump() {
        MeasurementRun current = this.run;
        return HeapDump.write(current == null ? this.server.getServerDirectory().resolve("logs").resolve("mapple") : current.directory());
    }

    private void tickRun() {
        MeasurementRun current = this.run;
        if (current != null) {
            current.sampleIfDue(this.server.getTickCount(), this::collect);
        }
    }
}
