package com.ouroboros.metrics;

import java.util.regex.Pattern;

/** Naming rules for the ouro_* metric namespace. */
public final class MetricNames {

    private static final Pattern VALID = Pattern.compile("[a-z][a-z0-9_]*");

    private MetricNames() {}

    /**
     * Validates a plugin id or metric name fragment.
     *
     * @throws IllegalArgumentException when {@code name} does not match {@code [a-z][a-z0-9_]*}
     */
    public static String requireValid(String name, String what) {
        if (name == null || !VALID.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    what + " must match [a-z][a-z0-9_]*, got: " + name);
        }
        return name;
    }

    /** Counters end in _total. */
    public static String requireCounterName(String name) {
        requireValid(name, "counter name");
        if (!name.endsWith("_total")) {
            throw new IllegalArgumentException("counter name must end in _total, got: " + name);
        }
        return name;
    }

    /** Timers end in _seconds. */
    public static String requireTimerName(String name) {
        requireValid(name, "timer name");
        if (!name.endsWith("_seconds")) {
            throw new IllegalArgumentException("timer name must end in _seconds, got: " + name);
        }
        return name;
    }
}
