import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.net.URI

fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

plugins {
    java
    `java-library`
    `maven-publish`
    id("signing")
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
}

repositories {
    mavenCentral {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
}

kotlin {
    jvmToolchain(11)
//    jvmToolchain(17)
}

val kotlin_version = "1.7.22"
val jetty_version = "11.0.15"
dependencies {

    implementation("com.simiacryptus:joe-penai:1.0.10")

    implementation(project(":core"))
    implementation(project(":webui"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.7.1")
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.cloud:google-cloud-texttospeech:2.0.0")

    implementation("org.eclipse.jetty:jetty-server:$jetty_version")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("ch.qos.logback:logback-core:1.4.7")
    implementation("commons-io:commons-io:2.11.0")

    testImplementation(kotlin("script-runtime"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

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
        systemProperty("surefire.useManifestOnlyJar", "false")
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        jvmArgs(
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED"
        )
    }
}


val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {

    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "util"
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("SkyeNet Utilities")
                description.set("A very helpful puppy")
                url.set("https://github.com/SimiaCryptus/SkyeNet")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("acharneski")
                        name.set("Andrew Charneski")
                        email.set("acharneski@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://git@github.com/SimiaCryptus/SkyeNet.git")
                    developerConnection.set("scm:git:ssh://git@github.com/SimiaCryptus/SkyeNet.git")
                    url.set("https://github.com/SimiaCryptus/SkyeNet")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://oss.sonatype.org/mask/repositories/snapshots"
            url = URI(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: System.getProperty("ossrhUsername")
                        ?: properties("ossrhUsername")
                password = System.getenv("OSSRH_PASSWORD") ?: System.getProperty("ossrhPassword")
                        ?: properties("ossrhPassword")
            }
        }
    }
    if (System.getenv("GPG_PRIVATE_KEY") != null && System.getenv("GPG_PASSPHRASE") != null) afterEvaluate {
        signing {
            sign(publications["mavenJava"])
        }
    }
}

if (System.getenv("GPG_PRIVATE_KEY") != null && System.getenv("GPG_PASSPHRASE") != null) {
    apply<SigningPlugin>()
    configure<SigningExtension> {
        useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PASSPHRASE"))
        sign(configurations.archives.get())
    }
}
