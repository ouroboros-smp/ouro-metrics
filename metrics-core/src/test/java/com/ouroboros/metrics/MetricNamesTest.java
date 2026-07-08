package com.ouroboros.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MetricNamesTest {

    @Test
    void acceptsValidNames() {
        assertEquals("patrol", MetricNames.requireValid("patrol", "plugin id"));
        assertEquals("kills_total", MetricNames.requireCounterName("kills_total"));
        assertEquals("scan_duration_seconds", MetricNames.requireTimerName("scan_duration_seconds"));
    }

    @Test
    void rejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireValid(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireValid("", "x"));
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireValid("Patrol", "x"));
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireValid("1bad", "x"));
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireValid("has-dash", "x"));
    }

    @Test
    void enforcesSuffixConventions() {
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireCounterName("kills"));
        assertThrows(IllegalArgumentException.class, () -> MetricNames.requireTimerName("scan_ms"));
    }

    @Test
    void noopIsInertAndReusable() {
        PluginMetrics m = PluginMetrics.noop();
        m.counter("x_total", "h").inc();
        m.gauge("g", "h").set(4.0);
        try (var ignored = m.timer("t_seconds", "h").start()) {
            // no-op
        }
        assertEquals(PluginMetrics.noop(), m);
    }
}
