import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
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
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
//    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral() {
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

val kotlin_version = "1.7.21"
val jetty_version = "11.0.15"
val scala_version = "2.13.8"
dependencies {

//    implementation("com.simiacryptus:JoePenai:1.0.7")
    implementation("com.simiacryptus:joe-penai:1.0.7")

    implementation(files("lib/ui.jar"))
    implementation(project(":core"))
    implementation(project(":kotlin"))
    implementation(project(":java"))
    implementation(project(":groovy"))

    implementation("org.eclipse.jetty:jetty-server:$jetty_version")
    implementation("org.eclipse.jetty:jetty-servlet:$jetty_version")
    implementation("org.eclipse.jetty:jetty-annotations:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-server:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-client:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-servlet:$jetty_version")

    implementation("com.google.api-client:google-api-client:1.35.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-oauth2:v2-rev157-1.25.0")

    implementation("com.amazonaws:aws-java-sdk:1.12.454")

    implementation("org.scala-lang:scala-library:$scala_version")
    implementation("org.scala-lang:scala-compiler:$scala_version")
    implementation("org.scala-lang:scala-reflect:$scala_version")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.cloud:google-cloud-texttospeech:2.0.0")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("commons-io:commons-io:2.11.0")

    testImplementation(kotlin("script-runtime"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

}

tasks.withType(ShadowJar::class.java).configureEach {
    archiveClassifier.set("")
    mergeServiceFiles()
    append("META-INF/kotlin_module")

    exclude("**/META-INF/*.SF")
    exclude("**/META-INF/*.DSA")
    exclude("**/META-INF/*.RSA")
    exclude("**/META-INF/*.MF")
    exclude("META-INF/versions/9/module-info.class")
    exclude("META-INF/**")

    exclude("dependencies.properties")
    exclude("kotlinManifest.properties")
    exclude("testng-1.0.dtd")
    exclude("testng.css")

    exclude("jakarta/**")
    exclude("models/**")
    exclude("scala/**")
    exclude("software/**")
    exclude("com/amazonaws/**")
    exclude("org/apache/**")
    exclude("com/beust/**")
    exclude("com/fasterxml/**")
    exclude("com/google/**")
    exclude("com/sun/**")
    exclude("com/thoughtworks/**")
    exclude("com/simiacryptus/openai/**")
    exclude("com/simiacryptus/util/**")

    exclude("edu/**")
    exclude("*.bin")
    exclude("android/**")
    exclude("gnu/**")
    exclude("google/**")
    exclude("groovy/**")
    exclude("groovyjarjarantlr/**")
    exclude("groovyjarjarasm/**")
    exclude("groovyjarjarcommonscli/**")
    exclude("groovyjarjarpicocli/**")
    exclude("grpc/**")
    exclude("images/**")
    exclude("io/**")
    exclude("javaslang/**")
    exclude("javax/**")
    exclude("jline/**")
    exclude("junit/**")
    exclude("kotlin/**")
    exclude("kotlinx/**")
    exclude("messages/**")
    exclude("misc/**")
    exclude("mozilla/**")
    exclude("net/**")
    exclude("org/**")
    exclude("picocli/**")
    exclude("testngtasks/**")
    exclude("org/**")

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
    wrapper {
        gradleVersion = properties("gradleVersion")
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
            artifactId = "skyenet"
            artifact(tasks.shadowJar.get()) {
                classifier = null
            }
            artifact(javadocJar)
            artifact(sourcesJar)
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("SkyeNet")
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
                withXml {
                    asNode().appendNode("dependencies").apply {
                        // Required dependencies
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.simiacryptus")
                            appendNode("artifactId", "joe-penai")
                            appendNode("version", "1.0.7")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.slf4j")
                            appendNode("artifactId", "slf4j-api")
                            appendNode("version", "2.0.5")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "commons-io")
                            appendNode("artifactId", "commons-io")
                            appendNode("version", "2.11.0")
                        }
                        // Optional dependencies
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.google.cloud")
                            appendNode("artifactId", "google-cloud-texttospeech")
                            appendNode("version", "2.0.0")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty")
                            appendNode("artifactId", "jetty-server")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty")
                            appendNode("artifactId", "jetty-servlet")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty")
                            appendNode("artifactId", "jetty-annotations")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty.websocket")
                            appendNode("artifactId", "websocket-jetty-server")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty.websocket")
                            appendNode("artifactId", "websocket-jetty-client")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.eclipse.jetty.websocket")
                            appendNode("artifactId", "websocket-servlet")
                            appendNode("version", jetty_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.google.api-client")
                            appendNode("artifactId", "google-api-client")
                            appendNode("version", "1.35.2")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.google.oauth-client")
                            appendNode("artifactId", "google-oauth-client-jetty")
                            appendNode("version", "1.34.1")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.google.apis")
                            appendNode("artifactId", "google-api-services-oauth2")
                            appendNode("version", "v2-rev157-1.25.0")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.amazonaws")
                            appendNode("artifactId", "aws-java-sdk")
                            appendNode("version", "1.12.454")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.scala-lang")
                            appendNode("artifactId", "scala-library")
                            appendNode("version", scala_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.scala-lang")
                            appendNode("artifactId", "scala-compiler")
                            appendNode("version", scala_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.scala-lang")
                            appendNode("artifactId", "scala-reflect")
                            appendNode("version", scala_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.jetbrains.kotlin")
                            appendNode("artifactId", "kotlin-stdlib")
                            appendNode("version", kotlin_version)
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.jetbrains.kotlinx")
                            appendNode("artifactId", "kotlinx-coroutines-core")
                            appendNode("version", "1.6.4")
                            appendNode("optional", "true")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "org.jetbrains.kotlin")
                            appendNode("artifactId", "stdlib-jdk8")
                            appendNode("version", kotlin_version)
                            appendNode("optional", "true")
                        }
                    }
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
