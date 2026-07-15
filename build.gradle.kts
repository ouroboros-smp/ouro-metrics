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
