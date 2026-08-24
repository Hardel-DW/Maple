package fr.hardel.mapple.metrics;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class MeasurementRun implements MetricSink {
    private static final String HEADER = "tick,millis,scope,metric,value";

    private final Path directory;
    private final BufferedWriter writer;
    private final RunMetadata metadata;
    private final @Nullable JfrProfile profile;
    private final long startTick;
    private final long startedAtMillis = System.currentTimeMillis();
    private final Map<String, Map<String, Aggregate>> aggregates = new TreeMap<>();
    private long sampleTick;
    private long sampleMillis;
    private long samples;

    private MeasurementRun(Path directory, BufferedWriter writer, RunMetadata metadata, @Nullable JfrProfile profile, long startTick) {
        this.directory = directory;
        this.writer = writer;
        this.metadata = metadata;
        this.profile = profile;
        this.startTick = startTick;
    }

    public static MeasurementRun open(MinecraftServer server, String name, int periodTicks, boolean record) {
        Path directory = server.getServerDirectory().resolve("logs").resolve("mapple").resolve(name + "-" + Util.getFilenameFormattedDateTime());
        try {
            Files.createDirectories(directory);
            BufferedWriter writer = Files.newBufferedWriter(directory.resolve("metrics.csv"), StandardCharsets.UTF_8);
            writer.write(HEADER);
            writer.newLine();
            JfrProfile profile = record ? JfrProfile.start(directory.resolve("profile.jfr")) : null;
            return new MeasurementRun(directory, writer, RunMetadata.capture(server, name, periodTicks), profile, server.getTickCount());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to open the measurement run " + directory, exception);
        }
    }

    public void sampleIfDue(long tick, Consumer<MetricSink> collector) {
        if (tick % this.metadata.periodTicks() != 0)
            return;

        this.sampleTick = tick;
        this.sampleMillis = System.currentTimeMillis() - this.startedAtMillis;
        this.samples++;
        collector.accept(this);
        flush();
    }

    @Override
    public void put(String scope, String metric, long value) {
        write(this.sampleTick + "," + this.sampleMillis + "," + scope + "," + metric + "," + value);
        this.aggregates.computeIfAbsent(scope, key -> new TreeMap<>()).computeIfAbsent(metric, key -> new Aggregate()).accept(value);
    }

    public Path directory() {
        return this.directory;
    }

    public Path close() {
        if (this.profile != null) {
            this.profile.stop();
        }

        JsonObject summary = new JsonObject();
        summary.add("metadata", this.metadata.toJson());
        summary.add("run", runJson());
        summary.add("metrics", metricsJson());
        try {
            this.writer.close();
            Files.writeString(this.directory.resolve("summary.json"), new GsonBuilder().setPrettyPrinting().create().toJson(summary), StandardCharsets.UTF_8);
            Files.writeString(this.directory.resolve("histogram.txt"), ClassHistogram.capture(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to close the measurement run " + this.directory, exception);
        }

        return this.directory;
    }

    private JsonObject runJson() {
        JsonObject json = new JsonObject();
        json.addProperty("startTick", this.startTick);
        json.addProperty("endTick", this.sampleTick);
        json.addProperty("durationMillis", System.currentTimeMillis() - this.startedAtMillis);
        json.addProperty("samples", this.samples);

        return json;
    }

    private JsonObject metricsJson() {
        JsonObject scopes = new JsonObject();
        this.aggregates.forEach((scope, metrics) -> {
            JsonObject entries = new JsonObject();
            metrics.forEach((metric, aggregate) -> entries.add(metric, aggregate.toJson()));
            scopes.add(scope, entries);
        });

        return scopes;
    }

    private void write(String line) {
        try {
            this.writer.write(line);
            this.writer.newLine();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to append a metric row to " + this.directory, exception);
        }
    }

    private void flush() {
        try {
            this.writer.flush();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to flush " + this.directory, exception);
        }
    }

    private static final class Aggregate {
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        private long last;
        private long sum;
        private long count;

        private void accept(long value) {
            this.min = Math.min(this.min, value);
            this.max = Math.max(this.max, value);
            this.last = value;
            this.sum += value;
            this.count++;
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("min", this.min);
            json.addProperty("max", this.max);
            json.addProperty("last", this.last);
            json.addProperty("mean", (double) this.sum / this.count);
            json.addProperty("samples", this.count);

            return json;
        }
    }
}
