// Root build: shared conventions only. Each module declares its own dependencies.
plugins {
    java
}

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

val dashboard = layout.projectDirectory.file("ops/observability/grafana/dashboards/ouroboros-plugins.json")
val observabilityInventory = layout.projectDirectory.file("ops/observability/README.md")
val verifyObservabilityContracts by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies producer-owned metric names in observability assets."
    inputs.files(dashboard, observabilityInventory)
    doLast {
        for (asset in listOf(dashboard.asFile, observabilityInventory.asFile)) {
            val contents = asset.readText()
            require("coffer_containers_bound_total" in contents) {
                "${asset.path} must query coffer_containers_bound_total"
            }
            require("coffer_access_denied_total" in contents) {
                "${asset.path} must query coffer_access_denied_total"
            }
            require("ouro_coffer_" !in contents) {
                "${asset.path} contains the stale ouro_coffer_ namespace"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyObservabilityContracts)
}
