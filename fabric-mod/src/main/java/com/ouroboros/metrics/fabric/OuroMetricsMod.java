package com.ouroboros.metrics.fabric;

import com.ouroboros.metrics.Gauge;
import com.ouroboros.metrics.PluginMetrics;
import com.ouroboros.metrics.prometheus.PrometheusMetricsRegistry;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared Prometheus exporter for Ouroboros Fabric mods. */
public final class OuroMetricsMod implements DedicatedServerModInitializer {

    public static final String MOD_ID = "ouro_metrics";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int SERVER_METRICS_REFRESH_TICKS = 100;

    private ExporterConfig config;
    private PrometheusMetricsRegistry metricsRegistry;
    private HTTPServer httpServer;
    private Gauge playersOnline;
    private Gauge modsLoaded;
    private int ticksSinceRefresh;

    @Override
    public void onInitializeServer() {
        config = loadConfig();

        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        if (config.jvmMetrics()) {
            JvmMetrics.builder().register(prometheusRegistry);
        }

        metricsRegistry = new PrometheusMetricsRegistry(prometheusRegistry);
        OuroMetricsApi.publish(metricsRegistry);

        if (config.serverMetrics()) {
            PluginMetrics serverMetrics = metricsRegistry.forPlugin("server");
            playersOnline =
                    serverMetrics.gauge("players_online", "Players currently online");
            modsLoaded = serverMetrics.gauge("mods_loaded", "Fabric mods currently loaded");
            ServerLifecycleEvents.SERVER_STARTED.register(this::refreshServerMetrics);
            ServerTickEvents.END_SERVER_TICK.register(this::onEndServerTick);
        }
        ServerLifecycleEvents.SERVER_STOPPING.register(this::stopExporter);

        startHttpServer(prometheusRegistry);
    }

    private ExporterConfig loadConfig() {
        try {
            return ExporterConfig.load(FabricLoader.getInstance().getConfigDir());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to load OuroMetrics configuration", exception);
        }
    }

    private void startHttpServer(PrometheusRegistry prometheusRegistry) {
        try {
            httpServer = HTTPServer.builder()
                    .hostname(config.bind())
                    .port(config.port())
                    .registry(prometheusRegistry)
                    .buildAndStart();
            LOGGER.info(
                    "Prometheus exporter listening on {}:{}/metrics", config.bind(), config.port());
        } catch (IOException exception) {
            LOGGER.error(
                    "Failed to start Prometheus exporter on {}:{}",
                    config.bind(),
                    config.port(),
                    exception);
        }
    }

    private void onEndServerTick(MinecraftServer server) {
        ticksSinceRefresh++;
        if (ticksSinceRefresh >= SERVER_METRICS_REFRESH_TICKS) {
            refreshServerMetrics(server);
        }
    }

    private void refreshServerMetrics(MinecraftServer server) {
        ticksSinceRefresh = 0;
        playersOnline.set(server.getPlayerList().getPlayerCount());
        modsLoaded.set(FabricLoader.getInstance().getAllMods().size());
    }

    private void stopExporter(MinecraftServer server) {
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
        OuroMetricsApi.clear(metricsRegistry);
        LOGGER.info("Prometheus exporter stopped");
    }
}
