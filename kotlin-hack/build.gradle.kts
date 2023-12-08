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

val kotlin_version = "1.9.21"
dependencies {

  implementation(group = "org.jetbrains.kotlin", name = "kotlin-compiler-cli-for-ide", version = "1.9.21-631")
  { isTransitive = false }

//  implementation(project(":kotlin"))
//  {
//    exclude(group = "com.simiacryptus", module = "jo-penai")
//    exclude(group = "com.simiacryptus.skyenet", module = "core")
//    exclude(group = "commons-io", module = "")
//    exclude(group = "org.slf4j", module = "")
//    exclude(group = "org.jetbrains.kotlinx", module = "")
//    exclude(group = "org.jetbrains.kotlin", module = "")
//  }


  implementation(group = "org.jetbrains.kotlin", name = "kotlin-scripting-jsr223", version = kotlin_version)
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

  implementation(group = "org.jetbrains.kotlin", name = "kotlin-scripting-jvm-host", version = kotlin_version)
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

  implementation(group = "org.jetbrains.kotlin", name = "kotlin-scripting-compiler-embeddable", version = kotlin_version)
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

  implementation(group = "org.jetbrains.kotlin", name = "kotlin-scripting-jvm", version = kotlin_version)
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

  implementation(group = "org.jetbrains.kotlin", name = "kotlin-compiler-embeddable", version = kotlin_version)
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

}

tasks.withType(ShadowJar::class.java).configureEach {
  archiveClassifier.set("")
  isZip64 = true
  mergeServiceFiles()
  relocate("org.jetbrains.kotlin.com.intellij.core.", "aicoder.com.intellij.core.")
  relocate("org.jetbrains.kotlin.com.intellij.mock.", "aicoder.com.intellij.mock.")
  relocate("org.jetbrains.kotlin.com.intellij.openapi.extensions.", "aicoder.com.intellij.openapi.extensions.")
  relocate("org.jetbrains.kotlin.com.", "com.")

  relocate("org.jetbrains.org.", "org.")
  relocate("org.jetbrains.kotlin.org.", "org.")
  relocate("org.jetbrains.kotlin.it.", "it.")

  // need com/intellij/openapi/util/StaxFactory
  // exclude com.intellij.openapi.editor.Document
  // exclude com.intellij.openapi.application
  // exclude com.intellij.openapi.progress.Task.WithResult
  exclude("org/jetbrains/kotlin/com/intellij/openapi/editor/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/application/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/progress/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/command/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/components/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/diagnostic/**")
  //exclude("org/jetbrains/kotlin/com/intellij/openapi/extensions/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/fileEditor/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/fileTypes/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/module/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/progress/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/project/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/projectRoots/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/roots/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/ui/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/vfs/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/wm/**")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/util/TextRange.class")
  exclude("org/jetbrains/kotlin/com/intellij/openapi/*.class")


  // exclude com.intellij.mock.MockApplication
  //include com.intellij.mock.MockComponentManager (needs to have classloader in plugin)
  //exclude("org/jetbrains/kotlin/com/intellij/mock/**")

  // exclude com.intellij.core.CoreApplicationEnvironment
  //exclude("org/jetbrains/kotlin/com/intellij/core/**")

  // exclude com.fasterxml.aalto.in.ReaderScanner
  exclude("org/jetbrains/kotlin/com/fasterxml/**")


  // needs com.intellij.util.messages.impl.MessageBusImpl
  // exclude com.intellij.util.Consumer
  // exclude com.intellij.util.messages.MessageBus
  exclude("org/jetbrains/kotlin/com/intellij/util/*.class")
  exclude("org/jetbrains/kotlin/com/intellij/util/messages/*.class")


  exclude("org/jetbrains/kotlin/com/intellij/lang/**")

  // ??? include com.intellij.psi.compiled.ClassFileDecompilers$Decompiler (in com.intellij.java)
  // exclude com.intellij.psi.PsiManager
//  exclude("org/jetbrains/kotlin/com/intellij/psi/*.class")
  exclude("org/jetbrains/kotlin/com/intellij/psi/**")


  exclude("org/jetbrains/kotlin/config/**")

}

tasks.named("build") {
  dependsOn("shadowJar")
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
      artifact(tasks["shadowJar"])
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
