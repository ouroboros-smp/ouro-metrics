package com.ouroboros.metrics.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExporterConfigTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesAndLoadsDefaultsOnFirstRun() throws IOException {
        ExporterConfig config = ExporterConfig.load(tempDirectory);

        assertEquals(ExporterConfig.DEFAULT_BIND, config.bind());
        assertEquals(ExporterConfig.DEFAULT_PORT, config.port());
        assertTrue(config.jvmMetrics());
        assertTrue(config.serverMetrics());
        assertTrue(Files.isRegularFile(tempDirectory.resolve(ExporterConfig.FILE_NAME)));
    }

    @Test
    void loadsOverridesFromPropertiesFile() throws IOException {
        Files.writeString(
                tempDirectory.resolve(ExporterConfig.FILE_NAME),
                "bind=127.0.0.1\nport=19940\njvm-metrics=false\nserver-metrics=false\n",
                StandardCharsets.UTF_8);

        ExporterConfig config = ExporterConfig.load(tempDirectory);

        assertEquals("127.0.0.1", config.bind());
        assertEquals(19940, config.port());
        assertFalse(config.jvmMetrics());
        assertFalse(config.serverMetrics());
    }

    @Test
    void rejectsInvalidPort() {
        Properties properties = new Properties();
        properties.setProperty("port", "70000");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ExporterConfig.from(properties));

        assertTrue(exception.getMessage().contains("1 to 65535"));
    }

    @Test
    void rejectsInvalidBoolean() {
        Properties properties = new Properties();
        properties.setProperty("jvm-metrics", "yes");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ExporterConfig.from(properties));

        assertTrue(exception.getMessage().contains("jvm-metrics"));
    }
}
