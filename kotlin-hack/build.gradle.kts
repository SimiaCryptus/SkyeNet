import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocatePathContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
  java
  `java-library`
  `maven-publish`
  id("signing")
  id("com.github.johnrengelman.shadow") version "8.1.1"
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
}

//val kVersion = "1.9.21"
//val kGroup = "org.jetbrains.kotlin"
dependencies {
//  implementation(kGroup, name = "kotlin-compiler-cli-for-ide", version = "1.9.21-631") { isTransitive = false }


  implementation(kotlin("stdlib"))
  implementation(kotlin("scripting-jsr223"))
  implementation(kotlin("scripting-jvm"))
  implementation(kotlin("scripting-jvm-host"))
  implementation(kotlin("script-runtime"))
  implementation(kotlin("scripting-compiler-embeddable"))
  implementation(kotlin("compiler-embeddable"))

//  implementation(kGroup, name = "kotlin-compiler-embeddable", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-compiler-embeddable", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-compiler-impl-embeddable", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-jsr223", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-jvm", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-jvm-host", version = kVersion)
//  implementation(kGroup, name = "kotlin-scripting-common", version = kVersion)
}

val outputPackagePrefix = "aicoder"

val verbose = false

fun shouldRelocate(path: String) = when {
  path.startsWith("META-INF/") -> false
  path.startsWith("kotlin/") -> false

  // We want to maintain this interface:
  path.contains("/KotlinJsr223") -> false
  path.contains("/ScriptArgsWithTypes") -> false
  path.startsWith("kotlin/script/experimental/jsr223") -> false
  path.startsWith("kotlin/script/experimental/jvm") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/jsr223") -> false

  isOverride(path) -> true
  else -> false
}

fun FileVisitDetails.relocations() = when {
  path.startsWith("org/jetbrains/kotlin/com/") -> path.removePrefix("org/jetbrains/kotlin/")
  path.startsWith("org/jetbrains/kotlin/org/") -> path.removePrefix("org/jetbrains/kotlin/")
  path.startsWith("org/jetbrains/kotlin/it/") -> path.removePrefix("org/jetbrains/kotlin/")
  path.startsWith("org/jetbrains/org/") -> path.removePrefix("org/jetbrains/")
  path.startsWith("org/jetbrains/com/") -> path.removePrefix("org/jetbrains/")
  else -> path
}

fun ShadowJar.relocations() {
  relocate("org.jetbrains.kotlin.com.", "com.")
  relocate("org.jetbrains.org.", "org.")
  relocate("org.jetbrains.kotlin.org.", "org.")
  relocate("org.jetbrains.kotlin.it.", "it.")
}

tasks.register("fullShadowJar", ShadowJar::class) {
  archiveClassifier.set("full")
  isZip64 = true
  mergeServiceFiles()
  dependsOn("jar")
  configurations = listOf(project.configurations.getByName("runtimeClasspath"))
  doFirst {
    relocations()
  }
}

// Filtering and assembly
val shadowJarStage1 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("stage1")
  isZip64 = true
  //mergeServiceFiles()
  dependsOn("jar")
  configurations = listOf(project.configurations.getByName("runtimeClasspath"))
  doFirst {
    this@registering.includedDependencies.forEach { file ->
      try {
        zipTree(file).visit {
          if (this.isDirectory) return@visit
          // Adjust the path so we can express rules based on the desired final paths
          val path = relocations()
          if(path.startsWith("META-INF/")) {
            if (verbose) println("${this.path} excluded from plugin: ${file.name} as $path")
            exclude(this.path)
          } else if (isConflicting(path)) {
            if (verbose) println("${this.path} excluded conflict from plugin: ${file.name} as $path")
            exclude(this.path)
          } else if (isPruned(path)) {
            if (verbose) println("${this.path} pruned from plugin:${file.name} as $path")
            exclude(this.path)
          } else {
            if (verbose) println("${this.path} included in plugin:${file.name} as $path")
          }
        }
      } catch (e: Exception) {
        println("Error processing $file")
        e.printStackTrace()
        throw e
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
    from("src/main/resources")
    from(zipTree(shadowJarStage1.get().archiveFile))
    relocations()
  }
}

// Class isolations to avoid conflicts with the IntelliJ classpath
val shadowJarFinalStage by tasks.registering(ShadowJar::class) {
  val verbose = false
  archiveClassifier.set("")
  isZip64 = true
  dependsOn(shadowJarStage2)
  doFirst {
    from(zipTree(shadowJarStage2.get().archiveFile))
    val inputFiles: MutableSet<String> = mutableSetOf()
    zipTree(shadowJarStage2.get().archiveFile).visit {
      if (!this.isDirectory) inputFiles.add(this.relativePath.toString())
    }
    relocate(object : Relocator {

      override fun canRelocatePath(path: String?) = true
      fun shouldRelocatePath(path: String?) = when {
        null == path -> false
        !inputFiles.contains(path) -> false
        shouldRelocate(path) -> true
        else -> false
      }

      override fun relocatePath(context: RelocatePathContext?) = context?.path?.let { from ->
        if (shouldRelocatePath(from + ".class")) {
          val to = outputPackagePrefix + "/" + from
          if (verbose) println("""path relocate("$from", "$to")""")
          to
        } else {
          if (verbose) println("""leaving path "$from" as-is""")
          from
        }
      }

      override fun canRelocateClass(className: String?) = true

      fun shouldRelocateClass(className: String?) = shouldRelocatePath(className?.replace(".", "/") + ".class")

      override fun relocateClass(context: RelocateClassContext?) =
        context?.className?.let { from ->
          if (shouldRelocateClass(from)) {
            val to = outputPackagePrefix + "." + from
            if (verbose) println("""class relocate("$from", "$to")""")
            to
          } else {
            if (verbose) println("""leaving class "$from" as-is""")
            from
          }
        }

      override fun applyToSourceContent(sourceContent: String?) = sourceContent
    })
  }
}

val platformJarStage1 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("platform-1")
  isZip64 = true
  mergeServiceFiles()
  dependsOn("jar")
  configurations = listOf(project.configurations.getByName("runtimeClasspath"))
  doFirst {
    this@registering.includedDependencies.forEach { file ->
      try {
        zipTree(file).visit {
          if (this.isDirectory) return@visit
          val path = relocations()
          if (!isConflicting(path)) {
            if (verbose) println("${this.path} excluded from platform: ${file.name} as $path")
            exclude(this.path)
          } else {
            if (verbose) println("${this.path} included conflict in platform:${file.name} as $path")
          }
        }
      } catch (e: Exception) {
        println("Error processing $file")
        e.printStackTrace()
        throw e
      }
    }
  }
}

