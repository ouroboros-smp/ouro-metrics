package com.ouroboros.metrics;

/**
 * Entry-point service for the Ouroboros metrics pipeline.
 *
 * <p>Each platform's OuroMetrics exporter publishes one implementation of this interface through
 * its platform-specific discovery mechanism. Consumers resolve it once during initialization and
 * hold the resulting {@link PluginMetrics}:
 *
 * <pre>{@code
 * MetricsRegistry registry = platformMetricsRegistry();
 * PluginMetrics metrics = registry.forPlugin("mehen");
 * }</pre>
 *
 * <p>Only discovery is platform-specific; instrumentation against this interface and
 * {@link PluginMetrics} remains portable.
 */
public interface MetricsRegistry {

    /**
     * Returns the metrics facade for one plugin. Every series created through the returned facade
     * is prefixed {@code ouro_<pluginId>_}.
     *
     * @param pluginId lowercase identifier matching {@code [a-z][a-z0-9_]*}, e.g. {@code "patrol"}
     * @return a cached, thread-safe facade; calling twice with the same id returns the same instance
     */
    PluginMetrics forPlugin(String pluginId);
}
