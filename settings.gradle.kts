pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
    }
    plugins {
        id("fabric-loom") version providers.gradleProperty("loomVersion").get()
    }
}

rootProject.name = "ouro-metrics"

include("metrics-core", "metrics-prometheus", "folia-plugin", "fabric-mod")