val platformJarStage2 by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("platform")
  isZip64 = true
  dependsOn(platformJarStage1)
  doFirst {
    from(zipTree(platformJarStage1.get().archiveFile))
    relocations()
  }
}

tasks.named("build") {
  dependsOn(shadowJarFinalStage, platformJarStage2)
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
    create<MavenPublication>("plugin") {
      artifactId = "plugin"
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
    create<MavenPublication>("platform") {
      artifactId = "platform"
      //from(components["java"])
      artifact(platformJarStage2.get())
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
      sign(publications["plugin"])
      sign(publications["platform"])
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
/*
fun isConflicting(path: String) = false
fun isPruned(path: String) = false
fun isOverride(path: String) = false
*/

// GENERATED CODE

// Conflicts: 361
// Pruned: 406
// Required Classes: 37575
// Override Classes: 31543

// Conflicts:
fun isConflicting(path: String) = when {
  path.startsWith("org/i") -> true
  path.startsWith("kotlin/jvm/internal/Ref\$") -> true
  path.startsWith("org/jdom/C") -> true
  path.startsWith("org/jetbrains/a") -> true
  path.startsWith("com/intellij/util/containers/Im") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/BooleanCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/chars/CharC") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCom") -> true
  path.startsWith("com/google/common/u") -> true
  path.startsWith("org/jdom/A") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/C") -> true
  path.startsWith("org/jdom/I") -> true
  path.startsWith("com/intellij/lang/folding/F") -> true
  path.startsWith("com/intellij/psi/codeStyle/D") -> true
  path.startsWith("com/intellij/util/containers/N") -> true
  path.startsWith("com/intellij/util/containers/Sin") -> true
  path.startsWith("com/intellij/util/io/DataEn") -> true
  path.startsWith("it/unimi/dsi/fastutil/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/D") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeA") -> true
  path.startsWith("org/a") -> true
  path.startsWith("org/codehaus/stax2/XMLStreamL") -> true
  path.startsWith("org/codehaus/stax2/typed/TypedX") -> true
  path.startsWith("org/jdom/D") -> true
  path.startsWith("org/jdom/E") -> true
  path.startsWith("org/jdom/N") -> true
  path.startsWith("org/jdom/P") -> true
  path.startsWith("com/fasterxml/aalto/U") -> true
  path.startsWith("com/fasterxml/aalto/W") -> true
  path.startsWith("com/fasterxml/aalto/impl/C") -> true
  path.startsWith("com/fasterxml/aalto/impl/I") -> true
  path.startsWith("com/fasterxml/aalto/impl/L") -> true
  path.startsWith("com/fasterxml/aalto/impl/S") -> true
  path.startsWith("com/fasterxml/aalto/util/N") -> true
  path.startsWith("com/fasterxml/aalto/util/S") -> true
  path.startsWith("com/fasterxml/aalto/util/XmlCharT") -> true
  path.startsWith("com/google/common/collect/AbstractSeq") -> true
  path.startsWith("com/google/common/collect/ForwardingO") -> true
  path.startsWith("com/google/common/collect/UnmodifiableI") -> true
  path.startsWith("com/google/common/collect/UnmodifiableL") -> true
  path.startsWith("com/google/common/hash/F") -> true
  path.startsWith("com/google/common/hash/P") -> true
  path.startsWith("com/google/common/io/Closea") -> true
  path.startsWith("com/intellij/codeInsight/F") -> true
  path.startsWith("com/intellij/codeInsight/folding/C") -> true
  path.startsWith("com/intellij/con") -> true
  path.startsWith("com/intellij/diagnostic/ActivityC") -> true
  path.startsWith("com/intellij/f") -> true
  path.startsWith("com/intellij/mock/MockDumbU") -> true
  path.startsWith("com/intellij/model/S") -> true
  path.startsWith("com/intellij/openapi/F") -> true
  path.startsWith("com/intellij/openapi/application/ApplicationI") -> true
  path.startsWith("com/intellij/openapi/application/ApplicationL") -> true
  path.startsWith("com/intellij/openapi/application/BaseA") -> true
  path.startsWith("com/intellij/openapi/application/ModalityI") -> true
  path.startsWith("com/intellij/openapi/command/CommandT") -> true
  path.startsWith("com/intellij/openapi/command/U") -> true
  path.startsWith("com/intellij/openapi/components/Service.") -> true
  path.startsWith("com/intellij/openapi/diagnostic/C") -> true
  path.startsWith("com/intellij/openapi/editor/actionSystem/R") -> true
  path.startsWith("com/intellij/openapi/editor/ex/M") -> true
  path.startsWith("com/intellij/openapi/editor/impl/S") -> true
  path.startsWith("com/intellij/openapi/extensions/ExtensionN") -> true
  path.startsWith("com/intellij/openapi/extensions/ExtensionPointP") -> true
  path.startsWith("com/intellij/openapi/extensions/I") -> true
  path.startsWith("com/intellij/openapi/progress/Proc") -> true
  path.startsWith("com/intellij/openapi/progress/TaskI") -> true
  path.startsWith("com/intellij/openapi/project/DumbA") -> true
  path.startsWith("com/intellij/openapi/project/DumbU") -> true
  path.startsWith("com/intellij/openapi/util/G") -> true
  path.startsWith("com/intellij/openapi/util/I") -> true
  path.startsWith("com/intellij/openapi/util/JDOMExternalizable.") -> true
  path.startsWith("com/intellij/openapi/util/Stac") -> true
  path.startsWith("com/intellij/openapi/util/SystemInfoR") -> true
  path.startsWith("com/intellij/openapi/util/io/D") -> true
  path.startsWith("com/intellij/openapi/util/io/FileT") -> true
  path.startsWith("com/intellij/openapi/util/text/CharS") -> true
  path.startsWith("com/intellij/openapi/util/text/LineC") -> true
  path.startsWith("com/intellij/openapi/util/text/StringH") -> true
  path.startsWith("com/intellij/openapi/vfs/N") -> true
  path.startsWith("com/intellij/openapi/vfs/VF") -> true
  path.startsWith("com/intellij/openapi/vfs/VirtualFileManagerL") -> true
  path.startsWith("com/intellij/openapi/vfs/newvfs/C") -> true
  path.startsWith("com/intellij/openapi/vfs/newvfs/events/C") -> true
  path.startsWith("com/intellij/pom/PomModelA") -> true
  path.startsWith("com/intellij/pom/tree/T") -> true
  path.startsWith("com/intellij/psi/CommonC") -> true
  path.startsWith("com/intellij/psi/ExternalChangeAction.") -> true
  path.startsWith("com/intellij/psi/Ig") -> true
  path.startsWith("com/intellij/psi/O") -> true
  path.startsWith("com/intellij/psi/PsiLock") -> true
  path.startsWith("com/intellij/psi/PsiRecursiveV") -> true
  path.startsWith("com/intellij/psi/Synth") -> true
  path.startsWith("com/intellij/psi/codeStyle/I") -> true
  path.startsWith("com/intellij/psi/impl/Fr") -> true
  path.startsWith("com/intellij/psi/search/impl/p") -> true
  path.startsWith("com/intellij/psi/stubs/Se") -> true
  path.startsWith("com/intellij/psi/templateLanguages/I") -> true
  path.startsWith("com/intellij/psi/templateLanguages/TemplateLanguage.") -> true
  path.startsWith("com/intellij/psi/tree/Ch") -> true
  path.startsWith("com/intellij/psi/tree/IStr") -> true
  path.startsWith("com/intellij/serialization/S") -> true
  path.startsWith("com/intellij/ui/icons/C") -> true
  path.startsWith("com/intellij/util/In") -> true
  path.startsWith("com/intellij/util/Patc") -> true
  path.startsWith("com/intellij/util/Sh") -> true
  path.startsWith("com/intellij/util/Unm") -> true
  path.startsWith("com/intellij/util/cl") -> true
  path.startsWith("com/intellij/util/concurrency/C") -> true
  path.startsWith("com/intellij/util/containers/Com") -> true
  path.startsWith("com/intellij/util/containers/P") -> true
  path.startsWith("com/intellij/util/containers/RefV") -> true
  path.startsWith("com/intellij/util/graph/GraphA") -> true
  path.startsWith("com/intellij/util/graph/i") -> true
  path.startsWith("com/intellij/util/indexing/IndexI") -> true
  path.startsWith("com/intellij/util/io/AppendablePersistentMap.") -> true
  path.startsWith("com/intellij/util/io/Corrupte") -> true
  path.startsWith("com/intellij/util/io/Dif") -> true
  path.startsWith("com/intellij/util/io/KeyV") -> true
  path.startsWith("com/intellij/util/io/PersistentMap.") -> true
  path.startsWith("com/intellij/util/io/V") -> true
  path.startsWith("com/intellij/util/io/keyStorage/N") -> true
  path.startsWith("com/intellij/util/messages/MessageBusF") -> true
  path.startsWith("com/intellij/util/text/Me") -> true
  path.startsWith("com/intellij/util/xmlb/Accessor.") -> true
  path.startsWith("com/intellij/util/xmlb/Serializa") -> true
  path.startsWith("com/intellij/util/xmlb/XmlSerializa") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/C") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/Te") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/Tr") -> true
  path.startsWith("gnu/trove/C") -> true
  path.startsWith("gnu/trove/E") -> true
  path.startsWith("gnu/trove/I") -> true
  path.startsWith("it/unimi/dsi/fastutil/A") -> true
  path.startsWith("it/unimi/dsi/fastutil/B") -> true
  path.startsWith("it/unimi/dsi/fastutil/Hash.") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/AbstractBooleanS") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/BooleanCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/BooleanP") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/BooleanSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/booleans/BooleanU") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/AbstractByteS") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteP") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteU") -> true
  path.startsWith("it/unimi/dsi/fastutil/chars/CharI") -> true
  path.startsWith("it/unimi/dsi/fastutil/chars/CharS") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/AbstractDoubleSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleP") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleU") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/AbstractFloatS") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatP") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatU") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/AbstractIntSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntP") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntU") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/AbstractLongSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongP") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongU") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/AbstractObjectI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/AbstractObjectSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectBidirectionalIterat") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$AbstractIndexBasedI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$E") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$U") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectListI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ReferenceCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/AbstractShortSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortP") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortU") -> true
  path.startsWith("kotlin/jvm/internal/De") -> true
  path.startsWith("kotlin/jvm/internal/Ref.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/annotations/KotlinR") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/java/JavaVisibilities.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaEl") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/incremental/components/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaEl") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/Lig") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/M") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/TypeComponentPosition.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/metadata/ProtoBuf.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/platform/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/platform/TargetPlatformV") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/protobuf/Pr") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/renderer/K") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/renderer/O") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/sam/SamConversionResolver.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/descriptors/DeserializedContainerA") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/storage/CacheWithNu") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/R") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/F") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/I") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/K") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeC") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeP") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemO") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeVariab") -> true
  path.startsWith("org/codehaus/stax2/ri/S") -> true
  path.startsWith("org/codehaus/stax2/ri/typed/B") -> true
  path.startsWith("org/codehaus/stax2/ri/typed/C") -> true
  path.startsWith("org/jdom/T") -> true
  path.startsWith("org/jdom/V") -> true
  path.startsWith("org/jdom/output/E") -> true
  path.startsWith("org/jdom/output/Format\$1") -> true
  path.startsWith("org/jdom/output/Format\$D") -> true
  path.startsWith("org/jdom/output/Format\$T") -> true
  path.startsWith("org/jdom/output/Format.") -> true
  path.startsWith("org/jdom/output/N") -> true
  path.startsWith("org/jdom/output/XMLOutputter\$") -> true
  path.startsWith("org/jdom/output/XMLOutputter.") -> true
  path.startsWith("org/jetbrains/c") -> true
  path.startsWith("org/picocontainer/PicoC") -> true

  else -> false
}

