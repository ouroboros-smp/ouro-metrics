package com.ouroboros.metrics;

/** Duration histogram in seconds. */
public interface Timer {

    /** Records a duration observation in seconds. */
    void observe(double seconds, String... labelValues);

    /**
     * Starts a sample; closing it records the elapsed time. Designed for try-with-resources:
     *
     * <pre>{@code
     * try (var ignored = metrics.timer("scan_duration_seconds", "Room scan cost").start()) {
     *     scan();
     * }
     * }</pre>
     */
    default Sample start(String... labelValues) {
        long t0 = System.nanoTime();
        return () -> observe((System.nanoTime() - t0) / 1_000_000_000.0, labelValues);
    }

    /** A running timer sample. {@link #close()} records the observation and never throws. */
    interface Sample extends AutoCloseable {
        @Override
        void close();
    }
}
