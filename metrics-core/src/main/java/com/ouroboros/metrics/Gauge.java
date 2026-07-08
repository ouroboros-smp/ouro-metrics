package com.ouroboros.metrics;

/** Point-in-time value. Label values must match the label names the gauge was created with. */
public interface Gauge {

    void set(double value, String... labelValues);

    void inc(String... labelValues);

    void dec(String... labelValues);
}
