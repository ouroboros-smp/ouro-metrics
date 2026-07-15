package com.ouroboros.metrics.fabric;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ouroboros.metrics.MetricsRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OuroMetricsApiTest {

    private final MetricsRegistry registry = pluginId -> null;

    @AfterEach
    void clearRegistry() {
        OuroMetricsApi.clear(registry);
    }

    @Test
    void rejectsAccessBeforeInitialization() {
        assertThrows(IllegalStateException.class, OuroMetricsApi::registry);
    }

    @Test
    void publishesRegistryForConsumers() {
        OuroMetricsApi.publish(registry);

        assertSame(registry, OuroMetricsApi.registry());
    }

    @Test
    void rejectsDuplicatePublisher() {
        OuroMetricsApi.publish(registry);

        assertThrows(
                IllegalStateException.class,
                () -> OuroMetricsApi.publish(pluginId -> null));
    }
}
