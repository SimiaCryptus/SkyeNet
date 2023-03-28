import java.net.URI

fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

plugins {
    `maven-publish`
    id("signing")
}

repositories {
    mavenCentral()
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "skyenet"
//            from(components["java"])
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
