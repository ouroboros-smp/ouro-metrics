package com.ouroboros.metrics.fabric;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/** Configuration for the Prometheus HTTP exporter and built-in metric groups. */
public record ExporterConfig(String bind, int port, boolean jvmMetrics, boolean serverMetrics) {

    public static final String FILE_NAME = "ouro-metrics.properties";
    public static final String DEFAULT_BIND = "0.0.0.0";
    public static final int DEFAULT_PORT = 9940;
    public static final boolean DEFAULT_JVM_METRICS = true;
    public static final boolean DEFAULT_SERVER_METRICS = true;

    /**
     * Loads the exporter properties, writing a default file on first use.
     *
     * @param configDirectory Fabric's configuration directory
     * @return the validated exporter configuration
     * @throws IOException if the configuration cannot be read or created
     * @throws IllegalArgumentException if a configured value is invalid
     */
    public static ExporterConfig load(Path configDirectory) throws IOException {
        Path configFile = configDirectory.resolve(FILE_NAME);
        if (Files.notExists(configFile)) {
            writeDefaults(configDirectory, configFile);
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return from(properties);
    }

    static ExporterConfig from(Properties properties) {
        String bind = properties.getProperty("bind", DEFAULT_BIND).trim();
        if (bind.isEmpty()) {
            throw new IllegalArgumentException("Property 'bind' must not be blank");
        }

        int port = parsePort(properties.getProperty("port", Integer.toString(DEFAULT_PORT)));
        boolean jvmMetrics = parseBoolean(
                "jvm-metrics",
                properties.getProperty("jvm-metrics", Boolean.toString(DEFAULT_JVM_METRICS)));
        boolean serverMetrics = parseBoolean(
                "server-metrics",
                properties.getProperty("server-metrics", Boolean.toString(DEFAULT_SERVER_METRICS)));
        return new ExporterConfig(bind, port, jvmMetrics, serverMetrics);
    }

    private static void writeDefaults(Path configDirectory, Path configFile) throws IOException {
        Files.createDirectories(configDirectory);
        Properties defaults = new Properties();
        defaults.setProperty("bind", DEFAULT_BIND);
        defaults.setProperty("port", Integer.toString(DEFAULT_PORT));
        defaults.setProperty("jvm-metrics", Boolean.toString(DEFAULT_JVM_METRICS));
        defaults.setProperty("server-metrics", Boolean.toString(DEFAULT_SERVER_METRICS));
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            defaults.store(writer, "OuroMetrics Fabric exporter configuration");
        }
    }

    private static int parsePort(String value) {
        final int port;
        try {
            port = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Property 'port' must be an integer from 1 to 65535: " + value, exception);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "Property 'port' must be from 1 to 65535: " + port);
        }
        return port;
    }

    private static boolean parseBoolean(String key, String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(
                    "Property '" + key + "' must be true or false: " + value);
        };
    }
}
