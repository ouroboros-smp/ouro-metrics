package com.ouroboros.metrics;

/** Monotonic counter. Label values must match the label names the counter was created with. */
public interface Counter {

    /** Increments by 1. */
    void inc(String... labelValues);

    /** Increments by {@code amount}; must be non-negative. */
    void add(double amount, String... labelValues);
}
