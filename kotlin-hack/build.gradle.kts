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
  implementation(kGroup, name = "kotlin-compiler-embeddable", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-compiler-embeddable", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-compiler-impl-embeddable", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-jsr223", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-jvm", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-jvm-host", version = kVersion)
  implementation(kGroup, name = "kotlin-scripting-common", version = kVersion)
}

val outputPackagePrefix = "aicoder"

val verbose = false

fun shouldRelocate(path: String) = when {
  !path.endsWith(".class") -> false

  // Ensure these conflicts are relocated
  path.startsWith("com/intellij/openapi/application/") -> true
  path.startsWith("kotlin/script/experimental/jvm/compiler") -> true

  // We want to maintain this interface:
  path.contains("/KotlinJsr223") -> false
  path.contains("/ScriptArgsWithTypes") -> false
  path.startsWith("kotlin/script/experimental/jsr223") -> false
  path.startsWith("kotlin/script/experimental/jvm") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/jsr223") -> false

  path.startsWith("kotlin/") -> false

  else -> true
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
      zipTree(file).visit {
        if (this.isDirectory) return@visit
        // Adjust the path so we can express rules based on the desired final paths
        val path = relocations()
        if (isConflicting(path)) {
          if (verbose) println("${this.path} excluded conflict from plugin: ${file.name} as $path")
          exclude(this.path)
        } else if (isPruned(path)) {
          if (verbose) println("${this.path} pruned from plugin:${file.name} as $path")
          exclude(this.path)
        } else {
          if (verbose) println("${this.path} included in plugin:${file.name} as $path")
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
        if (shouldRelocatePath(from)) {
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

// GENERATED CODE

// Conflicts: 6787
// Pruned: 7339
// Required Classes: 13637

// Conflicts:
fun isConflicting(path: String) = when {
  path.startsWith("kotlin/reflect/jvm/internal/impl/") -> {
    val path = path.substring(33)
    when {
      path.startsWith("load/java/typeEnhancement/SignatureEnhancement") -> {
        val path = path.substring(46)
        when {
          path.startsWith("\$S") -> false
          path.startsWith("\$P") -> false
          path.startsWith("K") -> false

          else -> true
        }
      }

      path.startsWith("load/java/") -> {
        val path = path.substring(10)
        when {
          path.startsWith("AnnotationTypeQualifierResolver\$") -> false
          path.startsWith("AnnotationTypeQualifierResolverK") -> false
          path.startsWith("Dep") -> false
          path.startsWith("components/T") -> false
          path.startsWith("descriptors/V") -> false
          path.startsWith("lazy/JavaM") -> false
          path.startsWith("lazy/descriptors/LazyJavaScope\$res") -> false
          path.startsWith("lazy/descriptors/LazyJavaStaticClassScope\$flatMapJavaStaticSupertypesScopes\$1.") -> false
          path.startsWith("lazy/types/RawSubstitution\$W") -> false
          path.startsWith("lazy/types/T") -> false
          path.startsWith("structure/JavaAnnotation\$") -> false
          path.startsWith("structure/JavaMethod\$") -> false
          path.startsWith("structure/JavaType\$") -> false
          path.startsWith("structure/ListBasedJavaAnnotationOwner\$") -> false
          path.startsWith("typeEnhancement/No") -> false
          path.startsWith("typeEnhancement/TypeA") -> false

          else -> true
        }
      }

      path.startsWith("load/kotlin/AbstractBinaryClassAnnotationAndConstantLoader\$") -> {
        val path = path.substring(59)
        when {
          path.startsWith("P") -> false
          path.startsWith("S") -> false
          path.startsWith("W") -> false
          path.startsWith("loadC") -> false

          else -> true
        }
      }

      path.startsWith("load/kotlin/BinaryClassAnnotationAndConstantLoaderImpl\$loadAnnotation\$1\$") -> false
      path.startsWith("serialization/deserialization/") -> {
        val path = path.substring(30)
        when {
          path.startsWith("DeserializationConfiguration\$DefaultI") -> false
          path.startsWith("DeserializedA") -> false
          path.startsWith("MemberDeserializer\$l") -> false
          path.startsWith("descriptors/DeserializedClassDescriptor\$i") -> false

          else -> true
        }
      }

      path.startsWith("types/") -> {
        val path = path.substring(6)
        when {
          path.startsWith("An") -> false
          path.startsWith("Cu") -> false
          path.startsWith("Er") -> false
          path.startsWith("NotNullT") -> false
          path.startsWith("TypeSystemCommonBackendContext\$") -> false
          path.startsWith("Unr") -> false
          path.startsWith("e") -> false
          path.startsWith("model/R") -> false
          path.startsWith("model/TypeSystemContext\$") -> false
          path.startsWith("model/TypeSystemInferenceExtensionContext\$") -> false

          else -> true
        }
      }

      path.startsWith("builtins/functions/FunctionClass") -> {
        val path = path.substring(32)
        when {
          path.startsWith("Descriptor\$FunctionTypeConstructor\$") -> false
          path.startsWith("Kind\$Companion\$") -> false
          path.startsWith("Scope\$") -> false

          else -> true
        }
      }

      path.startsWith("metadata/jvm/deserialization/JvmNameResolver\$") -> false
      path.startsWith("builtins/jvm/JvmBuiltInsCustomizer\$getJdkMethodStatus\$1") -> false
      path.startsWith("builtins/jvm/JvmBuiltInsCustomizer\$isMutabilityViolation\$1") -> false
      path.startsWith("descriptors/runtime/structure/ReflectJavaAnnotationOwner\$") -> false
      path.startsWith("descriptors/runtime/structure/ReflectJavaModifierListOwner\$") -> false
      path.startsWith("load/kotlin/TypeMappingConfiguration\$") -> false
      path.startsWith("load/kotlin/header/ReadKotlinClassHeaderAnnotationVisitor\$KotlinMetadataArgumentVisitor\$3") -> false
      path.startsWith("resolve/DescriptorEquivalenceForOverrides\$areCallableDescriptorsEquivalent\$overridingUtil\$1.") -> false
      path.startsWith("resolve/constants/ConstantValueFactory\$createArrayValue\$3") -> false
      path.startsWith("resolve/dep") -> false
      path.startsWith("resolve/descriptorUtil/DescriptorUtilsKt\$declaresOrInheritsDefaultValue\$1") -> false
      path.startsWith("resolve/descriptorUtil/DescriptorUtilsKt\$firstOverridden\$1") -> false

      else -> true
    }
  }

  path.startsWith("com/intellij/") -> {
    val path = path.substring(13)
    when {
      path.startsWith("psi/impl/") -> {
        val path = path.substring(9)
        when {
          path.startsWith("source/") -> {
            val path = path.substring(7)
            when {
              path.startsWith("resolve/reference/") -> {
                val path = path.substring(18)
                when {
                  path.startsWith("impl/m") -> false
                  path.startsWith("impl/providers/G") -> false

                  else -> true
                }
              }

              path.startsWith("tree/") -> {
                val path = path.substring(5)
                when {
                  path.startsWith("T") -> true
                  path.startsWith("R") -> true
                  path.startsWith("Cha") -> true
                  path.startsWith("L") -> true
                  path.startsWith("A") -> true
                  path.startsWith("Co") -> true
                  path.startsWith("P") -> true
                  path.startsWith("F") -> true
                  path.startsWith("IC") -> true
                  path.startsWith("O") -> true
                  path.startsWith("S") -> true
                  path.startsWith("injected/C") -> true

                  else -> false
                }
              }

              path.startsWith("resolve/ResolveCa") -> true
              path.startsWith("PsiFil") -> true
              path.startsWith("D") -> true
              path.startsWith("S") -> {
                val path = path.substring(1)
                when {
                  path.startsWith("ourceJ") -> false

                  else -> true
                }
              }

              path.startsWith("Ch") -> true
              path.startsWith("c") -> true
              path.startsWith("Cod") -> true
              path.startsWith("FileT") -> true
              path.startsWith("Ho") -> true
              path.startsWith("PsiPl") -> true
              path.startsWith("resolve/F") -> true

              else -> false
            }
          }

          path.startsWith("smartPointers/") -> {
            val path = path.substring(14)
            when {
              path.startsWith("SmartT") -> false
              path.startsWith("P") -> false
              path.startsWith("T") -> false

              else -> true
            }
          }

          path.startsWith("Psi") -> {
            val path = path.substring(3)
            when {
              path.startsWith("ElementF") -> false
              path.startsWith("Cl") -> false
              path.startsWith("S") -> false
              path.startsWith("N") -> false
              path.startsWith("Co") -> false
              path.startsWith("Di") -> false
              path.startsWith("Ex") -> false
              path.startsWith("I") -> false
              path.startsWith("J") -> false
              path.startsWith("V") -> false

              else -> true
            }
          }

          path.startsWith("file/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("PsiPackageI") -> false
              path.startsWith("impl/J") -> false

              else -> true
            }
          }

          path.startsWith("ElementB") -> true
          path.startsWith("D") -> true
          path.startsWith("cache/C") -> true
          path.startsWith("Ch") -> true
          path.startsWith("Re") -> {
            val path = path.substring(2)
            when {
              path.startsWith("n") -> true
              path.startsWith("p") -> true
              path.startsWith("s") -> true

              else -> false
            }
          }

          path.startsWith("B") -> true
          path.startsWith("A") -> true
          path.startsWith("F") -> true
          path.startsWith("G") -> true
          path.startsWith("Po") -> true
          path.startsWith("Sh") -> true
          path.startsWith("Sy") -> true
          path.startsWith("light/LightEl") -> true
          path.startsWith("m") -> true

          else -> false
        }
      }

      path.startsWith("psi/") -> {
        val path = path.substring(4)
        when {
          path.startsWith("Psi") -> {
            val path = path.substring(3)
            when {
              path.startsWith("Reference") -> {
                val path = path.substring(9)
                when {
                  path.startsWith("L") -> false
                  path.startsWith("E") -> false
                  path.startsWith("Pa") -> false

                  else -> true
                }
              }

              path.startsWith("Element") -> {
                val path = path.substring(7)
                when {
                  path.startsWith("F") -> false

                  else -> true
                }
              }

              path.startsWith("Anc") -> true
              path.startsWith("BinaryF") -> true
              path.startsWith("Ch") -> true
              path.startsWith("CodeF") -> true
              path.startsWith("Com") -> true
              path.startsWith("Dir") -> true
              path.startsWith("DocCommentB") -> true
              path.startsWith("Docu") -> true
              path.startsWith("Er") -> true
              path.startsWith("Fil") -> true
              path.startsWith("Inv") -> true
              path.startsWith("Lan") -> true
              path.startsWith("Lar") -> true
              path.startsWith("Lis") -> true
              path.startsWith("LiteralV") -> true
              path.startsWith("Lock") -> true
              path.startsWith("Ma") -> true
              path.startsWith("Mir") -> true
              path.startsWith("NameI") -> true
              path.startsWith("Named") -> true
              path.startsWith("Pars") -> true
              path.startsWith("Pl") -> true
              path.startsWith("PolyV") -> true
              path.startsWith("QualifiedN") -> true
              path.startsWith("QualifiedR") -> true
              path.startsWith("Recu") -> true
              path.startsWith("Ta") -> true
              path.startsWith("Tre") -> true
              path.startsWith("Wa") -> true
              path.startsWith("Whit") -> true

              else -> false
            }
          }

          path.startsWith("util/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("Psi") -> {
                val path = path.substring(3)
                when {
                  path.startsWith("UtilC") -> true
                  path.startsWith("Tr") -> true
                  path.startsWith("Mo") -> true
                  path.startsWith("A") -> true
                  path.startsWith("Ca") -> true
                  path.startsWith("El") -> true
                  path.startsWith("FormatUtilB") -> true

                  else -> false
                }
              }

              path.startsWith("Ca") -> true
              path.startsWith("Pa") -> true
              path.startsWith("Q") -> true

              else -> false
            }
          }

          path.startsWith("con") -> false
          path.startsWith("scope/") -> {
            val path = path.substring(6)
            when {
              path.startsWith("PsiS") -> true
              path.startsWith("B") -> true
              path.startsWith("D") -> true
              path.startsWith("Pr") -> true
              path.startsWith("processor/FilterE") -> true

              else -> false
            }
          }

          path.startsWith("codeStyle/") -> {
            val path = path.substring(10)
            when {
              path.startsWith("J") -> false
              path.startsWith("R") -> false
              path.startsWith("NameUtil\$Matcher.") -> false
              path.startsWith("V") -> false

              else -> true
            }
          }

          path.startsWith("B") -> false
          path.startsWith("Cl") -> false
          path.startsWith("CommonR") -> false
          path.startsWith("Cons") -> false
          path.startsWith("EmptyS") -> false
          path.startsWith("G") -> false
          path.startsWith("Hie") -> false
          path.startsWith("Im") -> false
          path.startsWith("J") -> false
          path.startsWith("Lam") -> false
          path.startsWith("LanguageA") -> false
          path.startsWith("No") -> false
          path.startsWith("Se") -> false
          path.startsWith("SmartT") -> false
          path.startsWith("Th") -> false
          path.startsWith("Ty") -> false
          path.startsWith("a") -> false
          path.startsWith("com") -> false
          path.startsWith("filters/Cons") -> false
          path.startsWith("filters/c") -> false
          path.startsWith("filters/e") -> false
          path.startsWith("in") -> false
          path.startsWith("j") -> false
          path.startsWith("p") -> false
          path.startsWith("search/Pa") -> false
          path.startsWith("search/searches/D") -> false
          path.startsWith("search/searches/S") -> false
          path.startsWith("stub/") -> false
          path.startsWith("stubs/PsiC") -> false
          path.startsWith("tree/IStubFileElementType\$") -> false
          path.startsWith("tree/j") -> false

          else -> true
        }
      }

      path.startsWith("codeInsight/") -> {
        val path = path.substring(12)
        when {
          path.startsWith("CodeInsightUtilCore.") -> true
          path.startsWith("Con") -> true
          path.startsWith("F") -> true
          path.startsWith("folding/C") -> true

          else -> false
        }
      }

      path.startsWith("lang/j") -> false
      path.startsWith("util/") -> {
        val path = path.substring(5)
        when {
          path.startsWith("io/") -> {
            val path = path.substring(3)
            when {
              path.startsWith("WriteAheadLogKt\$t") -> false
              path.startsWith("Persistent") -> {
                val path = path.substring(10)
                when {
                  path.startsWith("MapWal\$") -> {
                    val path = path.substring(7)
                    when {
                      path.startsWith("c") -> false
                      path.startsWith("f") -> false
                      path.startsWith("s") -> false

                      else -> true
                    }
                  }

                  path.startsWith("EnumeratorBase\$C") -> false
                  path.startsWith("HashMapValueStorage\$O") -> false

                  else -> true
                }
              }

              path.startsWith("IOCancellationCallbackHolder\$") -> false
              path.startsWith("IntToIntBtree\$") -> {
                val path = path.substring(14)
                when {
                  path.startsWith("BtreeD") -> false
                  path.startsWith("BtreeP") -> false
                  path.startsWith("K") -> false

                  else -> true
                }
              }

              path.startsWith("Page") -> {
                val path = path.substring(4)
                when {
                  path.startsWith("d") -> true

                  else -> false
                }
              }

              path.startsWith("UnI") -> false
              path.startsWith("DirectByteBufferP") -> false
              path.startsWith("FileChannelU") -> false
              path.startsWith("Fin") -> false
              path.startsWith("M") -> false
              path.startsWith("OpenChannelsCache\$ChannelP") -> false
              path.startsWith("Po") -> false
              path.startsWith("Ra") -> false

              else -> true
            }
          }

          path.startsWith("messages/impl/MessageBusImpl\$") -> {
            val path = path.substring(29)
            when {
              path.startsWith("MessageH") -> true

              else -> false
            }
          }

          path.startsWith("lang/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("ClasspathCache\$LoaderData.") -> false
              path.startsWith("ClasspathCache\$N") -> false
              path.startsWith("JdkZipResourceFile\$S") -> false
              path.startsWith("Loader\$") -> false
              path.startsWith("S") -> false
              path.startsWith("f") -> false

              else -> true
            }
          }

          path.startsWith("containers/") -> {
            val path = path.substring(11)
            when {
              path.startsWith("ConcurrentFactoryMap\$CollectionWrapper\$S") -> false
              path.startsWith("ConcurrentRefHashMap\$2") -> false
              path.startsWith("ContainerUtil\$8\$") -> false
              path.startsWith("SLRUMap\$") -> false

              else -> true
            }
          }

          path.startsWith("Functions\$") -> {
            val path = path.substring(10)
            when {
              path.startsWith("1") -> true
              path.startsWith("2") -> true

              else -> false
            }
          }

          path.startsWith("text/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("K") -> false
              path.startsWith("Ca") -> false
              path.startsWith("L") -> false

              else -> true
            }
          }

          path.startsWith("X") -> false
          path.startsWith("Bl") -> false
          path.startsWith("Consumer\$") -> false
          path.startsWith("NoO") -> false
          path.startsWith("NullableFunction\$") -> false
          path.startsWith("Ur") -> false
          path.startsWith("V") -> false
          path.startsWith("graph/DFSTBuilder\$D") -> false
          path.startsWith("keyFMap/MapBackedFMap\$") -> false
          path.startsWith("lo") -> false
          path.startsWith("messages/impl/CompositeMessageBus\$") -> false

          else -> true
        }
      }

      path.startsWith("ide/plugins/IdeaPluginDescriptorImpl\$") -> false
      path.startsWith("openapi/") -> {
        val path = path.substring(8)
        when {
          path.startsWith("util/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("io/FileUtilRt\$N") -> false
              path.startsWith("BuildNumber\$") -> false
              path.startsWith("Pair\$3") -> false
              path.startsWith("Stax") -> false
              path.startsWith("VolatileNullableLazyValue\$") -> false
              path.startsWith("io/FileSystemUtil\$I") -> false
              path.startsWith("io/w") -> false

              else -> true
            }
          }

          path.startsWith("application/") -> {
            val path = path.substring(12)
            when {
              path.startsWith("ApplicationM") -> false
              path.startsWith("ex/ApplicationM") -> false
              path.startsWith("ex/P") -> false
              path.startsWith("impl/FlushQueue\$F") -> false

              else -> true
            }
          }

          path.startsWith("roots/") -> {
            val path = path.substring(6)
            when {
              path.startsWith("impl/Pa") -> false
              path.startsWith("L") -> false
              path.startsWith("Pa") -> false

              else -> true
            }
          }

          path.startsWith("vfs/StandardFileSystems\$") -> false
          path.startsWith("progress/J") -> false
          path.startsWith("components/ServiceDescriptor\$C") -> false
          path.startsWith("editor/colors/TextAttributesKey\$1") -> false
          path.startsWith("editor/impl/IntervalTreeImpl\$IntervalNode\$") -> false
          path.startsWith("extensions/impl/ExtensionPointImpl\$4") -> false
          path.startsWith("extensions/impl/XmlExtensionAdapter\$") -> false
          path.startsWith("projectR") -> false
          path.startsWith("vfs/Str") -> false
          path.startsWith("vfs/impl/ZipEntryMap\$EntrySet\$") -> false

          else -> true
        }
      }

      path.startsWith("core/Core") -> {
        val path = path.substring(9)
        when {
          path.startsWith("J") -> false
          path.startsWith("ApplicationEnvironment\$2") -> false
          path.startsWith("L") -> false
          path.startsWith("Pa") -> false
          path.startsWith("PsiP") -> false

          else -> true
        }
      }

      path.startsWith("codeWithMe/ClientId\$Companion\$") -> false
      path.startsWith("codeWithMe/ClientIdS") -> false
      path.startsWith("core/J") -> false
      path.startsWith("diagnostic/EventWatcher\$") -> false
      path.startsWith("ide/highlighter/J") -> false
      path.startsWith("ide/plugins/DescriptorListLoadingContext\$") -> false
      path.startsWith("ide/plugins/PathResolver\$") -> false
      path.startsWith("ide/plugins/PluginDescriptorLoader\$") -> false
      path.startsWith("ide/plugins/PluginLoadingResult\$") -> false
      path.startsWith("ide/plugins/PluginManagerCore\$") -> false
      path.startsWith("ide/plugins/PluginSet\$") -> false
      path.startsWith("lexer/J") -> false
      path.startsWith("model/B") -> false
      path.startsWith("model/ModelBranchU") -> false
      path.startsWith("patterns/PsiJ") -> false
      path.startsWith("pl") -> false
      path.startsWith("pom/j") -> false
      path.startsWith("ui/DummyIconManager\$") -> false
      path.startsWith("ui/IconManagerH") -> false

      else -> true
    }
  }

  path.startsWith("com/google/common/collect/") -> {
    val path = path.substring(26)
    when {
      path.startsWith("Multimaps\$") -> {
        val path = path.substring(10)
        when {
          path.startsWith("AsMap\$EntrySet\$") -> false
          path.startsWith("TransformedEntriesMultimap\$") -> false
          path.startsWith("UnmodifiableMultimap\$") -> false

          else -> true
        }
      }

      path.startsWith("FilteredEntryMultimap\$Keys\$1\$") -> false
      path.startsWith("ImmutableSortedMap\$1.") -> false
      path.startsWith("Iterables\$10") -> false
      path.startsWith("Iterators\$MergingIterator\$") -> false
      path.startsWith("Maps\$Ac") -> false
      path.startsWith("Sets\$A") -> false

      else -> true
    }
  }

  path.startsWith("kotlin/") -> {
    val path = path.substring(7)
    when {
      path.startsWith("reflect/jvm/internal/") -> {
        val path = path.substring(21)
        when {
          path.startsWith("i") -> false
          path.startsWith("KClassC") -> false
          path.startsWith("calls/AnnotationConstructorCallerKt\$createAnnotationInstance\$r") -> false
          path.startsWith("calls/Inl") -> false
          path.startsWith("p") -> false

          else -> true
        }
      }

      path.startsWith("sc") -> false
      path.startsWith("reflect/KMutableProperty") -> {
        val path = path.substring(24)
        when {
          path.startsWith("\$D") -> false
          path.startsWith("0\$D") -> false
          path.startsWith("1\$D") -> false
          path.startsWith("2\$D") -> false

          else -> true
        }
      }

      path.startsWith("reflect/KProperty") -> {
        val path = path.substring(17)
        when {
          path.startsWith("0\$D") -> false
          path.startsWith("1\$D") -> false
          path.startsWith("2\$D") -> false

          else -> true
        }
      }

      path.startsWith("enums/EnumEntriesJ") -> false
      path.startsWith("experimental/ExperimentalN") -> false
      path.startsWith("js/ExperimentalJsF") -> false
      path.startsWith("js/ExperimentalJsR") -> false
      path.startsWith("jvm/I") -> false
      path.startsWith("reflect/full/KClasses\$allSupertypes\$1") -> false
      path.startsWith("reflect/full/KClasses\$s") -> false

      else -> true
    }
  }

  path.startsWith("com/google/common/") -> {
    val path = path.substring(18)
    when {
      path.startsWith("c") -> false

      else -> true
    }
  }

  path.startsWith("gnu/trove/T") -> {
    val path = path.substring(11)
    when {
      path.startsWith("Object") -> {
        val path = path.substring(6)
        when {
          path.startsWith("Fl") -> false
          path.startsWith("B") -> false
          path.startsWith("D") -> false
          path.startsWith("Id") -> false

          else -> true
        }
      }

      path.startsWith("Int") -> {
        val path = path.substring(3)
        when {
          path.startsWith("Fl") -> false
          path.startsWith("B") -> false
          path.startsWith("D") -> false
          path.startsWith("LongI") -> false

          else -> true
        }
      }

      path.startsWith("Long") -> {
        val path = path.substring(4)
        when {
          path.startsWith("A") -> true
          path.startsWith("Fu") -> true
          path.startsWith("H") -> true
          path.startsWith("IntH") -> true
          path.startsWith("IntP") -> true
          path.startsWith("ObjectH") -> true
          path.startsWith("ObjectP") -> true
          path.startsWith("P") -> true

          else -> false
        }
      }

      path.startsWith("Byte") -> {
        val path = path.substring(4)
        when {
          path.startsWith("A") -> true
          path.startsWith("Fu") -> true
          path.startsWith("Hashi") -> true
          path.startsWith("P") -> true

          else -> false
        }
      }

      path.startsWith("H") -> true
      path.startsWith("P") -> true
      path.startsWith("It") -> true

      else -> false
    }
  }

  path.startsWith("i") -> true
  path.startsWith("org/") -> {
    val path = path.substring(4)
    when {
      path.startsWith("jdom/") -> {
        val path = path.substring(5)
        when {
          path.startsWith("output/") -> {
            val path = path.substring(7)
            when {
              path.startsWith("Format") -> {
                val path = path.substring(6)
                when {
                  path.startsWith("\$E") -> false

                  else -> true
                }
              }

              path.startsWith("E") -> true
              path.startsWith("N") -> true
              path.startsWith("XMLOutputter\$") -> true
              path.startsWith("XMLOutputter.") -> true

              else -> false
            }
          }

          path.startsWith("i") -> false

          else -> true
        }
      }

      path.startsWith("jetbrains/") -> {
        val path = path.substring(10)
        when {
          path.startsWith("a") -> true
          path.startsWith("c") -> true
          path.startsWith("j") -> true

          else -> false
        }
      }

      path.startsWith("picocontainer/") -> {
        val path = path.substring(14)
        when {
          path.startsWith("C") -> true
          path.startsWith("M") -> true
          path.startsWith("PicoC") -> true

          else -> false
        }
      }

      path.startsWith("i") -> true
      path.startsWith("c") -> true
      path.startsWith("a") -> true

      else -> false
    }
  }

  path.startsWith("com/f") -> true
  path.startsWith("gnu/trove/") -> {
    val path = path.substring(10)
    when {
      path.startsWith("C") -> true
      path.startsWith("E") -> true
      path.startsWith("H") -> true
      path.startsWith("I") -> true
      path.startsWith("P") -> true
      path.startsWith("S") -> true

      else -> false
    }
  }

  else -> false
}

