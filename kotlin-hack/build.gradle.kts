import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
  java
  `java-library`
  `maven-publish`
  id("signing")
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

repositories {
  mavenCentral {
    metadataSources {
      mavenPom()
      artifact()
    }
  }
  maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

val kVersion = "1.9.21"
val kGroup = "org.jetbrains.kotlin"
dependencies {
  implementation(kGroup, name = "kotlin-compiler-cli-for-ide", version = "1.9.21-631") { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jsr223", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm-host", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-compiler-embeddable", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-compiler-embeddable", version = kVersion) { isTransitive = false }
}

// Filtering and assembly
val shadowJarStage1 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("stage1")
  isZip64 = true
  mergeServiceFiles()
  dependsOn("jar")
  configurations = listOf(project.configurations.getByName("runtimeClasspath"))
  doFirst {
    this@registering.includedDependencies.forEach { file ->
      zipTree(file).visit {
        if (this.isDirectory) return@visit
        // Adjust the path so we can express rules based on the desired final paths
        val path = when {
          path.startsWith("org/jetbrains/kotlin/com/") -> path.removePrefix("org/jetbrains/kotlin/")
          path.startsWith("org/jetbrains/kotlin/org/") -> path.removePrefix("org/jetbrains/kotlin/")
          path.startsWith("org/jetbrains/kotlin/it/") -> path.removePrefix("org/jetbrains/kotlin/")
          path.startsWith("org/jetbrains/org/") -> path.removePrefix("org/jetbrains/")
          path.startsWith("org/jetbrains/com/") -> path.removePrefix("org/jetbrains/")
          else -> path
        }
        if (!when {
            path.startsWith("com/intellij/openapi/fileTypes/") -> when {
              path.endsWith("/FileTypeRegistry.class") -> true
              else -> false
            }

            path.startsWith("com/intellij/openapi/application/") -> when {
              path.endsWith("/ApplicationManager.class") -> true
              path.endsWith("/CachedSingletonsRegistry.class") -> true
              else -> false
            }

            path.startsWith("com/intellij/openapi/components/") -> true
            path.startsWith("com/intellij/openapi/extensions/") -> true
            path.startsWith("com/intellij/psi/impl/source/resolve/") -> true
            path.startsWith("com/intellij/util/messages/impl/") -> true

            path.startsWith("com/intellij/") -> when {
              path.endsWith("/DisabledPluginsState.class") -> true
              else -> false
            }

            path.startsWith("org/jetbrains/kotlin/com/intellij/psi/compiled/") -> true

            path.startsWith("org/jetbrains/kotlin/") -> when {
              path.contains("CLICompilerKt") -> true
              path.contains("UtilsKt") -> true
              path.contains("ArgumentsKt") -> true
              path.contains("CompilationContextKt") -> true
              path.contains("JvmReplCompiler") -> true
              else -> false
            }

            path.startsWith("kotlin/script/") -> true
            path.startsWith("kotlin/") -> false
            path.startsWith("misc/") -> false
            path.startsWith("javaslang/") -> false
            path.startsWith("config/") -> false
            path.startsWith("idea/") -> false
            path.startsWith("org/apache/") -> false
            path.startsWith("org/codehaus/stax2/") -> false
            path.startsWith("org/objectweb/") -> false
            else -> true
          }
        ) {
          println("${this.path} excluded from ${file.name} as $path")
          exclude(this.path)
        } else {
          println("${this.path} included from ${file.name} as $path")
        }
      }
    }
  }
}

// Cannonicalization - previously relocated classes should be moved back to their original locations
val shadowJarStage2 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("stage2")
  isZip64 = true
  dependsOn(shadowJarStage1)
  doFirst {
    from(zipTree(shadowJarStage1.get().archiveFile))
    relocate("org.jetbrains.kotlin.com.", "com.")
    relocate("org.jetbrains.org.", "org.")
    relocate("org.jetbrains.kotlin.org.", "org.")
    relocate("org.jetbrains.kotlin.it.", "it.")
  }
}

// Class isolations to avoid conflicts with the IntelliJ classpath
val shadowJarStage3 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("stage3")
  isZip64 = true
  dependsOn(shadowJarStage2)
  doFirst {
    from(zipTree(shadowJarStage2.get().archiveFile))
    val prefix = "aicoder."
    zipTree(shadowJarStage2.get().archiveFile).visit {
      if (this.isDirectory) return@visit
      if (when {
          path.endsWith("/ExtensionPointName.class") -> false
          path.startsWith("com/intellij/openapi/extensions/") -> true
          path.startsWith("com/intellij/") -> true
          path.startsWith("cli/") -> true
          else -> false
        }
      ) {
        val from = path.replace('/', '.').removeSuffix(".class")
        val to = prefix + path.replace('/', '.').removeSuffix(".class")
        println("""relocate("$from", "$to")""")
        relocate(from, to)
      }
    }
  }
}

// Final stage - may not be needed anymore
val shadowJarFinalStage by tasks.registering(ShadowJar::class) {
  dependsOn(shadowJarStage3)
  isZip64 = true
  archiveClassifier.set("") // Final artifact should not have a classifier
  doFirst {
    from(zipTree(shadowJarStage3.get().archiveFile))
//    // Make a backup of the "" classifier jar
//    val backup = archiveFile.get().asFile
//    if (!backup.exists()) return@doFirst
//    val backupFile = File(backup.parentFile, backup.name.removeSuffix(".jar") + "-input.jar")
//    backup.copyTo(backupFile, true)
//    // Final isolations to fix remaining references
//    listOf(
//      "com.intellij.openapi.extensions.AreaInstance"
//    ).forEach { className -> relocate("$className.class", "aicoder.$className.class") }
  }
}

// Update the build task to depend on the second shadowJar stage
tasks.named("build") {
  dependsOn(shadowJarFinalStage)
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
      artifactId = "kotlin-hack"
      //from(components["java"])
      artifact(shadowJarFinalStage.get())
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
        name.set("SkyeNet Kotlin Hacks")
        description.set("Repackaging workaround needed to run Kotlin scripts in IntelliJ")
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
