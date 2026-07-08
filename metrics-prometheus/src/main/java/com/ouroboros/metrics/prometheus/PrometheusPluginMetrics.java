package com.ouroboros.metrics.prometheus;

import com.ouroboros.metrics.Counter;
import com.ouroboros.metrics.Gauge;
import com.ouroboros.metrics.MetricNames;
import com.ouroboros.metrics.PluginMetrics;
import com.ouroboros.metrics.Timer;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-plugin facade backed by a shared {@link PrometheusRegistry}. Instrument creation is
 * idempotent per name; observation paths are lock-free after first creation.
 */
final class PrometheusPluginMetrics implements PluginMetrics {

    private final PrometheusRegistry registry;
    private final String prefix;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    PrometheusPluginMetrics(PrometheusRegistry registry, String prefix) {
        this.registry = registry;
        this.prefix = prefix;
    }

    @Override
    public Counter counter(String name, String help, String... labelNames) {
        return counters.computeIfAbsent(name, n -> {
            MetricNames.requireCounterName(n);
            var c = io.prometheus.metrics.core.metrics.Counter.builder()
                    .name(prefix + n)
                    .help(help)
                    .labelNames(labelNames)
                    .register(registry);
            boolean labeled = labelNames.length > 0;
            return new Counter() {
                @Override
                public void inc(String... labelValues) {
                    if (labeled) {
                        c.labelValues(labelValues).inc();
                    } else {
                        c.inc();
                    }
                }

                @Override
                public void add(double amount, String... labelValues) {
                    if (labeled) {
                        c.labelValues(labelValues).inc(amount);
                    } else {
                        c.inc(amount);
                    }
                }
            };
        });
    }

    @Override
    public Gauge gauge(String name, String help, String... labelNames) {
        return gauges.computeIfAbsent(name, n -> {
            MetricNames.requireValid(n, "gauge name");
            var g = io.prometheus.metrics.core.metrics.Gauge.builder()
                    .name(prefix + n)
                    .help(help)
                    .labelNames(labelNames)
                    .register(registry);
            boolean labeled = labelNames.length > 0;
            return new Gauge() {
                @Override
                public void set(double value, String... labelValues) {
                    if (labeled) {
                        g.labelValues(labelValues).set(value);
                    } else {
                        g.set(value);
                    }
                }

                @Override
                public void inc(String... labelValues) {
                    if (labeled) {
                        g.labelValues(labelValues).inc();
                    } else {
                        g.inc();
                    }
                }

                @Override
                public void dec(String... labelValues) {
                    if (labeled) {
                        g.labelValues(labelValues).dec();
                    } else {
                        g.dec();
                    }
                }
            };
        });
    }

    @Override
    public Timer timer(String name, String help, String... labelNames) {
        return timers.computeIfAbsent(name, n -> {
            MetricNames.requireTimerName(n);
            Histogram h = Histogram.builder()
                    .name(prefix + n)
                    .help(help)
                    .labelNames(labelNames)
                    .register(registry);
            boolean labeled = labelNames.length > 0;
            return (seconds, labelValues) -> {
                if (labeled) {
                    h.labelValues(labelValues).observe(seconds);
                } else {
                    h.observe(seconds);
                }
            };
        });
    }
}
