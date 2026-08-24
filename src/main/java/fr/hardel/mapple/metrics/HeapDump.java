package fr.hardel.mapple.metrics;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.util.Util;

public final class HeapDump {
    private HeapDump() {
    }

    public static Path write(Path directory) {
        Path file = directory.resolve("heap-" + Util.getFilenameFormattedDateTime() + ".hprof");
        try {
            Files.createDirectories(directory);
            ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).dumpHeap(file.toString(), true);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write the heap dump " + file, exception);
        }

        return file;
    }
}
