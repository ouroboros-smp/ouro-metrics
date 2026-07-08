package com.ouroboros.metrics;

/**
 * Per-plugin metrics facade. All factory methods are idempotent per name: calling
 * {@code counter("kills_total", ...)} twice returns the same instrument.
 *
 * <p>Naming convention (enforced): names are lowercase snake_case without the {@code ouro_<plugin>_}
 * prefix (the facade adds it). Counters end in {@code _total}. Timers end in {@code _seconds}.
 * Keep label cardinality bounded; never use player names or UUIDs as label values.
 */
public interface PluginMetrics {

    /** Monotonic counter. Name must end in {@code _total}. */
    Counter counter(String name, String help, String... labelNames);

    /** Point-in-time value. */
    Gauge gauge(String name, String help, String... labelNames);

    /** Duration histogram in seconds. Name must end in {@code _seconds}. */
    Timer timer(String name, String help, String... labelNames);

    /** Discards everything. Use when the exporter plugin is absent. */
    static PluginMetrics noop() {
        return NoopPluginMetrics.INSTANCE;
    }
}
