package com.ouroboros.metrics.prometheus;

import com.ouroboros.metrics.MetricNames;
import com.ouroboros.metrics.MetricsRegistry;
import com.ouroboros.metrics.PluginMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Prometheus-backed implementation of the metrics port. Thread-safe. */
public final class PrometheusMetricsRegistry implements MetricsRegistry {

    private final PrometheusRegistry registry;
    private final ConcurrentMap<String, PluginMetrics> plugins = new ConcurrentHashMap<>();

    public PrometheusMetricsRegistry(PrometheusRegistry registry) {
        this.registry = registry;
    }

    @Override
    public PluginMetrics forPlugin(String pluginId) {
        MetricNames.requireValid(pluginId, "plugin id");
        return plugins.computeIfAbsent(
                pluginId, id -> new PrometheusPluginMetrics(registry, "ouro_" + id + "_"));
    }
}
