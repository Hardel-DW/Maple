package fr.hardel.mapple.metrics.probe;

import fr.hardel.mapple.metrics.MetricProbe;
import fr.hardel.mapple.metrics.MetricSink;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public final class JvmProbe implements MetricProbe {
    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
    private final BufferPoolMXBean directBuffers = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream().filter(pool -> pool.getName().equals("direct")).findFirst().orElseThrow();

    @Override
    public void collect(MetricSink sink) {
        MemoryUsage heap = this.memory.getHeapMemoryUsage();
        sink.put(MetricSink.SERVER, "jvm.heapUsed", heap.getUsed());
        sink.put(MetricSink.SERVER, "jvm.heapCommitted", heap.getCommitted());
        sink.put(MetricSink.SERVER, "jvm.heapMax", heap.getMax());
        sink.put(MetricSink.SERVER, "jvm.directBufferBytes", this.directBuffers.getMemoryUsed());
        sink.put(MetricSink.SERVER, "jvm.directBufferCount", this.directBuffers.getCount());

        long collections = 0L;
        long collectionMillis = 0L;
        for (GarbageCollectorMXBean collector : this.collectors) {
            collections += collector.getCollectionCount();
            collectionMillis += collector.getCollectionTime();
        }

        sink.put(MetricSink.SERVER, "jvm.gcCountTotal", collections);
        sink.put(MetricSink.SERVER, "jvm.gcTimeMillisTotal", collectionMillis);
    }
}
