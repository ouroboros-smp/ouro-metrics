package com.ouroboros.metrics.folia;

import com.ouroboros.metrics.Gauge;
import com.ouroboros.metrics.MetricsRegistry;
import com.ouroboros.metrics.PluginMetrics;
import com.ouroboros.metrics.prometheus.PrometheusMetricsRegistry;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared Prometheus exporter for all Ouroboros plugins.
 *
 * <p>Owns one {@link PrometheusRegistry} and one HTTP exposition endpoint per server process.
 * Consumer plugins declare {@code depend: [OuroMetrics]} (or a guarded softdepend) and obtain
 * their facade through the ServicesManager. Folia-safe: the exporter runs on its own daemon
 * thread; the server gauges refresh on the async scheduler.
 */
public final class OuroMetricsPlugin extends JavaPlugin {

    private HTTPServer server;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PrometheusRegistry registry = new PrometheusRegistry();

        if (getConfig().getBoolean("jvm-metrics", true)) {
            JvmMetrics.builder().register(registry);
        }

        PrometheusMetricsRegistry service = new PrometheusMetricsRegistry(registry);
        getServer().getServicesManager()
                .register(MetricsRegistry.class, service, this, ServicePriority.Normal);

        if (getConfig().getBoolean("server-metrics", true)) {
            PluginMetrics self = service.forPlugin("server");
            Gauge playersOnline = self.gauge("players_online", "Players currently online");
            Gauge pluginsLoaded = self.gauge("plugins_loaded", "Plugins currently loaded");
            getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
                playersOnline.set(getServer().getOnlinePlayers().size());
                pluginsLoaded.set(getServer().getPluginManager().getPlugins().length);
            }, 5, 5, TimeUnit.SECONDS);
        }

        String bind = getConfig().getString("bind", "0.0.0.0");
        int port = getConfig().getInt("port", 9940);
        try {
            server = HTTPServer.builder()
                    .hostname(bind)
                    .port(port)
                    .registry(registry)
                    .buildAndStart();
            getLogger().info("Prometheus exporter listening on " + bind + ":" + port + "/metrics");
        } catch (IOException e) {
            getLogger().severe("Failed to start Prometheus exporter on " + bind + ":" + port
                    + ": " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.close();
            server = null;
        }
        getServer().getServicesManager().unregisterAll(this);
    }
}
