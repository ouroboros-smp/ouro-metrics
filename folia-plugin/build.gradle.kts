// folia-plugin: the OuroMetrics exporter plugin. Owns the shared PrometheusRegistry and the
// /metrics HTTP endpoint, and registers MetricsRegistry in the Bukkit ServicesManager.
// Prometheus classes are relocated; com.ouroboros.metrics is NOT relocated because consumer
// plugins link against it through the plugin classloader.
plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

val paperApiVersion: String by project
val prometheusMetricsVersion: String by project

dependencies {
    compileOnly("dev.folia:folia-api:$paperApiVersion")
    implementation(project(":metrics-prometheus"))
    // Used directly here (endpoint + JVM metrics); metrics-prometheus keeps them as
    // implementation details so they stay off consumer compile classpaths.
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:$prometheusMetricsVersion")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:$prometheusMetricsVersion")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("OuroMetrics")
    archiveClassifier.set("")
    relocate("io.prometheus", "com.ouroboros.metrics.libs.prometheus")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
