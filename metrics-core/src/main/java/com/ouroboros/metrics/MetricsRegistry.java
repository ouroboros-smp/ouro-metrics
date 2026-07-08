package com.ouroboros.metrics;

/**
 * Entry-point service for the Ouroboros metrics pipeline.
 *
 * <p>The OuroMetrics exporter plugin registers an implementation of this interface in the Bukkit
 * ServicesManager. Consumer plugins load it once in {@code onEnable} and hold the resulting
 * {@link PluginMetrics}:
 *
 * <pre>{@code
 * MetricsRegistry svc = getServer().getServicesManager().load(MetricsRegistry.class);
 * PluginMetrics metrics = svc != null ? svc.forPlugin("mehen") : PluginMetrics.noop();
 * }</pre>
 *
 * <p>On Minestom the same interface is implemented by a Minestom adapter; consumer code does not
 * change.
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