// Pruned:
fun isPruned(path: String) = when {
  path.startsWith("kotlin/reflect/jvm/internal/impl/") -> {
    val path = path.substring(33)
    when {
      path.startsWith("load/java/lazy/types/TypeParameterUpperBoundEraser") -> {
        val path = path.substring(50)
        when {
          path.startsWith("\$D") -> true

          else -> false
        }
      }

      path.startsWith("types/ErrorUtils") -> {
        val path = path.substring(16)
        when {
          path.startsWith("\$T") -> true

          else -> false
        }
      }

      path.startsWith("metadata/jvm/deserialization/JvmNameResolver\$") -> false
      path.startsWith("descriptors/runtime/structure/ReflectJavaModifierListOwner\$") -> false
      path.startsWith("load/java/AnnotationTypeQualifierResolver\$r") -> false
      path.startsWith("load/java/components/T") -> false
      path.startsWith("load/java/lazy/JavaM") -> false
      path.startsWith("serialization/deserialization/DeserializationConfiguration\$DefaultI") -> false
      path.startsWith("serialization/deserialization/descriptors/DeserializedClassDescriptor\$i") -> false
      path.startsWith("types/Cu") -> false
      path.startsWith("types/ErrorT") -> false

      else -> true
    }
  }

  path.startsWith("com/intellij/") -> {
    val path = path.substring(13)
    when {
      path.startsWith("psi/") -> {
        val path = path.substring(4)
        when {
          path.startsWith("impl/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("source/") -> {
                val path = path.substring(7)
                when {
                  path.startsWith("resolve/") -> {
                    val path = path.substring(8)
                    when {
                      path.startsWith("JavaResolveCache\$1") -> false
                      path.startsWith("JavaResolveCache.") -> false
                      path.startsWith("PsiR") -> false

                      else -> true
                    }
                  }

                  path.startsWith("tree/java/Psi") -> {
                    val path = path.substring(13)
                    when {
                      path.startsWith("Id") -> false
                      path.startsWith("J") -> false
                      path.startsWith("K") -> false

                      else -> true
                    }
                  }

                  path.startsWith("JavaDummy") -> {
                    val path = path.substring(9)
                    when {
                      path.startsWith("Holder\$") -> true

                      else -> false
                    }
                  }

                  path.startsWith("JavaF") -> false
                  path.startsWith("JavaLightStubBuilder.") -> false
                  path.startsWith("PsiClassReferenceType.") -> false
                  path.startsWith("PsiImmediateClassType.") -> false
                  path.startsWith("javadoc/PsiDocTagV") -> false
                  path.startsWith("javadoc/PsiDocTo") -> false
                  path.startsWith("tree/JavaA") -> false

                  else -> true
                }
              }

              path.startsWith("light/LightJavaModule") -> {
                val path = path.substring(21)
                when {
                  path.startsWith("\$L") -> true

                  else -> false
                }
              }

              path.startsWith("file/PsiPackageImpl") -> {
                val path = path.substring(19)
                when {
                  path.startsWith("\$1") -> true

                  else -> false
                }
              }

              path.startsWith("PsiElementF") -> false
              path.startsWith("compiled/Cl") -> {
                val path = path.substring(11)
                when {
                  path.startsWith("assFileS") -> false
                  path.startsWith("sCustomNavigationPolicy.") -> false
                  path.startsWith("sDecompilerImpl\$M") -> false
                  path.startsWith("sDecompilerImpl.") -> false

                  else -> true
                }
              }

              path.startsWith("ConstantExpressionE") -> false
              path.startsWith("Is") -> false
              path.startsWith("JavaClassSupersImpl.") -> false
              path.startsWith("JavaP") -> false
              path.startsWith("Jv") -> false
              path.startsWith("L") -> false
              path.startsWith("PsiCo") -> false
              path.startsWith("PsiEx") -> false
              path.startsWith("PsiJ") -> false
              path.startsWith("PsiSubstitutorF") -> false
              path.startsWith("file/impl/J") -> false
              path.startsWith("java/stubs/PsiJavaF") -> false
              path.startsWith("light/LightMo") -> false
              path.startsWith("se") -> false

              else -> true
            }
          }

          path.startsWith("Psi") -> {
            val path = path.substring(3)
            when {
              path.startsWith("Java") -> {
                val path = path.substring(4)
                when {
                  path.startsWith("CodeReferenceC") -> true
                  path.startsWith("F") -> true
                  path.startsWith("ModuleReference.") -> true

                  else -> false
                }
              }

              path.startsWith("Annotation") -> {
                val path = path.substring(10)
                when {
                  path.startsWith(".") -> false
                  path.startsWith("Mem") -> false
                  path.startsWith("O") -> false

                  else -> true
                }
              }

              path.startsWith("Class") -> {
                val path = path.substring(5)
                when {
                  path.startsWith(".") -> false
                  path.startsWith("Owner.") -> false
                  path.startsWith("Type\$S") -> false
                  path.startsWith("Type.") -> false

                  else -> true
                }
              }

              path.startsWith("CaseLabelElement.") -> false
              path.startsWith("ConstantEvaluationHelper.") -> false
              path.startsWith("DocCommentO") -> false
              path.startsWith("ElementFactory.") -> false
              path.startsWith("ElementFi") -> false
              path.startsWith("Expression.") -> false
              path.startsWith("Fie") -> false
              path.startsWith("Id") -> false
              path.startsWith("ImportH") -> false
              path.startsWith("ImportStatementB") -> false
              path.startsWith("Inf") -> false
              path.startsWith("JvmM") -> false
              path.startsWith("K") -> false
              path.startsWith("Loca") -> false
              path.startsWith("Mem") -> false
              path.startsWith("Method.") -> false
              path.startsWith("Modifia") -> false
              path.startsWith("ModifierL") -> false
              path.startsWith("Package.") -> false
              path.startsWith("Parameter.") -> false
              path.startsWith("ParameterListO") -> false
              path.startsWith("Primi") -> false
              path.startsWith("ReferenceE") -> false
              path.startsWith("ResolveHelper.") -> false
              path.startsWith("Sub") -> false
              path.startsWith("Type\$") -> false
              path.startsWith("Type.") -> false
              path.startsWith("TypeE") -> false
              path.startsWith("TypeParameterListO") -> false
              path.startsWith("V") -> false

              else -> true
            }
          }

          path.startsWith("presentation/java/") -> {
            val path = path.substring(18)
            when {
              path.startsWith("ClassPresentationProvider.") -> false
              path.startsWith("F") -> false
              path.startsWith("M") -> false
              path.startsWith("PackagePresentationProvider.") -> false
              path.startsWith("VariablePresentationProvider.") -> false

              else -> true
            }
          }

          path.startsWith("compiled/ClassFileDecompilers") -> {
            val path = path.substring(29)
            when {
              path.startsWith("\$L") -> true

              else -> false
            }
          }

          path.startsWith("scope/processor/") -> {
            val path = path.substring(16)
            when {
              path.startsWith("C") -> false
              path.startsWith("FilterS") -> false
              path.startsWith("MethodCandidatesProcessor.") -> false
              path.startsWith("Methods") -> false

              else -> true
            }
          }

          path.startsWith("controlFlow/ControlFlow") -> {
            val path = path.substring(23)
            when {
              path.startsWith(".") -> false
              path.startsWith("Factory\$1") -> false
              path.startsWith("Factory.") -> false

              else -> true
            }
          }

          path.startsWith("ClassFileViewProviderF") -> false
          path.startsWith("JVMElementFactory.") -> false
          path.startsWith("JavaCodeFragment.") -> false
          path.startsWith("JavaDi") -> false
          path.startsWith("JavaE") -> false
          path.startsWith("JavaM") -> false
          path.startsWith("JavaP") -> false
          path.startsWith("JavaRecursiveElementWalkingVisitor.") -> false
          path.startsWith("JvmP") -> false
          path.startsWith("NonClasspathClassFinder.") -> false
          path.startsWith("Ty") -> false
          path.startsWith("augment/PsiA") -> false
          path.startsWith("codeStyle/JavaC") -> false
          path.startsWith("codeStyle/JavaFileCodeStyleFacadeF") -> false
          path.startsWith("compiled/Cls") -> false
          path.startsWith("javadoc/PsiDocTagV") -> false
          path.startsWith("javadoc/PsiDocTo") -> false
          path.startsWith("scope/ElementClassHint.") -> false
          path.startsWith("scope/M") -> false
          path.startsWith("scope/N") -> false
          path.startsWith("search/searches/SuperMethodsSearch\$1") -> false
          path.startsWith("search/searches/SuperMethodsSearch.") -> false
          path.startsWith("stubs/PsiC") -> false
          path.startsWith("tree/IStubFileElementType\$") -> false
          path.startsWith("util/JavaC") -> false
          path.startsWith("util/MethodSignature.") -> false

          else -> true
        }
      }

      path.startsWith("lang/jvm/") -> {
        val path = path.substring(9)
        when {
          path.startsWith("types/Jvm") -> {
            val path = path.substring(9)
            when {
              path.startsWith("A") -> true
              path.startsWith("S") -> true
              path.startsWith("TypeR") -> true
              path.startsWith("W") -> true

              else -> false
            }
          }

          path.startsWith("Jvm") -> {
            val path = path.substring(3)
            when {
              path.startsWith("ClassK") -> true
              path.startsWith("ElementV") -> true
              path.startsWith("En") -> true
              path.startsWith("Modifier.") -> true

              else -> false
            }
          }

          path.startsWith("a") -> true
          path.startsWith("D") -> true

          else -> false
        }
      }

      path.startsWith("core/Core") -> {
        val path = path.substring(9)
        when {
          path.startsWith("J") -> false
          path.startsWith("ApplicationEnvironment\$2") -> false
          path.startsWith("L") -> false
          path.startsWith("Pa") -> false
          path.startsWith("PsiP") -> false

          else -> true
        }
      }

      path.startsWith("codeInsight/") -> {
        val path = path.substring(12)
        when {
          path.startsWith("ExternalAnnotationsL") -> false
          path.startsWith("ExternalAnnotationsManager.") -> false
          path.startsWith("I") -> false
          path.startsWith("J") -> false
          path.startsWith("folding/J") -> false
          path.startsWith("folding/i") -> false
          path.startsWith("r") -> false

          else -> true
        }
      }

      path.startsWith("util/messages/impl/MessageBusImpl\$") -> {
        val path = path.substring(34)
        when {
          path.startsWith("MessageH") -> true

          else -> false
        }
      }

      path.startsWith("openapi/") -> {
        val path = path.substring(8)
        when {
          path.startsWith("application/ApplicationM") -> false
          path.startsWith("extensions/impl/ExtensionPointImpl\$4") -> false
          path.startsWith("extensions/impl/XmlExtensionAdapter\$") -> false
          path.startsWith("progress/JobC") -> false
          path.startsWith("projectRoots/JavaV") -> false
          path.startsWith("roots/L") -> false
          path.startsWith("roots/Pa") -> false
          path.startsWith("util/Stax") -> false

          else -> true
        }
      }

      path.startsWith("lang/java/") -> {
        val path = path.substring(10)
        when {
          path.startsWith("Ja") -> false
          path.startsWith("lexer/JavaDocLexer.") -> false
          path.startsWith("lexer/JavaL") -> false
          path.startsWith("parser/JavaParserUtil\$Pa") -> false

          else -> true
        }
      }

      path.startsWith("core/J") -> false
      path.startsWith("ide/highlighter/Ja") -> false
      path.startsWith("ide/plugins/DescriptorListLoadingContext\$") -> false
      path.startsWith("ide/plugins/IdeaPluginDescriptorImpl\$C") -> false
      path.startsWith("ide/plugins/PluginDescriptorLoader\$") -> false
      path.startsWith("model/B") -> false
      path.startsWith("platform/util/plugins/DataLoader.") -> false
      path.startsWith("pom/j") -> false
      path.startsWith("util/containers/ConcurrentRefHashMap\$2") -> false
      path.startsWith("util/containers/SLRUMap\$") -> false
      path.startsWith("util/messages/impl/CompositeMessageBus\$") -> false
      path.startsWith("util/text/Ca") -> false
      path.startsWith("util/text/L") -> false

      else -> true
    }
  }

  path.startsWith("org/jetbrains/kotlin/") -> {
    val path = path.substring(21)
    when {
      path.startsWith("incremental/") -> {
        val path = path.substring(12)
        when {
          path.startsWith("IncrementalCompilerRunner\$") -> {
            val path = path.substring(26)
            when {
              path.startsWith("tryCompileIncrementally\$2\$compile\$abiSnapshotData\$1") -> false

              else -> true
            }
          }

          path.startsWith("cl") -> true
          path.startsWith("Incremental") -> {
            val path = path.substring(11)
            when {
              path.startsWith("JvmCompilerRunner\$") -> {
                val path = path.substring(18)
                when {
                  path.startsWith("calculateSourcesToCompileImpl\$1") -> false
                  path.startsWith("calculateSourcesToCompileImpl\$changedAndImpactedSymbols\$1") -> false
                  path.startsWith("co") -> false
                  path.startsWith("j") -> false
                  path.startsWith("ps") -> false

                  else -> true
                }
              }

              path.startsWith("JsCompilerRunner") -> {
                val path = path.substring(16)
                when {
                  path.startsWith("\$I") -> false
                  path.startsWith(".") -> false
                  path.startsWith("Kt.") -> false

                  else -> true
                }
              }

              path.startsWith("Caches") -> true
              path.startsWith("FirC") -> true
              path.startsWith("FirJvmCompilerRunner\$") -> true
              path.startsWith("JsCaches") -> true
              path.startsWith("JvmCaches") -> true

              else -> false
            }
          }

          path.startsWith("Change") -> {
            val path = path.substring(6)
            when {
              path.startsWith("I") -> false
              path.startsWith("sC") -> false

              else -> true
            }
          }

          path.startsWith("storage/") -> {
            val path = path.substring(8)
            when {
              path.startsWith("AppendableBasicM") -> true
              path.startsWith("AppendableD") -> true
              path.startsWith("Ca") -> true
              path.startsWith("Def") -> true
              path.startsWith("G") -> true
              path.startsWith("InM") -> true
              path.startsWith("No") -> true
              path.startsWith("SourceToO") -> true

              else -> false
            }
          }

          path.startsWith("CompilerRunnerUtils\$") -> true
          path.startsWith("Abi") -> true
          path.startsWith("BuildD") -> true
          path.startsWith("BuildI") -> true
          path.startsWith("CompileS") -> true
          path.startsWith("DirtyF") -> true
          path.startsWith("Inp") -> true
          path.startsWith("m") -> true
          path.startsWith("sn") -> true
          path.startsWith("u") -> true

          else -> false
        }
      }

      path.startsWith("fir/backend/IrBuiltInsOverFir\$") -> {
        val path = path.substring(30)
        when {
          path.startsWith("createF") -> false
          path.startsWith("arrayO") -> false
          path.startsWith("uns") -> false
          path.startsWith("me") -> false
          path.startsWith("e") -> false
          path.startsWith("anyN") -> false
          path.startsWith("booleanN") -> false
          path.startsWith("f") -> false
          path.startsWith("no") -> false
          path.startsWith("p") -> false
          path.startsWith("t") -> false

          else -> true
        }
      }

      path.startsWith("cli/common/") -> {
        val path = path.substring(11)
        when {
          path.startsWith("messages/") -> {
            val path = path.substring(9)
            when {
              path.startsWith("MessageR") -> true
              path.startsWith("AnalyzerWithCompilerReport\$a") -> true
              path.startsWith("Gra") -> true
              path.startsWith("P") -> true
              path.startsWith("X") -> true
              path.startsWith("De") -> true
              path.startsWith("IrMessageCollector\$Companion\$") -> true

              else -> false
            }
          }

          path.startsWith("UtilsKt\$checkKotlinPackageUsageForL") -> true
          path.startsWith("ArgumentsKt\$") -> true
          path.startsWith("Cr") -> true
          path.startsWith("F") -> true
          path.startsWith("f") -> true
          path.startsWith("p") -> true
          path.startsWith("CLICompiler\$C") -> true
          path.startsWith("CLITool\$d") -> true
          path.startsWith("GroupedKtSources.") -> true
          path.startsWith("S") -> true
          path.startsWith("UtilsKt\$t") -> true
          path.startsWith("o") -> true

          else -> false
        }
      }

      path.startsWith("fir/backend/Fir2IrDeclarationStorage\$") -> {
        val path = path.substring(37)
        when {
          path.startsWith("C") -> false
          path.startsWith("E") -> false
          path.startsWith("F") -> false
          path.startsWith("N") -> false
          path.startsWith("getC") -> false
          path.startsWith("getIrCa") -> false
          path.startsWith("l") -> false

          else -> true
        }
      }

      path.startsWith("cli/jvm/compiler/") -> {
        val path = path.substring(17)
        when {
          path.startsWith("ClassF") -> true
          path.startsWith("CliCompilerUtilsKt\$createC") -> true
          path.startsWith("CliCompilerUtilsKt\$w") -> true
          path.startsWith("CliE") -> true
          path.startsWith("Compi") -> true
          path.startsWith("Dup") -> true
          path.startsWith("Fil") -> true
          path.startsWith("Fin") -> true
          path.startsWith("FirKotlinToJvmBytecodeCompiler\$") -> true
          path.startsWith("FirKotlinToJvmBytecodeCompilerK") -> true
          path.startsWith("KotlinToJVMBytecodeCompiler\$D") -> true
          path.startsWith("L") -> true
          path.startsWith("pipeline/CompilerPipelineKt\$compileModuleToAnalyzedFir\$c") -> true
          path.startsWith("pipeline/CompilerPipelineKt\$compileModules") -> true
          path.startsWith("pipeline/CompilerPipelineKt\$g") -> true
          path.startsWith("pipeline/CompilerPipelineKt\$w") -> true
          path.startsWith("pipeline/M") -> true

          else -> false
        }
      }

      path.startsWith("fir/backend/Fir2IrClassifierStorage\$") -> true
      path.startsWith("javac") -> true
      path.startsWith("fir/") -> {
        val path = path.substring(4)
        when {
          path.startsWith("builder/PsiRawFirBuilder\$Visitor\$") -> {
            val path = path.substring(33)
            when {
              path.startsWith("convertScript\$2\$destructuringB") -> true
              path.startsWith("toFirValueParameter\$1") -> true
              path.startsWith("visitDestructuringDeclaration\$1") -> true
              path.startsWith("visitDestructuringDeclaration\$2") -> true
              path.startsWith("visitForExpression\$1\$2\$") -> true
              path.startsWith("visitL") -> true

              else -> false
            }
          }

          path.startsWith("resolve/") -> {
            val path = path.substring(8)
            when {
              path.startsWith("providers/impl/FirBuiltinSymbolProvider\$BuiltInsPackageFragment\$l") -> true
              path.startsWith("transformers/") -> {
                val path = path.substring(13)
                when {
                  path.startsWith("body/resolve/FirDeclarationsResolveTransformer\$transformAnonymousO") -> true
                  path.startsWith("body/resolve/ReturnTypeCalculatorWithJump\$F") -> true
                  path.startsWith("mpp/FirExpectActualResolver\$") -> true
                  path.startsWith("plugin/AbstractFirSpecificAnnotationResolveTransformer\$A") -> true
                  path.startsWith("plugin/AbstractFirSpecificAnnotationResolveTransformer\$C") -> true
                  path.startsWith("plugin/CompilerRequiredAnnotationsH") -> true

                  else -> false
                }
              }

              path.startsWith("dfa/FirDataFlowAnalyzer\$") -> {
                val path = path.substring(24)
                when {
                  path.startsWith("processW") -> true
                  path.startsWith("F") -> true
                  path.startsWith("b") -> true

                  else -> false
                }
              }

              path.startsWith("TypeA") -> true
              path.startsWith("calls/TypeAl") -> true
              path.startsWith("dfa/cfg/Al") -> true
              path.startsWith("dfa/cfg/CFGNodeKt\$") -> true
              path.startsWith("diagnostics/ConeNoCon") -> true
              path.startsWith("inference/FirDelegatedPropertyInferenceSession\$con") -> true
              path.startsWith("inference/FirDelegatedPropertyInferenceSession\$h") -> true
              path.startsWith("providers/impl/FirTypeResolverImpl\$C") -> true
              path.startsWith("providers/impl/FirTypeResolverImpl\$P") -> true

              else -> false
            }
          }

          path.startsWith("analysis/checkers/") -> {
            val path = path.substring(18)
            when {
              path.startsWith("declaration/Fir") -> {
                val path = path.substring(15)
                when {
                  path.startsWith("TopLevelF") -> true
                  path.startsWith("ImplementationMismatchChecker\$checkC") -> true
                  path.startsWith("ModifierChecker\$") -> true

                  else -> false
                }
              }

              path.startsWith("FirD") -> true

              else -> false
            }
          }

          path.startsWith("symbols/Fir2Ir") -> {
            val path = path.substring(14)
            when {
              path.startsWith("B") -> false

              else -> true
            }
          }

          path.startsWith("backend/ConversionUtilsKt\$g") -> true
          path.startsWith("analysis/FirE") -> true
          path.startsWith("analysis/FirR") -> true
          path.startsWith("analysis/diagnostics/ConeDiagnosticToFirDiagnosticKt\$f") -> true
          path.startsWith("analysis/jvm/checkers/expression/FirS") -> true
          path.startsWith("backend/generators/DataClassMembersGenerator\$MyDataClassMethodsGenerator\$g") -> true
          path.startsWith("backend/jvm/FirJvmBackendExtension\$") -> true
          path.startsWith("declarations/builder/FirErrorC") -> true
          path.startsWith("declarations/impl/FirErrorC") -> true
          path.startsWith("deserialization/LibraryPathFilter\$LibraryList\$") -> true
          path.startsWith("expressions/impl/FirNo") -> true
          path.startsWith("java/deserialization/JvmClassFileBasedSymbolProvider\$extractClassMetadata\$2") -> true
          path.startsWith("java/scopes/JavaClassUseSiteMemberScope\$s") -> true
          path.startsWith("li") -> true
          path.startsWith("renderer/ConeTypeRendererW") -> true
          path.startsWith("scopes/Fa") -> true
          path.startsWith("scopes/impl/FirSco") -> true
          path.startsWith("symbols/impl/FirClassLikeSymbolK") -> true

          else -> false
        }
      }

      path.startsWith("daemon/") -> {
        val path = path.substring(7)
        when {
          path.startsWith("CompileServiceImpl") -> {
            val path = path.substring(18)
            when {
              path.startsWith("Base\$") -> {
                val path = path.substring(5)
                when {
                  path.startsWith("c") -> false
                  path.startsWith("p") -> false
                  path.startsWith("s") -> false

                  else -> true
                }
              }

              path.startsWith("\$initiateElections\$l") -> true
              path.startsWith("K") -> true

              else -> false
            }
          }

          path.startsWith("report/") -> {
            val path = path.substring(7)
            when {
              path.startsWith("CompileServicesFacadeMessageCollector.") -> false

              else -> true
            }
          }

          path.startsWith("KotlinCompileDaemon") -> {
            val path = path.substring(19)
            when {
              path.startsWith("Base\$l") -> true
              path.startsWith("Base\$mainImpl\$1\$compilerSelector\$1\$W") -> true
              path.startsWith("K") -> true

              else -> false
            }
          }

          path.startsWith("R") -> true
          path.startsWith("L") -> true
          path.startsWith("E") -> true
          path.startsWith("Ke") -> true
          path.startsWith("KotlinJvmReplServiceBase\$l") -> true
          path.startsWith("KotlinR") -> true

          else -> false
        }
      }

      path.startsWith("cli/js/") -> {
        val path = path.substring(7)
        when {
          path.startsWith("dce/K2JSDce\$collectInputFilesFrom") -> {
            val path = path.substring(33)
            when {
              path.startsWith("Directory\$3") -> false
              path.startsWith("Directory\$4") -> false
              path.startsWith("Zip\$1\$3") -> false
              path.startsWith("Zip\$1\$5") -> false

              else -> true
            }
          }

          path.startsWith("K2JSCompiler\$1") -> false
          path.startsWith("K2JSCompiler.") -> false
          path.startsWith("K2JsIrCompiler\$C") -> false
          path.startsWith("K2JsIrCompiler.") -> false
          path.startsWith("dce/K2JSDce\$C") -> false
          path.startsWith("dce/K2JSDce.") -> false

          else -> true
        }
      }

      path.startsWith("backend/common/") -> {
        val path = path.substring(15)
        when {
          path.startsWith("Col") -> true
          path.startsWith("actualizer/IrC") -> true
          path.startsWith("actualizer/IrExpectActualA") -> true
          path.startsWith("lower/inline/Inliner") -> true
          path.startsWith("overrides/FakeOverrideBuilder\$b") -> true
          path.startsWith("overrides/FakeOverrideBuilder\$l") -> true
          path.startsWith("serialization/E") -> true
          path.startsWith("serialization/proto/Act") -> true
          path.startsWith("serialization/signature/IdSignatureSerializer\$") -> true

          else -> false
        }
      }

      path.startsWith("kot") -> true
      path.startsWith("ir/backend/js/lower/serialization/ir/JsIrFileSerializer\$") -> true
      path.startsWith("cli/metadata/") -> {
        val path = path.substring(13)
        when {
          path.startsWith("FirMetadataSerializer") -> {
            val path = path.substring(21)
            when {
              path.startsWith("\$analyze\$outputs\$p") -> false
              path.startsWith("\$analyze\$outputs\$sessionsWithSources\$1") -> false
              path.startsWith(".") -> false

              else -> true
            }
          }

          path.startsWith("MetadataSerializer") -> {
            val path = path.substring(18)
            when {
              path.startsWith("\$a") -> true
              path.startsWith("E") -> true

              else -> false
            }
          }

          path.startsWith("CommonAnalysisK") -> false
          path.startsWith("K2Metadata") -> {
            val path = path.substring(10)
            when {
              path.startsWith("KlibSerializerK") -> true
              path.startsWith("Compiler\$K") -> true

              else -> false
            }
          }

          path.startsWith("Ab") -> false

          else -> true
        }
      }

      path.startsWith("serialization/builtins/BuiltInsSerializer") -> {
        val path = path.substring(41)
        when {
          path.startsWith("\$Companion.") -> false
          path.startsWith(".") -> false

          else -> true
        }
      }

      path.startsWith("psi2ir/generators/Ex") -> true
      path.startsWith("cli/jvm/JvmArgumentsKt\$") -> true
      path.startsWith("ne") -> true
      path.startsWith("KtFakeSourceElementKind\$SuperCallE") -> true
      path.startsWith("backend/jvm/JvmLowerKt\$functionInliningPhase\$1\$") -> true
      path.startsWith("backend/jvm/JvmLowerKt\$initializersCleanupPhase\$1\$") -> true
      path.startsWith("backend/jvm/JvmLowerKt\$localDeclarationsPhase\$1\$2") -> true
      path.startsWith("backend/jvm/JvmLowerKt\$m") -> true
      path.startsWith("backend/jvm/lower/AnnotationLowering\$") -> true
      path.startsWith("backend/jvm/lower/ObjectClassLowering\$") -> true
      path.startsWith("cli/jvm/K2JVMCompiler\$K") -> true
      path.startsWith("cli/jvm/config/K") -> true
      path.startsWith("cli/jvm/p") -> true
      path.startsWith("codegen/inline/SMAPParser\$") -> true
      path.startsWith("codegen/optimization/fixStack/FixStackAnalyzer\$I") -> true
      path.startsWith("diagnostics/Du") -> true
      path.startsWith("ir/backend/js/KlibKt\$serializeModuleIntoKlib\$lambda\$3") -> true
      path.startsWith("ir/backend/js/Pre") -> true
      path.startsWith("ir/backend/js/transformers/irToJs/IrModuleToJsTransformer\$m") -> true
      path.startsWith("ir/backend/js/transformers/irToJs/Merger\$m") -> true
      path.startsWith("ir/backend/js/transformers/irToJs/P") -> true
      path.startsWith("ir/linkage/partial/IrUnimplementedOverridesStrategy\$C") -> true
      path.startsWith("ir/linkage/partial/IrUnimplementedOverridesStrategy\$D") -> true
      path.startsWith("ir/overrides/IrOverridingUtil\$") -> true
      path.startsWith("ir/overrides/IrOverridingUtilK") -> true
      path.startsWith("javax/inject/N") -> true
      path.startsWith("javax/inject/P") -> true
      path.startsWith("javax/inject/Q") -> true
      path.startsWith("javax/inject/S") -> true
      path.startsWith("name/StandardClassIds\$Annotations\$J") -> true
      path.startsWith("name/StandardClassIds\$J") -> true
      path.startsWith("o") -> true
      path.startsWith("resolve/multiplatform/ExpectActualCompatibility\$Incompatible\$U") -> true
      path.startsWith("serialization/builtins/RunKt\$") -> true

      else -> false
    }
  }

  path.startsWith("com/google/c") -> true
  path.startsWith("kotlin/") -> {
    val path = path.substring(7)
    when {
      path.startsWith("script/experimental/") -> {
        val path = path.substring(20)
        when {
          path.startsWith("jvmhost/") -> {
            val path = path.substring(8)
            when {
              path.startsWith("BasicJvmScript") -> {
                val path = path.substring(14)
                when {
                  path.startsWith("ingHost.") -> false
                  path.startsWith("ingHostKt.") -> false

                  else -> true
                }
              }

              path.startsWith("C") -> true
              path.startsWith("JvmScriptE") -> true
              path.startsWith("JvmScriptS") -> true
              path.startsWith("K") -> true
              path.startsWith("O") -> true
              path.startsWith("jsr223/KotlinJsr223InvocableScriptEngine\$") -> true
              path.startsWith("jsr223/KotlinJsr223InvocableScriptEngineK") -> true
              path.startsWith("jsr223/KotlinJsr223ScriptEngineImpl\$b") -> true
              path.startsWith("repl/JvmReplEvaluator\$eval\$1\$1") -> true
              path.startsWith("repl/JvmReplEvaluator\$eval\$1\$currentConfiguration\$1\$") -> true

              else -> false
            }
          }

          path.startsWith("jsr223/KotlinJsr223DefaultScriptEngineFactory\$") -> {
            val path = path.substring(46)
            when {
              path.startsWith("getScriptEngine\$1\$1.") -> false
              path.startsWith("getScriptEngine\$1.") -> false

              else -> true
            }
          }

          path.startsWith("jsr223/KotlinJsr223DefaultScript.") -> true

          else -> false
        }
      }

      path.startsWith("reflect/jvm/internal/") -> {
        val path = path.substring(21)
        when {
          path.startsWith("i") -> false
          path.startsWith("p") -> false
          path.startsWith("KClassC") -> false

          else -> true
        }
      }

      path.startsWith("script/") -> {
        val path = path.substring(7)
        when {
          path.startsWith("d") -> false
          path.startsWith("t") -> false
          path.startsWith("ext") -> false

          else -> true
        }
      }

      else -> true
    }
  }

  path.startsWith("org/") -> {
    val path = path.substring(4)
    when {
      path.startsWith("objectweb/asm/") -> {
        val path = path.substring(14)
        when {
          path.startsWith("tree/") -> {
            val path = path.substring(5)
            when {
              path.startsWith("analysis/") -> {
                val path = path.substring(9)
                when {
                  path.startsWith("Analyzer.") -> true
                  path.startsWith("BasicVe") -> true
                  path.startsWith("Si") -> true
                  path.startsWith("Su") -> true

                  else -> false
                }
              }

              path.startsWith("Mo") -> true
              path.startsWith("C") -> true
              path.startsWith("FieldN") -> true
              path.startsWith("Inn") -> true
              path.startsWith("MethodNode\$") -> true
              path.startsWith("P") -> true
              path.startsWith("R") -> true
              path.startsWith("Tr") -> true

              else -> false
            }
          }

          path.startsWith("util/Trace") -> {
            val path = path.substring(10)
            when {
              path.startsWith("Me") -> false

              else -> true
            }
          }

          path.startsWith("commons/") -> {
            val path = path.substring(8)
            when {
              path.startsWith("Mo") -> true
              path.startsWith("C") -> true
              path.startsWith("F") -> true
              path.startsWith("Rec") -> true

              else -> false
            }
          }

          path.startsWith("Cons") -> true
          path.startsWith("Attribute\$") -> true
          path.startsWith("TypeP") -> true
          path.startsWith("TypeR") -> true
          path.startsWith("signature/SignatureR") -> true
          path.startsWith("util/TextifierS") -> true

          else -> false
        }
      }

      path.startsWith("jl") -> true
      path.startsWith("jd") -> true
      path.startsWith("jetbrains/") -> {
        val path = path.substring(10)
        when {
          path.startsWith("a") -> true
          path.startsWith("c") -> true
          path.startsWith("j") -> true

          else -> false
        }
      }

      path.startsWith("f") -> true
      path.startsWith("picocontainer/") -> {
        val path = path.substring(14)
        when {
          path.startsWith("C") -> true
          path.startsWith("M") -> true
          path.startsWith("PicoC") -> true
          path.startsWith("defaults/As") -> true

          else -> false
        }
      }

      path.startsWith("i") -> true
      path.startsWith("c") -> true
      path.startsWith("a") -> true

      else -> false
    }
  }

  path.startsWith("i") -> true
  path.startsWith("g") -> true
  path.startsWith("com/f") -> true
  path.startsWith("META-INF/v") -> true

  else -> false
}