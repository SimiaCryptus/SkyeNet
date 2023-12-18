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

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("scripting-jsr223"))
  implementation(kotlin("scripting-jvm"))
  implementation(kotlin("scripting-jvm-host"))
  implementation(kotlin("script-runtime"))
  implementation(kotlin("scripting-compiler-embeddable"))
  implementation(kotlin("compiler-embeddable"))
}

val outputPackagePrefix = "aicoder"

val verbose = false

fun shouldRelocate(path: String) = when {
  path.startsWith("META-INF/") -> false
  path.startsWith("kotlin/") -> false
  //path.startsWith("org/jetbrains/kotlin/psi/") -> false

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
  mergeServiceFiles()
  dependsOn("jar")
  configurations = listOf(project.configurations.getByName("runtimeClasspath"))
  doFirst {
    this@registering.includedDependencies.forEach { file ->
      try {
        zipTree(file).visit {
          if (this.isDirectory) return@visit
          // Adjust the path so we can express rules based on the desired final paths
          val path = relocations()
          when {
//            path.startsWith("META-INF/") -> {
//              if (verbose) println("${this.path} excluded from plugin: ${file.name} as $path")
//              exclude(this.path)
//            }
            isConflicting(path) -> {
              if (verbose) println("${this.path} excluded conflict from plugin: ${file.name} as $path")
              exclude(this.path)
            }

            isPruned(path) -> {
              if (verbose) println("${this.path} pruned from plugin:${file.name} as $path")
              exclude(this.path)
            }

            else -> {
              if (verbose) println("${this.path} included in plugin:${file.name} as $path")
            }
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
    //from("src/main/resources")
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

// Conflicts: 28203
// Pruned: 6513
// Required Classes: 29945
// Override Classes: 0

// Conflicts:
fun isConflicting(path: String) = when {
  path.startsWith("gnu/trove/d") -> false
  path.startsWith("org/jl") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/cl") -> false
  path.startsWith("org/jetbrains/kotlin/ja") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/j") -> false
  path.startsWith("org/o") -> false
  path.startsWith("com/intellij/psi/impl/j") -> false
  path.startsWith("org/jetbrains/kotlin/kot") -> false
  path.startsWith("com/intellij/psi/con") -> false
  path.startsWith("com/intellij/psi/impl/co") -> false
  path.startsWith("kotlin/script/experimental/jvmh") -> false
  path.startsWith("com/intellij/lang/j") -> false
  path.startsWith("org/jetbrains/kotlin/cli/js") -> false
  path.startsWith("org/jetbrains/kotlin/ne") -> false
  path.startsWith("gnu/trove/TD") -> false
  path.startsWith("gnu/trove/TF") -> false
  path.startsWith("org/jetbrains/kotlin/fir/li") -> false
  path.startsWith("org/f") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerK") -> false
  path.startsWith("org/jetbrains/kotlin/cli/m") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/J") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/g") -> false
  path.startsWith("com/intellij/psi/impl/source/j") -> false
  path.startsWith("com/intellij/psi/J") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/C") -> false
  path.startsWith("org/jetbrains/kotlin/o") -> false
  path.startsWith("com/intellij/psi/PsiJ") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/F") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/K") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrClassifierStorage\$") -> false
  path.startsWith("kotlin/script/experimental/js") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalCompile") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiJ") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/R") -> false
  path.startsWith("com/intellij/psi/PsiTy") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/r") -> false
  path.startsWith("com/intellij/psi/impl/source/J") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/F") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createC") -> false
  path.startsWith("com/intellij/psi/PsiDia") -> false
  path.startsWith("com/intellij/psi/p") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalJvmCo") -> false
  path.startsWith("com/intellij/psi/PsiAnn") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/p") -> false
  path.startsWith("com/intellij/psi/PsiS") -> false
  path.startsWith("com/intellij/psi/PsiCl") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$c") -> false
  path.startsWith("com/intellij/codeInsight/E") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/Cr") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/f") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/L") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/m") -> false
  path.startsWith("com/intellij/psi/impl/J") -> false
  path.startsWith("com/intellij/psi/impl/light/LightR") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiI") -> false
  path.startsWith("com/intellij/psi/j") -> false
  path.startsWith("org/jetbrains/kotlin/serialization/b") -> false
  path.startsWith("com/intellij/core/CoreJ") -> false
  path.startsWith("com/intellij/ide/plugins/IdeaPluginDescriptorImpl\$") -> false
  path.startsWith("com/intellij/psi/impl/PsiS") -> false
  path.startsWith("com/intellij/psi/impl/light/LightP") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Er") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/A") -> false
  path.startsWith("org/jetbrains/kotlin/psi2ir/generators/Ex") -> false
  path.startsWith("com/intellij/psi/PsiMe") -> false
  path.startsWith("com/intellij/psi/impl/smartPointers/SmartT") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/P") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/overrides/FakeOverrideB") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/U") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/J") -> false
  path.startsWith("org/jetbrains/kotlin/fir/scopes/Fa") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/Abi") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/BuildD") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/Changed") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/ChangesD") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalJsCo") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/u") -> false
  path.startsWith("com/intellij/psi/PsiCa") -> false
  path.startsWith("com/intellij/psi/PsiCon") -> false
  path.startsWith("com/intellij/psi/PsiIm") -> false
  path.startsWith("com/intellij/psi/PsiRes") -> false
  path.startsWith("com/intellij/psi/com") -> false
  path.startsWith("com/intellij/psi/impl/PsiCl") -> false
  path.startsWith("com/intellij/psi/impl/cache/T") -> false
  path.startsWith("com/intellij/psi/impl/light/LightC") -> false
  path.startsWith("com/intellij/psi/impl/light/LightJ") -> false
  path.startsWith("com/intellij/psi/impl/source/Cl") -> false
  path.startsWith("com/intellij/psi/util/M") -> false
  path.startsWith("gnu/trove/TByteI") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancement\$S") -> false
  path.startsWith("kotlin/reflect/jvm/internal/p") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/KotlinT") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createM") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/Compile") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalF") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/sn") -> false
  path.startsWith("org/picocontainer/d") -> false
  path.startsWith("com/intellij/codeInsight/N") -> false
  path.startsWith("com/intellij/psi/PsiEx") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiR") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/J") -> false
  path.startsWith("com/intellij/psi/scope/c") -> false
  path.startsWith("com/intellij/psi/scope/processor/M") -> false
  path.startsWith("com/intellij/psi/util/Cl") -> false
  path.startsWith("com/intellij/psi/util/J") -> false
  path.startsWith("com/intellij/psi/util/PsiTy") -> false
  path.startsWith("com/intellij/psi/util/T") -> false
  path.startsWith("com/intellij/util/lang/f") -> false
  path.startsWith("gnu/trove/TByteB") -> false
  path.startsWith("gnu/trove/TByteD") -> false
  path.startsWith("gnu/trove/TByteF") -> false
  path.startsWith("gnu/trove/TByteH") -> false
  path.startsWith("gnu/trove/TByteL") -> false
  path.startsWith("gnu/trove/TByteO") -> false
  path.startsWith("gnu/trove/TIntB") -> false
  path.startsWith("gnu/trove/TIntD") -> false
  path.startsWith("gnu/trove/TIntF") -> false
  path.startsWith("gnu/trove/TLongB") -> false
  path.startsWith("gnu/trove/TLongD") -> false
  path.startsWith("gnu/trove/TLongF") -> false
  path.startsWith("gnu/trove/TLongL") -> false
  path.startsWith("gnu/trove/TObjectB") -> false
  path.startsWith("gnu/trove/TObjectD") -> false
  path.startsWith("gnu/trove/TObjectF") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/MessageR") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/p") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/FirD") -> false
  path.startsWith("org/jetbrains/kotlin/fir/scopes/impl/FirSco") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/lower/serialization/ir/JsIrFileSerializer\$") -> false
  path.startsWith("org/jetbrains/kotlin/ir/linkage/partial/I") -> false
  path.startsWith("com/intellij/codeInsight/A") -> false
  path.startsWith("com/intellij/codeWithMe/ClientId\$Companion\$") -> false
  path.startsWith("com/intellij/core/J") -> false
  path.startsWith("com/intellij/psi/Cl") -> false
  path.startsWith("com/intellij/psi/PsiAr") -> false
  path.startsWith("com/intellij/psi/PsiMo") -> false
  path.startsWith("com/intellij/psi/PsiPac") -> false
  path.startsWith("com/intellij/psi/PsiPr") -> false
  path.startsWith("com/intellij/psi/codeStyle/J") -> false
  path.startsWith("com/intellij/psi/impl/PsiElementF") -> false
  path.startsWith("com/intellij/psi/impl/Rec") -> false
  path.startsWith("com/intellij/psi/impl/light/LightM") -> false
  path.startsWith("com/intellij/psi/impl/light/LightT") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiC") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiE") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiM") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/S") -> false
  path.startsWith("com/intellij/psi/in") -> false
  path.startsWith("com/intellij/psi/scope/E") -> false
  path.startsWith("com/intellij/psi/util/Pr") -> false
  path.startsWith("com/intellij/util/X") -> false
  path.startsWith("com/intellij/util/io/WriteAheadLogKt\$t") -> false
  path.startsWith("gnu/trove/TLi") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/AnnotationTypeQualifierResolver\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/types/T") -> false
  path.startsWith("org/jdom/output/s") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/actualizer/IrExpectActualA") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/proto/Act") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/A") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/CLICom") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/K") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/ClassF") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/CliC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/ConversionUtilsKt\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$d") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$boolean\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createP") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/Def") -> false
  path.startsWith("org/picocontainer/P") -> false
  path.startsWith("com/intellij/ide/highlighter/J") -> false
  path.startsWith("com/intellij/openapi/progress/J") -> false
  path.startsWith("com/intellij/patterns/PsiJ") -> false
  path.startsWith("com/intellij/pl") -> false
  path.startsWith("com/intellij/psi/PsiElementF") -> false
  path.startsWith("com/intellij/psi/PsiLam") -> false
  path.startsWith("com/intellij/psi/PsiPara") -> false
  path.startsWith("com/intellij/psi/Ty") -> false
  path.startsWith("com/intellij/psi/impl/Sc") -> false
  path.startsWith("com/intellij/psi/impl/T") -> false
  path.startsWith("com/intellij/psi/impl/file/PsiPackageI") -> false
  path.startsWith("com/intellij/psi/impl/source/FileL") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiPa") -> false
  path.startsWith("com/intellij/psi/search/searches/S") -> false
  path.startsWith("com/intellij/psi/tree/j") -> false
  path.startsWith("com/intellij/psi/util/I") -> false
  path.startsWith("com/intellij/util/io/PageP") -> false
  path.startsWith("kotlin/reflect/jvm/internal/calls/Inl") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/BinaryClassAnnotationAndConstantLoaderImpl\$loadAnnotation\$1\$") -> false
  path.startsWith("org/jdom/output/Format\$E") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/E") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/CLIT") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/I") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/P") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/X") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/o") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/CliE") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/Dup") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/L") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/V") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ch") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/cfg/CFGNodeKt\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirBuiltinSymbolProvider\$BuiltInsPackageFragment\$l") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/BuildI") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/ChangesE") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/Inp") -> false
  path.startsWith("org/jetbrains/kotlin/ir/overrides/IrO") -> false
  path.startsWith("com/intellij/codeInsight/folding/i") -> false
  path.startsWith("com/intellij/codeWithMe/ClientIdS") -> false
  path.startsWith("com/intellij/model/B") -> false
  path.startsWith("com/intellij/openapi/projectR") -> false
  path.startsWith("com/intellij/openapi/roots/impl/Pa") -> false
  path.startsWith("com/intellij/openapi/util/io/FileUtilRt\$N") -> false
  path.startsWith("com/intellij/openapi/util/io/w") -> false
  path.startsWith("com/intellij/openapi/vfs/StandardFileSystems\$") -> false
  path.startsWith("com/intellij/openapi/vfs/Str") -> false
  path.startsWith("com/intellij/psi/EmptyS") -> false
  path.startsWith("com/intellij/psi/G") -> false
  path.startsWith("com/intellij/psi/Lam") -> false
  path.startsWith("com/intellij/psi/No") -> false
  path.startsWith("com/intellij/psi/PsiAs") -> false
  path.startsWith("com/intellij/psi/PsiDe") -> false
  path.startsWith("com/intellij/psi/PsiEn") -> false
  path.startsWith("com/intellij/psi/PsiFo") -> false
  path.startsWith("com/intellij/psi/PsiNe") -> false
  path.startsWith("com/intellij/psi/PsiPare") -> false
  path.startsWith("com/intellij/psi/PsiPat") -> false
  path.startsWith("com/intellij/psi/PsiReco") -> false
  path.startsWith("com/intellij/psi/PsiReferenceL") -> false
  path.startsWith("com/intellij/psi/PsiTh") -> false
  path.startsWith("com/intellij/psi/PsiU") -> false
  path.startsWith("com/intellij/psi/SmartT") -> false
  path.startsWith("com/intellij/psi/a") -> false
  path.startsWith("com/intellij/psi/codeStyle/R") -> false
  path.startsWith("com/intellij/psi/filters/e") -> false
  path.startsWith("com/intellij/psi/impl/Co") -> false
  path.startsWith("com/intellij/psi/impl/I") -> false
  path.startsWith("com/intellij/psi/impl/PsiN") -> false
  path.startsWith("com/intellij/psi/impl/light/LightF") -> false
  path.startsWith("com/intellij/psi/impl/light/LightV") -> false
  path.startsWith("com/intellij/psi/impl/smartPointers/P") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiA") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiFie") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiT") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/D") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/V") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/reference/impl/m") -> false
  path.startsWith("com/intellij/psi/scope/Pa") -> false
  path.startsWith("com/intellij/psi/scope/processor/V") -> false
  path.startsWith("com/intellij/psi/util/Co") -> false
  path.startsWith("com/intellij/psi/util/PsiEx") -> false
  path.startsWith("com/intellij/psi/util/PsiL") -> false
  path.startsWith("com/intellij/psi/util/PsiUtil\$") -> false
  path.startsWith("com/intellij/ui/DummyIconManager\$") -> false
  path.startsWith("com/intellij/util/V") -> false
  path.startsWith("com/intellij/util/containers/SLRUMap\$") -> false
  path.startsWith("com/intellij/util/io/IOCancellationCallbackHolder\$") -> false
  path.startsWith("com/intellij/util/io/UnI") -> false
  path.startsWith("com/intellij/util/text/K") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/metadata/jvm/deserialization/JvmNameResolver\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/dep") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/DeserializedA") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/e") -> false
  path.startsWith("org/jdom/output/XMLOutputter2") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/actualizer/IrC") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/signature/IdSignatureS") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/lower/ObjectClassLowering\$") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/Gr") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/D") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/Gra") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/Compi") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/p") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/boxing/BoxingA") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/fixStack/FixStackAnalyzer\$I") -> false
  path.startsWith("org/jetbrains/kotlin/daemon/E") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/FirR") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirTopLevelF") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$getIrCo") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$getO") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$B") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ad") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$any\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$l") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$nu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$uni") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/generators/DataClassMembersGenerator\$MyDataClassMethodsGenerator\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$convertScript\$2\$destructuringB") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitForExpression\$1\$2\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitL") -> false
  path.startsWith("org/jetbrains/kotlin/fir/declarations/builder/FirErrorC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/TypeA") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/calls/TypeAl") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$processW") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/cfg/Al") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/mpp/FirExpectActualResolver\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrS") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalCaches") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/Ca") -> false
  path.startsWith("META-INF/v") -> false
  path.startsWith("com/google/common/collect/FilteredEntryMultimap\$Keys\$1\$") -> false
  path.startsWith("com/google/common/collect/ImmutableSortedMap\$1.") -> false
  path.startsWith("com/google/common/collect/Iterables\$10") -> false
  path.startsWith("com/google/common/collect/Iterators\$MergingIterator\$") -> false
  path.startsWith("com/google/common/collect/Maps\$Ac") -> false
  path.startsWith("com/google/common/collect/Multimaps\$AsMap\$EntrySet\$") -> false
  path.startsWith("com/google/common/collect/Multimaps\$TransformedEntriesMultimap\$") -> false
  path.startsWith("com/google/common/collect/Multimaps\$UnmodifiableMultimap\$") -> false
  path.startsWith("com/google/common/collect/Sets\$A") -> false
  path.startsWith("com/intellij/codeInsight/B") -> false
  path.startsWith("com/intellij/codeInsight/CodeInsightUtilCore\$") -> false
  path.startsWith("com/intellij/codeInsight/Cu") -> false
  path.startsWith("com/intellij/codeInsight/I") -> false
  path.startsWith("com/intellij/codeInsight/J") -> false
  path.startsWith("com/intellij/codeInsight/T") -> false
  path.startsWith("com/intellij/codeInsight/c") -> false
  path.startsWith("com/intellij/codeInsight/d") -> false
  path.startsWith("com/intellij/codeInsight/folding/J") -> false
  path.startsWith("com/intellij/codeInsight/j") -> false
  path.startsWith("com/intellij/codeInsight/r") -> false
  path.startsWith("com/intellij/core/CoreApplicationEnvironment\$2") -> false
  path.startsWith("com/intellij/core/CoreL") -> false
  path.startsWith("com/intellij/core/CorePa") -> false
  path.startsWith("com/intellij/core/CorePsiP") -> false
  path.startsWith("com/intellij/diagnostic/EventWatcher\$") -> false
  path.startsWith("com/intellij/ide/plugins/DescriptorListLoadingContext\$") -> false
  path.startsWith("com/intellij/ide/plugins/PathResolver\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginDescriptorLoader\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginLoadingResult\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginManagerCore\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginSet\$") -> false
  path.startsWith("com/intellij/lexer/J") -> false
  path.startsWith("com/intellij/model/ModelBranchU") -> false
  path.startsWith("com/intellij/openapi/application/ex/P") -> false
  path.startsWith("com/intellij/openapi/application/impl/FlushQueue\$F") -> false
  path.startsWith("com/intellij/openapi/components/ServiceDescriptor\$C") -> false
  path.startsWith("com/intellij/openapi/editor/colors/TextAttributesKey\$") -> false
  path.startsWith("com/intellij/openapi/editor/impl/IntervalTreeImpl\$IntervalNode\$") -> false
  path.startsWith("com/intellij/openapi/extensions/impl/ExtensionPointImpl\$4") -> false
  path.startsWith("com/intellij/openapi/extensions/impl/XmlExtensionAdapter\$") -> false
  path.startsWith("com/intellij/openapi/roots/L") -> false
  path.startsWith("com/intellij/openapi/roots/Pa") -> false
  path.startsWith("com/intellij/openapi/util/BuildNumber\$") -> false
  path.startsWith("com/intellij/openapi/util/Pair\$3") -> false
  path.startsWith("com/intellij/openapi/util/Stax") -> false
  path.startsWith("com/intellij/openapi/util/VolatileNullableLazyValue\$") -> false
  path.startsWith("com/intellij/openapi/util/io/FileSystemUtil\$I") -> false
  path.startsWith("com/intellij/openapi/vfs/impl/ZipEntryMap\$EntrySet\$") -> false
  path.startsWith("com/intellij/pom/j") -> false
  path.startsWith("com/intellij/psi/B") -> false
  path.startsWith("com/intellij/psi/CommonR") -> false
  path.startsWith("com/intellij/psi/Cons") -> false
  path.startsWith("com/intellij/psi/Hie") -> false
  path.startsWith("com/intellij/psi/I") -> false
  path.startsWith("com/intellij/psi/LanguageA") -> false
  path.startsWith("com/intellij/psi/PsiAno") -> false
  path.startsWith("com/intellij/psi/PsiBinaryE") -> false
  path.startsWith("com/intellij/psi/PsiBl") -> false
  path.startsWith("com/intellij/psi/PsiBr") -> false
  path.startsWith("com/intellij/psi/PsiCodeB") -> false
  path.startsWith("com/intellij/psi/PsiDis") -> false
  path.startsWith("com/intellij/psi/PsiDoW") -> false
  path.startsWith("com/intellij/psi/PsiDocCommentO") -> false
  path.startsWith("com/intellij/psi/PsiEll") -> false
  path.startsWith("com/intellij/psi/PsiEm") -> false
  path.startsWith("com/intellij/psi/PsiFie") -> false
  path.startsWith("com/intellij/psi/PsiFu") -> false
  path.startsWith("com/intellij/psi/PsiG") -> false
  path.startsWith("com/intellij/psi/PsiId") -> false
  path.startsWith("com/intellij/psi/PsiIf") -> false
  path.startsWith("com/intellij/psi/PsiInf") -> false
  path.startsWith("com/intellij/psi/PsiIns") -> false
  path.startsWith("com/intellij/psi/PsiInt") -> false
  path.startsWith("com/intellij/psi/PsiK") -> false
  path.startsWith("com/intellij/psi/PsiLab") -> false
  path.startsWith("com/intellij/psi/PsiLiteral.") -> false
  path.startsWith("com/intellij/psi/PsiLiteralE") -> false
  path.startsWith("com/intellij/psi/PsiLoca") -> false
  path.startsWith("com/intellij/psi/PsiLoo") -> false
  path.startsWith("com/intellij/psi/PsiMig") -> false
  path.startsWith("com/intellij/psi/PsiNameH") -> false
  path.startsWith("com/intellij/psi/PsiNameV") -> false
  path.startsWith("com/intellij/psi/PsiPolya") -> false
  path.startsWith("com/intellij/psi/PsiPos") -> false
  path.startsWith("com/intellij/psi/PsiQualifiedE") -> false
  path.startsWith("com/intellij/psi/PsiRece") -> false
  path.startsWith("com/intellij/psi/PsiReferenceE") -> false
  path.startsWith("com/intellij/psi/PsiReferencePa") -> false
  path.startsWith("com/intellij/psi/PsiReq") -> false
  path.startsWith("com/intellij/psi/PsiRet") -> false
  path.startsWith("com/intellij/psi/PsiTe") -> false
  path.startsWith("com/intellij/psi/PsiTry") -> false
  path.startsWith("com/intellij/psi/PsiV") -> false
  path.startsWith("com/intellij/psi/PsiWhil") -> false
  path.startsWith("com/intellij/psi/PsiWi") -> false
  path.startsWith("com/intellij/psi/PsiY") -> false
  path.startsWith("com/intellij/psi/Se") -> false
  path.startsWith("com/intellij/psi/Th") -> false
  path.startsWith("com/intellij/psi/codeStyle/V") -> false
  path.startsWith("com/intellij/psi/filters/Cons") -> false
  path.startsWith("com/intellij/psi/filters/c") -> false
  path.startsWith("com/intellij/psi/impl/ElementP") -> false
  path.startsWith("com/intellij/psi/impl/Ex") -> false
  path.startsWith("com/intellij/psi/impl/L") -> false
  path.startsWith("com/intellij/psi/impl/PsiCo") -> false
  path.startsWith("com/intellij/psi/impl/PsiDi") -> false
  path.startsWith("com/intellij/psi/impl/PsiEx") -> false
  path.startsWith("com/intellij/psi/impl/PsiI") -> false
  path.startsWith("com/intellij/psi/impl/PsiJ") -> false
  path.startsWith("com/intellij/psi/impl/PsiV") -> false
  path.startsWith("com/intellij/psi/impl/cache/M") -> false
  path.startsWith("com/intellij/psi/impl/cache/R") -> false
  path.startsWith("com/intellij/psi/impl/file/impl/J") -> false
  path.startsWith("com/intellij/psi/impl/light/A") -> false
  path.startsWith("com/intellij/psi/impl/light/D") -> false
  path.startsWith("com/intellij/psi/impl/light/I") -> false
  path.startsWith("com/intellij/psi/impl/light/J") -> false
  path.startsWith("com/intellij/psi/impl/light/LightEm") -> false
  path.startsWith("com/intellij/psi/impl/light/LightEx") -> false
  path.startsWith("com/intellij/psi/impl/light/LightI") -> false
  path.startsWith("com/intellij/psi/impl/light/LightK") -> false
  path.startsWith("com/intellij/psi/impl/se") -> false
  path.startsWith("com/intellij/psi/impl/smartPointers/T") -> false
  path.startsWith("com/intellij/psi/impl/source/Con") -> false
  path.startsWith("com/intellij/psi/impl/source/Hi") -> false
  path.startsWith("com/intellij/psi/impl/source/I") -> false
  path.startsWith("com/intellij/psi/impl/source/Pa") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiD") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiL") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiPr") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiU") -> false
  path.startsWith("com/intellij/psi/impl/source/SourceJ") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/C") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/ResolveCl") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/ResolveV") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/reference/impl/providers/G") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/Chi") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/E") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/IJ") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/injected/S") -> false
  path.startsWith("com/intellij/psi/scope/J") -> false
  path.startsWith("com/intellij/psi/scope/M") -> false
  path.startsWith("com/intellij/psi/scope/N") -> false
  path.startsWith("com/intellij/psi/scope/PsiC") -> false
  path.startsWith("com/intellij/psi/scope/processor/C") -> false
  path.startsWith("com/intellij/psi/scope/processor/FilterS") -> false
  path.startsWith("com/intellij/psi/scope/u") -> false
  path.startsWith("com/intellij/psi/search/Pa") -> false
  path.startsWith("com/intellij/psi/search/searches/D") -> false
  path.startsWith("com/intellij/psi/stub/") -> false
  path.startsWith("com/intellij/psi/stubs/PsiC") -> false
  path.startsWith("com/intellij/psi/tree/IStubFileElementType\$") -> false
  path.startsWith("com/intellij/psi/util/A") -> false
  path.startsWith("com/intellij/psi/util/F") -> false
  path.startsWith("com/intellij/psi/util/PsiCl") -> false
  path.startsWith("com/intellij/psi/util/PsiCo") -> false
  path.startsWith("com/intellij/psi/util/PsiFormatUtil.") -> false
  path.startsWith("com/intellij/psi/util/PsiMe") -> false
  path.startsWith("com/intellij/psi/util/PsiP") -> false
  path.startsWith("com/intellij/psi/util/PsiS") -> false
  path.startsWith("com/intellij/psi/util/PsiUtil.") -> false
  path.startsWith("com/intellij/ui/IconManagerH") -> false
  path.startsWith("com/intellij/util/Bl") -> false
  path.startsWith("com/intellij/util/Consumer\$") -> false
  path.startsWith("com/intellij/util/Functions\$3") -> false
  path.startsWith("com/intellij/util/Functions\$4") -> false
  path.startsWith("com/intellij/util/Functions\$6") -> false
  path.startsWith("com/intellij/util/Functions\$7") -> false
  path.startsWith("com/intellij/util/Functions\$8") -> false
  path.startsWith("com/intellij/util/NoO") -> false
  path.startsWith("com/intellij/util/NullableFunction\$") -> false
  path.startsWith("com/intellij/util/Ur") -> false
  path.startsWith("com/intellij/util/containers/ConcurrentFactoryMap\$CollectionWrapper\$S") -> false
  path.startsWith("com/intellij/util/containers/ConcurrentRefHashMap\$2") -> false
  path.startsWith("com/intellij/util/containers/ContainerUtil\$8\$") -> false
  path.startsWith("com/intellij/util/io/DirectByteBufferP") -> false
  path.startsWith("com/intellij/util/io/FileChannelU") -> false
  path.startsWith("com/intellij/util/io/Fin") -> false
  path.startsWith("com/intellij/util/io/IntToIntBtree\$BtreeP") -> false
  path.startsWith("com/intellij/util/io/IntToIntBtree\$K") -> false
  path.startsWith("com/intellij/util/io/M") -> false
  path.startsWith("com/intellij/util/io/Page.") -> false
  path.startsWith("com/intellij/util/io/PersistentEnumeratorBase\$C") -> false
  path.startsWith("com/intellij/util/io/PersistentHashMapValueStorage\$O") -> false
  path.startsWith("com/intellij/util/io/PersistentMapWal\$c") -> false
  path.startsWith("com/intellij/util/io/PersistentMapWal\$f") -> false
  path.startsWith("com/intellij/util/io/PersistentMapWal\$s") -> false
  path.startsWith("com/intellij/util/io/Po") -> false
  path.startsWith("com/intellij/util/io/Ra") -> false
  path.startsWith("com/intellij/util/keyFMap/MapBackedFMap\$") -> false
  path.startsWith("com/intellij/util/lang/ClasspathCache\$LoaderData.") -> false
  path.startsWith("com/intellij/util/lang/ClasspathCache\$N") -> false
  path.startsWith("com/intellij/util/lang/JdkZipResourceFile\$S") -> false
  path.startsWith("com/intellij/util/lang/Loader\$") -> false
  path.startsWith("com/intellij/util/lang/S") -> false
  path.startsWith("com/intellij/util/lo") -> false
  path.startsWith("com/intellij/util/messages/impl/CompositeMessageBus\$") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$MessageP") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$R") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$T") -> false
  path.startsWith("com/intellij/util/text/Ca") -> false
  path.startsWith("com/intellij/util/text/L") -> false
  path.startsWith("gnu/trove/TIntLongI") -> false
  path.startsWith("gnu/trove/TLongIntI") -> false
  path.startsWith("gnu/trove/TLongIt") -> false
  path.startsWith("gnu/trove/TLongObjectI") -> false
  path.startsWith("gnu/trove/TObjectId") -> false
  path.startsWith("gnu/trove/To") -> false
  path.startsWith("kotlin/enums/EnumEntriesJ") -> false
  path.startsWith("kotlin/experimental/ExperimentalN") -> false
  path.startsWith("kotlin/js/ExperimentalJsF") -> false
  path.startsWith("kotlin/js/ExperimentalJsR") -> false
  path.startsWith("kotlin/jvm/I") -> false
  path.startsWith("kotlin/reflect/KMutableProperty\$D") -> false
  path.startsWith("kotlin/reflect/KMutableProperty0\$D") -> false
  path.startsWith("kotlin/reflect/KMutableProperty1\$D") -> false
  path.startsWith("kotlin/reflect/KMutableProperty2\$D") -> false
  path.startsWith("kotlin/reflect/KProperty0\$D") -> false
  path.startsWith("kotlin/reflect/KProperty1\$D") -> false
  path.startsWith("kotlin/reflect/KProperty2\$D") -> false
  path.startsWith("kotlin/reflect/full/KClasses\$allSupertypes\$1") -> false
  path.startsWith("kotlin/reflect/full/KClasses\$s") -> false
  path.startsWith("kotlin/reflect/jvm/internal/KClassC") -> false
  path.startsWith("kotlin/reflect/jvm/internal/calls/AnnotationConstructorCallerKt\$createAnnotationInstance\$r") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassDescriptor\$FunctionTypeConstructor\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassKind\$Companion\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassScope\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/jvm/JvmBuiltInsCustomizer\$getJdkMethodStatus\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/jvm/JvmBuiltInsCustomizer\$isMutabilityViolation\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaAnnotationOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaModifierListOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/AnnotationTypeQualifierResolverK") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/Dep") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/components/T") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/descriptors/V") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/JavaM") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/descriptors/LazyJavaScope\$res") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/descriptors/LazyJavaStaticClassScope\$flatMapJavaStaticSupertypesScopes\$1.") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/types/RawSubstitution\$W") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaAnnotation\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaMethod\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaType\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/ListBasedJavaAnnotationOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/No") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancement\$P") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancementK") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/TypeA") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$P") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$S") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$W") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$loadC") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/TypeMappingConfiguration\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/header/ReadKotlinClassHeaderAnnotationVisitor\$KotlinMetadataArgumentVisitor\$3") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/DescriptorEquivalenceForOverrides\$areCallableDescriptorsEquivalent\$overridingUtil\$1.") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/constants/ConstantValueFactory\$createArrayValue\$3") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/descriptorUtil/DescriptorUtilsKt\$declaresOrInheritsDefaultValue\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/descriptorUtil/DescriptorUtilsKt\$firstOverridden\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/DeserializationConfiguration\$DefaultI") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/MemberDeserializer\$l") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/descriptors/DeserializedClassDescriptor\$i") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/An") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Cu") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/NotNullT") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/TypeSystemCommonBackendContext\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Unr") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/R") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemContext\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemInferenceExtensionContext\$") -> false
  path.startsWith("org/jdom/output/L") -> false
  path.startsWith("org/jetbrains/kotlin/KtFakeSourceElementKind\$SuperCallE") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/Col") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/lower/inline/Inliner") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/lower/AnnotationLowering\$") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/S") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/MessageCollectorB") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/P") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/config/K") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/inline/MethodInlinerUtilKt\$analyzeMethodNodeWithInterpreter\$analyzer\$1\$") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/inline/SMAPParser\$") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/transformer/CompositeMethodTransformer\$") -> false
  path.startsWith("org/jetbrains/kotlin/config/AnalysisFlags\$expe") -> false
  path.startsWith("org/jetbrains/kotlin/diagnostics/Du") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/FirE") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirImplementationMismatchChecker\$checkC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirModifierChecker\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/diagnostics/ConeDiagnosticToFirDiagnosticKt\$f") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/jvm/checkers/expression/FirS") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$I") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$N") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$W") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$array\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$bu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createN") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createS") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$i") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ma") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$mu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$s") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/jvm/FirJvmBackendExtension\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$toFirValueParameter\$1") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitDestructuringDeclaration\$1") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitDestructuringDeclaration\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/declarations/impl/FirErrorC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/deserialization/LibraryPathFilter\$LibraryList\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/expressions/impl/FirNo") -> false
  path.startsWith("org/jetbrains/kotlin/fir/java/deserialization/JvmClassFileBasedSymbolProvider\$extractClassMetadata\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/java/scopes/JavaClassUseSiteMemberScope\$s") -> false
  path.startsWith("org/jetbrains/kotlin/fir/renderer/ConeTypeRendererW") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$F") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$b") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/diagnostics/ConeNoCon") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/inference/FirDelegatedPropertyInferenceSession\$con") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/inference/FirDelegatedPropertyInferenceSession\$h") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirTypeResolverImpl\$C") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirTypeResolverImpl\$P") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirDeclarationsResolveTransformer\$transformAnonymousO") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/ReturnTypeCalculatorWithJump\$F") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/AbstractFirSpecificAnnotationResolveTransformer\$A") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/AbstractFirSpecificAnnotationResolveTransformer\$C") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/CompilerRequiredAnnotationsH") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrE") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrP") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrT") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/impl/FirClassLikeSymbolK") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/DirtyF") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalJsCaches") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/IncrementalJvmCaches") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/p") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/AppendableBasicM") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/AppendableD") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/G") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/InM") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/No") -> false
  path.startsWith("org/jetbrains/kotlin/incremental/storage/SourceToO") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/KlibKt\$serializeModuleIntoKlib\$lambda\$3") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/Pre") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/transformers/irToJs/IrModuleToJsTransformer\$m") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/transformers/irToJs/Merger\$m") -> false
  path.startsWith("org/jetbrains/kotlin/ir/backend/js/transformers/irToJs/P") -> false
  path.startsWith("org/jetbrains/kotlin/ir/util/FakeOverrideB") -> false
  path.startsWith("org/jetbrains/kotlin/name/Jv") -> false
  path.startsWith("org/jetbrains/kotlin/name/StandardClassIds\$Annotations\$J") -> false
  path.startsWith("org/jetbrains/kotlin/name/StandardClassIds\$J") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/multiplatform/ExpectActualCompatibility\$Incompatible\$U") -> false

  else -> true
}

