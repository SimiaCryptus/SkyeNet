import com.sass_lang.embedded_protocol.OutputStyle
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.net.URI

fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

plugins {
    java
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    `maven-publish`
    id("signing")
    id("io.freefair.sass-base") version "8.4"
    id("io.freefair.sass-java") version "8.4"
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

val kotlin_version = "2.0.20"
val jetty_version = "11.0.24"
val jackson_version = "2.17.2"

dependencies {

    implementation(group = "com.simiacryptus", name = "jo-penai", version = "1.1.7") {
        exclude(group = "org.slf4j")
    }

    implementation(project(":core"))
    implementation(project(":kotlin"))

    implementation("org.apache.pdfbox:pdfbox:2.0.27")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.16.1")
    compileOnly("org.jsoup:jsoup:1.17.2")

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    compileOnly(group = "software.amazon.awssdk", name = "aws-sdk-java", version = "2.27.23")
    runtimeOnly(group = "software.amazon.awssdk", name = "aws-sdk-java", version = "2.27.23")

    implementation("org.openapitools:openapi-generator:7.3.0") {
        exclude(group = "org.slf4j")
    }
    compileOnly("org.openapitools:openapi-generator-cli:7.3.0") {
        exclude(group = "org.slf4j")
    }
    testRuntimeOnly("org.openapitools:openapi-generator-cli:7.3.0")

    implementation(group = "org.eclipse.jetty", name = "jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-servlet", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-annotations", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-server", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-jetty-client", version = jetty_version)
    implementation(group = "org.eclipse.jetty.websocket", name = "websocket-servlet", version = jetty_version)
    implementation(group = "org.eclipse.jetty", name = "jetty-webapp", version = jetty_version)

    implementation(group = "com.vladsch.flexmark", name = "flexmark-all", version = "0.64.8")

    compileOnly(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.8.0-RC")

    compileOnly(kotlin("stdlib"))
    testImplementation(kotlin("stdlib"))

    testImplementation(project(":groovy"))
    testImplementation(project(":kotlin"))
    testImplementation(project(":scala"))

    implementation(group = "org.apache.httpcomponents.client5", name = "httpclient5", version = "5.3.1") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = jackson_version)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jackson_version)

    implementation(group = "com.google.api-client", name = "google-api-client", version = "1.35.2" /*"1.35.2"*/)
    implementation(group = "com.google.oauth-client", name = "google-oauth-client-jetty", version = "1.34.1")
    implementation(group = "com.google.apis", name = "google-api-services-oauth2", version = "v2-rev157-1.25.0")
    implementation(group = "commons-io", name = "commons-io", version = "2.15.0")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.16.0")

    implementation(group = "org.slf4j", name = "slf4j-api", version = "2.0.16")
    runtimeOnly(group = "ch.qos.logback", name = "logback-classic", version = "1.5.8")
    runtimeOnly(group = "ch.qos.logback", name = "logback-core", version = "1.5.8")

    testImplementation(kotlin("script-runtime"))
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.10.1")
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.10.1")
}

sass {
    omitSourceMapUrl.set(false)
    outputStyle.set(OutputStyle.EXPANDED)
    sourceMapContents.set(false)
    sourceMapEmbed.set(false)
    sourceMapEnabled.set(true)
}


tasks {
    compileKotlin {
        compilerOptions {
            javaParameters.set(true)
        }
    }
    compileTestKotlin {
        compilerOptions {
            javaParameters.set(true)
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
tasks.withType<io.freefair.gradle.plugins.sass.SassCompile>().configureEach {
    onlyIf { !project.hasProperty("skipSass") }
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
            artifactId = "webui"
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
                name.set("SkyeNet Web Interface")
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