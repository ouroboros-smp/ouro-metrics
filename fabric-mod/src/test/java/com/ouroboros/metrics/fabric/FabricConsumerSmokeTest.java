package com.ouroboros.metrics.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ouroboros.metrics.MetricsRegistry;
import com.ouroboros.metrics.prometheus.PrometheusMetricsRegistry;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FabricConsumerSmokeTest {

    @Test
    void consumerMetricAppearsWithPluginPrefix() throws IOException {
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        MetricsRegistry metricsRegistry = new PrometheusMetricsRegistry(prometheusRegistry);
        OuroMetricsApi.publish(metricsRegistry);
        try {
            OuroMetricsApi.registry()
                    .forPlugin("smoke")
                    .counter("pings_total", "Smoke-test pings")
                    .inc();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new PrometheusTextFormatWriter(false).write(output, prometheusRegistry.scrape());
            String exposition = output.toString(StandardCharsets.UTF_8);
            assertTrue(
                    exposition.lines().anyMatch(line -> line.startsWith("ouro_smoke_pings_total ")),
                    exposition);
        } finally {
            OuroMetricsApi.clear(metricsRegistry);
        }
    }
}
