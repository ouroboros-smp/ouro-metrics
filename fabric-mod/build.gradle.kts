import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

// fabric-mod: the OuroMetrics exporter mod. It owns the shared Prometheus registry and
// /metrics endpoint, and exposes MetricsRegistry to consumer mods through OuroMetricsApi.
plugins {
    id("fabric-loom")
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

val minecraftVersion: String by project
val fabricLoaderVersion: String by project
val fabricApiVersion: String by project
val prometheusMetricsVersion: String by project
val modVersion = version.toString()

val shade by configurations.creating

configurations.implementation {
    extendsFrom(shade)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Prometheus and the platform-neutral OuroMetrics modules are flattened into the final jar.
    // Fabric dependencies stay outside this configuration and are supplied by the server.
    shade(project(":metrics-prometheus"))
    shade("io.prometheus:prometheus-metrics-exporter-httpserver:$prometheusMetricsVersion")
    shade("io.prometheus:prometheus-metrics-instrumentation-jvm:$prometheusMetricsVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

val shadowJar by tasks.existing(ShadowJar::class) {
    configurations = listOf(shade)
    archiveBaseName.set("OuroMetrics")
    archiveClassifier.set("dev-shadow")
    relocate("io.prometheus", "com.ouroboros.metrics.libs.prometheus")
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
    archiveBaseName.set("OuroMetrics")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.named("remapJar"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "fabric-mod"
            artifact(tasks.named("remapJar"))
        }
    }
}