// Pruned:
fun isPruned(path: String) = when {
  path.startsWith("org/jetbrains/kotlin/javac") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/j") -> false
  path.startsWith("org/jetbrains/kotlin/kot") -> false
  path.startsWith("com/intellij/psi/impl/compiled/Cls") -> false
  path.startsWith("com/intellij/psi/controlFlow/Con") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/J") -> false
  path.startsWith("org/jetbrains/kotlin/fir/li") -> false
  path.startsWith("org/jline/r") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/j") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/g") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/i") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/P") -> false
  path.startsWith("org/jline/u") -> false
  path.startsWith("com/intellij/lang/java/p") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/F") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrClassifierStorage\$") -> false
  path.startsWith("com/intellij/lang/jvm/J") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/r") -> false
  path.startsWith("com/intellij/psi/PsiTy") -> false
  path.startsWith("com/intellij/psi/PsiDia") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiJa") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/p") -> false
  path.startsWith("com/intellij/psi/PsiS") -> false
  path.startsWith("com/intellij/psi/impl/source/javadoc/Ps") -> false
  path.startsWith("org/objectweb/asm/c") -> false
  path.startsWith("org/objectweb/asm/u") -> false
  path.startsWith("com/intellij/psi/PsiCl") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/JavaE") -> false
  path.startsWith("org/fusesource/jansi/i") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$c") -> false
  path.startsWith("com/intellij/codeInsight/E") -> false
  path.startsWith("com/intellij/psi/PsiJa") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$i") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/f") -> false
  path.startsWith("com/intellij/psi/impl/J") -> false
  path.startsWith("com/intellij/psi/impl/compiled/S") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiI") -> false
  path.startsWith("com/intellij/core/CoreJ") -> false
  path.startsWith("com/intellij/ide/plugins/IdeaPluginDescriptorImpl\$") -> false
  path.startsWith("com/intellij/psi/impl/PsiS") -> false
  path.startsWith("com/intellij/psi/impl/light/LightP") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Er") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/A") -> false
  path.startsWith("org/jetbrains/kotlin/psi2ir/generators/Ex") -> false
  path.startsWith("org/objectweb/asm/C") -> false
  path.startsWith("com/intellij/psi/PsiMe") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/overrides/FakeOverrideB") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/J") -> false
  path.startsWith("org/jetbrains/kotlin/fir/scopes/Fa") -> false
  path.startsWith("org/objectweb/asm/tree/I") -> false
  path.startsWith("com/intellij/lang/jvm/a") -> false
  path.startsWith("com/intellij/lang/jvm/t") -> false
  path.startsWith("com/intellij/psi/PsiCa") -> false
  path.startsWith("com/intellij/psi/PsiCon") -> false
  path.startsWith("com/intellij/psi/PsiIm") -> false
  path.startsWith("com/intellij/psi/PsiRes") -> false
  path.startsWith("com/intellij/psi/com") -> false
  path.startsWith("com/intellij/psi/impl/PsiCl") -> false
  path.startsWith("com/intellij/psi/impl/cache/T") -> false
  path.startsWith("com/intellij/psi/impl/light/LightC") -> false
  path.startsWith("com/intellij/psi/impl/light/LightJ") -> false
  path.startsWith("com/intellij/psi/impl/source/Cl") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/JavaD") -> false
  path.startsWith("com/intellij/psi/util/M") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancement\$S") -> false
  path.startsWith("kotlin/reflect/jvm/internal/p") -> false
  path.startsWith("kotlin/script/experimental/jsr223/KotlinJsr223DefaultScriptC") -> false
  path.startsWith("org/fusesource/jansi/Ansi\$") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/Ut") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createM") -> false
  path.startsWith("org/jline/terminal/A") -> false
  path.startsWith("org/objectweb/asm/tree/L") -> false
  path.startsWith("org/picocontainer/d") -> false
  path.startsWith("com/intellij/codeInsight/N") -> false
  path.startsWith("com/intellij/lang/java/l") -> false
  path.startsWith("com/intellij/psi/JavaCodeFragment\$") -> false
  path.startsWith("com/intellij/psi/JavaR") -> false
  path.startsWith("com/intellij/psi/impl/compiled/F") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiR") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/J") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/Ps") -> false
  path.startsWith("com/intellij/psi/javadoc/P") -> false
  path.startsWith("com/intellij/psi/scope/c") -> false
  path.startsWith("com/intellij/psi/util/PsiTy") -> false
  path.startsWith("com/intellij/psi/util/T") -> false
  path.startsWith("com/intellij/util/lang/f") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/BasicJvmScriptingHostK") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/MessageR") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/FirD") -> false
  path.startsWith("org/jetbrains/kotlin/fir/scopes/impl/FirSco") -> false
  path.startsWith("org/jetbrains/kotlin/ir/linkage/partial/I") -> false
  path.startsWith("org/jline/terminal/T") -> false
  path.startsWith("org/objectweb/asm/M") -> false
  path.startsWith("com/intellij/codeInsight/A") -> false
  path.startsWith("com/intellij/codeWithMe/ClientId\$Companion\$") -> false
  path.startsWith("com/intellij/core/J") -> false
  path.startsWith("com/intellij/psi/PsiAr") -> false
  path.startsWith("com/intellij/psi/PsiJv") -> false
  path.startsWith("com/intellij/psi/PsiMo") -> false
  path.startsWith("com/intellij/psi/PsiPac") -> false
  path.startsWith("com/intellij/psi/PsiPr") -> false
  path.startsWith("com/intellij/psi/codeStyle/J") -> false
  path.startsWith("com/intellij/psi/impl/PsiElementF") -> false
  path.startsWith("com/intellij/psi/impl/light/LightRecordC") -> false
  path.startsWith("com/intellij/psi/impl/light/LightT") -> false
  path.startsWith("com/intellij/psi/impl/source/JavaD") -> false
  path.startsWith("com/intellij/psi/impl/source/JavaV") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiC") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiE") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiM") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/S") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/JavaS") -> false
  path.startsWith("com/intellij/psi/scope/E") -> false
  path.startsWith("com/intellij/psi/util/ClassU") -> false
  path.startsWith("com/intellij/util/X") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/AnnotationTypeQualifierResolver\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/types/T") -> false
  path.startsWith("org/jdom/output/s") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/actualizer/IrExpectActualA") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/proto/Act") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/A") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/CliC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/ConversionUtilsKt\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$d") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$boolean\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createP") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/Inte") -> false
  path.startsWith("org/jline/terminal/M") -> false
  path.startsWith("org/jline/terminal/impl/P") -> false
  path.startsWith("org/objectweb/asm/A") -> false
  path.startsWith("org/objectweb/asm/tree/T") -> false
  path.startsWith("org/picocontainer/P") -> false
  path.startsWith("com/intellij/lang/jvm/f") -> false
  path.startsWith("com/intellij/openapi/progress/J") -> false
  path.startsWith("com/intellij/patterns/PsiJ") -> false
  path.startsWith("com/intellij/pl") -> false
  path.startsWith("com/intellij/psi/ClassF") -> false
  path.startsWith("com/intellij/psi/PsiLam") -> false
  path.startsWith("com/intellij/psi/PsiPara") -> false
  path.startsWith("com/intellij/psi/Ty") -> false
  path.startsWith("com/intellij/psi/controlFlow/R") -> false
  path.startsWith("com/intellij/psi/impl/Sc") -> false
  path.startsWith("com/intellij/psi/impl/T") -> false
  path.startsWith("com/intellij/psi/impl/file/PsiPackageI") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiPa") -> false
  path.startsWith("com/intellij/psi/javadoc/J") -> false
  path.startsWith("com/intellij/psi/presentation/java/C") -> false
  path.startsWith("com/intellij/psi/presentation/java/JavaP") -> false
  path.startsWith("com/intellij/psi/search/searches/S") -> false
  path.startsWith("com/intellij/psi/tree/j") -> false
  path.startsWith("com/intellij/psi/util/I") -> false
  path.startsWith("kotlin/reflect/jvm/internal/calls/Inl") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/BinaryClassAnnotationAndConstantLoaderImpl\$loadAnnotation\$1\$") -> false
  path.startsWith("kotlin/script/experimental/jsr223/KotlinJsr223DefaultScriptEv") -> false
  path.startsWith("org/jdom/output/Format\$E") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/E") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$d") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$l") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$p") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/P") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/X") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/o") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/V") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ch") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$3") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/cfg/CFGNodeKt\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirBuiltinSymbolProvider\$BuiltInsPackageFragment\$l") -> false
  path.startsWith("org/jetbrains/kotlin/ir/overrides/IrO") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/S") -> false
  path.startsWith("org/jline/terminal/impl/L") -> false
  path.startsWith("org/jline/terminal/s") -> false
  path.startsWith("org/objectweb/asm/F") -> false
  path.startsWith("org/objectweb/asm/S") -> false
  path.startsWith("org/objectweb/asm/T") -> false
  path.startsWith("org/objectweb/asm/s") -> false
  path.startsWith("org/objectweb/asm/tree/Me") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/B") -> false
  path.startsWith("com/intellij/codeInsight/folding/i") -> false
  path.startsWith("com/intellij/codeWithMe/ClientIdS") -> false
  path.startsWith("com/intellij/ide/highlighter/Ja") -> false
  path.startsWith("com/intellij/lang/java/Ja") -> false
  path.startsWith("com/intellij/model/B") -> false
  path.startsWith("com/intellij/openapi/projectR") -> false
  path.startsWith("com/intellij/openapi/roots/impl/Pa") -> false
  path.startsWith("com/intellij/openapi/util/io/FileUtilRt\$N") -> false
  path.startsWith("com/intellij/openapi/util/io/w") -> false
  path.startsWith("com/intellij/openapi/vfs/StandardFileSystems\$") -> false
  path.startsWith("com/intellij/openapi/vfs/Str") -> false
  path.startsWith("com/intellij/psi/EmptyS") -> false
  path.startsWith("com/intellij/psi/G") -> false
  path.startsWith("com/intellij/psi/JavaD") -> false
  path.startsWith("com/intellij/psi/Lam") -> false
  path.startsWith("com/intellij/psi/No") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationA") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationC") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationM") -> false
  path.startsWith("com/intellij/psi/PsiAs") -> false
  path.startsWith("com/intellij/psi/PsiDe") -> false
  path.startsWith("com/intellij/psi/PsiEn") -> false
  path.startsWith("com/intellij/psi/PsiExpressionL") -> false
  path.startsWith("com/intellij/psi/PsiFo") -> false
  path.startsWith("com/intellij/psi/PsiNe") -> false
  path.startsWith("com/intellij/psi/PsiPare") -> false
  path.startsWith("com/intellij/psi/PsiPat") -> false
  path.startsWith("com/intellij/psi/PsiReco") -> false
  path.startsWith("com/intellij/psi/PsiReferenceL") -> false
  path.startsWith("com/intellij/psi/PsiTh") -> false
  path.startsWith("com/intellij/psi/PsiU") -> false
  path.startsWith("com/intellij/psi/a") -> false
  path.startsWith("com/intellij/psi/controlFlow/An") -> false
  path.startsWith("com/intellij/psi/controlFlow/B") -> false
  path.startsWith("com/intellij/psi/controlFlow/L") -> false
  path.startsWith("com/intellij/psi/filters/e") -> false
  path.startsWith("com/intellij/psi/impl/Co") -> false
  path.startsWith("com/intellij/psi/impl/I") -> false
  path.startsWith("com/intellij/psi/impl/compiled/M") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/L") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/M") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/S") -> false
  path.startsWith("com/intellij/psi/impl/light/LightF") -> false
  path.startsWith("com/intellij/psi/impl/light/LightMet") -> false
  path.startsWith("com/intellij/psi/impl/light/LightRef") -> false
  path.startsWith("com/intellij/psi/impl/source/JavaLightS") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiA") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiFie") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiT") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/D") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/V") -> false
  path.startsWith("com/intellij/psi/infos/C") -> false
  path.startsWith("com/intellij/psi/presentation/java/P") -> false
  path.startsWith("com/intellij/psi/presentation/java/V") -> false
  path.startsWith("com/intellij/psi/scope/Pa") -> false
  path.startsWith("com/intellij/psi/scope/processor/MethodC") -> false
  path.startsWith("com/intellij/psi/util/Co") -> false
  path.startsWith("com/intellij/psi/util/JavaP") -> false
  path.startsWith("com/intellij/psi/util/PsiL") -> false
  path.startsWith("com/intellij/psi/util/PsiUtil\$") -> false
  path.startsWith("com/intellij/ui/DummyIconManager\$") -> false
  path.startsWith("com/intellij/util/V") -> false
  path.startsWith("com/intellij/util/containers/SLRUMap\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/metadata/jvm/deserialization/JvmNameResolver\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/dep") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/DeserializedA") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/e") -> false
  path.startsWith("org/fusesource/jansi/AnsiC") -> false
  path.startsWith("org/fusesource/jansi/AnsiRendere") -> false
  path.startsWith("org/jdom/output/XMLOutputter2") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/actualizer/IrC") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/serialization/signature/IdSignatureS") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$a") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$f") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$k") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$s") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$v") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/lower/ObjectClassLowering\$") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/Gr") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/D") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/Gra") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/Compi") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/FirKotlinToJvmBytecodeCompilerK") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/p") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/boxing/BoxingA") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/fixStack/FixStackAnalyzer\$I") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/FirR") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirTopLevelF") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$getIrCo") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$getO") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$B") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ad") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$any\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createCon") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$l") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$nu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$uni") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/generators/DataClassMembersGenerator\$MyDataClassMethodsGenerator\$g") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$convertScript\$2\$destructuringB") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitForExpression\$1\$2\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitL") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/TypeA") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/calls/TypeAl") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$processW") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/cfg/Al") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/mpp/FirExpectActualResolver\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrS") -> false
  path.startsWith("org/jline/k") -> false
  path.startsWith("org/jline/terminal/impl/AbstractT") -> false
  path.startsWith("org/jline/terminal/impl/D") -> false
  path.startsWith("org/jline/terminal/impl/E") -> false
  path.startsWith("org/jline/terminal/impl/M") -> false
  path.startsWith("org/objectweb/asm/H") -> false
  path.startsWith("org/objectweb/asm/R") -> false
  path.startsWith("org/objectweb/asm/tree/A") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/Sm") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/So") -> false
  path.startsWith("com/google/common/collect/FilteredEntryMultimap\$Keys\$1\$") -> false
  path.startsWith("com/google/common/collect/ImmutableSortedMap\$1.") -> false
  path.startsWith("com/google/common/collect/Iterables\$10") -> false
  path.startsWith("com/google/common/collect/Iterators\$MergingIterator\$") -> false
  path.startsWith("com/google/common/collect/Maps\$Ac") -> false
  path.startsWith("com/google/common/collect/Multimaps\$AsMap\$EntrySet\$") -> false
  path.startsWith("com/google/common/collect/Multimaps\$TransformedEntriesMultimap\$") -> false
  path.startsWith("com/google/common/collect/Multimaps\$UnmodifiableMultimap\$") -> false
  path.startsWith("com/google/common/collect/Sets\$A") -> false
  path.startsWith("com/intellij/codeInsight/B") -> false
  path.startsWith("com/intellij/codeInsight/CodeInsightUtilCore\$") -> false
  path.startsWith("com/intellij/codeInsight/Cu") -> false
  path.startsWith("com/intellij/codeInsight/I") -> false
  path.startsWith("com/intellij/codeInsight/J") -> false
  path.startsWith("com/intellij/codeInsight/T") -> false
  path.startsWith("com/intellij/codeInsight/c") -> false
  path.startsWith("com/intellij/codeInsight/d") -> false
  path.startsWith("com/intellij/codeInsight/folding/J") -> false
  path.startsWith("com/intellij/codeInsight/j") -> false
  path.startsWith("com/intellij/codeInsight/r") -> false
  path.startsWith("com/intellij/core/CoreApplicationEnvironment\$2") -> false
  path.startsWith("com/intellij/core/CoreL") -> false
  path.startsWith("com/intellij/core/CorePa") -> false
  path.startsWith("com/intellij/core/CorePsiP") -> false
  path.startsWith("com/intellij/diagnostic/EventWatcher\$") -> false
  path.startsWith("com/intellij/ide/plugins/DescriptorListLoadingContext\$") -> false
  path.startsWith("com/intellij/ide/plugins/PathResolver\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginDescriptorLoader\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginLoadingResult\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginManagerCore\$") -> false
  path.startsWith("com/intellij/ide/plugins/PluginSet\$") -> false
  path.startsWith("com/intellij/lexer/J") -> false
  path.startsWith("com/intellij/model/ModelBranchU") -> false
  path.startsWith("com/intellij/openapi/application/ex/P") -> false
  path.startsWith("com/intellij/openapi/application/impl/FlushQueue\$F") -> false
  path.startsWith("com/intellij/openapi/components/ServiceDescriptor\$C") -> false
  path.startsWith("com/intellij/openapi/editor/colors/TextAttributesKey\$") -> false
  path.startsWith("com/intellij/openapi/editor/impl/IntervalTreeImpl\$IntervalNode\$") -> false
  path.startsWith("com/intellij/openapi/extensions/impl/ExtensionPointImpl\$4") -> false
  path.startsWith("com/intellij/openapi/extensions/impl/XmlExtensionAdapter\$") -> false
  path.startsWith("com/intellij/openapi/roots/L") -> false
  path.startsWith("com/intellij/openapi/roots/Pa") -> false
  path.startsWith("com/intellij/openapi/util/BuildNumber\$") -> false
  path.startsWith("com/intellij/openapi/util/Pair\$3") -> false
  path.startsWith("com/intellij/openapi/util/Stax") -> false
  path.startsWith("com/intellij/openapi/util/VolatileNullableLazyValue\$") -> false
  path.startsWith("com/intellij/openapi/util/io/FileSystemUtil\$I") -> false
  path.startsWith("com/intellij/openapi/vfs/impl/ZipEntryMap\$EntrySet\$") -> false
  path.startsWith("com/intellij/pom/j") -> false
  path.startsWith("com/intellij/psi/B") -> false
  path.startsWith("com/intellij/psi/Cons") -> false
  path.startsWith("com/intellij/psi/Hie") -> false
  path.startsWith("com/intellij/psi/I") -> false
  path.startsWith("com/intellij/psi/JVMElementFactory.") -> false
  path.startsWith("com/intellij/psi/JavaCodeFragment.") -> false
  path.startsWith("com/intellij/psi/JavaE") -> false
  path.startsWith("com/intellij/psi/JavaM") -> false
  path.startsWith("com/intellij/psi/JavaP") -> false
  path.startsWith("com/intellij/psi/JavaT") -> false
  path.startsWith("com/intellij/psi/Jv") -> false
  path.startsWith("com/intellij/psi/PsiAnnotate") -> false
  path.startsWith("com/intellij/psi/PsiAnnotation\$") -> false
  path.startsWith("com/intellij/psi/PsiAnnotation.") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationE") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationO") -> false
  path.startsWith("com/intellij/psi/PsiAnnotationP") -> false
  path.startsWith("com/intellij/psi/PsiAno") -> false
  path.startsWith("com/intellij/psi/PsiBinaryE") -> false
  path.startsWith("com/intellij/psi/PsiBl") -> false
  path.startsWith("com/intellij/psi/PsiBr") -> false
  path.startsWith("com/intellij/psi/PsiCodeB") -> false
  path.startsWith("com/intellij/psi/PsiDis") -> false
  path.startsWith("com/intellij/psi/PsiDoW") -> false
  path.startsWith("com/intellij/psi/PsiDocCommentO") -> false
  path.startsWith("com/intellij/psi/PsiElementFactory.") -> false
  path.startsWith("com/intellij/psi/PsiElementFi") -> false
  path.startsWith("com/intellij/psi/PsiEll") -> false
  path.startsWith("com/intellij/psi/PsiEm") -> false
  path.startsWith("com/intellij/psi/PsiExpression.") -> false
  path.startsWith("com/intellij/psi/PsiExpressionS") -> false
  path.startsWith("com/intellij/psi/PsiFie") -> false
  path.startsWith("com/intellij/psi/PsiFu") -> false
  path.startsWith("com/intellij/psi/PsiG") -> false
  path.startsWith("com/intellij/psi/PsiId") -> false
  path.startsWith("com/intellij/psi/PsiIf") -> false
  path.startsWith("com/intellij/psi/PsiInf") -> false
  path.startsWith("com/intellij/psi/PsiIns") -> false
  path.startsWith("com/intellij/psi/PsiInt") -> false
  path.startsWith("com/intellij/psi/PsiK") -> false
  path.startsWith("com/intellij/psi/PsiLab") -> false
  path.startsWith("com/intellij/psi/PsiLiteral.") -> false
  path.startsWith("com/intellij/psi/PsiLiteralE") -> false
  path.startsWith("com/intellij/psi/PsiLoca") -> false
  path.startsWith("com/intellij/psi/PsiLoo") -> false
  path.startsWith("com/intellij/psi/PsiNameH") -> false
  path.startsWith("com/intellij/psi/PsiNameV") -> false
  path.startsWith("com/intellij/psi/PsiPolya") -> false
  path.startsWith("com/intellij/psi/PsiPos") -> false
  path.startsWith("com/intellij/psi/PsiQualifiedE") -> false
  path.startsWith("com/intellij/psi/PsiRece") -> false
  path.startsWith("com/intellij/psi/PsiReferenceE") -> false
  path.startsWith("com/intellij/psi/PsiReferencePa") -> false
  path.startsWith("com/intellij/psi/PsiReq") -> false
  path.startsWith("com/intellij/psi/PsiRet") -> false
  path.startsWith("com/intellij/psi/PsiTry") -> false
  path.startsWith("com/intellij/psi/PsiV") -> false
  path.startsWith("com/intellij/psi/PsiWhil") -> false
  path.startsWith("com/intellij/psi/PsiWi") -> false
  path.startsWith("com/intellij/psi/PsiY") -> false
  path.startsWith("com/intellij/psi/Se") -> false
  path.startsWith("com/intellij/psi/Th") -> false
  path.startsWith("com/intellij/psi/codeStyle/V") -> false
  path.startsWith("com/intellij/psi/controlFlow/Ab") -> false
  path.startsWith("com/intellij/psi/controlFlow/Ca") -> false
  path.startsWith("com/intellij/psi/controlFlow/Comm") -> false
  path.startsWith("com/intellij/psi/controlFlow/E") -> false
  path.startsWith("com/intellij/psi/controlFlow/G") -> false
  path.startsWith("com/intellij/psi/controlFlow/Instruction.") -> false
  path.startsWith("com/intellij/psi/controlFlow/InstructionB") -> false
  path.startsWith("com/intellij/psi/controlFlow/InstructionC") -> false
  path.startsWith("com/intellij/psi/controlFlow/S") -> false
  path.startsWith("com/intellij/psi/controlFlow/T") -> false
  path.startsWith("com/intellij/psi/controlFlow/W") -> false
  path.startsWith("com/intellij/psi/filters/Cons") -> false
  path.startsWith("com/intellij/psi/impl/ElementP") -> false
  path.startsWith("com/intellij/psi/impl/L") -> false
  path.startsWith("com/intellij/psi/impl/PsiCo") -> false
  path.startsWith("com/intellij/psi/impl/PsiEx") -> false
  path.startsWith("com/intellij/psi/impl/PsiI") -> false
  path.startsWith("com/intellij/psi/impl/PsiJ") -> false
  path.startsWith("com/intellij/psi/impl/PsiV") -> false
  path.startsWith("com/intellij/psi/impl/cache/M") -> false
  path.startsWith("com/intellij/psi/impl/cache/R") -> false
  path.startsWith("com/intellij/psi/impl/compiled/A") -> false
  path.startsWith("com/intellij/psi/impl/compiled/ClassFileS") -> false
  path.startsWith("com/intellij/psi/impl/compiled/I") -> false
  path.startsWith("com/intellij/psi/impl/compiled/O") -> false
  path.startsWith("com/intellij/psi/impl/file/impl/J") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/C") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/FunctionalExpressionE") -> false
  path.startsWith("com/intellij/psi/impl/java/stubs/FunctionalExpressionS") -> false
  path.startsWith("com/intellij/psi/impl/light/A") -> false
  path.startsWith("com/intellij/psi/impl/light/LightEm") -> false
  path.startsWith("com/intellij/psi/impl/light/LightEx") -> false
  path.startsWith("com/intellij/psi/impl/light/LightI") -> false
  path.startsWith("com/intellij/psi/impl/light/LightK") -> false
  path.startsWith("com/intellij/psi/impl/light/LightMo") -> false
  path.startsWith("com/intellij/psi/impl/light/LightRecordF") -> false
  path.startsWith("com/intellij/psi/impl/light/LightRecordMem") -> false
  path.startsWith("com/intellij/psi/impl/light/LightVariableBu") -> false
  path.startsWith("com/intellij/psi/impl/se") -> false
  path.startsWith("com/intellij/psi/impl/source/Con") -> false
  path.startsWith("com/intellij/psi/impl/source/Hi") -> false
  path.startsWith("com/intellij/psi/impl/source/JavaF") -> false
  path.startsWith("com/intellij/psi/impl/source/JavaS") -> false
  path.startsWith("com/intellij/psi/impl/source/Pa") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiD") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiL") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiPr") -> false
  path.startsWith("com/intellij/psi/impl/source/PsiU") -> false
  path.startsWith("com/intellij/psi/impl/source/SourceJ") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/C") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/Pa") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/ResolveCl") -> false
  path.startsWith("com/intellij/psi/impl/source/resolve/ResolveV") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/Chi") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/E") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/JavaA") -> false
  path.startsWith("com/intellij/psi/impl/source/tree/injected/S") -> false
  path.startsWith("com/intellij/psi/infos/MethodCandidateInfo.") -> false
  path.startsWith("com/intellij/psi/presentation/java/F") -> false
  path.startsWith("com/intellij/psi/presentation/java/M") -> false
  path.startsWith("com/intellij/psi/scope/J") -> false
  path.startsWith("com/intellij/psi/scope/M") -> false
  path.startsWith("com/intellij/psi/scope/N") -> false
  path.startsWith("com/intellij/psi/scope/PsiC") -> false
  path.startsWith("com/intellij/psi/scope/processor/C") -> false
  path.startsWith("com/intellij/psi/scope/processor/FilterS") -> false
  path.startsWith("com/intellij/psi/scope/processor/MethodResolver") -> false
  path.startsWith("com/intellij/psi/scope/processor/Methods") -> false
  path.startsWith("com/intellij/psi/scope/u") -> false
  path.startsWith("com/intellij/psi/search/Pa") -> false
  path.startsWith("com/intellij/psi/search/searches/D") -> false
  path.startsWith("com/intellij/psi/stub/") -> false
  path.startsWith("com/intellij/psi/stubs/PsiC") -> false
  path.startsWith("com/intellij/psi/tree/IStubFileElementType\$") -> false
  path.startsWith("com/intellij/psi/util/JavaC") -> false
  path.startsWith("com/intellij/psi/util/PsiCl") -> false
  path.startsWith("com/intellij/psi/util/PsiExpressionTrimRenderer.") -> false
  path.startsWith("com/intellij/psi/util/PsiFormatUtil.") -> false
  path.startsWith("com/intellij/psi/util/PsiMe") -> false
  path.startsWith("com/intellij/psi/util/PsiS") -> false
  path.startsWith("com/intellij/psi/util/PsiUtil.") -> false
  path.startsWith("com/intellij/ui/IconManagerH") -> false
  path.startsWith("com/intellij/util/Bl") -> false
  path.startsWith("com/intellij/util/Consumer\$") -> false
  path.startsWith("com/intellij/util/Functions\$3") -> false
  path.startsWith("com/intellij/util/Functions\$4") -> false
  path.startsWith("com/intellij/util/Functions\$6") -> false
  path.startsWith("com/intellij/util/Functions\$7") -> false
  path.startsWith("com/intellij/util/Functions\$8") -> false
  path.startsWith("com/intellij/util/NoO") -> false
  path.startsWith("com/intellij/util/NullableFunction\$") -> false
  path.startsWith("com/intellij/util/Ur") -> false
  path.startsWith("com/intellij/util/containers/ConcurrentFactoryMap\$CollectionWrapper\$S") -> false
  path.startsWith("com/intellij/util/containers/ConcurrentRefHashMap\$2") -> false
  path.startsWith("com/intellij/util/containers/ContainerUtil\$8\$") -> false
  path.startsWith("com/intellij/util/io/DirectByteBufferP") -> false
  path.startsWith("com/intellij/util/io/M") -> false
  path.startsWith("com/intellij/util/keyFMap/MapBackedFMap\$") -> false
  path.startsWith("com/intellij/util/lang/ClasspathCache\$LoaderData.") -> false
  path.startsWith("com/intellij/util/lang/ClasspathCache\$N") -> false
  path.startsWith("com/intellij/util/lang/JdkZipResourceFile\$S") -> false
  path.startsWith("com/intellij/util/lang/Loader\$") -> false
  path.startsWith("com/intellij/util/lang/S") -> false
  path.startsWith("com/intellij/util/lo") -> false
  path.startsWith("com/intellij/util/messages/impl/CompositeMessageBus\$") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$MessageP") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$R") -> false
  path.startsWith("com/intellij/util/messages/impl/MessageBusImpl\$T") -> false
  path.startsWith("com/intellij/util/text/Ca") -> false
  path.startsWith("com/intellij/util/text/L") -> false
  path.startsWith("gnu/trove/TObjectId") -> false
  path.startsWith("gnu/trove/To") -> false
  path.startsWith("kotlin/reflect/full/KClasses\$allSupertypes\$1") -> false
  path.startsWith("kotlin/reflect/full/KClasses\$s") -> false
  path.startsWith("kotlin/reflect/jvm/internal/KClassC") -> false
  path.startsWith("kotlin/reflect/jvm/internal/calls/AnnotationConstructorCallerKt\$createAnnotationInstance\$r") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassDescriptor\$FunctionTypeConstructor\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassKind\$Companion\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/functions/FunctionClassScope\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/jvm/JvmBuiltInsCustomizer\$getJdkMethodStatus\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/builtins/jvm/JvmBuiltInsCustomizer\$isMutabilityViolation\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaAnnotationOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/descriptors/runtime/structure/ReflectJavaModifierListOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/AnnotationTypeQualifierResolverK") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/Dep") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/components/T") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/descriptors/V") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/JavaM") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/descriptors/LazyJavaScope\$res") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/descriptors/LazyJavaStaticClassScope\$flatMapJavaStaticSupertypesScopes\$1.") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/lazy/types/RawSubstitution\$W") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaAnnotation\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaMethod\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/JavaType\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/structure/ListBasedJavaAnnotationOwner\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/No") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancement\$P") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/SignatureEnhancementK") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/java/typeEnhancement/TypeA") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$P") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$S") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$W") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$loadC") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/TypeMappingConfiguration\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/load/kotlin/header/ReadKotlinClassHeaderAnnotationVisitor\$KotlinMetadataArgumentVisitor\$3") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/DescriptorEquivalenceForOverrides\$areCallableDescriptorsEquivalent\$overridingUtil\$1.") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/constants/ConstantValueFactory\$createArrayValue\$3") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/descriptorUtil/DescriptorUtilsKt\$declaresOrInheritsDefaultValue\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/resolve/descriptorUtil/DescriptorUtilsKt\$firstOverridden\$1") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/DeserializationConfiguration\$DefaultI") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/MemberDeserializer\$l") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/serialization/deserialization/descriptors/DeserializedClassDescriptor\$i") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/An") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Cu") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/NotNullT") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/TypeSystemCommonBackendContext\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/Unr") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/R") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemContext\$") -> false
  path.startsWith("kotlin/reflect/jvm/internal/impl/types/model/TypeSystemInferenceExtensionContext\$") -> false
  path.startsWith("kotlin/script/experimental/jsr223/KotlinJsr223DefaultScriptEngineFactoryK") -> false
  path.startsWith("org/fusesource/hawtjni/runtime/L") -> false
  path.startsWith("org/fusesource/hawtjni/runtime/P") -> false
  path.startsWith("org/fusesource/jansi/Ansi.") -> false
  path.startsWith("org/fusesource/jansi/AnsiO") -> false
  path.startsWith("org/fusesource/jansi/W") -> false
  path.startsWith("org/jdom/output/L") -> false
  path.startsWith("org/jetbrains/kotlin/KtFakeSourceElementKind\$SuperCallE") -> false
  path.startsWith("org/jetbrains/kotlin/backend/common/lower/inline/Inliner") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$c") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$e") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$j") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$r") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt\$t") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/JvmLowerKt.") -> false
  path.startsWith("org/jetbrains/kotlin/backend/jvm/lower/AnnotationLowering\$") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/CLICompilerK") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/S") -> false
  path.startsWith("org/jetbrains/kotlin/cli/common/messages/MessageCollectorB") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/KotlinToJVMBytecodeCompiler\$D") -> false
  path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/P") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/inline/MethodInlinerUtilKt\$analyzeMethodNodeWithInterpreter\$analyzer\$1\$") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/inline/SMAPParser\$") -> false
  path.startsWith("org/jetbrains/kotlin/codegen/optimization/transformer/CompositeMethodTransformer\$") -> false
  path.startsWith("org/jetbrains/kotlin/config/AnalysisFlags\$expe") -> false
  path.startsWith("org/jetbrains/kotlin/diagnostics/Du") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/FirE") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirImplementationMismatchChecker\$checkC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirModifierChecker\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/diagnostics/ConeDiagnosticToFirDiagnosticKt\$f") -> false
  path.startsWith("org/jetbrains/kotlin/fir/analysis/jvm/checkers/expression/FirS") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/Fir2IrDeclarationStorage\$I") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$N") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$W") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$array\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$bu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$1") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$4") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$5") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$8") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createClass\$9") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createCompanionObject\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createN") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$createS") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$i") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$ma") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$mu") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/IrBuiltInsOverFir\$s") -> false
  path.startsWith("org/jetbrains/kotlin/fir/backend/jvm/FirJvmBackendExtension\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$toFirValueParameter\$1") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitDestructuringDeclaration\$1") -> false
  path.startsWith("org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder\$Visitor\$visitDestructuringDeclaration\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/declarations/builder/FirErrorConstructorBuilder.") -> false
  path.startsWith("org/jetbrains/kotlin/fir/declarations/impl/FirErrorC") -> false
  path.startsWith("org/jetbrains/kotlin/fir/deserialization/LibraryPathFilter\$LibraryList\$") -> false
  path.startsWith("org/jetbrains/kotlin/fir/expressions/impl/FirNo") -> false
  path.startsWith("org/jetbrains/kotlin/fir/java/deserialization/JvmClassFileBasedSymbolProvider\$extractClassMetadata\$2") -> false
  path.startsWith("org/jetbrains/kotlin/fir/java/scopes/JavaClassUseSiteMemberScope\$s") -> false
  path.startsWith("org/jetbrains/kotlin/fir/renderer/ConeTypeRendererW") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/dfa/FirDataFlowAnalyzer\$b") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/diagnostics/ConeNoCon") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/inference/FirDelegatedPropertyInferenceSession\$con") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/inference/FirDelegatedPropertyInferenceSession\$h") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirTypeResolverImpl\$C") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/providers/impl/FirTypeResolverImpl\$P") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/FirDeclarationsResolveTransformer\$transformAnonymousO") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/body/resolve/ReturnTypeCalculatorWithJump\$F") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/AbstractFirSpecificAnnotationResolveTransformer\$A") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/AbstractFirSpecificAnnotationResolveTransformer\$C") -> false
  path.startsWith("org/jetbrains/kotlin/fir/resolve/transformers/plugin/CompilerRequiredAnnotationsH") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrE") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrP") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/Fir2IrT") -> false
  path.startsWith("org/jetbrains/kotlin/fir/symbols/impl/FirClassLikeSymbolK") -> false
  path.startsWith("org/jetbrains/kotlin/ir/util/FakeOverrideB") -> false
  path.startsWith("org/jetbrains/kotlin/name/Jv") -> false
  path.startsWith("org/jetbrains/kotlin/name/StandardClassIds\$Annotations\$J") -> false
  path.startsWith("org/jetbrains/kotlin/name/StandardClassIds\$J") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/A") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/B") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/C") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/DoubleS") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/E") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/IntS") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/LongS") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/O") -> false
  path.startsWith("org/jetbrains/kotlin/one/util/streamex/U") -> false
  path.startsWith("org/jetbrains/kotlin/resolve/multiplatform/ExpectActualCompatibility\$Incompatible\$U") -> false
  path.startsWith("org/jline/terminal/C") -> false
  path.startsWith("org/jline/terminal/S") -> false
  path.startsWith("org/jline/terminal/impl/AbstractP") -> false
  path.startsWith("org/jline/terminal/impl/C") -> false
  path.startsWith("org/jline/terminal/impl/N") -> false
  path.startsWith("org/objectweb/asm/B") -> false
  path.startsWith("org/objectweb/asm/E") -> false
  path.startsWith("org/objectweb/asm/L") -> false
  path.startsWith("org/objectweb/asm/O") -> false
  path.startsWith("org/objectweb/asm/tree/FieldI") -> false
  path.startsWith("org/objectweb/asm/tree/Fr") -> false
  path.startsWith("org/objectweb/asm/tree/J") -> false
  path.startsWith("org/objectweb/asm/tree/Mu") -> false
  path.startsWith("org/objectweb/asm/tree/P") -> false
  path.startsWith("org/objectweb/asm/tree/U") -> false
  path.startsWith("org/objectweb/asm/tree/V") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/AnalyzerE") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/F") -> false
  path.startsWith("org/objectweb/asm/tree/analysis/I") -> false

  else -> true
}

// Overrides:
fun isOverride(path: String) = when {

  else -> false
}