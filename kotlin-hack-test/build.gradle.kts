import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  java
  `java-library`
  `maven-publish`
  id("signing")
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
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
  maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
  maven(url = "https://www.jetbrains.com/intellij-repository/releases/")
}

kotlin {
  jvmToolchain(11)
}

val kVersion = "1.9.21"
val kGroup = "org.jetbrains.kotlin"
dependencies {
  implementation(project(":core"))

  implementation(project(":kotlin"))
  {
    exclude(group = "org.jetbrains.kotlin", module = "")
  }

  implementation(files(project.projectDir.resolve("..\\kotlin-hack\\build\\libs\\kotlin-hack-${project.version}.jar")))
//  implementation(project(":kotlin-hack"))
  testImplementation(files(project.projectDir.resolve("..\\kotlin-hack\\build\\libs\\kotlin-hack-${project.version}-platform.jar")))
//  testImplementation(project(":kotlin-hack", "platform"))

  testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.10.1")
  testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.10.1")
  testImplementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.11")
  testImplementation(group = "ch.qos.logback", name = "logback-core", version = "1.4.11")
  testImplementation("org.ow2.asm:asm:9.6")
  testImplementation(kGroup, name = "kotlin-script-runtime", version = kVersion)
}


tasks {

  compileKotlin {
    compilerOptions {
      javaParameters = true
    }
  }
  compileTestKotlin {
    compilerOptions {
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
      "-Xlog:class+load=info:classloader.log",
      "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens", "java.base/java.util=ALL-UNNAMED",
      "--add-opens", "java.base/java.lang=ALL-UNNAMED"
    )
  }
}

