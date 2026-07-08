package com.ouroboros.metrics;

/** Discards all observations. Singleton behind {@link PluginMetrics#noop()}. */
final class NoopPluginMetrics implements PluginMetrics {

    static final NoopPluginMetrics INSTANCE = new NoopPluginMetrics();

    private static final Counter NOOP_COUNTER = new Counter() {
        @Override public void inc(String... labelValues) {}
        @Override public void add(double amount, String... labelValues) {}
    };

    private static final Gauge NOOP_GAUGE = new Gauge() {
        @Override public void set(double value, String... labelValues) {}
        @Override public void inc(String... labelValues) {}
        @Override public void dec(String... labelValues) {}
    };

    private static final Timer.Sample NOOP_SAMPLE = () -> {};

    private static final Timer NOOP_TIMER = new Timer() {
        @Override public void observe(double seconds, String... labelValues) {}
        @Override public Sample start(String... labelValues) { return NOOP_SAMPLE; }
    };

    private NoopPluginMetrics() {}

    @Override
    public Counter counter(String name, String help, String... labelNames) {
        return NOOP_COUNTER;
    }

    @Override
    public Gauge gauge(String name, String help, String... labelNames) {
        return NOOP_GAUGE;
    }

    @Override
    public Timer timer(String name, String help, String... labelNames) {
        return NOOP_TIMER;
    }
}