// Pruned:
fun isPruned(path: String) = when {
  path.startsWith("it/unimi/dsi/fastutil/bo") -> true
  path.startsWith("org/i") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/Long2") -> true
  path.startsWith("it/unimi/dsi/fastutil/c") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/Byte2") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/Float2") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/Short2") -> true
  path.startsWith("kotlin/jvm/internal/Ref\$") -> true
  path.startsWith("org/jdom/C") -> true
  path.startsWith("org/jetbrains/a") -> true
  path.startsWith("com/intellij/util/containers/Im") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCom") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCom") -> true
  path.startsWith("com/google/common/u") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2B") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Object2B") -> true
  path.startsWith("org/jdom/A") -> true
  path.startsWith("gnu/trove/TLi") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2D") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2F") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2L") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Object2D") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Object2F") -> true
  path.startsWith("com/intellij/psi/util/PsiFormatUtil\$") -> true
  path.startsWith("kotlin/internal/R") -> true
  path.startsWith("kotlin/js") -> true
  path.startsWith("kotlin/jvm/JvmD") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/C") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/N") -> true
  path.startsWith("org/jdom/I") -> true
  path.startsWith("com/intellij/lang/folding/F") -> true
  path.startsWith("com/intellij/psi/ExternalC") -> true
  path.startsWith("com/intellij/psi/codeStyle/D") -> true
  path.startsWith("com/intellij/util/containers/N") -> true
  path.startsWith("com/intellij/util/containers/Sin") -> true
  path.startsWith("com/intellij/util/io/DataEn") -> true
  path.startsWith("it/unimi/dsi/fastutil/S") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2B") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2IntS") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2ObjectS") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Object2ObjectS") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectB") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2B") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2ObjectS") -> true
  path.startsWith("kotlin/Ch") -> true
  path.startsWith("kotlin/Op") -> true
  path.startsWith("kotlin/experimental/ExperimentalO") -> true
  path.startsWith("kotlin/jvm/S") -> true
  path.startsWith("kotlin/jvm/T") -> true
  path.startsWith("kotlin/jvm/internal/Loc") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/D") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeA") -> true
  path.startsWith("kotlin/text/T") -> true
  path.startsWith("org/a") -> true
  path.startsWith("org/codehaus/stax2/XMLStreamL") -> true
  path.startsWith("org/codehaus/stax2/typed/TypedX") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/C") -> true
  path.startsWith("org/jdom/D") -> true
  path.startsWith("org/jdom/E") -> true
  path.startsWith("org/jdom/N") -> true
  path.startsWith("org/jdom/P") -> true
  path.startsWith("org/jetbrains/kotlin/javax/inject/S") -> true
  path.startsWith("META-INF/v") -> true
  path.startsWith("com/fasterxml/aalto/U") -> true
  path.startsWith("com/fasterxml/aalto/W") -> true
  path.startsWith("com/fasterxml/aalto/impl/C") -> true
  path.startsWith("com/fasterxml/aalto/impl/I") -> true
  path.startsWith("com/fasterxml/aalto/impl/L") -> true
  path.startsWith("com/fasterxml/aalto/impl/S") -> true
  path.startsWith("com/fasterxml/aalto/util/N") -> true
  path.startsWith("com/fasterxml/aalto/util/S") -> true
  path.startsWith("com/fasterxml/aalto/util/XmlCharT") -> true
  path.startsWith("com/google/common/collect/AbstractSeq") -> true
  path.startsWith("com/google/common/collect/ForwardingO") -> true
  path.startsWith("com/google/common/collect/Ro") -> true
  path.startsWith("com/google/common/collect/UnmodifiableI") -> true
  path.startsWith("com/google/common/collect/UnmodifiableL") -> true
  path.startsWith("com/google/common/hash/F") -> true
  path.startsWith("com/google/common/hash/Hashe") -> true
  path.startsWith("com/google/common/hash/P") -> true
  path.startsWith("com/google/common/io/Closea") -> true
  path.startsWith("com/intellij/codeInsight/F") -> true
  path.startsWith("com/intellij/codeInsight/folding/C") -> true
  path.startsWith("com/intellij/con") -> true
  path.startsWith("com/intellij/diagnostic/ActivityC") -> true
  path.startsWith("com/intellij/f") -> true
  path.startsWith("com/intellij/lang/jvm/D") -> true
  path.startsWith("com/intellij/mock/MockDumbU") -> true
  path.startsWith("com/intellij/model/S") -> true
  path.startsWith("com/intellij/openapi/F") -> true
  path.startsWith("com/intellij/openapi/application/ApplicationI") -> true
  path.startsWith("com/intellij/openapi/application/ApplicationL") -> true
  path.startsWith("com/intellij/openapi/application/BaseA") -> true
  path.startsWith("com/intellij/openapi/application/ModalityI") -> true
  path.startsWith("com/intellij/openapi/command/CommandT") -> true
  path.startsWith("com/intellij/openapi/command/U") -> true
  path.startsWith("com/intellij/openapi/components/Service.") -> true
  path.startsWith("com/intellij/openapi/diagnostic/C") -> true
  path.startsWith("com/intellij/openapi/editor/actionSystem/R") -> true
  path.startsWith("com/intellij/openapi/editor/ex/M") -> true
  path.startsWith("com/intellij/openapi/editor/impl/S") -> true
  path.startsWith("com/intellij/openapi/extensions/ExtensionN") -> true
  path.startsWith("com/intellij/openapi/extensions/ExtensionPointP") -> true
  path.startsWith("com/intellij/openapi/extensions/I") -> true
  path.startsWith("com/intellij/openapi/module/ModuleS") -> true
  path.startsWith("com/intellij/openapi/progress/Proc") -> true
  path.startsWith("com/intellij/openapi/progress/TaskI") -> true
  path.startsWith("com/intellij/openapi/project/DumbA") -> true
  path.startsWith("com/intellij/openapi/project/DumbU") -> true
  path.startsWith("com/intellij/openapi/roots/ContentIteratorE") -> true
  path.startsWith("com/intellij/openapi/util/G") -> true
  path.startsWith("com/intellij/openapi/util/I") -> true
  path.startsWith("com/intellij/openapi/util/JDOMExternalizable.") -> true
  path.startsWith("com/intellij/openapi/util/Stac") -> true
  path.startsWith("com/intellij/openapi/util/SystemInfoR") -> true
  path.startsWith("com/intellij/openapi/util/io/D") -> true
  path.startsWith("com/intellij/openapi/util/io/FileT") -> true
  path.startsWith("com/intellij/openapi/util/text/CharS") -> true
  path.startsWith("com/intellij/openapi/util/text/LineC") -> true
  path.startsWith("com/intellij/openapi/util/text/StringH") -> true
  path.startsWith("com/intellij/openapi/vfs/N") -> true
  path.startsWith("com/intellij/openapi/vfs/VF") -> true
  path.startsWith("com/intellij/openapi/vfs/VirtualFileManagerL") -> true
  path.startsWith("com/intellij/openapi/vfs/newvfs/C") -> true
  path.startsWith("com/intellij/openapi/vfs/newvfs/events/C") -> true
  path.startsWith("com/intellij/pom/PomModelA") -> true
  path.startsWith("com/intellij/pom/tree/T") -> true
  path.startsWith("com/intellij/psi/CommonC") -> true
  path.startsWith("com/intellij/psi/CustomHighlighterTokenType.") -> true
  path.startsWith("com/intellij/psi/EmptyR") -> true
  path.startsWith("com/intellij/psi/ExternallyA") -> true
  path.startsWith("com/intellij/psi/Ig") -> true
  path.startsWith("com/intellij/psi/JavaCodeFragmentF") -> true
  path.startsWith("com/intellij/psi/JvmC") -> true
  path.startsWith("com/intellij/psi/O") -> true
  path.startsWith("com/intellij/psi/Pa") -> true
  path.startsWith("com/intellij/psi/PsiElementFactory\$") -> true
  path.startsWith("com/intellij/psi/PsiExpressionC") -> true
  path.startsWith("com/intellij/psi/PsiFileW") -> true
  path.startsWith("com/intellij/psi/PsiLis") -> true
  path.startsWith("com/intellij/psi/PsiLock") -> true
  path.startsWith("com/intellij/psi/PsiMig") -> true
  path.startsWith("com/intellij/psi/PsiParserFacade\$") -> true
  path.startsWith("com/intellij/psi/PsiRecursiveV") -> true
  path.startsWith("com/intellij/psi/PsiTe") -> true
  path.startsWith("com/intellij/psi/Resolvi") -> true
  path.startsWith("com/intellij/psi/Synth") -> true
  path.startsWith("com/intellij/psi/codeStyle/I") -> true
  path.startsWith("com/intellij/psi/controlFlow/Al") -> true
  path.startsWith("com/intellij/psi/filters/ElementFilterB") -> true
  path.startsWith("com/intellij/psi/impl/Fr") -> true
  path.startsWith("com/intellij/psi/impl/source/I") -> true
  path.startsWith("com/intellij/psi/impl/source/resolve/reference/impl/providers/G") -> true
  path.startsWith("com/intellij/psi/infos/MethodCandidateInfo\$ApplicabilityLevel.") -> true
  path.startsWith("com/intellij/psi/meta/PsiW") -> true
  path.startsWith("com/intellij/psi/scope/B") -> true
  path.startsWith("com/intellij/psi/scope/Pr") -> true
  path.startsWith("com/intellij/psi/search/PsiN") -> true
  path.startsWith("com/intellij/psi/search/impl/p") -> true
  path.startsWith("com/intellij/psi/stubs/Se") -> true
  path.startsWith("com/intellij/psi/targets/AliasingPsiTargetM") -> true
  path.startsWith("com/intellij/psi/templateLanguages/I") -> true
  path.startsWith("com/intellij/psi/templateLanguages/TemplateLanguage.") -> true
  path.startsWith("com/intellij/psi/tree/Ch") -> true
  path.startsWith("com/intellij/psi/tree/IStr") -> true
  path.startsWith("com/intellij/psi/util/ClassK") -> true
  path.startsWith("com/intellij/psi/util/F") -> true
  path.startsWith("com/intellij/psi/util/PropertyM") -> true
  path.startsWith("com/intellij/psi/util/PsiA") -> true
  path.startsWith("com/intellij/serialization/S") -> true
  path.startsWith("com/intellij/ui/icons/C") -> true
  path.startsWith("com/intellij/util/CommonProcessors.") -> true
  path.startsWith("com/intellij/util/In") -> true
  path.startsWith("com/intellij/util/Patc") -> true
  path.startsWith("com/intellij/util/Sh") -> true
  path.startsWith("com/intellij/util/Unm") -> true
  path.startsWith("com/intellij/util/cl") -> true
  path.startsWith("com/intellij/util/concurrency/C") -> true
  path.startsWith("com/intellij/util/containers/Com") -> true
  path.startsWith("com/intellij/util/containers/P") -> true
  path.startsWith("com/intellij/util/containers/RefV") -> true
  path.startsWith("com/intellij/util/graph/GraphA") -> true
  path.startsWith("com/intellij/util/graph/i") -> true
  path.startsWith("com/intellij/util/indexing/IndexI") -> true
  path.startsWith("com/intellij/util/io/AppendablePersistentMap.") -> true
  path.startsWith("com/intellij/util/io/Corrupte") -> true
  path.startsWith("com/intellij/util/io/Dif") -> true
  path.startsWith("com/intellij/util/io/FileChannelU") -> true
  path.startsWith("com/intellij/util/io/KeyV") -> true
  path.startsWith("com/intellij/util/io/PersistentMap.") -> true
  path.startsWith("com/intellij/util/io/V") -> true
  path.startsWith("com/intellij/util/io/keyStorage/N") -> true
  path.startsWith("com/intellij/util/messages/MessageBusF") -> true
  path.startsWith("com/intellij/util/text/Me") -> true
  path.startsWith("com/intellij/util/xmlb/Accessor.") -> true
  path.startsWith("com/intellij/util/xmlb/Serializa") -> true
  path.startsWith("com/intellij/util/xmlb/XmlSerializa") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/C") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/Te") -> true
  path.startsWith("com/intellij/util/xmlb/annotations/Tr") -> true
  path.startsWith("gnu/trove/C") -> true
  path.startsWith("gnu/trove/E") -> true
  path.startsWith("gnu/trove/I") -> true
  path.startsWith("it/unimi/dsi/fastutil/A") -> true
  path.startsWith("it/unimi/dsi/fastutil/B") -> true
  path.startsWith("it/unimi/dsi/fastutil/Hash.") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/AbstractByteS") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteP") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/bytes/ByteU") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/AbstractDoubleSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2C") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2D") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2F") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2I") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2L") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2R") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/Double2S") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleP") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/doubles/DoubleU") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/AbstractFloatS") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatP") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatSe") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/floats/FloatU") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/AbstractIntSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2C") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2R") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/Int2S") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntP") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/ints/IntU") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/AbstractLongSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongBidirectionalIterab") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongP") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongSo") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/longs/LongU") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/AbstractObjectI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/AbstractObjectSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Object2C") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$AbstractIndexBasedI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$E") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators\$U") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectIterators.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectListI") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectSo") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ObjectSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2C") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2D") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2F") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2L") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/Reference2S") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ReferenceCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/objects/ReferenceSo") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/AbstractShortSp") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortBin") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCollections.") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortCon") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortP") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortSpliterator.") -> true
  path.startsWith("it/unimi/dsi/fastutil/shorts/ShortU") -> true
  path.startsWith("kotlin/B") -> true
  path.startsWith("kotlin/Con") -> true
  path.startsWith("kotlin/ExperimentalM") -> true
  path.startsWith("kotlin/ExperimentalSu") -> true
  path.startsWith("kotlin/Ext") -> true
  path.startsWith("kotlin/H") -> true
  path.startsWith("kotlin/Metadata\$") -> true
  path.startsWith("kotlin/Par") -> true
  path.startsWith("kotlin/Sub") -> true
  path.startsWith("kotlin/Sup") -> true
  path.startsWith("kotlin/Th") -> true
  path.startsWith("kotlin/TypeA") -> true
  path.startsWith("kotlin/UnsafeV") -> true
  path.startsWith("kotlin/annotation/Rep") -> true
  path.startsWith("kotlin/collections/T") -> true
  path.startsWith("kotlin/concurrent/V") -> true
  path.startsWith("kotlin/coroutines/c") -> true
  path.startsWith("kotlin/experimental/B") -> true
  path.startsWith("kotlin/experimental/ExperimentalN") -> true
  path.startsWith("kotlin/internal/A") -> true
  path.startsWith("kotlin/internal/D") -> true
  path.startsWith("kotlin/internal/E") -> true
  path.startsWith("kotlin/internal/N") -> true
  path.startsWith("kotlin/internal/O") -> true
  path.startsWith("kotlin/internal/PlatformD") -> true
  path.startsWith("kotlin/internal/Pu") -> true
  path.startsWith("kotlin/io/Const") -> true
  path.startsWith("kotlin/io/S") -> true
  path.startsWith("kotlin/jvm/I") -> true
  path.startsWith("kotlin/jvm/JvmM") -> true
  path.startsWith("kotlin/jvm/JvmP") -> true
  path.startsWith("kotlin/jvm/JvmR") -> true
  path.startsWith("kotlin/jvm/JvmSe") -> true
  path.startsWith("kotlin/jvm/JvmSu") -> true
  path.startsWith("kotlin/jvm/JvmSy") -> true
  path.startsWith("kotlin/jvm/JvmW") -> true
  path.startsWith("kotlin/jvm/P") -> true
  path.startsWith("kotlin/jvm/V") -> true
  path.startsWith("kotlin/jvm/internal/BooleanC") -> true
  path.startsWith("kotlin/jvm/internal/ByteC") -> true
  path.startsWith("kotlin/jvm/internal/CharC") -> true
  path.startsWith("kotlin/jvm/internal/De") -> true
  path.startsWith("kotlin/jvm/internal/E") -> true
  path.startsWith("kotlin/jvm/internal/FunI") -> true
  path.startsWith("kotlin/jvm/internal/FunctionA") -> true
  path.startsWith("kotlin/jvm/internal/FunctionI") -> true
  path.startsWith("kotlin/jvm/internal/IntC") -> true
  path.startsWith("kotlin/jvm/internal/LongC") -> true
  path.startsWith("kotlin/jvm/internal/Ma") -> true
  path.startsWith("kotlin/jvm/internal/MutableL") -> true
  path.startsWith("kotlin/jvm/internal/Ref.") -> true
  path.startsWith("kotlin/jvm/internal/Se") -> true
  path.startsWith("kotlin/jvm/internal/ShortC") -> true
  path.startsWith("kotlin/jvm/internal/u") -> true
  path.startsWith("kotlin/jvm/j") -> true
  path.startsWith("kotlin/math/U") -> true
  path.startsWith("kotlin/properties/P") -> true
  path.startsWith("kotlin/reflect/KCallable\$") -> true
  path.startsWith("kotlin/reflect/KClass\$") -> true
  path.startsWith("kotlin/reflect/KFunction\$") -> true
  path.startsWith("kotlin/reflect/KMutableProperty\$D") -> true
  path.startsWith("kotlin/reflect/KMutableProperty0\$D") -> true
  path.startsWith("kotlin/reflect/KMutableProperty1\$D") -> true
  path.startsWith("kotlin/reflect/KMutableProperty2\$D") -> true
  path.startsWith("kotlin/reflect/KParameter\$D") -> true
  path.startsWith("kotlin/reflect/KProperty\$D") -> true
  path.startsWith("kotlin/reflect/KProperty0\$D") -> true
  path.startsWith("kotlin/reflect/KProperty1\$D") -> true
  path.startsWith("kotlin/reflect/KProperty2\$D") -> true
  path.startsWith("kotlin/reflect/KType\$") -> true
  path.startsWith("kotlin/reflect/TypeO") -> true
  path.startsWith("kotlin/reflect/full/N") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/annotations/KotlinR") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/java/JavaVisibilities.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaEl") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/incremental/components/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaEl") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/Lig") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/M") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/TypeComponentPosition.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/metadata/ProtoBuf.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/platform/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/platform/TargetPlatformV") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/protobuf/Pr") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/renderer/K") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/renderer/O") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/S") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/sam/SamConversionResolver.") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/descriptors/DeserializedContainerA") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/storage/CacheWithNu") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/R") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/F") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/I") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/K") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeC") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeP") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemO") -> true
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeVariab") -> true
  path.startsWith("kotlin/script/experimental/jvmhost/BasicJvmScriptE") -> true
  path.startsWith("kotlin/system/P") -> true
  path.startsWith("kotlin/time/MonoT") -> true
  path.startsWith("org/codehaus/stax2/ri/S") -> true
  path.startsWith("org/codehaus/stax2/ri/typed/B") -> true
  path.startsWith("org/codehaus/stax2/ri/typed/C") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/A") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/F") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/JN") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/JniA") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/M") -> true
  path.startsWith("org/fusesource/hawtjni/runtime/T") -> true
  path.startsWith("org/fusesource/jansi/AnsiS") -> true
  path.startsWith("org/fusesource/jansi/H") -> true
  path.startsWith("org/jdom/T") -> true
  path.startsWith("org/jdom/V") -> true
  path.startsWith("org/jdom/output/E") -> true
  path.startsWith("org/jdom/output/Format\$1") -> true
  path.startsWith("org/jdom/output/Format\$D") -> true
  path.startsWith("org/jdom/output/Format\$T") -> true
  path.startsWith("org/jdom/output/Format.") -> true
  path.startsWith("org/jdom/output/N") -> true
  path.startsWith("org/jdom/output/XMLOutputter\$") -> true
  path.startsWith("org/jdom/output/XMLOutputter.") -> true
  path.startsWith("org/jetbrains/c") -> true
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$F") -> true
  path.startsWith("org/jetbrains/kotlin/javax/inject/N") -> true
  path.startsWith("org/jetbrains/kotlin/javax/inject/P") -> true
  path.startsWith("org/jetbrains/kotlin/javax/inject/Q") -> true
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/DoubleC") -> true
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/IntC") -> true
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/LongC") -> true
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/M") -> true
  path.startsWith("org/jline/terminal/impl/jna/JnaS") -> true
  path.startsWith("org/picocontainer/PicoC") -> true

  else -> false
}

