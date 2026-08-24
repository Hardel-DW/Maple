package fr.hardel.mapple.metrics;

@FunctionalInterface
public interface MetricSink {
    String SERVER = "server";

    void put(String scope, String metric, long value);
}
