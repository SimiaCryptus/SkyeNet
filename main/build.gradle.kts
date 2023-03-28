import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
}

repositories {
    mavenCentral() {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
}

tasks {

    compileKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation("com.simiacryptus:joe-penai:1.0.2")

    implementation("com.google.cloud:google-cloud-texttospeech:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("commons-io:commons-io:2.11.0")

    implementation(kotlin("stdlib"))
    implementation(project(":interpreter"))
    testImplementation(kotlin("script-runtime"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}
