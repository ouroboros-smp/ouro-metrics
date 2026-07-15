package com.ouroboros.metrics.fabric;

import com.ouroboros.metrics.MetricsRegistry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Entry point for Fabric mods that publish metrics through the shared OuroMetrics registry. */
public final class OuroMetricsApi {

    private static final AtomicReference<MetricsRegistry> REGISTRY = new AtomicReference<>();

    private OuroMetricsApi() {}

    /**
     * Returns the shared registry published by the OuroMetrics server initializer.
     *
     * @return the process-wide metrics registry
     * @throws IllegalStateException if OuroMetrics has not finished initializing
     */
    public static MetricsRegistry registry() {
        MetricsRegistry registry = REGISTRY.get();
        if (registry == null) {
            throw new IllegalStateException(
                    "OuroMetrics is not initialized; declare a dependency on mod 'ouro_metrics'");
        }
        return registry;
    }

    static void publish(MetricsRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        if (!REGISTRY.compareAndSet(null, registry)) {
            throw new IllegalStateException("OuroMetrics registry is already initialized");
        }
    }

    static void clear(MetricsRegistry registry) {
        REGISTRY.compareAndSet(registry, null);
    }
}