// Overrides:
fun isOverride(path: String) = when {
  path.startsWith("org/jd") -> false
  path.startsWith("javaslang/m") -> false
  path.startsWith("org/picocontainer/P") -> false
  path.startsWith("javaslang/F") -> false
  path.startsWith("org/fusesource/hawtjni/runtime/J") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/By") -> false
  path.startsWith("org/jetbrains/kotlin/util/slicedMap/Ma") -> false
  path.startsWith("com/intellij/lang/jvm/annotation/JvmAnnotationC") -> false
  path.startsWith("com/intellij/lang/jvm/types/JvmT") -> false
  path.startsWith("com/google/gwt/dev/js/rhino/Ev") -> false
  path.startsWith("com/google/gwt/dev/js/rhino/J") -> false
  path.startsWith("com/google/gwt/dev/js/rhino/M") -> false
  path.startsWith("com/intellij/codeInsight/ExternalAnnotationsL") -> false
  path.startsWith("com/intellij/codeInsight/Nullability.") -> false
  path.startsWith("com/intellij/codeInsight/folding/J") -> false
  path.startsWith("com/intellij/codeInsight/folding/impl/JavaC") -> false
  path.startsWith("com/intellij/lang/jvm/JvmAnnotati") -> false
  path.startsWith("com/intellij/lang/jvm/JvmClassK") -> false
  path.startsWith("com/intellij/lang/jvm/JvmModifier.") -> false
  path.startsWith("com/intellij/lang/jvm/annotation/JvmAnnotationAr") -> false
  path.startsWith("com/intellij/lang/jvm/annotation/JvmAnnotationAttributeV") -> false
  path.startsWith("com/intellij/lang/jvm/annotation/JvmAnnotationE") -> false
  path.startsWith("com/intellij/lang/jvm/annotation/JvmN") -> false
  path.startsWith("com/intellij/lang/jvm/types/JvmA") -> false
  path.startsWith("com/intellij/lang/jvm/types/JvmPrimitiveType.") -> false
  path.startsWith("com/intellij/lang/jvm/types/JvmR") -> false
  path.startsWith("com/intellij/lang/jvm/types/JvmW") -> false
  path.startsWith("com/intellij/openapi/application/ex/P") -> false
  path.startsWith("com/intellij/openapi/progress/JobC") -> false
  path.startsWith("com/intellij/psi/PsiModifier\$") -> false
  path.startsWith("com/intellij/psi/PsiModifier.") -> false
  path.startsWith("com/intellij/psi/codeStyle/V") -> false
  path.startsWith("com/intellij/psi/impl/compiled/O") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/Do") -> false
  path.startsWith("com/intellij/psi/scope/M") -> false
  path.startsWith("javaslang/collection/F") -> false
  path.startsWith("javaslang/collection/HashArrayMappedTrieModule\$Ac") -> false
  path.startsWith("javaslang/collection/HashArrayMappedTrieModule.") -> false
  path.startsWith("javaslang/collection/IteratorModule.") -> false
  path.startsWith("javaslang/collection/RedBlackTreeModule.") -> false
  path.startsWith("javaslang/collection/StreamModule.") -> false
  path.startsWith("javaslang/collection/TreeModule.") -> false
  path.startsWith("javaslang/λ") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/components/T") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/R") -> false
  path.startsWith("org/fusesource/jansi/AnsiO") -> false
  path.startsWith("org/jetbrains/kotlin/cfg/Bl") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/arguments/K2Js") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/modules/D") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/CompileEnvironmentE") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/E") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/ClassBuilderM") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/inline/InlineE") -> false
  path.startsWith("org/jetbrains/kotlin/javax") -> false
  path.startsWith("org/jetbrains/kotlin/js/S") -> false
  path.startsWith("org/jetbrains/kotlin/js/common/H") -> false
  path.startsWith("org/jetbrains/kotlin/js/common/S") -> false
  path.startsWith("org/jetbrains/kotlin/js/facade/F") -> false
  path.startsWith("org/jetbrains/kotlin/js/facade/exceptions/TranslationE") -> false
  path.startsWith("org/jetbrains/kotlin/js/facade/exceptions/U") -> false
  path.startsWith("org/jetbrains/kotlin/js/util/M") -> false
  path.startsWith("org/jetbrains/kotlin/lexer/KotlinLexerE") -> false
  path.startsWith("org/jetbrains/kotlin/net/jpountz/lz4/LZ4Decompressor.") -> false
  path.startsWith("org/jetbrains/kotlin/net/jpountz/lz4/LZ4E") -> false
  path.startsWith("org/jetbrains/kotlin/net/jpountz/lz4/LZ4Unk") -> false
  path.startsWith("org/jetbrains/kotlin/protobuf/ServiceE") -> false
  path.startsWith("org/jetbrains/kotlin/psi/I") -> false
  path.startsWith("org/jetbrains/kotlin/psi/KtSta") -> false
  path.startsWith("org/jetbrains/kotlin/renderer/K") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/calls/context/Can") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/calls/context/Co") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/jvm/KotlinF") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/scopes/receivers/Receiver.") -> false
  path.startsWith("org/jetbrains/kotlin/storage/NoL") -> false
  path.startsWith("org/jetbrains/kotlin/types/expressions/Ca") -> false
  path.startsWith("org/jetbrains/kotlin/types/expressions/Coe") -> false
  path.startsWith("org/jetbrains/kotlin/utils/ThreadS") -> false
  path.startsWith("org/jline/reader/Bi") -> false
  path.startsWith("org/jline/reader/En") -> false
  path.startsWith("org/jline/reader/S") -> false
  path.startsWith("org/jline/reader/U") -> false
  path.startsWith("org/jline/terminal/impl/jna/win/A") -> false
  path.startsWith("org/jline/utils/Cl") -> false
  path.startsWith("org/jline/utils/Le") -> false
  path.startsWith("org/jline/utils/O") -> false
  path.startsWith("org/objectweb/asm/ClassT") -> false
  path.startsWith("org/objectweb/asm/MethodT") -> false
  path.startsWith("org/objectweb/asm/O") -> false
  path.startsWith("org/objectweb/asm/tree/Un") -> false
  path.startsWith("org/picocontainer/defaults/As") -> false
  path.startsWith("org/picocontainer/defaults/P") -> false
  path.startsWith("org/picocontainer/defaults/T") -> false
  path.startsWith("org/picocontainer/defaults/U") -> false

  else -> true
}