// metrics-core: the zero-dependency metrics port. Plugins compile against this and nothing else.
// Publish with `./gradlew publishToMavenLocal`; consumers resolve com.ouroboros:metrics-core:0.1.0
// from mavenLocal().
plugins {
    `java-library`
    `maven-publish`
}

dependencies {
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
