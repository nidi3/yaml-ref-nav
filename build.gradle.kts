plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "guru.nidi"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        intellijDependencies()
    }
}

dependencies {
    intellijPlatform {
        // Build against any IDE that bundles the YAML plugin.
        // To run on YOUR IDE most reliably, set this to your exact version,
        // e.g. intellijIdeaCommunity("2026.1") or use a different IDE type.
        intellijIdeaCommunity("2024.2.5")
        bundledPlugin("org.jetbrains.plugins.yaml")
        instrumentationTools()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    // The platform test framework throws org.opentest4j.AssertionFailedError but
    // doesn't put opentest4j on the Gradle test runtime classpath — add it ourselves.
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

tasks.test {
    // BasePlatformTestCase derives from JUnit3 TestCase; the vintage engine in
    // junit 4.13.2 runs it. Keep the JVM headless for the in-memory fixture.
    systemProperty("java.awt.headless", "true")
}

// This plugin adds no settings UI, so there are no searchable options to index.
// The task launches a full headless IDE (slow, needs network for the marketplace
// update-check) and only emits warnings on CI — disable it.
tasks.buildSearchableOptions {
    enabled = false
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            // Wide upper bound so the plugin loads on newer IDEs without a rebuild.
            untilBuild = "299.*"
        }
    }
}

kotlin {
    jvmToolchain(21)
}
