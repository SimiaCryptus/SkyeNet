import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    scala
}

repositories {
    mavenCentral()
}

tasks {
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

val ammonite_version = "3.0.0-M0-5-0af4d9e7"
val scala_version = "2.13.10"
dependencies {

    implementation("org.scala-lang:scala-library:$scala_version")
    implementation("com.lihaoyi:ammonite_$scala_version:$ammonite_version")
    implementation("com.lihaoyi:ammonite-runtime_$scala_version:$ammonite_version")
    implementation("com.lihaoyi:ammonite-repl_$scala_version:$ammonite_version")
    implementation("com.lihaoyi:ammonite-repl-api_$scala_version:$ammonite_version")
    implementation("com.lihaoyi:ammonite-interp-api_$scala_version:$ammonite_version")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("commons-io:commons-io:2.11.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}
