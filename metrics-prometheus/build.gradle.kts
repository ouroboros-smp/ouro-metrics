// metrics-prometheus: Prometheus adapter for the metrics-core port. Only the OuroMetrics exporter
// plugin (and later a Minestom equivalent) depends on this; consumer plugins never do.
plugins {
    `java-library`
    `maven-publish`
}

val prometheusMetricsVersion: String by project

dependencies {
    api(project(":metrics-core"))

    api("io.prometheus:prometheus-metrics-core:$prometheusMetricsVersion")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:$prometheusMetricsVersion")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:$prometheusMetricsVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
