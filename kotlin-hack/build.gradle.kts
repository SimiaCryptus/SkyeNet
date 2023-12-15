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
  implementation(kGroup, name = "kotlin-compiler-embeddable", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-compiler-embeddable", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-compiler-impl-embeddable", version = kVersion) {
    isTransitive = false
  }
  implementation(kGroup, name = "kotlin-scripting-jsr223", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm-host", version = kVersion) { isTransitive = false }
}

val outputPackagePrefix = "aicoder"

val verbose = false

fun shouldInclude(path: String) = when {
  shouldOverride(path) -> true
  isConflicting(path) -> false
  else -> isPruned(path)
}

fun shouldRelocate(path: String) = when {

  // We want to maintain this interface:
  path.startsWith("kotlin/script/experimental/jvm/compiler") -> true
  path.contains("/KotlinJsr223") -> false
  path.contains("/ScriptArgsWithTypes") -> false
  path.startsWith("kotlin/script/experimental/jsr223") -> false
  path.startsWith("kotlin/script/experimental/jvm") -> false
  path.startsWith("kotlin/script/experimental/jvmhost/jsr223") -> false

  shouldOverride(path) -> true
  else -> true
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
        if (shouldInclude(path)) {
          if (verbose) println("${this.path} included from ${file.name} as $path")
        } else {
          if (verbose) println("${this.path} excluded from ${file.name} as $path")
          exclude(this.path)
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
val shadowJarFinalStage by tasks.registering(ShadowJar::class) {
  archiveClassifier.set("")
  isZip64 = true
  dependsOn(shadowJarStage2)
  doFirst {
    from(zipTree(shadowJarStage2.get().archiveFile))
    val inputFiles: MutableSet<String> = mutableSetOf()
    zipTree(shadowJarStage2.get().archiveFile).visit {
      if (this.isDirectory) return@visit
      inputFiles.add(this.relativePath.toString().removeSuffix(".class"))
    }
    relocate(object : Relocator {

      override fun canRelocatePath(path: String?) = true
      fun shouldRelocatePath(path: String?): Boolean {
        if (!inputFiles.contains(path)) {
          if (verbose) println("""ignoring non-present "$path"""")
          return false
        }
        return path?.let { path -> shouldRelocate(path) } ?: false
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

      fun shouldRelocateClass(className: String?) = shouldRelocatePath(className?.replace(".(?!class)".toRegex(), "/"))

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

                                                                                                                                                                                                                                                                                                                                                                  // GENERATED CODE

      // Required Classes: 8003
      // Overrides: 5069
      // Pruned: 21187
      // Conflicts: 5838
      // Dead Weight: 21187

      // Overrides:
      fun shouldOverride(path: String) = when {
                path.startsWith("org/jetbrains/kotlin/") -> {
                  val path = path.removePrefix("org/jetbrains/kotlin/")
                  when {
                  path.startsWith("scripting/compiler/plugin/") -> {
                    val path = path.removePrefix("scripting/compiler/plugin/")
                    when {
      path.startsWith("A") -> false
      path.startsWith("C") -> false
      path.startsWith("E") -> false
      path.startsWith("JvmCliScriptEvaluationExtension\$") -> false
      path.startsWith("JvmCliScriptEvaluationExtensionK") -> false
      path.startsWith("ScriptC") -> false
      path.startsWith("ScriptingCommandLineProcessor\$") -> false
      path.startsWith("ScriptingCompilerConfigurationExtension\$") -> false
      path.startsWith("definitions/CliScriptDef") -> false
      path.startsWith("definitions/CliScriptReportSink\$") -> false
      path.startsWith("dependencies/ScriptsCompilationDependenciesKt\$") -> false
      path.startsWith("extensions/ScriptingProcessSourcesBeforeCompilingExtension\$") -> false
      path.startsWith("repl/GenericReplCheckerState.") -> false
      path.startsWith("repl/GenericReplCompiler\$") -> false
      path.startsWith("repl/I") -> false
      path.startsWith("repl/ReplCodeAnalyzerBase\$C") -> false
      path.startsWith("repl/ReplCodeAnalyzerBase\$Rep") -> false
      path.startsWith("repl/ReplCodeAnalyzerBase\$ScriptMutableDeclarationProviderFactory\$") -> false
      path.startsWith("repl/ReplCom") -> false
      path.startsWith("repl/ReplE") -> false
      path.startsWith("repl/ReplFromTerminal\$") -> false
      path.startsWith("repl/ReplIm") -> false
      path.startsWith("repl/ReplInterpreter\$1") -> false
      path.startsWith("repl/ReplInterpreter\$C") -> false
      path.startsWith("repl/ReplInterpreter\$W") -> false
      path.startsWith("repl/ReplInterpreter\$m") -> false
      path.startsWith("repl/Res") -> false
      path.startsWith("repl/S") -> false
      path.startsWith("repl/c") -> false
      path.startsWith("repl/messages/C") -> false
      path.startsWith("repl/messages/D") -> false
      path.startsWith("repl/reader/C") -> false
      path.startsWith("repl/reader/I") -> false
      path.startsWith("repl/reader/ReplC") -> false
      path.startsWith("repl/w") -> false
      path.startsWith("services/Fir2IrScriptConfiguratorExtensionImpl\$") -> false
      path.startsWith("services/FirScriptConfigurati") -> false
      path.startsWith("services/FirScriptConfiguratorExtensionImpl\$") -> false
      path.startsWith("services/FirScriptDefinitionProviderService\$") -> false
      path.startsWith("services/FirScriptDefinitionProviderService.") -> false
      path.startsWith("services/S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("fir/") -> {
                    val path = path.removePrefix("fir/")
                    when {
      path.startsWith("Bu") -> true
      path.startsWith("Cl") -> true
      path.startsWith("DelegatedWrapperData.") -> true
      path.startsWith("Ef") -> true
      path.startsWith("FirFi") -> true
      path.startsWith("FirLanguageSettingsComponentK") -> true
      path.startsWith("FirNameConflictsTrackerComponent.") -> true
      path.startsWith("FirVisibilityChecker.") -> true
      path.startsWith("FirVisibilityCheckerKt.") -> true
      path.startsWith("U") -> true
      path.startsWith("analysis/CheckersComponent.") -> true
      path.startsWith("analysis/checkers/CommonL") -> true
      path.startsWith("analysis/checkers/ExtendedL") -> true
      path.startsWith("analysis/checkers/La") -> true
      path.startsWith("analysis/checkers/Op") -> true
      path.startsWith("analysis/checkers/conf") -> true
      path.startsWith("analysis/checkers/context/CheckerContext.") -> true
      path.startsWith("analysis/checkers/extended/CanBeR") -> true
      path.startsWith("analysis/collectors/components/L") -> true
      path.startsWith("analysis/diagnostics/FirErrors.") -> true
      path.startsWith("analysis/extensions/FirAdditionalCheckersExtension.") -> true
      path.startsWith("backend/ConversionUtilsKt.") -> true
      path.startsWith("backend/Fir2IrA") -> true
      path.startsWith("backend/Fir2IrComponents.") -> true
      path.startsWith("backend/Fir2IrConf") -> true
      path.startsWith("backend/Fir2IrPluginContext.") -> true
      path.startsWith("backend/Fir2IrScriptConfiguratorExtension.") -> true
      path.startsWith("backend/jvm/FirJvmBackendC") -> true
      path.startsWith("backend/jvm/JvmFir2IrExtensions.") -> true
      path.startsWith("contracts/i") -> true
      path.startsWith("declarations/DeclarationUtilsKt.") -> true
      path.startsWith("declarations/DeprecationUtilsKt.") -> true
      path.startsWith("declarations/DeprecationsProvider.") -> true
      path.startsWith("declarations/FirA") -> true
      path.startsWith("declarations/FirB") -> true
      path.startsWith("declarations/FirCa") -> true
      path.startsWith("declarations/FirCl") -> true
      path.startsWith("declarations/FirCod") -> true
      path.startsWith("declarations/FirCons") -> true
      path.startsWith("declarations/FirDa") -> true
      path.startsWith("declarations/FirEn") -> true
      path.startsWith("declarations/FirErrorF") -> true
      path.startsWith("declarations/FirErrorP") -> true
      path.startsWith("declarations/FirF") -> true
      path.startsWith("declarations/FirO") -> true
      path.startsWith("declarations/FirProperty.") -> true
      path.startsWith("declarations/FirPropertyA") -> true
      path.startsWith("declarations/FirReg") -> true
      path.startsWith("declarations/FirS") -> true
      path.startsWith("declarations/FirTypeA") -> true
      path.startsWith("declarations/FirTypeParameter.") -> true
      path.startsWith("declarations/FirTypeParameterRef.") -> true
      path.startsWith("declarations/FirValueP") -> true
      path.startsWith("declarations/FirVar") -> true
      path.startsWith("declarations/U") -> true
      path.startsWith("declarations/builder/FirConstructorBuilder.") -> true
      path.startsWith("declarations/builder/FirContextReceiverBuilder.") -> true
      path.startsWith("declarations/builder/FirDefaultSetterValueParameterBuilder.") -> true
      path.startsWith("declarations/builder/FirFieldBuilder.") -> true
      path.startsWith("declarations/builder/FirImportBuilder.") -> true
      path.startsWith("declarations/builder/FirPropertyAccessorBuilder.") -> true
      path.startsWith("declarations/builder/FirPropertyBuilder.") -> true
      path.startsWith("declarations/builder/FirReceiverParameterBuilder.") -> true
      path.startsWith("declarations/builder/FirRegularClassBuilder.") -> true
      path.startsWith("declarations/builder/FirResolvedImportBuilder.") -> true
      path.startsWith("declarations/builder/FirSimpleFunctionBuilder.") -> true
      path.startsWith("declarations/builder/FirTypeParameterBuilder.") -> true
      path.startsWith("declarations/builder/FirValueParameterBuilder.") -> true
      path.startsWith("declarations/i") -> true
      path.startsWith("declarations/synthetic/FirSyntheticProperty.") -> true
      path.startsWith("declarations/synthetic/FirSyntheticPropertyA") -> true
      path.startsWith("declarations/utils/FirD") -> true
      path.startsWith("deserialization/AbstractAnnotationDeserializer.") -> true
      path.startsWith("deserialization/ClassDeserializationKt.") -> true
      path.startsWith("deserialization/FirDeserializationC") -> true
      path.startsWith("deserialization/FirM") -> true
      path.startsWith("expressions/FirErrorR") -> true
      path.startsWith("expressions/FirExpressionU") -> true
      path.startsWith("expressions/FirResolve") -> true
      path.startsWith("expressions/builder/FirAnnotationArgumentMappingBuilder.") -> true
      path.startsWith("expressions/builder/FirAnnotationBuilder.") -> true
      path.startsWith("expressions/builder/FirCons") -> true
      path.startsWith("expressions/i") -> true
      path.startsWith("extensions/BunchOfRegisteredExtensions.") -> true
      path.startsWith("extensions/DeclarationGenerationContext\$") -> true
      path.startsWith("extensions/FirDeclarationGenerationExtension.") -> true
      path.startsWith("extensions/FirDeclarationP") -> true
      path.startsWith("extensions/FirExtension.") -> true
      path.startsWith("extensions/FirExtensionR") -> true
      path.startsWith("extensions/FirFunctionTypeKindExtension\$Fu") -> true
      path.startsWith("extensions/FirFunctionTypeKindExtension.") -> true
      path.startsWith("extensions/FirStatusTransformerExtension.") -> true
      path.startsWith("i") -> true
      path.startsWith("java/FirJavaElementFinder.") -> true
      path.startsWith("java/FirJavaFacade.") -> true
      path.startsWith("pipeline/A") -> true
      path.startsWith("pipeline/ConvertToIrKt.") -> true
      path.startsWith("pipeline/FirU") -> true
      path.startsWith("pipeline/M") -> true
      path.startsWith("references/FirB") -> true
      path.startsWith("references/FirD") -> true
      path.startsWith("references/FirReferenceU") -> true
      path.startsWith("references/i") -> true
      path.startsWith("resolve/BodyResolveComponents.") -> true
      path.startsWith("resolve/De") -> true
      path.startsWith("resolve/FirDoubleColonExpressionResolver.") -> true
      path.startsWith("resolve/FirQualifierResolver.") -> true
      path.startsWith("resolve/FirSamR") -> true
      path.startsWith("resolve/ImplicitIntegerCoercionK") -> true
      path.startsWith("resolve/L") -> true
      path.startsWith("resolve/ResolveUtilsKt.") -> true
      path.startsWith("resolve/ScopeUtilsKt.") -> true
      path.startsWith("resolve/SupertypeUtilsKt.") -> true
      path.startsWith("resolve/TypeExpansionUtilsKt.") -> true
      path.startsWith("resolve/calls/ArgumentsKt.") -> true
      path.startsWith("resolve/calls/Candidate.") -> true
      path.startsWith("resolve/diagnostics/ConeOu") -> true
      path.startsWith("resolve/diagnostics/ConeW") -> true
      path.startsWith("resolve/inference/InferenceU") -> true
      path.startsWith("resolve/inference/PostponedArgumentsK") -> true
      path.startsWith("resolve/providers/FirCompositeCachedSymbolNamesProvider.") -> true
      path.startsWith("resolve/providers/FirProvider.") -> true
      path.startsWith("resolve/providers/FirProviderU") -> true
      path.startsWith("resolve/providers/FirSymbolProvider.") -> true
      path.startsWith("resolve/providers/FirSymbolProviderK") -> true
      path.startsWith("resolve/providers/i") -> true
      path.startsWith("resolve/substitution/ConeSubstitutorByMap.") -> true
      path.startsWith("resolve/substitution/S") -> true
      path.startsWith("resolve/transformers/FirImportResolveT") -> true
      path.startsWith("resolve/transformers/Im") -> true
      path.startsWith("resolve/transformers/PackageResolutionResult\$P") -> true
      path.startsWith("resolve/transformers/Ph") -> true
      path.startsWith("resolve/transformers/ReturnTypeCalculator.") -> true
      path.startsWith("scopes/FirKotlinScopeProvider.") -> true
      path.startsWith("scopes/FirKotlinScopeProviderK") -> true
      path.startsWith("scopes/FirN") -> true
      path.startsWith("scopes/FirOverrideCheckerK") -> true
      path.startsWith("scopes/FirOverrideService.") -> true
      path.startsWith("scopes/FirScope.") -> true
      path.startsWith("scopes/FirScopeKt.") -> true
      path.startsWith("scopes/FirTypeP") -> true
      path.startsWith("scopes/FirTypeScope\$") -> true
      path.startsWith("scopes/FirTypeScope.") -> true
      path.startsWith("scopes/FirTypeScopeKt.") -> true
      path.startsWith("scopes/M") -> true
      path.startsWith("scopes/i") -> true
      path.startsWith("symbols/ConeT") -> true
      path.startsWith("symbols/FirLazyDeclarationResolverK") -> true
      path.startsWith("symbols/i") -> true
      path.startsWith("types/ConeInferenceContext.") -> true
      path.startsWith("types/ConeTypeContext.") -> true
      path.startsWith("types/ConeTypeUtilsKt.") -> true
      path.startsWith("types/FirFunctionTypeKindService.") -> true
      path.startsWith("types/FirTypeU") -> true
      path.startsWith("types/FunctionalTypeUtilsKt.") -> true
      path.startsWith("types/TypeCon") -> true
      path.startsWith("types/TypeUtilsKt.") -> true
      path.startsWith("types/builder/FirResolvedTypeRefBuilder.") -> true
      path.startsWith("types/i") -> true
      path.startsWith("utils/exceptions/FirExceptionUtilsKt.") -> true
      
      else -> false
    }
                  }
    path.startsWith("KtIo") -> true
    path.startsWith("KtL") -> true
    path.startsWith("KtNodeTypes") -> true
    path.startsWith("KtPsiSourceElement.") -> true
    path.startsWith("KtPsiSourceFile.") -> true
    path.startsWith("KtSourceElement.") -> true
    path.startsWith("KtSourceElementKt") -> true
    path.startsWith("KtV") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInDecompiler.") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInF") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache\$C") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinClsStubBuilder.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinMetadataStubBuilder.") -> true
    path.startsWith("analysis/project/structure/KtModule.") -> true
    path.startsWith("analysis/project/structure/i") -> true
    path.startsWith("analysis/providers/KotlinAnnotationsResolverF") -> true
    path.startsWith("analysis/providers/KotlinDeclarationProvider.") -> true
    path.startsWith("analysis/providers/KotlinDeclarationProviderFactory.") -> true
    path.startsWith("analysis/providers/KotlinDeclarationProviderK") -> true
    path.startsWith("analysis/providers/KotlinMessageBusProvider.") -> true
    path.startsWith("analysis/providers/KotlinMessageBusProviderK") -> true
    path.startsWith("analysis/providers/KotlinModificationTrackerFactory.") -> true
    path.startsWith("analysis/providers/KotlinPackageProviderF") -> true
    path.startsWith("analysis/providers/KotlinResolutionScopeProvider.") -> true
    path.startsWith("analysis/providers/i") -> true
    path.startsWith("analysis/providers/topics/KotlinT") -> true
    path.startsWith("analyzer/KotlinModificationTrackerService.") -> true
    path.startsWith("asJava/KotlinAsJavaSupport.") -> true
    path.startsWith("asJava/KotlinAsJavaSupportBase.") -> true
    path.startsWith("asJava/LightClassGenerationSupport.") -> true
    path.startsWith("asJava/builder/ClsWrapperStubPsiFactory.") -> true
    path.startsWith("asJava/classes/ImplUtilsKt.") -> true
    path.startsWith("asJava/classes/KtDescriptorBasedFakeLightClass.") -> true
    path.startsWith("asJava/classes/KtFakeLightClass.") -> true
    path.startsWith("asJava/classes/KtLightClassForFacade.") -> true
    path.startsWith("asJava/classes/KtLightClassForSourceDeclarationKt.") -> true
    path.startsWith("asJava/classes/KtUltraLightClass.") -> true
    path.startsWith("asJava/classes/KtUltraLightClassForFacade.") -> true
    path.startsWith("asJava/classes/KtUltraLightClassForScript.") -> true
    path.startsWith("asJava/classes/KtUltraLightSup") -> true
    path.startsWith("asJava/classes/UltraLightUtilsKt.") -> true
    path.startsWith("asJava/finder/JavaElementFinder.") -> true
    path.startsWith("backend/common/extensions/IrA") -> true
    path.startsWith("backend/common/extensions/IrPluginContext.") -> true
    path.startsWith("backend/common/extensions/IrPluginContextImpl\$D") -> true
    path.startsWith("backend/common/serialization/IrFileDeserializerKt.") -> true
    path.startsWith("backend/common/serialization/encodings/ClassFlags\$") -> true
    path.startsWith("backend/common/serialization/metadata/i") -> true
    path.startsWith("backend/jvm/JvmBackendContext.") -> true
    path.startsWith("backend/jvm/JvmCachedDeclarations.") -> true
    path.startsWith("backend/jvm/JvmGeneratorExtensionsImpl.") -> true
    path.startsWith("backend/jvm/JvmIrCodegenFactory.") -> true
    path.startsWith("backend/jvm/MemoizedInlineClassReplacements\$getS") -> true
    path.startsWith("backend/jvm/MemoizedInlineClassReplacements.") -> true
    path.startsWith("backend/jvm/MemoizedMultiFieldValueClassReplacements\$gr") -> true
    path.startsWith("backend/jvm/MfvcNodeFactoryKt.") -> true
    path.startsWith("backend/jvm/ReceiverBasedMfvcNodeInstance.") -> true
    path.startsWith("backend/jvm/ir/JvmIrTypeUtilsKt.") -> true
    path.startsWith("backend/jvm/ir/JvmIrUtilsKt.") -> true
    path.startsWith("backend/jvm/lower/InheritedDefaultMethodsOnClassesLoweringKt.") -> true
    path.startsWith("backend/jvm/lower/JvmMultiFieldValueClassLowering.") -> true
    path.startsWith("backend/jvm/lower/ReplaceKFunctionInvokeWithFunctionInvoke.") -> true
    path.startsWith("backend/jvm/lower/ReplaceNumberToCharCallSitesLowering.") -> true
    path.startsWith("builtins/KotlinBuiltIns.") -> true
    path.startsWith("builtins/jvm/JvmBuiltIns.") -> true
    path.startsWith("cli/common/ArgumentsKt.") -> true
    path.startsWith("cli/common/CLICompilerK") -> true
    path.startsWith("cli/common/CLICon") -> true
    path.startsWith("cli/common/FirSessionConstructionUtilsKt.") -> true
    path.startsWith("cli/common/GroupedKtSourcesK") -> true
    path.startsWith("cli/common/UtilsKt.") -> true
    path.startsWith("cli/common/arguments/CommonCompilerArguments.") -> true
    path.startsWith("cli/common/arguments/K2JSCompilerArguments.") -> true
    path.startsWith("cli/common/arguments/K2JVMCompilerArguments.") -> true
    path.startsWith("cli/common/config/ContentRoots") -> true
    path.startsWith("cli/common/extensions/ScriptEvaluationExtension.") -> true
    path.startsWith("cli/common/extensions/ShellExtension.") -> true
    path.startsWith("cli/common/fir/FirDiagnosticsCompilerResultsReporter.") -> true
    path.startsWith("cli/common/fir/FirDiagnosticsCompilerResultsReporterK") -> true
    path.startsWith("cli/common/messages/AnalyzerWithCompilerReport.") -> true
    path.startsWith("cli/common/messages/MessageU") -> true
    path.startsWith("cli/common/messages/OutputMessageUtil.") -> true
    path.startsWith("cli/common/modules/ModuleXmlParser.") -> true
    path.startsWith("cli/common/output/OutputUtilsKt.") -> true
    path.startsWith("cli/common/repl/KotlinJsr223JvmScriptEngineBase.") -> true
    path.startsWith("cli/common/repl/KotlinJsr223JvmScriptEngineBaseK") -> true
    path.startsWith("cli/common/repl/ReplUtilKt.") -> true
    path.startsWith("cli/jvm/JvmArgumentsKt.") -> true
    path.startsWith("cli/jvm/com") -> true
    path.startsWith("cli/jvm/config/C") -> true
    path.startsWith("cli/jvm/config/JvmContentRoots") -> true
    path.startsWith("cli/jvm/config/K") -> true
    path.startsWith("cli/jvm/config/V") -> true
    path.startsWith("cli/jvm/index/JavaRoot.") -> true
    path.startsWith("cli/jvm/index/JvmDependenciesDynamicCompoundIndex.") -> true
    path.startsWith("cli/jvm/index/JvmDependenciesIndex.") -> true
    path.startsWith("cli/jvm/index/JvmDependenciesIndexImpl.") -> true
    path.startsWith("cli/jvm/index/SingleJavaFileRootsIndex.") -> true
    path.startsWith("cli/jvm/javac/JavacW") -> true
    path.startsWith("cli/jvm/modules/CliJavaModuleFinder.") -> true
    path.startsWith("cli/jvm/modules/CliJavaModuleResolver.") -> true
    path.startsWith("cli/jvm/modules/CoreJrtF") -> true
    path.startsWith("cli/jvm/modules/CoreJrtVirtualFile.") -> true
    path.startsWith("cli/jvm/modules/JavaV") -> true
    path.startsWith("cli/jvm/plugins/PluginCliParser.") -> true
    path.startsWith("cli/plugins/Plugins") -> true
    path.startsWith("codegen/ClassFileFactory.") -> true
    path.startsWith("codegen/CodegenFactory\$I") -> true
    path.startsWith("codegen/CodegenFactory.") -> true
    path.startsWith("codegen/DefaultCodegenFactory.") -> true
    path.startsWith("codegen/JvmC") -> true
    path.startsWith("codegen/K") -> true
    path.startsWith("codegen/MemberCodegen.") -> true
    path.startsWith("codegen/context/CodegenContext.") -> true
    path.startsWith("codegen/context/P") -> true
    path.startsWith("codegen/state/GenerationState\$B") -> true
    path.startsWith("codegen/state/GenerationState\$GenerateClassFilter.") -> true
    path.startsWith("codegen/state/GenerationState.") -> true
    path.startsWith("codegen/state/J") -> true
    path.startsWith("codegen/state/KotlinTypeMapper.") -> true
    path.startsWith("compiler/plugin/Comp") -> true
    path.startsWith("config/An") -> true
    path.startsWith("config/App") -> true
    path.startsWith("config/Com") -> true
    path.startsWith("config/JVMC") -> true
    path.startsWith("config/JvmAn") -> true
    path.startsWith("config/LanguageVersionS") -> true
    path.startsWith("context/ContextKt.") -> true
    path.startsWith("context/MutableModuleContext.") -> true
    path.startsWith("descriptors/DescriptorVisibilities.") -> true
    path.startsWith("descriptors/TypeA") -> true
    path.startsWith("descriptors/i") -> true
    path.startsWith("diagnostics/AbstractD") -> true
    path.startsWith("diagnostics/AbstractSourceElementPositioningStrategy.") -> true
    path.startsWith("diagnostics/DiagnosticC") -> true
    path.startsWith("diagnostics/DiagnosticFactoryW") -> true
    path.startsWith("diagnostics/DiagnosticReporterFactory.") -> true
    path.startsWith("diagnostics/Errors.") -> true
    path.startsWith("diagnostics/KtDiagnostic.") -> true
    path.startsWith("diagnostics/KtDiagnosticReportH") -> true
    path.startsWith("diagnostics/Unb") -> true
    path.startsWith("diagnostics/i") -> true
    path.startsWith("extensions/CollectAdditionalSourcesExtension.") -> true
    path.startsWith("extensions/PreprocessedFileCreator.") -> true
    path.startsWith("extensions/PreprocessedVirtualFileFactoryExtension.") -> true
    path.startsWith("extensions/ProcessSourcesBeforeCompilingExtension.") -> true
    path.startsWith("extensions/Proj") -> true
    path.startsWith("extensions/internal/TypeResolutionInterceptor.") -> true
    path.startsWith("fileClasses/JvmFileClassUtil.") -> true
    path.startsWith("fileClasses/JvmFileClassUtilK") -> true
    path.startsWith("frontend/java/di/InjectionKt.") -> true
    path.startsWith("idea/KotlinF") -> true
    path.startsWith("idea/KotlinL") -> true
    path.startsWith("idea/MainFunctionDetector.") -> true
    path.startsWith("idea/references/KotlinPsiReferenceR") -> true
    path.startsWith("incremental/Transa") -> true
    path.startsWith("ir/IrBuiltIns.") -> true
    path.startsWith("ir/backend/js/JsIrBackendContext.") -> true
    path.startsWith("ir/backend/js/ir/JsIrBuilder.") -> true
    path.startsWith("ir/backend/js/lower/SecondaryConstructorLowering\$T") -> true
    path.startsWith("ir/backend/js/lower/calls/Rep") -> true
    path.startsWith("ir/backend/jvm/JvmLibraryResolverK") -> true
    path.startsWith("ir/builders/E") -> true
    path.startsWith("ir/builders/IrBlockBu") -> true
    path.startsWith("ir/builders/S") -> true
    path.startsWith("ir/builders/declarations/DeclarationBuildersKt.") -> true
    path.startsWith("ir/builders/declarations/IrFu") -> true
    path.startsWith("ir/declarations/i") -> true
    path.startsWith("ir/expressions/IrExpressions") -> true
    path.startsWith("ir/expressions/i") -> true
    path.startsWith("ir/symbols/i") -> true
    path.startsWith("ir/types/IrD") -> true
    path.startsWith("ir/types/IrE") -> true
    path.startsWith("ir/types/IrSi") -> true
    path.startsWith("ir/types/IrTypesKt.") -> true
    path.startsWith("ir/types/i") -> true
    path.startsWith("ir/util/AdditionalIrUtilsKt.") -> true
    path.startsWith("ir/util/DescriptorSymbolTableExtension.") -> true
    path.startsWith("ir/util/IrTypeUtilsKt.") -> true
    path.startsWith("ir/util/IrUtilsKt.") -> true
    path.startsWith("ir/util/RenderIrElementKt.") -> true
    path.startsWith("ir/util/SymbolTable.") -> true
    path.startsWith("javac/JavaC") -> true
    path.startsWith("javac/JavacWrapper.") -> true
    path.startsWith("javac/JavacWrapperK") -> true
    path.startsWith("javac/components/JavacBasedC") -> true
    path.startsWith("javac/resolve/KotlinClassifiersCache.") -> true
    path.startsWith("javac/resolve/M") -> true
    path.startsWith("js/backend/ast/JsArrayL") -> true
    path.startsWith("js/backend/ast/JsFunction.") -> true
    path.startsWith("js/backend/ast/JsObjectL") -> true
    path.startsWith("js/backend/ast/JsVisitorWithContextImpl.") -> true
    path.startsWith("js/config/JS") -> true
    path.startsWith("js/translate/context/N") -> true
    path.startsWith("js/translate/utils/JsAstUtils.") -> true
    path.startsWith("kdoc/lexer/KDocTokens.") -> true
    path.startsWith("kdoc/parser/KDocE") -> true
    path.startsWith("kdoc/psi/api/KDoc.") -> true
    path.startsWith("kdoc/psi/i") -> true
    path.startsWith("konan/library/i") -> true
    path.startsWith("kotlinx/collections/immutable/E") -> true
    path.startsWith("kotlinx/collections/immutable/im") -> true
    path.startsWith("lexer/KtTokens") -> true
    path.startsWith("library/ToolingSingleFileKlibResolveStrategy.") -> true
    path.startsWith("library/abi/i") -> true
    path.startsWith("library/i") -> true
    path.startsWith("library/metadata/KlibMetadataF") -> true
    path.startsWith("library/metadata/KlibMetadataMo") -> true
    path.startsWith("library/metadata/KlibModuleD") -> true
    path.startsWith("library/metadata/KlibR") -> true
    path.startsWith("library/metadata/Ko") -> true
    path.startsWith("library/metadata/i") -> true
    path.startsWith("library/metadata/resolver/i") -> true
    path.startsWith("library/resolver/i") -> true
    path.startsWith("load/java/JavaClassFinderImplK") -> true
    path.startsWith("load/java/components/FilesByFacadeFqNameIndexer.") -> true
    path.startsWith("load/java/structure/i") -> true
    path.startsWith("load/kotlin/KotlinBinaryClassCache.") -> true
    path.startsWith("load/kotlin/Meta") -> true
    path.startsWith("load/kotlin/VirtualFileFinder.") -> true
    path.startsWith("load/kotlin/VirtualFileFinderFactory.") -> true
    path.startsWith("load/kotlin/incremental/IncrementalPackageFragmentProvider.") -> true
    path.startsWith("parsing/KotlinParserDefinition.") -> true
    path.startsWith("psi/Call.") -> true
    path.startsWith("psi/KtAnnotationE") -> true
    path.startsWith("psi/KtAnnotationU") -> true
    path.startsWith("psi/KtBa") -> true
    path.startsWith("psi/KtBinaryExpression.") -> true
    path.startsWith("psi/KtCallEx") -> true
    path.startsWith("psi/KtCallableR") -> true
    path.startsWith("psi/KtClass.") -> true
    path.startsWith("psi/KtClassB") -> true
    path.startsWith("psi/KtClassLit") -> true
    path.startsWith("psi/KtClassOrObject.") -> true
    path.startsWith("psi/KtCodeFragment.") -> true
    path.startsWith("psi/KtCol") -> true
    path.startsWith("psi/KtConsta") -> true
    path.startsWith("psi/KtConstructor.") -> true
    path.startsWith("psi/KtContextReceiver.") -> true
    path.startsWith("psi/KtContractEffect.") -> true
    path.startsWith("psi/KtDeclaration.") -> true
    path.startsWith("psi/KtDeclarationM") -> true
    path.startsWith("psi/KtElementI") -> true
    path.startsWith("psi/KtEnumEntryS") -> true
    path.startsWith("psi/KtExpression.") -> true
    path.startsWith("psi/KtFile.") -> true
    path.startsWith("psi/KtFunctionL") -> true
    path.startsWith("psi/KtImportA") -> true
    path.startsWith("psi/KtImportD") -> true
    path.startsWith("psi/KtLambdaE") -> true
    path.startsWith("psi/KtModifierList.") -> true
    path.startsWith("psi/KtNameReferenceExpression.") -> true
    path.startsWith("psi/KtNamedF") -> true
    path.startsWith("psi/KtObjectD") -> true
    path.startsWith("psi/KtPara") -> true
    path.startsWith("psi/KtProperty.") -> true
    path.startsWith("psi/KtPropertyA") -> true
    path.startsWith("psi/KtPsiUtil.") -> true
    path.startsWith("psi/KtRet") -> true
    path.startsWith("psi/KtScript.") -> true
    path.startsWith("psi/KtSimpleNameExpression.") -> true
    path.startsWith("psi/KtTypeAl") -> true
    path.startsWith("psi/KtTypeParameter.") -> true
    path.startsWith("psi/KtTypePr") -> true
    path.startsWith("psi/KtTypeReference.") -> true
    path.startsWith("psi/KtUs") -> true
    path.startsWith("psi/KtValueArgument.") -> true
    path.startsWith("psi/KtVisitorVoid.") -> true
    path.startsWith("psi/ValueArgument.") -> true
    path.startsWith("psi/psiUtil/KtPsiUtilKt.") -> true
    path.startsWith("psi/psiUtil/PsiUtilsKt.") -> true
    path.startsWith("psi/stubs/KotlinFi") -> true
    path.startsWith("psi/stubs/S") -> true
    path.startsWith("psi/stubs/elements/KtA") -> true
    path.startsWith("psi/stubs/elements/KtB") -> true
    path.startsWith("psi/stubs/elements/KtCl") -> true
    path.startsWith("psi/stubs/elements/KtCol") -> true
    path.startsWith("psi/stubs/elements/KtConstantExpressionElementType.") -> true
    path.startsWith("psi/stubs/elements/KtConstr") -> true
    path.startsWith("psi/stubs/elements/KtConte") -> true
    path.startsWith("psi/stubs/elements/KtContractEffectE") -> true
    path.startsWith("psi/stubs/elements/KtE") -> true
    path.startsWith("psi/stubs/elements/KtFileE") -> true
    path.startsWith("psi/stubs/elements/KtFu") -> true
    path.startsWith("psi/stubs/elements/KtI") -> true
    path.startsWith("psi/stubs/elements/KtM") -> true
    path.startsWith("psi/stubs/elements/KtN") -> true
    path.startsWith("psi/stubs/elements/KtO") -> true
    path.startsWith("psi/stubs/elements/KtPa") -> true
    path.startsWith("psi/stubs/elements/KtPlaceHolderS") -> true
    path.startsWith("psi/stubs/elements/KtPro") -> true
    path.startsWith("psi/stubs/elements/KtSc") -> true
    path.startsWith("psi/stubs/elements/KtStubElementTypes") -> true
    path.startsWith("psi/stubs/elements/KtTy") -> true
    path.startsWith("psi/stubs/elements/KtUserTypeElementType.") -> true
    path.startsWith("psi/stubs/elements/KtValueArgumentE") -> true
    path.startsWith("psi/stubs/i") -> true
    path.startsWith("resolve/BindingContext.") -> true
    path.startsWith("resolve/CompilerD") -> true
    path.startsWith("resolve/DescriptorFactory.") -> true
    path.startsWith("resolve/DescriptorT") -> true
    path.startsWith("resolve/DescriptorU") -> true
    path.startsWith("resolve/ImplicitIntegerCoercion.") -> true
    path.startsWith("resolve/InlineClassD") -> true
    path.startsWith("resolve/Lan") -> true
    path.startsWith("resolve/LazyTopDownAnalyzer.") -> true
    path.startsWith("resolve/OverridingUtil.") -> true
    path.startsWith("resolve/PlatformDependentAnalyzerServices.") -> true
    path.startsWith("resolve/TopDownAnalysisC") -> true
    path.startsWith("resolve/TypeResolver.") -> true
    path.startsWith("resolve/calls/context/B") -> true
    path.startsWith("resolve/calls/context/ResolutionC") -> true
    path.startsWith("resolve/calls/inference/model/NewConstraintSystemImpl.") -> true
    path.startsWith("resolve/calls/smartcasts/DataFlowInfo.") -> true
    path.startsWith("resolve/calls/tower/Expr") -> true
    path.startsWith("resolve/calls/tower/NewCallA") -> true
    path.startsWith("resolve/calls/tower/NewResolutionOldInferenceKt.") -> true
    path.startsWith("resolve/calls/tower/Su") -> true
    path.startsWith("resolve/calls/util/CallUtilKt.") -> true
    path.startsWith("resolve/deprecation/DeprecationResolver.") -> true
    path.startsWith("resolve/descriptorUtil/DescriptorUtilsKt.") -> true
    path.startsWith("resolve/diagnostics/Diagnostics.") -> true
    path.startsWith("resolve/extensions/AnalysisHandlerExtension.") -> true
    path.startsWith("resolve/extensions/ExtraImportsProviderExtension.") -> true
    path.startsWith("resolve/jvm/JvmCod") -> true
    path.startsWith("resolve/jvm/JvmCom") -> true
    path.startsWith("resolve/jvm/KotlinC") -> true
    path.startsWith("resolve/jvm/KotlinJavaPsiFacade.") -> true
    path.startsWith("resolve/jvm/modules/JavaModule\$") -> true
    path.startsWith("resolve/jvm/modules/JavaModule.") -> true
    path.startsWith("resolve/jvm/modules/JavaModuleInfo\$Companion.") -> true
    path.startsWith("resolve/jvm/modules/JavaModuleResolver.") -> true
    path.startsWith("resolve/jvm/multiplatform/OptionalAnnotationPackageFragmentProvider.") -> true
    path.startsWith("resolve/lazy/FileScopeFactory.") -> true
    path.startsWith("resolve/lazy/FileScopeProviderK") -> true
    path.startsWith("resolve/lazy/Fo") -> true
    path.startsWith("resolve/lazy/ResolveSession.") -> true
    path.startsWith("resolve/lazy/declarations/Cli") -> true
    path.startsWith("resolve/lazy/declarations/Co") -> true
    path.startsWith("resolve/lazy/declarations/DeclarationProviderFactory.") -> true
    path.startsWith("resolve/lazy/declarations/DeclarationProviderFactoryService.") -> true
    path.startsWith("resolve/lazy/declarations/FileBasedDeclarationProviderFactory.") -> true
    path.startsWith("resolve/lazy/declarations/Pa") -> true
    path.startsWith("resolve/multiplatform/I") -> true
    path.startsWith("resolve/rep") -> true
    path.startsWith("scripting/U") -> true
    path.startsWith("scripting/con") -> true
    path.startsWith("scripting/definitions/KotlinScriptDefinition.") -> true
    path.startsWith("scripting/definitions/ScriptCompilationConfigurationFromDefinitionK") -> true
    path.startsWith("scripting/definitions/ScriptDefinition.") -> true
    path.startsWith("scripting/definitions/ScriptDependenciesProvider.") -> true
    path.startsWith("scripting/definitions/ScriptP") -> true
    path.startsWith("scripting/extensions/ScriptExtraImportsProviderExtension.") -> true
    path.startsWith("scripting/resolve/KotlinScriptDefinitionFromAnnotatedTemplate.") -> true
    path.startsWith("scripting/resolve/RefineCompilationConfigurationKt.") -> true
    path.startsWith("scripting/resolve/Rep") -> true
    path.startsWith("scripting/resolve/Res") -> true
    path.startsWith("scripting/resolve/ScriptL") -> true
    path.startsWith("scripting/resolve/ScriptR") -> true
    path.startsWith("scripting/resolve/VirtualFileScriptSource.") -> true
    path.startsWith("serialization/deserialization/DeserializedPackageFragment.") -> true
    path.startsWith("serialization/konan/i") -> true
    path.startsWith("types/Des") -> true
    path.startsWith("types/KotlinTypeFactory.") -> true
    path.startsWith("types/expressions/ExpressionTypingCon") -> true
    path.startsWith("util/Li") -> true
    path.startsWith("utils/Met") -> true
    path.startsWith("utils/Pat") -> true
    path.startsWith("utils/Pla") -> true
    
    else -> false
  }
                }
                path.startsWith("com/intellij/") -> {
                  val path = path.removePrefix("com/intellij/")
                  when {
                  path.startsWith("psi/") -> {
                    val path = path.removePrefix("psi/")
                    when {
                    path.startsWith("controlFlow/ControlFlow") -> {
                      val path = path.removePrefix("controlFlow/ControlFlow")
                      when {
        path.startsWith("Factory.") -> true
        path.startsWith("Util.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Psi") -> {
                      val path = path.removePrefix("Psi")
                      when {
                      path.startsWith("Reference") -> {
                        val path = path.removePrefix("Reference")
                        when {
          path.startsWith("Base\$I") -> false
          path.startsWith("L") -> false
          path.startsWith("Pa") -> false
          path.startsWith("Service\$") -> false
          
          else -> true
        }
                      }
        path.startsWith("Anchor\$S") -> true
        path.startsWith("Anchor.") -> true
        path.startsWith("Annotation.") -> true
        path.startsWith("AnnotationMem") -> true
        path.startsWith("ArrayT") -> true
        path.startsWith("Assi") -> true
        path.startsWith("BinaryE") -> true
        path.startsWith("Cap") -> true
        path.startsWith("CaseLabelElement.") -> true
        path.startsWith("Cat") -> true
        path.startsWith("Class.") -> true
        path.startsWith("ClassI") -> true
        path.startsWith("ClassOwner.") -> true
        path.startsWith("ClassType\$S") -> true
        path.startsWith("ClassType.") -> true
        path.startsWith("CodeF") -> true
        path.startsWith("Comm") -> true
        path.startsWith("DiamondType.") -> true
        path.startsWith("DiamondTypeImpl.") -> true
        path.startsWith("Dir") -> true
        path.startsWith("Dis") -> true
        path.startsWith("DocumentL") -> true
        path.startsWith("DocumentManager.") -> true
        path.startsWith("Element.") -> true
        path.startsWith("ElementFactory.") -> true
        path.startsWith("ElementFi") -> true
        path.startsWith("ElementVisitor.") -> true
        path.startsWith("EnumConstant.") -> true
        path.startsWith("Expression.") -> true
        path.startsWith("Fie") -> true
        path.startsWith("File.") -> true
        path.startsWith("FileF") -> true
        path.startsWith("FileS") -> true
        path.startsWith("Fu") -> true
        path.startsWith("If") -> true
        path.startsWith("ImportL") -> true
        path.startsWith("ImportStatementB") -> true
        path.startsWith("ImportStaticR") -> true
        path.startsWith("Inf") -> true
        path.startsWith("Int") -> true
        path.startsWith("Inv") -> true
        path.startsWith("JavaCodeReferenceE") -> true
        path.startsWith("JavaF") -> true
        path.startsWith("JavaModule.") -> true
        path.startsWith("JavaP") -> true
        path.startsWith("JavaT") -> true
        path.startsWith("JvmS") -> true
        path.startsWith("LambdaExpressionT") -> true
        path.startsWith("LambdaP") -> true
        path.startsWith("LanguageInjectionHost\$I") -> true
        path.startsWith("LanguageInjectionHost.") -> true
        path.startsWith("Loca") -> true
        path.startsWith("Ma") -> true
        path.startsWith("Method.") -> true
        path.startsWith("MethodReferenceT") -> true
        path.startsWith("MethodReferenceUtil.") -> true
        path.startsWith("ModifierL") -> true
        path.startsWith("NameH") -> true
        path.startsWith("NameV") -> true
        path.startsWith("Named") -> true
        path.startsWith("Package.") -> true
        path.startsWith("Parameter.") -> true
        path.startsWith("ParserFacade.") -> true
        path.startsWith("Polya") -> true
        path.startsWith("Primi") -> true
        path.startsWith("RecordC") -> true
        path.startsWith("RecursiveElementWalkingVisitor.") -> true
        path.startsWith("ResolveHelper.") -> true
        path.startsWith("St") -> true
        path.startsWith("Sub") -> true
        path.startsWith("TreeChangeE") -> true
        path.startsWith("Type.") -> true
        path.startsWith("TypeE") -> true
        path.startsWith("TypeM") -> true
        path.startsWith("TypeParameter.") -> true
        path.startsWith("TypeParameterListO") -> true
        path.startsWith("Un") -> true
        path.startsWith("V") -> true
        path.startsWith("Wi") -> true
        
        else -> false
      }
                    }
                    path.startsWith("controlFlow/") -> {
                      val path = path.removePrefix("controlFlow/")
                      when {
        path.startsWith("Cont") -> true
        path.startsWith("L") -> true
        
        else -> false
      }
                    }
                    path.startsWith("codeStyle/") -> {
                      val path = path.removePrefix("codeStyle/")
                      when {
        path.startsWith("Co") -> true
        path.startsWith("JavaCodeStyleM") -> true
        path.startsWith("JavaFileCodeStyleFacade.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractFileViewProvider\$") -> false
      path.startsWith("B") -> false
      path.startsWith("ClassFileViewProvider\$") -> false
      path.startsWith("ClassT") -> false
      path.startsWith("Co") -> false
      path.startsWith("Cu") -> false
      path.startsWith("De") -> false
      path.startsWith("ElementD") -> false
      path.startsWith("EmptyR") -> false
      path.startsWith("EmptySubstitutor\$") -> false
      path.startsWith("Ex") -> false
      path.startsWith("FileC") -> false
      path.startsWith("GenericsUtil\$") -> false
      path.startsWith("Hin") -> false
      path.startsWith("I") -> false
      path.startsWith("JV") -> false
      path.startsWith("JavaC") -> false
      path.startsWith("JavaE") -> false
      path.startsWith("JavaRecursiveElementW") -> false
      path.startsWith("JavaRes") -> false
      path.startsWith("JvmC") -> false
      path.startsWith("LambdaUtil\$") -> false
      path.startsWith("LanguageA") -> false
      path.startsWith("LanguageSubstitutors\$") -> false
      path.startsWith("LiteralTextEscaper\$") -> false
      path.startsWith("M") -> false
      path.startsWith("N") -> false
      path.startsWith("O") -> false
      path.startsWith("Pa") -> false
      path.startsWith("PlainTextTokenTypes\$") -> false
      path.startsWith("ReferenceP") -> false
      path.startsWith("ResolveR") -> false
      path.startsWith("ResolveState\$") -> false
      path.startsWith("Resolvi") -> false
      path.startsWith("Se") -> false
      path.startsWith("SmartT") -> false
      path.startsWith("Str") -> false
      path.startsWith("StubBa") -> false
      path.startsWith("SyntaxTraverser\$1") -> false
      path.startsWith("SyntaxTraverser\$AS") -> false
      path.startsWith("SyntaxTraverser\$Api\$") -> false
      path.startsWith("SyntaxTraverser\$Api.") -> false
      path.startsWith("SyntaxTraverser\$ApiEx\$") -> false
      path.startsWith("SyntaxTraverser\$F") -> false
      path.startsWith("SyntaxTraverser\$L") -> false
      path.startsWith("SyntaxTraverser\$P") -> false
      path.startsWith("Synth") -> false
      path.startsWith("TokenType\$") -> false
      path.startsWith("Ty") -> false
      path.startsWith("W") -> false
      path.startsWith("augment/PsiE") -> false
      path.startsWith("compiled/ClassFileDecompilers\$Light\$") -> false
      path.startsWith("cs") -> false
      path.startsWith("filters/Co") -> false
      path.startsWith("filters/E") -> false
      path.startsWith("filters/N") -> false
      path.startsWith("filters/c") -> false
      path.startsWith("filters/element/ModifierFilter\$") -> false
      path.startsWith("filters/p") -> false
      path.startsWith("infos/Cl") -> false
      path.startsWith("infos/MethodCandidateInfo\$") -> false
      path.startsWith("javadoc/C") -> false
      path.startsWith("javadoc/J") -> false
      path.startsWith("javadoc/PsiDocC") -> false
      path.startsWith("javadoc/PsiDocTa") -> false
      path.startsWith("javadoc/PsiI") -> false
      path.startsWith("meta/PsiMetaO") -> false
      path.startsWith("meta/PsiP") -> false
      path.startsWith("meta/PsiW") -> false
      path.startsWith("p") -> false
      path.startsWith("scope/B") -> false
      path.startsWith("scope/ElementClassF") -> false
      path.startsWith("scope/ElementClassHint\$") -> false
      path.startsWith("scope/J") -> false
      path.startsWith("scope/M") -> false
      path.startsWith("scope/PatternResolveState\$") -> false
      path.startsWith("scope/Pr") -> false
      path.startsWith("scope/PsiC") -> false
      path.startsWith("scope/PsiScopeProcessor\$") -> false
      path.startsWith("scope/conflictResolvers/JavaMethodsConflictResolver\$") -> false
      path.startsWith("scope/processor/FilterE") -> false
      path.startsWith("scope/processor/MethodCandidatesProcessor\$") -> false
      path.startsWith("scope/processor/MethodResolveP") -> false
      path.startsWith("scope/processor/V") -> false
      path.startsWith("search/F") -> false
      path.startsWith("search/GlobalSearchScope\$") -> false
      path.startsWith("search/GlobalSearchScopeU") -> false
      path.startsWith("search/N") -> false
      path.startsWith("search/ProjectA") -> false
      path.startsWith("search/ProjectScopeB") -> false
      path.startsWith("search/ProjectScopeI") -> false
      path.startsWith("search/PsiElementProcessor\$CollectElementsW") -> false
      path.startsWith("search/PsiElementProcessor\$CollectF") -> false
      path.startsWith("search/PsiElementProcessorA") -> false
      path.startsWith("search/PsiN") -> false
      path.startsWith("search/searches/ExtensibleQueryFactory\$") -> false
      path.startsWith("stubs/BinaryFileStubBuilder.") -> false
      path.startsWith("stubs/DefaultStubBuilder\$") -> false
      path.startsWith("stubs/E") -> false
      path.startsWith("stubs/H") -> false
      path.startsWith("stubs/In") -> false
      path.startsWith("stubs/M") -> false
      path.startsWith("stubs/N") -> false
      path.startsWith("stubs/ObjectStubB") -> false
      path.startsWith("stubs/ObjectStubS") -> false
      path.startsWith("stubs/ObjectStubTree\$") -> false
      path.startsWith("stubs/PsiC") -> false
      path.startsWith("stubs/Se") -> false
      path.startsWith("stubs/Str") -> false
      path.startsWith("stubs/Stub.") -> false
      path.startsWith("stubs/StubBu") -> false
      path.startsWith("stubs/StubElementT") -> false
      path.startsWith("stubs/StubF") -> false
      path.startsWith("stubs/StubL") -> false
      path.startsWith("stubs/StubS") -> false
      path.startsWith("stubs/StubTe") -> false
      path.startsWith("stubs/U") -> false
      path.startsWith("ta") -> false
      path.startsWith("templateLanguages/I") -> false
      path.startsWith("templateLanguages/T") -> false
      
      else -> true
    }
                  }
                  path.startsWith("openapi/") -> {
                    val path = path.removePrefix("openapi/")
                    when {
                    path.startsWith("extensions/") -> {
                      val path = path.removePrefix("extensions/")
                      when {
                      path.startsWith("Extension") -> {
                        val path = path.removePrefix("Extension")
                        when {
                        path.startsWith("Point") -> {
                          val path = path.removePrefix("Point")
                          when {
            path.startsWith("\$") -> false
            path.startsWith("Ad") -> false
            path.startsWith("D") -> false
            path.startsWith("P") -> false
            path.startsWith("Util\$") -> false
            
            else -> true
          }
                        }
          path.startsWith("Descriptor\$") -> false
          path.startsWith("F") -> false
          path.startsWith("N") -> false
          
          else -> true
        }
                      }
        path.startsWith("B") -> false
        path.startsWith("C") -> false
        path.startsWith("D") -> false
        path.startsWith("I") -> false
        path.startsWith("LoadingOrder\$") -> false
        path.startsWith("Pl") -> false
        path.startsWith("S") -> false
        
        else -> true
      }
                    }
                    path.startsWith("editor/") -> {
                      val path = path.removePrefix("editor/")
                      when {
        path.startsWith("DocumentR") -> false
        path.startsWith("Re") -> false
        path.startsWith("a") -> false
        path.startsWith("c") -> false
        path.startsWith("event/B") -> false
        path.startsWith("event/DocumentE") -> false
        path.startsWith("ex/D") -> false
        path.startsWith("ex/L") -> false
        path.startsWith("ex/P") -> false
        path.startsWith("m") -> false
        
        else -> true
      }
                    }
      path.startsWith("D") -> false
      path.startsWith("F") -> false
      path.startsWith("application/Ac") -> false
      path.startsWith("application/AppU") -> false
      path.startsWith("application/As") -> false
      path.startsWith("application/B") -> false
      path.startsWith("application/E") -> false
      path.startsWith("application/ModalityI") -> false
      path.startsWith("application/ModalityStateL") -> false
      path.startsWith("application/N") -> false
      path.startsWith("application/ex/P") -> false
      path.startsWith("command/CommandT") -> false
      path.startsWith("command/U") -> false
      path.startsWith("command/u") -> false
      path.startsWith("components/ComponentC") -> false
      path.startsWith("components/ComponentManagerE") -> false
      path.startsWith("components/S") -> false
      path.startsWith("diagnostic/C") -> false
      path.startsWith("diagnostic/E") -> false
      path.startsWith("diagnostic/LoggerR") -> false
      path.startsWith("diagnostic/R") -> false
      path.startsWith("module/ModuleS") -> false
      path.startsWith("progress/C") -> false
      path.startsWith("progress/E") -> false
      path.startsWith("progress/J") -> false
      path.startsWith("progress/Pe") -> false
      path.startsWith("progress/Proc") -> false
      path.startsWith("progress/ProgressI") -> false
      path.startsWith("progress/Progressi") -> false
      path.startsWith("progress/S") -> false
      path.startsWith("progress/W") -> false
      path.startsWith("project/DumbA") -> false
      path.startsWith("project/DumbService\$") -> false
      path.startsWith("project/DumbU") -> false
      path.startsWith("project/I") -> false
      path.startsWith("project/Po") -> false
      path.startsWith("roots/ContentIteratorE") -> false
      path.startsWith("roots/LanguageLevelProjectExtension\$") -> false
      path.startsWith("roots/ProjectRootModificationTracker\$") -> false
      path.startsWith("ui") -> false
      
      else -> true
    }
                  }
                  path.startsWith("ide/plugins/") -> {
                    val path = path.removePrefix("ide/plugins/")
                    when {
      path.startsWith("C") -> true
      path.startsWith("Di") -> true
      path.startsWith("IdeaPluginDescriptorImpl.") -> true
      path.startsWith("PluginDescriptorLoader.") -> true
      path.startsWith("PluginManagerCore.") -> true
      path.startsWith("PluginU") -> true
      
      else -> false
    }
                  }
                  path.startsWith("patterns/") -> {
                    val path = path.removePrefix("patterns/")
                    when {
      path.startsWith("C") -> true
      path.startsWith("ElementPattern.") -> true
      path.startsWith("ElementPatternC") -> true
      path.startsWith("InitialPatternCondition.") -> true
      path.startsWith("ObjectPattern.") -> true
      path.startsWith("PatternCondition.") -> true
      path.startsWith("PsiElementPattern.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("diagnostic/") -> {
                    val path = path.removePrefix("diagnostic/")
                    when {
      path.startsWith("EventWatcher.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("core/Core") -> {
                    val path = path.removePrefix("core/Core")
                    when {
      path.startsWith("A") -> true
      path.startsWith("F") -> true
      path.startsWith("JavaD") -> true
      path.startsWith("JavaF") -> true
      path.startsWith("PsiP") -> true
      
      else -> false
    }
                  }
                  path.startsWith("serialization/") -> {
                    val path = path.removePrefix("serialization/")
                    when {
      path.startsWith("M") -> true
      path.startsWith("PropertyCollector.") -> true
      
      else -> false
    }
                  }
    path.startsWith("AbstractBundle\$") -> false
    path.startsWith("B") -> false
    path.startsWith("DynamicBundle\$1") -> false
    path.startsWith("DynamicBundle\$D") -> false
    path.startsWith("DynamicBundle.") -> false
    path.startsWith("U") -> false
    path.startsWith("codeW") -> false
    path.startsWith("con") -> false
    path.startsWith("core/JavaP") -> false
    path.startsWith("extapi/psi/P") -> false
    path.startsWith("extapi/psi/StubBasedPsiElementBase\$") -> false
    path.startsWith("f") -> false
    path.startsWith("ic") -> false
    path.startsWith("ide/IconP") -> false
    path.startsWith("model/B") -> false
    path.startsWith("model/S") -> false
    path.startsWith("model/psi/PsiE") -> false
    path.startsWith("model/psi/PsiSymbolR") -> false
    path.startsWith("model/psi/U") -> false
    path.startsWith("navigation/C") -> false
    path.startsWith("navigation/ItemPresentation.") -> false
    path.startsWith("navigation/ItemPresentationProvider.") -> false
    path.startsWith("navigation/N") -> false
    path.startsWith("no") -> false
    path.startsWith("pom/N") -> false
    path.startsWith("pom/PomI") -> false
    path.startsWith("pom/PomMa") -> false
    path.startsWith("pom/PomModelA") -> false
    path.startsWith("pom/PomN") -> false
    path.startsWith("pom/PomR") -> false
    path.startsWith("pom/PomTa") -> false
    path.startsWith("pom/Ps") -> false
    path.startsWith("pom/e") -> false
    path.startsWith("pom/tree/T") -> false
    path.startsWith("pom/tree/events/T") -> false
    path.startsWith("r") -> false
    path.startsWith("serviceContainer/L") -> false
    path.startsWith("testFramework/LightVirtualFile\$") -> false
    path.startsWith("testFramework/LightVirtualFileB") -> false
    path.startsWith("testFramework/R") -> false
    path.startsWith("ui/D") -> false
    path.startsWith("ui/IconManagerH") -> false
    path.startsWith("ui/i") -> false
    
    else -> true
  }
                }
                path.startsWith("kotlin/script/experimental/") -> {
                  val path = path.removePrefix("kotlin/script/experimental/")
                  when {
                  path.startsWith("jsr223/KotlinJsr223DefaultScript") -> {
                    val path = path.removePrefix("jsr223/KotlinJsr223DefaultScript")
                    when {
      path.startsWith("CompilationConfiguration\$1\$2") -> true
      path.startsWith("CompilationConfiguration\$1\$3") -> true
      path.startsWith("CompilationConfiguration\$1.") -> true
      path.startsWith("EngineFactory\$getScriptEngine\$1\$1.") -> true
      path.startsWith("EngineFactory\$getScriptEngine\$1.") -> true
      path.startsWith("EngineFactory.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("jvmhost/jsr223/KotlinJsr223ScriptEngineImpl\$") -> {
                    val path = path.removePrefix("jvmhost/jsr223/KotlinJsr223ScriptEngineImpl\$")
                    when {
      path.startsWith("compilationConfiguration\$2\$1\$2") -> true
      path.startsWith("compilationConfiguration\$2\$1.") -> true
      path.startsWith("evaluationConfiguration\$2\$1.") -> true
      path.startsWith("jsr223HostConfiguration\$1\$1.") -> true
      path.startsWith("jsr223HostConfiguration\$1.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("jvm") -> {
                    val path = path.removePrefix("jvm")
                    when {
                    path.startsWith("host/") -> {
                      val path = path.removePrefix("host/")
                      when {
                      path.startsWith("BasicJvmScript") -> {
                        val path = path.removePrefix("BasicJvmScript")
                        when {
          path.startsWith("ingHostK") -> true
          
          else -> false
        }
                      }
        path.startsWith("C") -> false
        path.startsWith("JvmScriptC") -> false
        path.startsWith("JvmScriptEvaluationConfigurationBuilder\$") -> false
        path.startsWith("jsr223/Jsr223CompilationConfigurationBuilder\$") -> false
        path.startsWith("jsr223/Jsr223EvaluationConfigurationBuilder\$") -> false
        path.startsWith("jsr223/Jsr223HostConfigurationBuilder\$") -> false
        path.startsWith("jsr223/KotlinJsr223I") -> false
        path.startsWith("repl/JvmReplEvaluator\$eval\$1\$1") -> false
        path.startsWith("repl/JvmReplEvaluator\$eval\$1\$h") -> false
        path.startsWith("repl/JvmReplEvaluatorS") -> false
        path.startsWith("repl/L") -> false
        path.startsWith("repl/R") -> false
        
        else -> true
      }
                    }
      path.startsWith("/BasicJvmReplEvaluator\$eval\$1") -> false
      path.startsWith("/BasicJvmReplEvaluator.") -> false
      path.startsWith("/BasicJvmScriptEvaluator\$") -> false
      path.startsWith("/C") -> false
      path.startsWith("/G") -> false
      path.startsWith("/Js") -> false
      path.startsWith("/JvmD") -> false
      path.startsWith("/JvmGetScriptingClass\$") -> false
      path.startsWith("/JvmScriptCompilationConfigurationBuilder\$") -> false
      path.startsWith("/JvmScriptEvaluationConfigurationBuilder\$") -> false
      path.startsWith("/JvmScriptingHostConfigurationBuilder\$") -> false
      path.startsWith("/K") -> false
      path.startsWith("/R") -> false
      path.startsWith("/c") -> false
      path.startsWith("/util/A") -> false
      path.startsWith("/util/C") -> false
      path.startsWith("/util/D") -> false
      path.startsWith("/util/I") -> false
      path.startsWith("/util/JvmClassLoaderUtilKt\$") -> false
      path.startsWith("/util/R") -> false
      path.startsWith("/util/S") -> false
      
      else -> true
    }
                  }
    
    else -> true
  }
                }
  path.startsWith("org/i") -> true
  path.startsWith("org/p") -> true
  
  else -> false
} 
      
      // Conflicts:
      fun isConflicting(path: String) = when {
                path.startsWith("org/jetbrains/kotlin/") -> {
                  val path = path.removePrefix("org/jetbrains/kotlin/")
                  when {
                  path.startsWith("scripting/compiler/plugin/") -> {
                    val path = path.removePrefix("scripting/compiler/plugin/")
                    when {
      path.startsWith("A") -> false
      path.startsWith("C") -> false
      path.startsWith("E") -> false
      path.startsWith("JvmCliScriptEvaluationExtension\$") -> false
      path.startsWith("JvmCliScriptEvaluationExtensionK") -> false
      path.startsWith("ScriptingCommandLineProcessor\$") -> false
      path.startsWith("ScriptingCompilerConfigurationExtension\$") -> false
      path.startsWith("definitions/CliScriptDefinitionProvider\$") -> false
      path.startsWith("definitions/CliScriptReportSink\$") -> false
      path.startsWith("dependencies/ScriptsCompilationDependenciesKt\$") -> false
      path.startsWith("extensions/ScriptingProcessSourcesBeforeCompilingExtension\$") -> false
      path.startsWith("services/Fir2IrScriptConfiguratorExtensionImpl\$c") -> false
      path.startsWith("services/FirScriptConfigurati") -> false
      path.startsWith("services/FirScriptDefinitionProviderService\$Companion\$") -> false
      path.startsWith("services/S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("fir/") -> {
                    val path = path.removePrefix("fir/")
                    when {
                    path.startsWith("scopes/impl/Fir") -> {
                      val path = path.removePrefix("scopes/impl/Fir")
                      when {
        path.startsWith("Sco") -> false
        
        else -> true
      }
                    }
                    path.startsWith("expressions/") -> {
                      val path = path.removePrefix("expressions/")
                      when {
                      path.startsWith("builder/Fir") -> {
                        val path = path.removePrefix("builder/Fir")
                        when {
          path.startsWith("AnnotationArgumentMappingBuilder.") -> true
          path.startsWith("AnnotationBuilder.") -> true
          path.startsWith("Cons") -> true
          
          else -> false
        }
                      }
        path.startsWith("ExhaustivenessStatus\$") -> false
        path.startsWith("ExhaustivenessStatusK") -> false
        path.startsWith("FirArgumentU") -> false
        path.startsWith("FirConte") -> false
        path.startsWith("FirEm") -> false
        path.startsWith("FirOperation\$") -> false
        path.startsWith("impl/FirNo") -> false
        
        else -> true
      }
                    }
                    path.startsWith("declarations/") -> {
                      val path = path.removePrefix("declarations/")
                      when {
                      path.startsWith("impl/Fir") -> {
                        val path = path.removePrefix("impl/Fir")
                        when {
          path.startsWith("ErrorC") -> false
          
          else -> true
        }
                      }
                      path.startsWith("FirDeclaration") -> {
                        val path = path.removePrefix("FirDeclaration")
                        when {
                        path.startsWith("Origin\$") -> {
                          val path = path.removePrefix("Origin\$")
                          when {
            path.startsWith("B") -> true
            path.startsWith("D") -> true
            path.startsWith("I") -> true
            path.startsWith("L") -> true
            path.startsWith("SubstitutionOverride.") -> true
            path.startsWith("Synthetic\$Da") -> true
            path.startsWith("Synthetic\$V") -> true
            path.startsWith("Synthetic.") -> true
            path.startsWith("W") -> true
            
            else -> false
          }
                        }
          path.startsWith(".") -> true
          path.startsWith("A") -> true
          path.startsWith("DataK") -> true
          path.startsWith("DataRegistry\$D") -> true
          path.startsWith("DataRegistry.") -> true
          path.startsWith("Origin.") -> true
          path.startsWith("S") -> true
          
          else -> false
        }
                      }
                      path.startsWith("builder/Fir") -> {
                        val path = path.removePrefix("builder/Fir")
                        when {
          path.startsWith("ConstructorBuilder.") -> true
          path.startsWith("ContextReceiverBuilder.") -> true
          path.startsWith("DefaultSetterValueParameterBuilder.") -> true
          path.startsWith("FieldBuilder.") -> true
          path.startsWith("Fu") -> true
          path.startsWith("ImportBuilder.") -> true
          path.startsWith("PropertyAccessorBuilder.") -> true
          path.startsWith("PropertyBuilder.") -> true
          path.startsWith("ReceiverParameterBuilder.") -> true
          path.startsWith("RegularClassBuilder.") -> true
          path.startsWith("ResolvedImportBuilder.") -> true
          path.startsWith("SimpleFunctionBuilder.") -> true
          path.startsWith("TypeParameterBuilder.") -> true
          path.startsWith("ValueParameterBuilder.") -> true
          path.startsWith("Var") -> true
          
          else -> false
        }
                      }
                      path.startsWith("Fir") -> {
                        val path = path.removePrefix("Fir")
                        when {
          path.startsWith("Contro") -> false
          path.startsWith("De") -> false
          path.startsWith("G") -> false
          path.startsWith("In") -> false
          path.startsWith("ResolvePhaseK") -> false
          path.startsWith("ResolvedToPhaseState\$") -> false
          path.startsWith("To") -> false
          path.startsWith("TypeS") -> false
          path.startsWith("ValueC") -> false
          path.startsWith("VersionRequirementsTableKey.") -> false
          
          else -> true
        }
                      }
        path.startsWith("DeclarationUtilsKt.") -> true
        path.startsWith("DeprecationUtilsKt.") -> true
        path.startsWith("DeprecationsPe") -> true
        path.startsWith("DeprecationsProvider.") -> true
        path.startsWith("G") -> true
        path.startsWith("In") -> true
        path.startsWith("U") -> true
        path.startsWith("synthetic/FirSyntheticProperty.") -> true
        path.startsWith("synthetic/FirSyntheticPropertyA") -> true
        path.startsWith("utils/FirD") -> true
        
        else -> false
      }
                    }
                    path.startsWith("resolve/providers/impl/Fir") -> {
                      val path = path.removePrefix("resolve/providers/impl/Fir")
                      when {
        path.startsWith("BuiltinSymbolProvider\$BuiltInsPackageFragment\$l") -> false
        path.startsWith("TypeResolverImpl\$C") -> false
        path.startsWith("TypeResolverImpl\$P") -> false
        
        else -> true
      }
                    }
                    path.startsWith("resolve/") -> {
                      val path = path.removePrefix("resolve/")
                      when {
                      path.startsWith("providers/Fir") -> {
                        val path = path.removePrefix("providers/Fir")
                        when {
          path.startsWith("Ca") -> false
          path.startsWith("CompositeCachedSymbolNamesProvider\$") -> false
          path.startsWith("D") -> false
          path.startsWith("E") -> false
          path.startsWith("N") -> false
          path.startsWith("SymbolNamesProviderK") -> false
          
          else -> true
        }
                      }
                      path.startsWith("transformers/") -> {
                        val path = path.removePrefix("transformers/")
                        when {
          path.startsWith("FirImportResolveT") -> true
          path.startsWith("Im") -> true
          path.startsWith("PackageResolutionResult\$P") -> true
          path.startsWith("PackageResolutionResult.") -> true
          path.startsWith("Ph") -> true
          path.startsWith("R") -> true
          path.startsWith("Sc") -> true
          
          else -> false
        }
                      }
                      path.startsWith("calls/") -> {
                        val path = path.removePrefix("calls/")
                        when {
          path.startsWith("AbstractCa") -> true
          path.startsWith("ArgumentT") -> true
          path.startsWith("Arguments") -> true
          path.startsWith("CallI") -> true
          path.startsWith("Candidate.") -> true
          path.startsWith("CandidateFactoryK") -> true
          path.startsWith("CheckerSink.") -> true
          path.startsWith("Er") -> true
          path.startsWith("FirN") -> true
          path.startsWith("InapplicableW") -> true
          path.startsWith("Nu") -> true
          path.startsWith("ResolutionC") -> true
          path.startsWith("ResolutionD") -> true
          path.startsWith("ResolvedCallArgument.") -> true
          path.startsWith("Unsa") -> true
          path.startsWith("Unst") -> true
          
          else -> false
        }
                      }
                      path.startsWith("inference/") -> {
                        val path = path.removePrefix("inference/")
                        when {
          path.startsWith("ConeTypeVariableForLambdaR") -> true
          path.startsWith("InferenceU") -> true
          path.startsWith("LambdaW") -> true
          path.startsWith("PostponedArgumentsK") -> true
          path.startsWith("PostponedR") -> true
          path.startsWith("Res") -> true
          path.startsWith("model/ConeA") -> true
          path.startsWith("model/ConeExpl") -> true
          path.startsWith("model/ConeR") -> true
          
          else -> false
        }
                      }
                      path.startsWith("diagnostics/Cone") -> {
                        val path = path.removePrefix("diagnostics/Cone")
                        when {
          path.startsWith("Ambigui") -> true
          path.startsWith("Fo") -> true
          path.startsWith("Ou") -> true
          path.startsWith("UnresolvedTypeQualifierError.") -> true
          path.startsWith("Vi") -> true
          path.startsWith("W") -> true
          
          else -> false
        }
                      }
        path.startsWith("BodyResolveComponents.") -> true
        path.startsWith("De") -> true
        path.startsWith("DoubleColonLHS.") -> true
        path.startsWith("FirDoubleColonExpressionResolver.") -> true
        path.startsWith("FirQ") -> true
        path.startsWith("FirSamConversionTransformerExtension\$F") -> true
        path.startsWith("FirSamConversionTransformerExtension.") -> true
        path.startsWith("FirSamR") -> true
        path.startsWith("FirTypeResolu") -> true
        path.startsWith("FirTypeResolver.") -> true
        path.startsWith("ImplicitIntegerCoercionK") -> true
        path.startsWith("L") -> true
        path.startsWith("ResolveUtilsKt.") -> true
        path.startsWith("ScopeS") -> true
        path.startsWith("ScopeUtilsKt.") -> true
        path.startsWith("SupertypeSupplier.") -> true
        path.startsWith("SupertypeUtilsKt.") -> true
        path.startsWith("TypeExpansionUtilsKt.") -> true
        path.startsWith("dfa/DataFlowV") -> true
        path.startsWith("dfa/Im") -> true
        path.startsWith("dfa/LogicSystemKt\$") -> true
        path.startsWith("dfa/RealVariable.") -> true
        path.startsWith("dfa/Stat") -> true
        path.startsWith("providers/impl/B") -> true
        path.startsWith("substitution/Ch") -> true
        path.startsWith("substitution/ConeSubstitutor\$") -> true
        path.startsWith("substitution/ConeSubstitutor.") -> true
        path.startsWith("substitution/ConeSubstitutorByMap.") -> true
        path.startsWith("substitution/S") -> true
        
        else -> false
      }
                    }
                    path.startsWith("types/") -> {
                      val path = path.removePrefix("types/")
                      when {
                      path.startsWith("Cone") -> {
                        val path = path.removePrefix("Cone")
                        when {
                        path.startsWith("Type") -> {
                          val path = path.removePrefix("Type")
                          when {
            path.startsWith("Context.") -> true
            path.startsWith("Intersector.") -> true
            path.startsWith("Pa") -> true
            path.startsWith("Projection\$") -> true
            path.startsWith("Projection.") -> true
            path.startsWith("UtilsKt.") -> true
            path.startsWith("VariableT") -> true
            
            else -> false
          }
                        }
          path.startsWith("Attributes\$") -> true
          path.startsWith("Attributes.") -> true
          path.startsWith("B") -> true
          path.startsWith("ClassLikeT") -> true
          path.startsWith("D") -> true
          path.startsWith("E") -> true
          path.startsWith("FlexibleType.") -> true
          path.startsWith("InferenceContext.") -> true
          path.startsWith("IntegerConstantOperatorType.") -> true
          path.startsWith("IntegerConstantOperatorTypeImpl.") -> true
          path.startsWith("IntegerLiteralType.") -> true
          path.startsWith("Inter") -> true
          path.startsWith("KotlinType.") -> true
          path.startsWith("L") -> true
          path.startsWith("N") -> true
          path.startsWith("Si") -> true
          
          else -> false
        }
                      }
                      path.startsWith("Fir") -> {
                        val path = path.removePrefix("Fir")
                        when {
          path.startsWith("C") -> false
          path.startsWith("FunctionTypeKindServiceI") -> false
          path.startsWith("TypeProjectionC") -> false
          path.startsWith("TypeRefC") -> false
          
          else -> true
        }
                      }
        path.startsWith("A") -> true
        path.startsWith("CompilerConeAttributes\$ContextFunctionTypeParams.") -> true
        path.startsWith("CompilerConeAttributes\$Ext") -> true
        path.startsWith("FunctionalTypeUtilsKt.") -> true
        path.startsWith("I") -> true
        path.startsWith("TypeComponentsK") -> true
        path.startsWith("TypeCon") -> true
        path.startsWith("TypeUtilsKt.") -> true
        path.startsWith("builder/FirResolvedTypeRefBuilder.") -> true
        path.startsWith("i") -> true
        
        else -> false
      }
                    }
                    path.startsWith("symbols/impl/Fir") -> {
                      val path = path.removePrefix("symbols/impl/Fir")
                      when {
        path.startsWith("ClassLikeSymbolK") -> false
        
        else -> true
      }
                    }
                    path.startsWith("extensions/Fir") -> {
                      val path = path.removePrefix("extensions/Fir")
                      when {
                      path.startsWith("Extension") -> {
                        val path = path.removePrefix("Extension")
                        when {
          path.startsWith("\$") -> true
          path.startsWith(".") -> true
          path.startsWith("R") -> true
          path.startsWith("Service.") -> true
          path.startsWith("ServiceK") -> true
          path.startsWith("SessionComponent\$F") -> true
          path.startsWith("SessionComponent.") -> true
          path.startsWith("SessionComponentK") -> true
          
          else -> false
        }
                      }
        path.startsWith("An") -> true
        path.startsWith("AssignExpressionAltererExtension\$F") -> true
        path.startsWith("AssignExpressionAltererExtension.") -> true
        path.startsWith("DeclarationGenerationExtension\$F") -> true
        path.startsWith("DeclarationGenerationExtension.") -> true
        path.startsWith("DeclarationGenerationExtensionK") -> true
        path.startsWith("DeclarationP") -> true
        path.startsWith("DeclarationsForMetadataProviderExtension\$F") -> true
        path.startsWith("DeclarationsForMetadataProviderExtension.") -> true
        path.startsWith("ExpressionResolutionExtension\$F") -> true
        path.startsWith("ExpressionResolutionExtension.") -> true
        path.startsWith("FunctionTypeKindExtension\$F") -> true
        path.startsWith("FunctionTypeKindExtension.") -> true
        path.startsWith("RegisteredPluginAnnotations.") -> true
        path.startsWith("RegisteredPluginAnnotationsK") -> true
        path.startsWith("StatusTransformerExtension\$F") -> true
        path.startsWith("StatusTransformerExtension.") -> true
        path.startsWith("SupertypeGenerationExtension\$F") -> true
        path.startsWith("SupertypeGenerationExtension.") -> true
        path.startsWith("TypeAttributeExtension\$F") -> true
        path.startsWith("TypeAttributeExtension.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("references/") -> {
                      val path = path.removePrefix("references/")
                      when {
                      path.startsWith("Fir") -> {
                        val path = path.removePrefix("Fir")
                        when {
          path.startsWith("NamedReferenceW") -> false
          
          else -> true
        }
                      }
        path.startsWith("i") -> true
        
        else -> false
      }
                    }
                    path.startsWith("scopes/") -> {
                      val path = path.removePrefix("scopes/")
                      when {
                      path.startsWith("impl/") -> {
                        val path = path.removePrefix("impl/")
                        when {
          path.startsWith("F") -> false
          
          else -> true
        }
                      }
                      path.startsWith("Fir") -> {
                        val path = path.removePrefix("Fir")
                        when {
          path.startsWith("ContainingNamesAwareScope.") -> true
          path.startsWith("KotlinScopeProvider.") -> true
          path.startsWith("KotlinScopeProviderK") -> true
          path.startsWith("N") -> true
          path.startsWith("OverrideC") -> true
          path.startsWith("OverrideService.") -> true
          path.startsWith("OverrideServiceK") -> true
          path.startsWith("Scope.") -> true
          path.startsWith("ScopeKt.") -> true
          path.startsWith("ScopeP") -> true
          path.startsWith("TypeP") -> true
          path.startsWith("TypeScope\$") -> true
          path.startsWith("TypeScope.") -> true
          path.startsWith("TypeScopeKt.") -> true
          
          else -> false
        }
                      }
        path.startsWith("M") -> true
        path.startsWith("ProcessorAction.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("analysis/checkers/") -> {
                      val path = path.removePrefix("analysis/checkers/")
                      when {
        path.startsWith("CommonL") -> true
        path.startsWith("ExtendedL") -> true
        path.startsWith("La") -> true
        path.startsWith("Op") -> true
        path.startsWith("conf") -> true
        path.startsWith("context/CheckerContext.") -> true
        path.startsWith("expression/FirExpressionChecker.") -> true
        path.startsWith("extended/A") -> true
        path.startsWith("extended/CanBeR") -> true
        
        else -> false
      }
                    }
      path.startsWith("B") -> true
      path.startsWith("Cl") -> true
      path.startsWith("CopyUtilsKt.") -> true
      path.startsWith("DelegatedWrapperData.") -> true
      path.startsWith("DependencyListForCliModule\$B") -> true
      path.startsWith("DependencyListForCliModule\$Companion.") -> true
      path.startsWith("DependencyListForCliModule.") -> true
      path.startsWith("Ef") -> true
      path.startsWith("FirAn") -> true
      path.startsWith("FirEl") -> true
      path.startsWith("FirExpr") -> true
      path.startsWith("FirFi") -> true
      path.startsWith("FirFunctionTy") -> true
      path.startsWith("FirGenerate") -> true
      path.startsWith("FirImpl") -> true
      path.startsWith("FirLab") -> true
      path.startsWith("FirLanguageSettingsComponentK") -> true
      path.startsWith("FirLook") -> true
      path.startsWith("FirModuleData.") -> true
      path.startsWith("FirModuleDataK") -> true
      path.startsWith("FirN") -> true
      path.startsWith("FirP") -> true
      path.startsWith("FirSession\$C") -> true
      path.startsWith("FirSession.") -> true
      path.startsWith("FirSessionC") -> true
      path.startsWith("FirTarget.") -> true
      path.startsWith("FirVisibilityChecker.") -> true
      path.startsWith("FirVisibilityCheckerKt.") -> true
      path.startsWith("Mu") -> true
      path.startsWith("Se") -> true
      path.startsWith("U") -> true
      path.startsWith("analysis/C") -> true
      path.startsWith("analysis/collectors/components/A") -> true
      path.startsWith("analysis/collectors/components/L") -> true
      path.startsWith("analysis/diagnostics/FirErrors.") -> true
      path.startsWith("analysis/extensions/FirAdditionalCheckersExtension\$F") -> true
      path.startsWith("analysis/extensions/FirAdditionalCheckersExtension.") -> true
      path.startsWith("backend/ConversionUtilsKt.") -> true
      path.startsWith("backend/Fir2IrA") -> true
      path.startsWith("backend/Fir2IrComponents.") -> true
      path.startsWith("backend/Fir2IrConf") -> true
      path.startsWith("backend/Fir2IrExtensions.") -> true
      path.startsWith("backend/Fir2IrPluginContext.") -> true
      path.startsWith("backend/Fir2IrScriptConfiguratorExtension\$F") -> true
      path.startsWith("backend/Fir2IrScriptConfiguratorExtension.") -> true
      path.startsWith("backend/jvm/FirJvmBackendC") -> true
      path.startsWith("backend/jvm/FirJvmBackendExtension.") -> true
      path.startsWith("backend/jvm/JvmFir2IrExtensions.") -> true
      path.startsWith("builder/FirBuilderDslK") -> true
      path.startsWith("builder/FirScriptConfiguratorExtension\$F") -> true
      path.startsWith("builder/FirScriptConfiguratorExtension.") -> true
      path.startsWith("builder/FirScriptConfiguratorExtensionK") -> true
      path.startsWith("caches/FirCache.") -> true
      path.startsWith("caches/FirCacheW") -> true
      path.startsWith("caches/FirCachesFactory.") -> true
      path.startsWith("caches/FirCachesFactoryKt.") -> true
      path.startsWith("caches/FirL") -> true
      path.startsWith("contracts/F") -> true
      path.startsWith("contracts/i") -> true
      path.startsWith("deserialization/AbstractAnnotationDeserializer.") -> true
      path.startsWith("deserialization/ClassDeserializationKt.") -> true
      path.startsWith("deserialization/FirB") -> true
      path.startsWith("deserialization/FirConstDeserializer.") -> true
      path.startsWith("deserialization/FirDeserializationC") -> true
      path.startsWith("deserialization/FirM") -> true
      path.startsWith("diagnostics/ConeAmbiguousF") -> true
      path.startsWith("diagnostics/ConeDiagnostic.") -> true
      path.startsWith("diagnostics/ConeSi") -> true
      path.startsWith("diagnostics/ConeSt") -> true
      path.startsWith("diagnostics/ConeUne") -> true
      path.startsWith("diagnostics/D") -> true
      path.startsWith("extensions/BunchOfRegisteredExtensions.") -> true
      path.startsWith("extensions/DeclarationGenerationContext\$") -> true
      path.startsWith("extensions/predicate/AbstractPredicate.") -> true
      path.startsWith("i") -> true
      path.startsWith("java/FirJavaElementFinder.") -> true
      path.startsWith("java/FirJavaFacade.") -> true
      path.startsWith("java/FirJavaFacadeF") -> true
      path.startsWith("pipeline/A") -> true
      path.startsWith("pipeline/ConvertToIrKt.") -> true
      path.startsWith("pipeline/F") -> true
      path.startsWith("pipeline/M") -> true
      path.startsWith("serialization/FirAd") -> true
      path.startsWith("session/IncrementalC") -> true
      path.startsWith("session/e") -> true
      path.startsWith("symbols/ConeClassL") -> true
      path.startsWith("symbols/ConeClassifierLookupTag.") -> true
      path.startsWith("symbols/ConeT") -> true
      path.startsWith("symbols/FirBasedSymbol.") -> true
      path.startsWith("symbols/FirLazyDeclarationResolverK") -> true
      path.startsWith("symbols/impl/C") -> true
      path.startsWith("symbols/impl/L") -> true
      path.startsWith("symbols/impl/U") -> true
      path.startsWith("util/PersistentM") -> true
      path.startsWith("utils/exceptions/FirExceptionUtilsKt.") -> true
      path.startsWith("visitors/FirDefaultVisitor.") -> true
      path.startsWith("visitors/FirT") -> true
      path.startsWith("visitors/FirV") -> true
      
      else -> false
    }
                  }
                  path.startsWith("library/") -> {
                    val path = path.removePrefix("library/")
                    when {
                    path.startsWith("metadata/KlibMetadata") -> {
                      val path = path.removePrefix("metadata/KlibMetadata")
                      when {
        path.startsWith("Ca") -> true
        path.startsWith("D") -> true
        path.startsWith("F") -> true
        path.startsWith("Mo") -> true
        path.startsWith("PackageFragment.") -> true
        path.startsWith("ProtoBuf\$Header.") -> true
        path.startsWith("Serialize") -> true
        
        else -> false
      }
                    }
                    path.startsWith("KotlinLibrary") -> {
                      val path = path.removePrefix("KotlinLibrary")
                      when {
        path.startsWith(".") -> true
        path.startsWith("K") -> true
        path.startsWith("Layout.") -> true
        path.startsWith("V") -> true
        path.startsWith("W") -> true
        
        else -> false
      }
                    }
      path.startsWith("C") -> false
      path.startsWith("IrKotlinLibraryLayout\$") -> false
      path.startsWith("KotlinAbiVersion\$") -> false
      path.startsWith("KotlinAbiVersionK") -> false
      path.startsWith("KotlinIrSignatureVersion\$") -> false
      path.startsWith("KotlinIrSignatureVersionK") -> false
      path.startsWith("L") -> false
      path.startsWith("MetadataKotlinLibraryLayout\$") -> false
      path.startsWith("R") -> false
      path.startsWith("SearchPathResolver\$") -> false
      path.startsWith("SingleFileResolveKt\$") -> false
      path.startsWith("SingleK") -> false
      path.startsWith("ToolingSingleFileKlibResolveStrategy\$") -> false
      path.startsWith("UnresolvedLibraryK") -> false
      path.startsWith("abi/AbiCompoundName\$") -> false
      path.startsWith("abi/AbiQualifiedName\$") -> false
      path.startsWith("abi/AbiReadingFilter\$E") -> false
      path.startsWith("abi/AbiReadingFilter\$N") -> false
      path.startsWith("abi/AbiRenderingSettings\$") -> false
      path.startsWith("abi/AbiSignatureVersion\$") -> false
      path.startsWith("abi/LibraryAbiR") -> false
      path.startsWith("metadata/C") -> false
      path.startsWith("metadata/DeserializedS") -> false
      path.startsWith("metadata/KlibDeserializedContainerSourceK") -> false
      path.startsWith("metadata/resolver/KotlinLibraryResolverK") -> false
      path.startsWith("resolver/D") -> false
      
      else -> true
    }
                  }
                  path.startsWith("load/java/structure/") -> {
                    val path = path.removePrefix("load/java/structure/")
                    when {
                    path.startsWith("Java") -> {
                      val path = path.removePrefix("Java")
                      when {
        path.startsWith("ElementsKt\$") -> false
        path.startsWith("Lo") -> false
        path.startsWith("N") -> false
        path.startsWith("TypeParameterL") -> false
        path.startsWith("Types") -> false
        
        else -> true
      }
                    }
      
      else -> true
    }
                  }
                  path.startsWith("ir/expressions/") -> {
                    val path = path.removePrefix("ir/expressions/")
                    when {
      path.startsWith("IrStatementC") -> false
      path.startsWith("IrStatementOrigin\$") -> false
      path.startsWith("IrStatementOriginK") -> false
      
      else -> true
    }
                  }
                  path.startsWith("descriptors/") -> {
                    val path = path.removePrefix("descriptors/")
                    when {
                    path.startsWith("annotations/Annotat") -> {
                      val path = path.removePrefix("annotations/Annotat")
                      when {
                      path.startsWith("ion") -> {
                        val path = path.removePrefix("ion")
                        when {
          path.startsWith("Descriptor.") -> true
          path.startsWith("DescriptorI") -> true
          path.startsWith("Us") -> true
          path.startsWith("s\$Companion.") -> true
          path.startsWith("s.") -> true
          path.startsWith("sK") -> true
          
          else -> false
        }
                      }
        path.startsWith("edI") -> true
        
        else -> false
      }
                    }
      path.startsWith("Cal") -> true
      path.startsWith("ClassC") -> true
      path.startsWith("ClassD") -> true
      path.startsWith("ClassKind.") -> true
      path.startsWith("Classi") -> true
      path.startsWith("Constr") -> true
      path.startsWith("DeclarationDescriptor.") -> true
      path.startsWith("DeclarationDescriptorN") -> true
      path.startsWith("DeclarationDescriptorV") -> true
      path.startsWith("DeclarationDescriptorWithS") -> true
      path.startsWith("DeclarationDescriptorWithV") -> true
      path.startsWith("DescriptorVisibilities.") -> true
      path.startsWith("DescriptorVisibility.") -> true
      path.startsWith("EffectiveVisibility\$L") -> true
      path.startsWith("EffectiveVisibility\$Pu") -> true
      path.startsWith("EffectiveVisibility.") -> true
      path.startsWith("Fie") -> true
      path.startsWith("Fu") -> true
      path.startsWith("Inl") -> true
      path.startsWith("InvalidModuleExceptionK") -> true
      path.startsWith("Modality.") -> true
      path.startsWith("ModuleC") -> true
      path.startsWith("ModuleDescriptor\$D") -> true
      path.startsWith("ModuleDescriptor.") -> true
      path.startsWith("NotFoundClasses.") -> true
      path.startsWith("PackageFragmentD") -> true
      path.startsWith("PackageFragmentProvider.") -> true
      path.startsWith("PackageFragmentProviderImpl.") -> true
      path.startsWith("PackageFragmentProviderK") -> true
      path.startsWith("PackageFragmentProviderO") -> true
      path.startsWith("PackageViewDescriptor.") -> true
      path.startsWith("Par") -> true
      path.startsWith("Pr") -> true
      path.startsWith("Rec") -> true
      path.startsWith("Scr") -> true
      path.startsWith("Si") -> true
      path.startsWith("SourceElement.") -> true
      path.startsWith("Sup") -> true
      path.startsWith("TypeA") -> true
      path.startsWith("TypeParameterD") -> true
      path.startsWith("ValueClassRepresentation.") -> true
      path.startsWith("ValueD") -> true
      path.startsWith("ValueP") -> true
      path.startsWith("VariableA") -> true
      path.startsWith("VariableDescriptor.") -> true
      path.startsWith("VariableDescriptorWithAccessors.") -> true
      path.startsWith("Visibilities\$L") -> true
      path.startsWith("Visibilities\$Private.") -> true
      path.startsWith("Visibilities\$Pro") -> true
      path.startsWith("Visibilities\$Pu") -> true
      path.startsWith("Visibilities.") -> true
      path.startsWith("Visibility") -> true
      path.startsWith("deserialization/AdditionalClassPartsProvider.") -> true
      path.startsWith("deserialization/PlatformDependentDeclarationFilter.") -> true
      path.startsWith("i") -> true
      path.startsWith("java/JavaVisibilities\$") -> true
      path.startsWith("runtime/components/ReflectJ") -> true
      path.startsWith("runtime/components/ReflectKotlinClassFinder.") -> true
      path.startsWith("runtime/components/RuntimeE") -> true
      path.startsWith("runtime/components/RuntimeSourceElementFactory.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("cli/jvm/compiler/") -> {
                    val path = path.removePrefix("cli/jvm/compiler/")
                    when {
      path.startsWith("ClassF") -> false
      path.startsWith("CliC") -> false
      path.startsWith("CliE") -> false
      path.startsWith("Compi") -> false
      path.startsWith("Dup") -> false
      path.startsWith("F") -> false
      path.startsWith("KotlinT") -> false
      path.startsWith("L") -> false
      path.startsWith("P") -> false
      path.startsWith("V") -> false
      path.startsWith("p") -> false
      
      else -> true
    }
                  }
                  path.startsWith("cli/common/") -> {
                    val path = path.removePrefix("cli/common/")
                    when {
                    path.startsWith("repl/") -> {
                      val path = path.removePrefix("repl/")
                      when {
        path.startsWith("CompiledClassData\$") -> false
        path.startsWith("HistoryActionsForRepeatAny\$") -> false
        path.startsWith("HistoryActionsForRepeatRecentOnly\$") -> false
        path.startsWith("LineId\$") -> false
        path.startsWith("ScriptArgsWithTypes\$") -> false
        
        else -> true
      }
                    }
                    path.startsWith("arguments/") -> {
                      val path = path.removePrefix("arguments/")
                      when {
        path.startsWith("Argument.") -> true
        path.startsWith("ArgumentP") -> true
        path.startsWith("CommonCompilerArguments.") -> true
        path.startsWith("CommonToolArguments.") -> true
        path.startsWith("I") -> true
        path.startsWith("K2JSCompilerArguments.") -> true
        path.startsWith("K2JSCompilerArgumentsK") -> true
        path.startsWith("K2JVMCompilerArguments.") -> true
        path.startsWith("M") -> true
        path.startsWith("P") -> true
        
        else -> false
      }
                    }
                    path.startsWith("messages/") -> {
                      val path = path.removePrefix("messages/")
                      when {
        path.startsWith("CompilerMessageLocation\$") -> true
        path.startsWith("CompilerMessageLocation.") -> true
        path.startsWith("CompilerMessageSeverity.") -> true
        path.startsWith("CompilerMessageSo") -> true
        path.startsWith("GroupingMessageCollector.") -> true
        path.startsWith("MessageCollector\$Companion.") -> true
        path.startsWith("MessageCollector.") -> true
        path.startsWith("MessageU") -> true
        path.startsWith("OutputMessageUtil.") -> true
        
        else -> false
      }
                    }
      path.startsWith("CLICon") -> true
      path.startsWith("Cl") -> true
      path.startsWith("CommonCompilerPerformanceManager.") -> true
      path.startsWith("CompilerSystemProperties.") -> true
      path.startsWith("E") -> true
      path.startsWith("Pr") -> true
      path.startsWith("c") -> true
      path.startsWith("e") -> true
      path.startsWith("modules/ModuleB") -> true
      path.startsWith("modules/ModuleC") -> true
      path.startsWith("modules/ModuleXmlParser.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("resolve/") -> {
                    val path = path.removePrefix("resolve/")
                    when {
                    path.startsWith("calls/") -> {
                      val path = path.removePrefix("calls/")
                      when {
                      path.startsWith("inference/model/") -> {
                        val path = path.removePrefix("inference/model/")
                        when {
                        path.startsWith("Constraint") -> {
                          val path = path.removePrefix("Constraint")
                          when {
            path.startsWith(".") -> true
            path.startsWith("Kind.") -> true
            path.startsWith("Position.") -> true
            path.startsWith("Storage.") -> true
            path.startsWith("Sy") -> true
            
            else -> false
          }
                        }
          path.startsWith("Inc") -> true
          path.startsWith("NewConstraintSystemImpl.") -> true
          path.startsWith("S") -> true
          path.startsWith("V") -> true
          
          else -> false
        }
                      }
                      path.startsWith("tower/") -> {
                        val path = path.removePrefix("tower/")
                        when {
          path.startsWith("CandidateApplicability.") -> true
          path.startsWith("Em") -> true
          path.startsWith("Expr") -> true
          path.startsWith("FunctionE") -> true
          path.startsWith("ImplicitsExtensionsResolutionFilter.") -> true
          path.startsWith("LambdaK") -> true
          path.startsWith("NewCallA") -> true
          path.startsWith("NewResolutionOldInferenceKt.") -> true
          path.startsWith("PSIF") -> true
          path.startsWith("PSIKotlinCallA") -> true
          path.startsWith("ScopeW") -> true
          path.startsWith("SimpleP") -> true
          path.startsWith("Su") -> true
          
          else -> false
        }
                      }
        path.startsWith("A") -> true
        path.startsWith("context/B") -> true
        path.startsWith("context/ResolutionC") -> true
        path.startsWith("inference/ConstraintSystemBuilder.") -> true
        path.startsWith("inference/ConstraintSystemBuilderKt.") -> true
        path.startsWith("model/KotlinCallA") -> true
        path.startsWith("model/PartialCallContainer.") -> true
        path.startsWith("model/PartialCallR") -> true
        path.startsWith("model/Q") -> true
        path.startsWith("model/ReceiverExpressionKotlinCallArgument.") -> true
        path.startsWith("model/ResolutionAtoms") -> true
        path.startsWith("model/ResolvedCallAtom.") -> true
        path.startsWith("smartcasts/DataFlowInfo.") -> true
        path.startsWith("smartcasts/DataFlowInfoF") -> true
        path.startsWith("smartcasts/DataFlowValueFactory.") -> true
        path.startsWith("tasks/E") -> true
        path.startsWith("util/CallUtilKt.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("scopes/") -> {
                      val path = path.removePrefix("scopes/")
                      when {
                      path.startsWith("receivers/") -> {
                        val path = path.removePrefix("receivers/")
                        when {
          path.startsWith("ContextR") -> true
          path.startsWith("ExpressionReceiver\$Companion.") -> true
          path.startsWith("ExpressionReceiver.") -> true
          path.startsWith("Ext") -> true
          path.startsWith("ImplicitC") -> true
          path.startsWith("Q") -> true
          path.startsWith("ReceiverV") -> true
          path.startsWith("Tr") -> true
          
          else -> false
        }
                      }
        path.startsWith("Ch") -> true
        path.startsWith("DescriptorKindExclude\$T") -> true
        path.startsWith("DescriptorKindFilter\$Companion.") -> true
        path.startsWith("DescriptorKindFilter.") -> true
        path.startsWith("HierarchicalScope.") -> true
        path.startsWith("ImportingScope.") -> true
        path.startsWith("In") -> true
        path.startsWith("LazyScopeAdapter.") -> true
        path.startsWith("LexicalScope.") -> true
        path.startsWith("MemberScope\$Companion.") -> true
        path.startsWith("MemberScope\$E") -> true
        path.startsWith("MemberScope.") -> true
        path.startsWith("MemberScopeI") -> true
        path.startsWith("SubstitutingScope.") -> true
        path.startsWith("TypeIntersectionScope.") -> true
        path.startsWith("optimization/OptimizingOptions.") -> true
        path.startsWith("utils/ScopeUtilsKt.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("jvm/modules/JavaModule") -> {
                      val path = path.removePrefix("jvm/modules/JavaModule")
                      when {
        path.startsWith("Info\$Companion\$") -> false
        path.startsWith("Info\$R") -> false
        path.startsWith("K") -> false
        path.startsWith("Resolver\$A") -> false
        
        else -> true
      }
                    }
                    path.startsWith("lazy/declarations/") -> {
                      val path = path.removePrefix("lazy/declarations/")
                      when {
        path.startsWith("C") -> true
        path.startsWith("DeclarationProviderFactory.") -> true
        path.startsWith("DeclarationProviderFactoryService.") -> true
        path.startsWith("FileBasedDeclarationProviderFactory.") -> true
        path.startsWith("Pa") -> true
        
        else -> false
      }
                    }
                    path.startsWith("jvm/") -> {
                      val path = path.removePrefix("jvm/")
                      when {
        path.startsWith("Ja") -> true
        path.startsWith("JvmCl") -> true
        path.startsWith("JvmCod") -> true
        path.startsWith("JvmCom") -> true
        path.startsWith("JvmPr") -> true
        path.startsWith("KotlinC") -> true
        path.startsWith("KotlinJavaPsiFacade.") -> true
        path.startsWith("NotFoundPackagesCachingStrategy\$C") -> true
        path.startsWith("NotFoundPackagesCachingStrategy.") -> true
        path.startsWith("Re") -> true
        path.startsWith("diagnostics/C") -> true
        path.startsWith("diagnostics/ErrorsJvm.") -> true
        path.startsWith("diagnostics/JvmDeclarationOrigin.") -> true
        path.startsWith("diagnostics/JvmDeclarationOriginKi") -> true
        path.startsWith("diagnostics/R") -> true
        path.startsWith("extensions/A") -> true
        path.startsWith("extensions/Pac") -> true
        path.startsWith("extensions/S") -> true
        path.startsWith("multiplatform/OptionalAnnotationPackageFragmentProvider.") -> true
        path.startsWith("platform/JvmPlatformA") -> true
        
        else -> false
      }
                    }
      path.startsWith("BindingContext.") -> true
      path.startsWith("BindingTrace.") -> true
      path.startsWith("BindingTraceContext.") -> true
      path.startsWith("CodeAnalyzerInitializer.") -> true
      path.startsWith("Compiler") -> true
      path.startsWith("DeclarationS") -> true
      path.startsWith("DescriptorEquivalenceForOverrides.") -> true
      path.startsWith("DescriptorFactory.") -> true
      path.startsWith("DescriptorT") -> true
      path.startsWith("DescriptorU") -> true
      path.startsWith("ImplicitIntegerCoercion.") -> true
      path.startsWith("ImportP") -> true
      path.startsWith("InlineClassD") -> true
      path.startsWith("Lan") -> true
      path.startsWith("LazyTopDownAnalyzer.") -> true
      path.startsWith("ModuleA") -> true
      path.startsWith("NonR") -> true
      path.startsWith("OverridingS") -> true
      path.startsWith("OverridingUtil.") -> true
      path.startsWith("PlatformDependentAnalyzerServices.") -> true
      path.startsWith("Se") -> true
      path.startsWith("StatementFilter.") -> true
      path.startsWith("TargetEnvironment.") -> true
      path.startsWith("To") -> true
      path.startsWith("TypeResolver.") -> true
      path.startsWith("checkers/OptInN") -> true
      path.startsWith("constants/ConstantValue.") -> true
      path.startsWith("deprecation/DeprecationI") -> true
      path.startsWith("deprecation/DeprecationL") -> true
      path.startsWith("deprecation/DeprecationResolver.") -> true
      path.startsWith("deprecation/DeprecationSettings.") -> true
      path.startsWith("descriptorUtil/DescriptorUtilsKt.") -> true
      path.startsWith("diagnostics/Diagnostics\$Companion.") -> true
      path.startsWith("diagnostics/Diagnostics.") -> true
      path.startsWith("extensions/AnalysisHandlerExtension.") -> true
      path.startsWith("extensions/As") -> true
      path.startsWith("extensions/ExtraImportsProviderExtension\$Companion.") -> true
      path.startsWith("extensions/ExtraImportsProviderExtension.") -> true
      path.startsWith("extensions/SyntheticResolveExtension\$Companion.") -> true
      path.startsWith("extensions/SyntheticResolveExtension.") -> true
      path.startsWith("lazy/DeclarationScopeProvider.") -> true
      path.startsWith("lazy/FileScopeFactory.") -> true
      path.startsWith("lazy/FileScopeProviderK") -> true
      path.startsWith("lazy/FileScopes") -> true
      path.startsWith("lazy/Fo") -> true
      path.startsWith("lazy/ImportF") -> true
      path.startsWith("lazy/K") -> true
      path.startsWith("lazy/ResolveSession.") -> true
      path.startsWith("lazy/ResolveSessionU") -> true
      path.startsWith("lazy/data/KtClassL") -> true
      path.startsWith("multiplatform/I") -> true
      path.startsWith("rep") -> true
      path.startsWith("sam/SamConversionResolver.") -> true
      path.startsWith("sam/SamConversionResolverImpl.") -> true
      path.startsWith("sam/SamW") -> true
      
      else -> false
    }
                  }
                  path.startsWith("psi/stubs/") -> {
                    val path = path.removePrefix("psi/stubs/")
                    when {
                    path.startsWith("elements/Kt") -> {
                      val path = path.removePrefix("elements/Kt")
                      when {
        path.startsWith("ConstantExpressionElementType\$") -> false
        path.startsWith("ContractEffectL") -> false
        path.startsWith("D") -> false
        path.startsWith("FileS") -> false
        path.startsWith("PlaceHolderW") -> false
        path.startsWith("Pri") -> false
        path.startsWith("Se") -> false
        path.startsWith("Str") -> false
        path.startsWith("StubElementType.") -> false
        path.startsWith("To") -> false
        path.startsWith("UserTypeElementType\$") -> false
        path.startsWith("ValueArgumentL") -> false
        
        else -> true
      }
                    }
      path.startsWith("KotlinStubV") -> false
      path.startsWith("elements/S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("backend/common/serialization/") -> {
                    val path = path.removePrefix("backend/common/serialization/")
                    when {
                    path.startsWith("encodings/") -> {
                      val path = path.removePrefix("encodings/")
                      when {
        path.startsWith("BinaryC") -> false
        path.startsWith("BinaryL") -> false
        path.startsWith("Fi") -> false
        path.startsWith("L") -> false
        path.startsWith("TypeA") -> false
        
        else -> true
      }
                    }
                    path.startsWith("proto/Ir") -> {
                      val path = path.removePrefix("proto/Ir")
                      when {
        path.startsWith("Class.") -> true
        path.startsWith("Constructor.") -> true
        path.startsWith("ConstructorCall.") -> true
        path.startsWith("Declaration\$D") -> true
        path.startsWith("Declaration.") -> true
        path.startsWith("DeclarationBase.") -> true
        path.startsWith("DefinitelyNotNullType.") -> true
        path.startsWith("EnumEntry.") -> true
        path.startsWith("File.") -> true
        path.startsWith("Function.") -> true
        path.startsWith("FunctionBase.") -> true
        path.startsWith("Property.") -> true
        path.startsWith("SimpleType.") -> true
        path.startsWith("SimpleTypeLegacy.") -> true
        path.startsWith("SimpleTypeNullability.") -> true
        path.startsWith("Type\$K") -> true
        path.startsWith("Type.") -> true
        path.startsWith("TypeParameter.") -> true
        path.startsWith("ValueParameter.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("FileDeserializerKt.") -> true
        path.startsWith("Fl") -> true
        path.startsWith("InterningService.") -> true
        path.startsWith("K") -> true
        path.startsWith("L") -> true
        
        else -> false
      }
                    }
      path.startsWith("BasicIrModuleDeserializerK") -> true
      path.startsWith("IdSignatureDeserializer.") -> true
      path.startsWith("metadata/i") -> true
      path.startsWith("proto/FileEntry.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("analysis/providers/") -> {
                    val path = path.removePrefix("analysis/providers/")
                    when {
                    path.startsWith("Kotlin") -> {
                      val path = path.removePrefix("Kotlin")
                      when {
        path.startsWith("Anc") -> false
        path.startsWith("AnnotationsResolverK") -> false
        path.startsWith("DeclarationProviderFactory\$") -> false
        path.startsWith("DeclarationProviderMerger\$") -> false
        path.startsWith("GlobalModificationService\$") -> false
        path.startsWith("MessageBusProvider\$") -> false
        path.startsWith("ModificationTrackerFactoryK") -> false
        path.startsWith("PackageProviderK") -> false
        path.startsWith("ResolutionScopeProvider\$") -> false
        
        else -> true
      }
                    }
      path.startsWith("F") -> false
      path.startsWith("P") -> false
      path.startsWith("topics/KotlinM") -> false
      
      else -> true
    }
                  }
                  path.startsWith("ir/declarations/") -> {
                    val path = path.removePrefix("ir/declarations/")
                    when {
                    path.startsWith("IrDeclarationOrigin") -> {
                      val path = path.removePrefix("IrDeclarationOrigin")
                      when {
        path.startsWith("\$DEFI") -> true
        path.startsWith("\$DELEGATED_P") -> true
        path.startsWith("\$ER") -> true
        path.startsWith("\$FA") -> true
        path.startsWith("\$GENERATED_M") -> true
        path.startsWith("\$GENERATED_SI") -> true
        path.startsWith("\$IR_B") -> true
        path.startsWith("\$MO") -> true
        path.startsWith("\$SYNTHETIC_G") -> true
        path.startsWith(".") -> true
        path.startsWith("I") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("A") -> true
        path.startsWith("C") -> true
        path.startsWith("Declaration.") -> true
        path.startsWith("DeclarationB") -> true
        path.startsWith("DeclarationP") -> true
        path.startsWith("Declarations") -> true
        path.startsWith("E") -> true
        path.startsWith("Factory\$") -> true
        path.startsWith("Factory.") -> true
        path.startsWith("Fi") -> true
        path.startsWith("Fu") -> true
        path.startsWith("L") -> true
        path.startsWith("Mem") -> true
        path.startsWith("Mo") -> true
        path.startsWith("Mu") -> true
        path.startsWith("OverridableD") -> true
        path.startsWith("Pa") -> true
        path.startsWith("Pr") -> true
        path.startsWith("ReturnTarget.") -> true
        path.startsWith("S") -> true
        path.startsWith("T") -> true
        path.startsWith("ValueDeclaration.") -> true
        path.startsWith("ValueP") -> true
        path.startsWith("Var") -> true
        
        else -> false
      }
                    }
      path.startsWith("Id") -> true
      path.startsWith("MetadataSource.") -> true
      path.startsWith("S") -> true
      path.startsWith("i") -> true
      
      else -> false
    }
                  }
                  path.startsWith("backend/jvm/") -> {
                    val path = path.removePrefix("backend/jvm/")
                    when {
      path.startsWith("A") -> true
      path.startsWith("InlineClassAbi.") -> true
      path.startsWith("InlineClassAbiK") -> true
      path.startsWith("Inte") -> true
      path.startsWith("JvmBackendContext.") -> true
      path.startsWith("JvmBackendExtension.") -> true
      path.startsWith("JvmCachedDeclarations.") -> true
      path.startsWith("JvmGeneratorExtensions.") -> true
      path.startsWith("JvmGeneratorExtensionsImpl.") -> true
      path.startsWith("JvmIrCodegenFactory\$I") -> true
      path.startsWith("JvmIrCodegenFactory\$JvmIrB") -> true
      path.startsWith("JvmIrCodegenFactory.") -> true
      path.startsWith("JvmIrD") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$GENERATED_MU") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$INLINE_C") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$MULTI_FIELD_VALUE_CLASS_G") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$ST") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$SYNTHETIC_I") -> true
      path.startsWith("JvmSymbolsK") -> true
      path.startsWith("L") -> true
      path.startsWith("Me") -> true
      path.startsWith("MfvcNode.") -> true
      path.startsWith("MfvcNodeFactoryKt.") -> true
      path.startsWith("MfvcNodeKt.") -> true
      path.startsWith("MfvcNodeWithSubnodes.") -> true
      path.startsWith("NameableMfvcNode.") -> true
      path.startsWith("ReceiverBasedMfvcNodeInstance.") -> true
      path.startsWith("RootMfvcNode.") -> true
      path.startsWith("extensions/ClassGeneratorE") -> true
      path.startsWith("ir/JvmIrB") -> true
      path.startsWith("ir/JvmIrTypeUtilsKt.") -> true
      path.startsWith("ir/JvmIrUtilsKt.") -> true
      path.startsWith("lower/InheritedDefaultMethodsOnClassesLoweringKt.") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$ValueDeclarationRemapper\$a") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$ValueDeclarationRemapper\$m") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$b") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering.") -> true
      path.startsWith("lower/Repl") -> true
      path.startsWith("serialization/JvmIdSignatureDescriptor.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/symbols/") -> {
                    val path = path.removePrefix("ir/symbols/")
                    when {
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("BindableSymbol\$") -> false
        path.startsWith("ClassifierE") -> false
        path.startsWith("ClassifierSymbol\$") -> false
        path.startsWith("D") -> false
        path.startsWith("PackageFragmentSymbol\$") -> false
        path.startsWith("ReturnTargetSymbol\$") -> false
        path.startsWith("Symbol\$") -> false
        path.startsWith("SymbolK") -> false
        path.startsWith("ValueSymbol\$") -> false
        
        else -> true
      }
                    }
      
      else -> true
    }
                  }
                  path.startsWith("serialization/deserialization/") -> {
                    val path = path.removePrefix("serialization/deserialization/")
                    when {
      path.startsWith("AnnotationAndConstantLoader.") -> true
      path.startsWith("AnnotationAndConstantLoaderImpl.") -> true
      path.startsWith("ClassDa") -> true
      path.startsWith("ContractDeserializer.") -> true
      path.startsWith("DeserializationCom") -> true
      path.startsWith("DeserializationConfiguration.") -> true
      path.startsWith("DeserializedC") -> true
      path.startsWith("DeserializedPackageFragment.") -> true
      path.startsWith("EnumEntriesDeserializationSupport.") -> true
      path.startsWith("ErrorReporter.") -> true
      path.startsWith("FlexibleTypeDeserializer.") -> true
      path.startsWith("K") -> true
      path.startsWith("L") -> true
      path.startsWith("N") -> true
      path.startsWith("ProtoBasedClassDataFinder.") -> true
      path.startsWith("builtins/BuiltInS") -> true
      path.startsWith("descriptors/DeserializedContainerS") -> true
      path.startsWith("descriptors/DeserializedMemberD") -> true
      
      else -> false
    }
                  }
                  path.startsWith("config/") -> {
                    val path = path.removePrefix("config/")
                    when {
      path.startsWith("AnalysisFlags\$expe") -> false
      path.startsWith("ApiVersion\$") -> false
      path.startsWith("Con") -> false
      path.startsWith("ExplicitApiMode\$") -> false
      path.startsWith("I") -> false
      path.startsWith("JvmDefaultMode\$") -> false
      path.startsWith("LanguageFeature\$C") -> false
      path.startsWith("LanguageFeature\$p") -> false
      path.startsWith("LanguageO") -> false
      path.startsWith("LanguageVersion\$") -> false
      path.startsWith("M") -> false
      path.startsWith("S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("konan/library/") -> {
                    val path = path.removePrefix("konan/library/")
                    when {
      path.startsWith("KonanLibraryK") -> false
      path.startsWith("KonanLibraryP") -> false
      path.startsWith("N") -> false
      path.startsWith("S") -> false
      path.startsWith("TargetedW") -> false
      
      else -> true
    }
                  }
                  path.startsWith("js/backend/ast/") -> {
                    val path = path.removePrefix("js/backend/ast/")
                    when {
                    path.startsWith("Js") -> {
                      val path = path.removePrefix("Js")
                      when {
        path.startsWith("ArrayL") -> true
        path.startsWith("BinaryOperati") -> true
        path.startsWith("Br") -> true
        path.startsWith("Cont") -> true
        path.startsWith("Deb") -> true
        path.startsWith("Expression.") -> true
        path.startsWith("ExpressionS") -> true
        path.startsWith("Function.") -> true
        path.startsWith("Int") -> true
        path.startsWith("La") -> true
        path.startsWith("Loo") -> true
        path.startsWith("Name.") -> true
        path.startsWith("NameR") -> true
        path.startsWith("No") -> true
        path.startsWith("ObjectL") -> true
        path.startsWith("Pa") -> true
        path.startsWith("Ret") -> true
        path.startsWith("Sta") -> true
        path.startsWith("Thi") -> true
        path.startsWith("Vars\$") -> true
        path.startsWith("VisitorWithContextImpl.") -> true
        
        else -> false
      }
                    }
      path.startsWith("HasN") -> true
      path.startsWith("metadata/C") -> true
      path.startsWith("metadata/HasMetadata.") -> true
      path.startsWith("metadata/MetadataProperti") -> true
      
      else -> false
    }
                  }
                  path.startsWith("diagnostics/") -> {
                    val path = path.removePrefix("diagnostics/")
                    when {
                    path.startsWith("Diagnostic") -> {
                      val path = path.removePrefix("Diagnostic")
                      when {
                      path.startsWith("Factory") -> {
                        val path = path.removePrefix("Factory")
                        when {
          path.startsWith("\$") -> true
          path.startsWith(".") -> true
          path.startsWith("0.") -> true
          path.startsWith("1.") -> true
          path.startsWith("2.") -> true
          path.startsWith("W") -> true
          
          else -> false
        }
                      }
        path.startsWith(".") -> true
        path.startsWith("C") -> true
        path.startsWith("Reporter.") -> true
        path.startsWith("ReporterFactory.") -> true
        path.startsWith("Sink\$D") -> true
        path.startsWith("WithParameters1.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractD") -> true
      path.startsWith("AbstractKtDiagnosticF") -> true
      path.startsWith("AbstractSourceElementPositioningStrategy.") -> true
      path.startsWith("Errors.") -> true
      path.startsWith("G") -> true
      path.startsWith("KtDiagnostic.") -> true
      path.startsWith("KtDiagnosticFactory0") -> true
      path.startsWith("KtDiagnosticRenderer.") -> true
      path.startsWith("KtDiagnosticReportH") -> true
      path.startsWith("Se") -> true
      path.startsWith("SimpleD") -> true
      path.startsWith("Unb") -> true
      path.startsWith("i") -> true
      path.startsWith("rendering/Ro") -> true
      
      else -> false
    }
                  }
                  path.startsWith("contracts/description/") -> {
                    val path = path.removePrefix("contracts/description/")
                    when {
                    path.startsWith("Kt") -> {
                      val path = path.removePrefix("Kt")
                      when {
        path.startsWith("ContractDescriptionVa") -> false
        path.startsWith("Er") -> false
        
        else -> true
      }
                    }
      path.startsWith("EventOccurrencesRange.") -> true
      path.startsWith("Lo") -> true
      
      else -> false
    }
                  }
                  path.startsWith("types/") -> {
                    val path = path.removePrefix("types/")
                    when {
                    path.startsWith("ConstantValueKind") -> {
                      val path = path.removePrefix("ConstantValueKind")
                      when {
        path.startsWith("\$E") -> false
        path.startsWith("\$Inte") -> false
        path.startsWith("\$U") -> false
        
        else -> true
      }
                    }
                    path.startsWith("model/") -> {
                      val path = path.removePrefix("model/")
                      when {
                      path.startsWith("Type") -> {
                        val path = path.removePrefix("Type")
                        when {
          path.startsWith("ArgumentM") -> true
          path.startsWith("C") -> true
          path.startsWith("P") -> true
          path.startsWith("SystemCom") -> true
          path.startsWith("SystemContext.") -> true
          path.startsWith("SystemContextH") -> true
          path.startsWith("VariableM") -> true
          
          else -> false
        }
                      }
        path.startsWith("C") -> true
        path.startsWith("F") -> true
        path.startsWith("K") -> true
        path.startsWith("Si") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Type") -> {
                      val path = path.removePrefix("Type")
                      when {
        path.startsWith("Attributes\$") -> true
        path.startsWith("Attributes.") -> true
        path.startsWith("CheckerState.") -> true
        path.startsWith("Constructor.") -> true
        path.startsWith("ConstructorSubstitution.") -> true
        path.startsWith("Projection.") -> true
        path.startsWith("ProjectionI") -> true
        path.startsWith("Substitution.") -> true
        path.startsWith("SubstitutionK") -> true
        path.startsWith("Substitutor.") -> true
        path.startsWith("Utils.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractTypeChecker.") -> true
      path.startsWith("AbstractTypeConstructor.") -> true
      path.startsWith("ClassT") -> true
      path.startsWith("Defa") -> true
      path.startsWith("Des") -> true
      path.startsWith("FlexibleTypes") -> true
      path.startsWith("KotlinType.") -> true
      path.startsWith("KotlinTypeFactory.") -> true
      path.startsWith("KotlinTypeK") -> true
      path.startsWith("SimpleType.") -> true
      path.startsWith("Sm") -> true
      path.startsWith("Sp") -> true
      path.startsWith("Un") -> true
      path.startsWith("Variance.") -> true
      path.startsWith("checker/KotlinTypeRefiner.") -> true
      path.startsWith("checker/NewKotlinTypeChecker.") -> true
      path.startsWith("error/ErrorType.") -> true
      path.startsWith("error/ErrorTypeK") -> true
      path.startsWith("error/ErrorUtils.") -> true
      path.startsWith("expressions/ExpressionTypingCon") -> true
      path.startsWith("expressions/K") -> true
      path.startsWith("expressions/O") -> true
      path.startsWith("typeUtil/TypeUtilsKt.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("psi/Kt") -> {
                    val path = path.removePrefix("psi/Kt")
                    when {
      path.startsWith("Annotated.") -> true
      path.startsWith("AnnotationE") -> true
      path.startsWith("AnnotationU") -> true
      path.startsWith("Ba") -> true
      path.startsWith("BinaryExpression.") -> true
      path.startsWith("CallEx") -> true
      path.startsWith("Calla") -> true
      path.startsWith("Class.") -> true
      path.startsWith("ClassB") -> true
      path.startsWith("ClassL") -> true
      path.startsWith("ClassOrObject.") -> true
      path.startsWith("CodeFragment.") -> true
      path.startsWith("Col") -> true
      path.startsWith("Consta") -> true
      path.startsWith("Constructor.") -> true
      path.startsWith("ContextReceiver.") -> true
      path.startsWith("ContractEffect.") -> true
      path.startsWith("Declaration.") -> true
      path.startsWith("DeclarationC") -> true
      path.startsWith("DeclarationM") -> true
      path.startsWith("Element.") -> true
      path.startsWith("ElementI") -> true
      path.startsWith("EnumEntryS") -> true
      path.startsWith("Expression.") -> true
      path.startsWith("File.") -> true
      path.startsWith("Function.") -> true
      path.startsWith("FunctionL") -> true
      path.startsWith("ImportA") -> true
      path.startsWith("ImportD") -> true
      path.startsWith("LambdaE") -> true
      path.startsWith("ModifierList.") -> true
      path.startsWith("ModifierListOwner.") -> true
      path.startsWith("NameReferenceExpression.") -> true
      path.startsWith("NamedDeclaration.") -> true
      path.startsWith("NamedF") -> true
      path.startsWith("ObjectD") -> true
      path.startsWith("Para") -> true
      path.startsWith("Proj") -> true
      path.startsWith("Property.") -> true
      path.startsWith("PropertyA") -> true
      path.startsWith("PsiUtil.") -> true
      path.startsWith("Ret") -> true
      path.startsWith("Script.") -> true
      path.startsWith("SimpleNameExpression.") -> true
      path.startsWith("TreeVisitorV") -> true
      path.startsWith("TypeAl") -> true
      path.startsWith("TypeParameter.") -> true
      path.startsWith("TypePr") -> true
      path.startsWith("TypeReference.") -> true
      path.startsWith("Us") -> true
      path.startsWith("ValueArgument.") -> true
      path.startsWith("VisitorVoid.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("scripting/definitions/Script") -> {
                    val path = path.removePrefix("scripting/definitions/Script")
                    when {
                    path.startsWith("Definition") -> {
                      val path = path.removePrefix("Definition")
                      when {
        path.startsWith("\$FromConfigurations.") -> true
        path.startsWith("\$FromLegacy.") -> true
        path.startsWith(".") -> true
        path.startsWith("P") -> true
        path.startsWith("sS") -> true
        
        else -> false
      }
                    }
      path.startsWith("CompilationConfigurationFromDefinition.") -> true
      path.startsWith("CompilationConfigurationFromDefinitionK") -> true
      path.startsWith("Dep") -> true
      path.startsWith("EvaluationConfigurationFromDefinition.") -> true
      path.startsWith("P") -> true
      
      else -> false
    }
                  }
                  path.startsWith("codegen/") -> {
                    val path = path.removePrefix("codegen/")
                    when {
                    path.startsWith("state/GenerationState") -> {
                      val path = path.removePrefix("state/GenerationState")
                      when {
        path.startsWith("\$B") -> true
        path.startsWith("\$F") -> true
        path.startsWith("\$GenerateClassFilter.") -> true
        path.startsWith(".") -> true
        path.startsWith("EventCallback\$Companion.") -> true
        path.startsWith("EventCallback.") -> true
        path.startsWith("Kt.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractClassBuilder.") -> true
      path.startsWith("BytesUrlUtils.") -> true
      path.startsWith("ClassBuilder.") -> true
      path.startsWith("ClassBuilderFactories.") -> true
      path.startsWith("ClassBuilderFactory") -> true
      path.startsWith("ClassBuilderM") -> true
      path.startsWith("ClassFileFactory.") -> true
      path.startsWith("CodegenF") -> true
      path.startsWith("DefaultCodegenFactory.") -> true
      path.startsWith("JvmBackendClassResolver.") -> true
      path.startsWith("JvmC") -> true
      path.startsWith("K") -> true
      path.startsWith("MemberCodegen.") -> true
      path.startsWith("MultifileClassCodegen.") -> true
      path.startsWith("PackageCodegen.") -> true
      path.startsWith("context/CodegenContext.") -> true
      path.startsWith("context/P") -> true
      path.startsWith("extensions/C") -> true
      path.startsWith("extensions/ExpressionCodegenExtension\$Com") -> true
      path.startsWith("extensions/ExpressionCodegenExtension.") -> true
      path.startsWith("state/J") -> true
      path.startsWith("state/KotlinTypeMapper.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/types/") -> {
                    val path = path.removePrefix("ir/types/")
                    when {
      path.startsWith("A") -> false
      path.startsWith("B") -> false
      path.startsWith("Id") -> false
      path.startsWith("IrC") -> false
      path.startsWith("IrTypeC") -> false
      path.startsWith("IrTypeS") -> false
      path.startsWith("IrTypeU") -> false
      path.startsWith("IrTypesKt\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("backend/common/") -> {
                    val path = path.removePrefix("backend/common/")
                    when {
                    path.startsWith("extensions/Ir") -> {
                      val path = path.removePrefix("extensions/Ir")
                      when {
        path.startsWith("A") -> true
        path.startsWith("GenerationExtension\$C") -> true
        path.startsWith("GenerationExtension.") -> true
        path.startsWith("PluginContext.") -> true
        path.startsWith("PluginContextImpl\$D") -> true
        
        else -> false
      }
                    }
      path.startsWith("FileLoweringPass.") -> true
      path.startsWith("actualizer/IrActualized") -> true
      path.startsWith("ir/V") -> true
      path.startsWith("lower/LocalDeclarationsLoweringKt.") -> true
      path.startsWith("lower/loops/Ini") -> true
      path.startsWith("lower/loops/Loo") -> true
      path.startsWith("output/O") -> true
      path.startsWith("output/SimpleOutputFileC") -> true
      path.startsWith("phaser/N") -> true
      path.startsWith("phaser/PhaseBuildersKt.") -> true
      path.startsWith("phaser/PhaseConfig.") -> true
      path.startsWith("phaser/PhaseConfigu") -> true
      
      else -> false
    }
                  }
                  path.startsWith("incremental/components/") -> {
                    val path = path.removePrefix("incremental/components/")
                    when {
      path.startsWith("ImportTracker\$") -> false
      path.startsWith("Loc") -> false
      path.startsWith("LookupI") -> false
      path.startsWith("P") -> false
      path.startsWith("S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("load/kotlin/") -> {
                    val path = path.removePrefix("load/kotlin/")
                    when {
      path.startsWith("DeserializationComponentsForJava.") -> true
      path.startsWith("DeserializationComponentsForJavaKt.") -> true
      path.startsWith("DeserializedDescriptorResolver.") -> true
      path.startsWith("JvmPackagePartProviderBase\$M") -> true
      path.startsWith("JvmPackagePartProviderBase.") -> true
      path.startsWith("KotlinBinaryClassCache.") -> true
      path.startsWith("KotlinClassFinder.") -> true
      path.startsWith("Meta") -> true
      path.startsWith("ModuleM") -> true
      path.startsWith("ModuleN") -> true
      path.startsWith("ModuleVisibilityM") -> true
      path.startsWith("PackagePartProvider.") -> true
      path.startsWith("VirtualFileFinder.") -> true
      path.startsWith("VirtualFileFinderF") -> true
      path.startsWith("incremental/IncrementalPackageFragmentProvider.") -> true
      path.startsWith("incremental/IncrementalPackagePartProvider.") -> true
      path.startsWith("incremental/components/I") -> true
      
      else -> false
    }
                  }
                  path.startsWith("constant/") -> {
                    val path = path.removePrefix("constant/")
                    when {
      path.startsWith("De") -> false
      path.startsWith("ErrorValue\$") -> false
      path.startsWith("Inte") -> false
      path.startsWith("KClassValue\$Value\$L") -> false
      path.startsWith("KClassValue\$Value.") -> false
      path.startsWith("Un") -> false
      
      else -> true
    }
                  }
                  path.startsWith("cli/jvm/") -> {
                    val path = path.removePrefix("cli/jvm/")
                    when {
                    path.startsWith("modules/") -> {
                      val path = path.removePrefix("modules/")
                      when {
        path.startsWith("CliJavaModuleFinder.") -> true
        path.startsWith("CliJavaModuleResolver.") -> true
        path.startsWith("CoreJrtF") -> true
        path.startsWith("CoreJrtVirtualFile.") -> true
        path.startsWith("JavaModuleGraph.") -> true
        path.startsWith("JavaV") -> true
        
        else -> false
      }
                    }
      path.startsWith("config/C") -> true
      path.startsWith("config/J") -> true
      path.startsWith("config/V") -> true
      path.startsWith("index/Ja") -> true
      path.startsWith("index/JvmDependenciesDynamicCompoundIndex.") -> true
      path.startsWith("index/JvmDependenciesIndex.") -> true
      path.startsWith("index/JvmDependenciesIndexImpl.") -> true
      path.startsWith("index/SingleJavaFileRootsIndex.") -> true
      path.startsWith("javac/J") -> true
      
      else -> false
    }
                  }
                  path.startsWith("extensions/") -> {
                    val path = path.removePrefix("extensions/")
                    when {
      path.startsWith("Ap") -> false
      path.startsWith("PreprocessedFileCreator\$") -> false
      path.startsWith("StorageComponentContainerContributor\$D") -> false
      path.startsWith("internal/Cal") -> false
      path.startsWith("internal/I") -> false
      path.startsWith("internal/TypeResolutionInterceptorE") -> false
      
      else -> true
    }
                  }
                  path.startsWith("metadata/") -> {
                    val path = path.removePrefix("metadata/")
                    when {
                    path.startsWith("deserialization/") -> {
                      val path = path.removePrefix("deserialization/")
                      when {
        path.startsWith("B") -> true
        path.startsWith("Flags\$B") -> true
        path.startsWith("Flags\$F") -> true
        path.startsWith("Flags.") -> true
        path.startsWith("NameResolver.") -> true
        path.startsWith("NameResolverImpl.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("ProtoBuf\$") -> {
                      val path = path.removePrefix("ProtoBuf\$")
                      when {
        path.startsWith("Annotation\$Argument\$Value\$Type.") -> true
        path.startsWith("Annotation\$Argument\$Value.") -> true
        path.startsWith("Annotation\$Argument.") -> true
        path.startsWith("Annotation.") -> true
        path.startsWith("Class.") -> true
        path.startsWith("Function.") -> true
        path.startsWith("Package.") -> true
        path.startsWith("PackageFragment.") -> true
        path.startsWith("QualifiedNameTable.") -> true
        path.startsWith("StringTable.") -> true
        path.startsWith("Visibility.") -> true
        
        else -> false
      }
                    }
      path.startsWith("builtins/BuiltInsB") -> true
      path.startsWith("jvm/deserialization/JvmMetadataVersion.") -> true
      path.startsWith("jvm/deserialization/ModuleMapping\$") -> true
      path.startsWith("jvm/deserialization/ModuleMapping.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/util/") -> {
                    val path = path.removePrefix("ir/util/")
                    when {
                    path.startsWith("IdSignature") -> {
                      val path = path.removePrefix("IdSignature")
                      when {
        path.startsWith("\$A") -> false
        path.startsWith("\$FileL") -> false
        path.startsWith("\$Low") -> false
        path.startsWith("\$S") -> false
        
        else -> true
      }
                    }
      path.startsWith("AdditionalIrUtilsKt.") -> true
      path.startsWith("DescriptorSymbolTableExtension.") -> true
      path.startsWith("DumpIrTreeKt.") -> true
      path.startsWith("DumpIrTreeO") -> true
      path.startsWith("Em") -> true
      path.startsWith("IrFakeOverrideUtilsKt.") -> true
      path.startsWith("IrTypeUtilsKt.") -> true
      path.startsWith("IrUtilsKt.") -> true
      path.startsWith("KotlinMangler\$DescriptorMangler.") -> true
      path.startsWith("KotlinMangler\$IrMangler.") -> true
      path.startsWith("NameProvider.") -> true
      path.startsWith("PatchDeclarationParentsK") -> true
      path.startsWith("RenderIrElementKt.") -> true
      path.startsWith("SymbolTable.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("asJava/") -> {
                    val path = path.removePrefix("asJava/")
                    when {
                    path.startsWith("classes/") -> {
                      val path = path.removePrefix("classes/")
                      when {
        path.startsWith("ImplUtilsKt.") -> true
        path.startsWith("KtDescriptorBasedFakeLightClass.") -> true
        path.startsWith("KtFakeLightClass.") -> true
        path.startsWith("KtLightClass.") -> true
        path.startsWith("KtLightClassForFacade.") -> true
        path.startsWith("KtLightClassForSourceDeclarationKt.") -> true
        path.startsWith("KtUltraLightClass.") -> true
        path.startsWith("KtUltraLightClassForFacade.") -> true
        path.startsWith("KtUltraLightClassForScript.") -> true
        path.startsWith("KtUltraLightSup") -> true
        path.startsWith("UltraLightUtilsKt.") -> true
        
        else -> false
      }
                    }
      path.startsWith("KotlinAsJavaSupport\$") -> true
      path.startsWith("KotlinAsJavaSupport.") -> true
      path.startsWith("KotlinAsJavaSupportBase\$D") -> true
      path.startsWith("KotlinAsJavaSupportBase.") -> true
      path.startsWith("Kt") -> true
      path.startsWith("LightClassG") -> true
      path.startsWith("S") -> true
      path.startsWith("builder/ClsWrapperStubPsiFactory.") -> true
      path.startsWith("builder/LightElementOrigin.") -> true
      path.startsWith("builder/LightElementOriginK") -> true
      path.startsWith("finder/JavaElementFinder.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("util") -> {
                    val path = path.removePrefix("util")
                    when {
                    path.startsWith("s/") -> {
                      val path = path.removePrefix("s/")
                      when {
        path.startsWith("Collections") -> true
        path.startsWith("DFS\$Ne") -> true
        path.startsWith("DFS.") -> true
        path.startsWith("E") -> true
        path.startsWith("KotlinPaths\$J") -> true
        path.startsWith("KotlinPaths.") -> true
        path.startsWith("KotlinPathsFromHomeDir.") -> true
        path.startsWith("Met") -> true
        path.startsWith("ParametersMapKt.") -> true
        path.startsWith("Pat") -> true
        path.startsWith("Pla") -> true
        path.startsWith("Printe") -> true
        path.startsWith("SmartList.") -> true
        path.startsWith("SmartSet\$C") -> true
        path.startsWith("SmartSet.") -> true
        path.startsWith("SortUtilsKt.") -> true
        path.startsWith("addToStdlib/AddToStdlibKt.") -> true
        path.startsWith("exceptions/ExceptionAttachmentBuilder.") -> true
        path.startsWith("exceptions/KotlinExceptionWithAttachments.") -> true
        path.startsWith("exceptions/KotlinIllegalA") -> true
        path.startsWith("r") -> true
        
        else -> false
      }
                    }
                    path.startsWith("/") -> {
                      val path = path.removePrefix("/")
                      when {
        path.startsWith("AbstractArrayMapOwner.") -> true
        path.startsWith("ArrayMapA") -> true
        path.startsWith("L") -> true
        path.startsWith("OperatorN") -> true
        path.startsWith("Pe") -> true
        path.startsWith("TypeRegistry.") -> true
        path.startsWith("U") -> true
        path.startsWith("WeakPair.") -> true
        path.startsWith("Wi") -> true
        path.startsWith("ca") -> true
        path.startsWith("j") -> true
        path.startsWith("slicedMap/Rea") -> true
        path.startsWith("slicedMap/W") -> true
        
        else -> false
      }
                    }
      
      else -> false
    }
                  }
                  path.startsWith("load/java/") -> {
                    val path = path.removePrefix("load/java/")
                    when {
      path.startsWith("JavaClassFinder\$") -> true
      path.startsWith("JavaClassFinder.") -> true
      path.startsWith("JavaClassFinderImplK") -> true
      path.startsWith("JavaClassesTracker.") -> true
      path.startsWith("JavaTypeEnhancementState\$Companion.") -> true
      path.startsWith("JavaTypeEnhancementState.") -> true
      path.startsWith("JvmAb") -> true
      path.startsWith("JvmAnnotationNamesK") -> true
      path.startsWith("components/FilesByFacadeFqNameIndexer.") -> true
      path.startsWith("components/JavaDeprecati") -> true
      path.startsWith("components/JavaResolverCache.") -> true
      path.startsWith("lazy/LazyJavaPackageFragmentProvider.") -> true
      path.startsWith("lazy/ModuleClassResolver.") -> true
      path.startsWith("lazy/S") -> true
      path.startsWith("sources/JavaSourceElementF") -> true
      
      else -> false
    }
                  }
                  path.startsWith("KtFakeSourceElementKind") -> {
                    val path = path.removePrefix("KtFakeSourceElementKind")
                    when {
                    path.startsWith("\$") -> {
                      val path = path.removePrefix("\$")
                      when {
        path.startsWith("Cont") -> true
        path.startsWith("Dat") -> true
        path.startsWith("Def") -> true
        path.startsWith("ImplicitT") -> true
        path.startsWith("Si") -> true
        path.startsWith("SmartCastE") -> true
        
        else -> false
      }
                    }
      path.startsWith(".") -> true
      
      else -> false
    }
                  }
    path.startsWith("A") -> true
    path.startsWith("KtIo") -> true
    path.startsWith("KtL") -> true
    path.startsWith("KtNodeTypes") -> true
    path.startsWith("KtPsiSourceElement.") -> true
    path.startsWith("KtPsiSourceFile.") -> true
    path.startsWith("KtSourceE") -> true
    path.startsWith("KtSourceFile.") -> true
    path.startsWith("KtSourceFileLinesMapping.") -> true
    path.startsWith("KtV") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInDecompiler.") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInF") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache\$C") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinClsStubBuilder.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinMetadataStubBuilder.") -> true
    path.startsWith("analysis/project/structure/KotlinModuleDependentsProvider.") -> true
    path.startsWith("analysis/project/structure/KtBi") -> true
    path.startsWith("analysis/project/structure/KtModule.") -> true
    path.startsWith("analysis/project/structure/KtModuleUtilsKt.") -> true
    path.startsWith("analysis/project/structure/i") -> true
    path.startsWith("analyzer/AnalysisResult\$Compa") -> true
    path.startsWith("analyzer/AnalysisResult\$R") -> true
    path.startsWith("analyzer/AnalysisResult.") -> true
    path.startsWith("analyzer/K") -> true
    path.startsWith("analyzer/ModuleInfo.") -> true
    path.startsWith("builtins/KotlinBuiltIns.") -> true
    path.startsWith("builtins/PrimitiveType.") -> true
    path.startsWith("builtins/StandardNames.") -> true
    path.startsWith("builtins/functions/FunctionInterfaceF") -> true
    path.startsWith("builtins/functions/FunctionTypeKind\$F") -> true
    path.startsWith("builtins/functions/FunctionTypeKind.") -> true
    path.startsWith("builtins/functions/FunctionTypeKindK") -> true
    path.startsWith("builtins/jvm/JvmBuiltIns\$K") -> true
    path.startsWith("builtins/jvm/JvmBuiltIns.") -> true
    path.startsWith("builtins/jvm/JvmBuiltInsPackageFragmentProvider.") -> true
    path.startsWith("cli/plugins/Plugins") -> true
    path.startsWith("compiler/plugin/CommandLineProcessor.") -> true
    path.startsWith("compiler/plugin/Comp") -> true
    path.startsWith("compiler/plugin/E") -> true
    path.startsWith("compilerRunner/OutputItemsCollector.") -> true
    path.startsWith("container/ComponentP") -> true
    path.startsWith("container/ContainerK") -> true
    path.startsWith("container/Ds") -> true
    path.startsWith("container/StorageComponentContainer.") -> true
    path.startsWith("context/ContextKt.") -> true
    path.startsWith("context/ModuleContext.") -> true
    path.startsWith("context/MutableModuleContext.") -> true
    path.startsWith("context/ProjectContext.") -> true
    path.startsWith("contracts/ContractDeserializerImpl.") -> true
    path.startsWith("fileClasses/JvmFileClassI") -> true
    path.startsWith("fileClasses/JvmFileClassUtil.") -> true
    path.startsWith("fileClasses/JvmFileClassUtilK") -> true
    path.startsWith("fileClasses/O") -> true
    path.startsWith("frontend/java/di/InjectionKt.") -> true
    path.startsWith("idea/KotlinF") -> true
    path.startsWith("idea/KotlinL") -> true
    path.startsWith("idea/MainFunctionDetector.") -> true
    path.startsWith("idea/references/KotlinP") -> true
    path.startsWith("incremental/CompilationTransaction.") -> true
    path.startsWith("incremental/Transa") -> true
    path.startsWith("incremental/js/IncrementalDataProvider.") -> true
    path.startsWith("incremental/js/IncrementalN") -> true
    path.startsWith("incremental/js/IncrementalResultsConsumer.") -> true
    path.startsWith("ir/IrBuiltIns.") -> true
    path.startsWith("ir/IrElement.") -> true
    path.startsWith("ir/IrFileEntry.") -> true
    path.startsWith("ir/IrS") -> true
    path.startsWith("ir/O") -> true
    path.startsWith("ir/backend/js/JsIrBackendContext.") -> true
    path.startsWith("ir/backend/js/ir/JsIrBuilder.") -> true
    path.startsWith("ir/backend/js/lower/SecondaryConstructorLowering\$T") -> true
    path.startsWith("ir/backend/js/lower/calls/CallsT") -> true
    path.startsWith("ir/backend/js/lower/calls/Rep") -> true
    path.startsWith("ir/backend/jvm/JvmLibraryResolverK") -> true
    path.startsWith("ir/backend/jvm/serialization/JvmDescriptorMangler.") -> true
    path.startsWith("ir/backend/jvm/serialization/JvmIrMangler.") -> true
    path.startsWith("ir/builders/E") -> true
    path.startsWith("ir/builders/IrBlockBu") -> true
    path.startsWith("ir/builders/IrBuilderW") -> true
    path.startsWith("ir/builders/IrGeneratorContext.") -> true
    path.startsWith("ir/builders/S") -> true
    path.startsWith("ir/builders/declarations/DeclarationBuildersKt.") -> true
    path.startsWith("ir/builders/declarations/IrFu") -> true
    path.startsWith("ir/builders/declarations/IrP") -> true
    path.startsWith("ir/builders/declarations/IrValueParameterBuilder.") -> true
    path.startsWith("ir/descriptors/IrBasedDescriptorsKt.") -> true
    path.startsWith("ir/descriptors/IrBasedEr") -> true
    path.startsWith("ir/descriptors/IrBasedPropertyD") -> true
    path.startsWith("ir/descriptors/IrBasedS") -> true
    path.startsWith("ir/visitors/IrElementTransformerVoid.") -> true
    path.startsWith("ir/visitors/IrElementVisitor.") -> true
    path.startsWith("ir/visitors/IrElementVisitorV") -> true
    path.startsWith("ir/visitors/IrV") -> true
    path.startsWith("js/config/Ec") -> true
    path.startsWith("js/config/ErrorTolerancePolicy.") -> true
    path.startsWith("js/config/JS") -> true
    path.startsWith("js/config/S") -> true
    path.startsWith("js/config/WasmTarget.") -> true
    path.startsWith("js/coroutine/CoroutineBl") -> true
    path.startsWith("js/coroutine/CoroutineM") -> true
    path.startsWith("js/coroutine/CoroutinePassesKt\$replaceC") -> true
    path.startsWith("js/coroutine/CoroutinePassesKt.") -> true
    path.startsWith("js/coroutine/CoroutineTransformationContext.") -> true
    path.startsWith("js/inline/util/rewriters/C") -> true
    path.startsWith("js/inline/util/rewriters/N") -> true
    path.startsWith("js/inline/util/rewriters/R") -> true
    path.startsWith("js/inline/util/rewriters/T") -> true
    path.startsWith("js/parser/sourcemaps/SourceMapSo") -> true
    path.startsWith("js/resolve/M") -> true
    path.startsWith("js/translate/context/N") -> true
    path.startsWith("js/translate/ext") -> true
    path.startsWith("js/translate/utils/JsAstUtils.") -> true
    path.startsWith("kdoc/lexer/KDocToken.") -> true
    path.startsWith("kdoc/lexer/KDocTokens.") -> true
    path.startsWith("kdoc/parser/KDocE") -> true
    path.startsWith("kdoc/parser/KDocK") -> true
    path.startsWith("kdoc/ps") -> true
    path.startsWith("konan/file/File\$C") -> true
    path.startsWith("konan/file/File.") -> true
    path.startsWith("konan/file/FileK") -> true
    path.startsWith("konan/file/ZipFileSystemA") -> true
    path.startsWith("konan/file/ZipFileSystemI") -> true
    path.startsWith("konan/file/ZipUtilKt.") -> true
    path.startsWith("konan/properties/P") -> true
    path.startsWith("konan/target/KonanTarget.") -> true
    path.startsWith("konan/util/DependencyDownloader\$R") -> true
    path.startsWith("konan/util/S") -> true
    path.startsWith("lexer/KtM") -> true
    path.startsWith("lexer/KtS") -> true
    path.startsWith("lexer/KtTokens") -> true
    path.startsWith("modules/J") -> true
    path.startsWith("modules/M") -> true
    path.startsWith("modules/T") -> true
    path.startsWith("mpp/C") -> true
    path.startsWith("mpp/Fu") -> true
    path.startsWith("mpp/P") -> true
    path.startsWith("mpp/R") -> true
    path.startsWith("mpp/S") -> true
    path.startsWith("mpp/T") -> true
    path.startsWith("mpp/V") -> true
    path.startsWith("name/CallableId.") -> true
    path.startsWith("name/Cl") -> true
    path.startsWith("name/FqName.") -> true
    path.startsWith("name/FqNameUnsafe.") -> true
    path.startsWith("name/FqNamesUtilKt.") -> true
    path.startsWith("name/Name.") -> true
    path.startsWith("name/NativeForwardDeclarationKind.") -> true
    path.startsWith("name/NativeS") -> true
    path.startsWith("name/Sp") -> true
    path.startsWith("name/StandardClassIds\$Ca") -> true
    path.startsWith("name/StandardClassIds.") -> true
    path.startsWith("parsing/KotlinParserDefinition.") -> true
    path.startsWith("platform/CommonPlatforms.") -> true
    path.startsWith("platform/TargetPlatform.") -> true
    path.startsWith("platform/jvm/JvmPlatformK") -> true
    path.startsWith("platform/jvm/JvmPlatforms.") -> true
    path.startsWith("platform/konan/NativePlatforms.") -> true
    path.startsWith("progress/P") -> true
    path.startsWith("protobuf/CodedInputStream.") -> true
    path.startsWith("protobuf/ExtensionRegistryLite.") -> true
    path.startsWith("protobuf/ProtocolS") -> true
    path.startsWith("psi/Ca") -> true
    path.startsWith("psi/ValueArgument.") -> true
    path.startsWith("psi/VisitorWrappersKt.") -> true
    path.startsWith("psi/psiUtil/KtPsiUtilKt.") -> true
    path.startsWith("psi/psiUtil/PsiUtilsKt.") -> true
    path.startsWith("psi2ir/generators/fragments/EvaluatorFragmentInfo.") -> true
    path.startsWith("renderer/DescriptorRenderer.") -> true
    path.startsWith("scripting/U") -> true
    path.startsWith("scripting/con") -> true
    path.startsWith("scripting/definitions/KotlinScriptDefinition.") -> true
    path.startsWith("scripting/extensions/ScriptExtraImportsProviderExtension.") -> true
    path.startsWith("scripting/extensions/Scripti") -> true
    path.startsWith("scripting/resolve/KotlinScriptDefinitionFromAnnotatedTemplate.") -> true
    path.startsWith("scripting/resolve/RefineCompilationConfigurationKt.") -> true
    path.startsWith("scripting/resolve/Rep") -> true
    path.startsWith("scripting/resolve/Res") -> true
    path.startsWith("scripting/resolve/ScriptCompilationConfigurationWrapper.") -> true
    path.startsWith("scripting/resolve/ScriptL") -> true
    path.startsWith("scripting/resolve/ScriptR") -> true
    path.startsWith("scripting/resolve/VirtualFileScriptSource.") -> true
    path.startsWith("serialization/DescriptorSerializerP") -> true
    path.startsWith("serialization/SerializerExtensionP") -> true
    path.startsWith("serialization/js/M") -> true
    path.startsWith("serialization/konan/i") -> true
    path.startsWith("storage/CacheWithNo") -> true
    path.startsWith("storage/La") -> true
    path.startsWith("storage/LockBasedStorageManager.") -> true
    path.startsWith("storage/M") -> true
    path.startsWith("storage/Not") -> true
    path.startsWith("storage/Nu") -> true
    path.startsWith("storage/St") -> true
    
    else -> false
  }
                }
                path.startsWith("com/intellij/") -> {
                  val path = path.removePrefix("com/intellij/")
                  when {
                  path.startsWith("psi/impl/") -> {
                    val path = path.removePrefix("psi/impl/")
                    when {
                    path.startsWith("source/") -> {
                      val path = path.removePrefix("source/")
                      when {
                      path.startsWith("resolve/reference/") -> {
                        val path = path.removePrefix("resolve/reference/")
                        when {
          path.startsWith("impl/m") -> false
          path.startsWith("impl/providers/G") -> false
          
          else -> true
        }
                      }
                      path.startsWith("tree/") -> {
                        val path = path.removePrefix("tree/")
                        when {
          path.startsWith("A") -> true
          path.startsWith("Cha") -> true
          path.startsWith("Co") -> true
          path.startsWith("F") -> true
          path.startsWith("IC") -> true
          path.startsWith("L") -> true
          path.startsWith("O") -> true
          path.startsWith("P") -> true
          path.startsWith("R") -> true
          path.startsWith("S") -> true
          path.startsWith("T") -> true
          path.startsWith("injected/C") -> true
          
          else -> false
        }
                      }
        path.startsWith("Ch") -> true
        path.startsWith("Cod") -> true
        path.startsWith("D") -> true
        path.startsWith("FileT") -> true
        path.startsWith("Ho") -> true
        path.startsWith("PsiFil") -> true
        path.startsWith("PsiPl") -> true
        path.startsWith("SourceT") -> true
        path.startsWith("Sp") -> true
        path.startsWith("St") -> true
        path.startsWith("Su") -> true
        path.startsWith("c") -> true
        path.startsWith("resolve/F") -> true
        path.startsWith("resolve/ResolveCa") -> true
        
        else -> false
      }
                    }
                    path.startsWith("smartPointers/") -> {
                      val path = path.removePrefix("smartPointers/")
                      when {
        path.startsWith("P") -> false
        path.startsWith("SmartT") -> false
        path.startsWith("T") -> false
        
        else -> true
      }
                    }
                    path.startsWith("Psi") -> {
                      val path = path.removePrefix("Psi")
                      when {
        path.startsWith("Cl") -> false
        path.startsWith("Co") -> false
        path.startsWith("Di") -> false
        path.startsWith("ElementF") -> false
        path.startsWith("Ex") -> false
        path.startsWith("I") -> false
        path.startsWith("J") -> false
        path.startsWith("N") -> false
        path.startsWith("S") -> false
        path.startsWith("V") -> false
        
        else -> true
      }
                    }
                    path.startsWith("file/") -> {
                      val path = path.removePrefix("file/")
                      when {
        path.startsWith("PsiPackageI") -> false
        path.startsWith("impl/J") -> false
        
        else -> true
      }
                    }
      path.startsWith("A") -> true
      path.startsWith("B") -> true
      path.startsWith("Ch") -> true
      path.startsWith("D") -> true
      path.startsWith("ElementB") -> true
      path.startsWith("F") -> true
      path.startsWith("G") -> true
      path.startsWith("Po") -> true
      path.startsWith("Ren") -> true
      path.startsWith("Rep") -> true
      path.startsWith("Res") -> true
      path.startsWith("Sh") -> true
      path.startsWith("Sy") -> true
      path.startsWith("cache/C") -> true
      path.startsWith("light/LightEl") -> true
      path.startsWith("m") -> true
      
      else -> false
    }
                  }
                  path.startsWith("psi/") -> {
                    val path = path.removePrefix("psi/")
                    when {
      path.startsWith("AbstractE") -> true
      path.startsWith("AbstractFileViewProvider.") -> true
      path.startsWith("CommonC") -> true
      path.startsWith("Cont") -> true
      path.startsWith("Du") -> true
      path.startsWith("ElementM") -> true
      path.startsWith("ExternalChangeAction.") -> true
      path.startsWith("ExternallyD") -> true
      path.startsWith("F") -> true
      path.startsWith("Hin") -> true
      path.startsWith("Ig") -> true
      path.startsWith("LanguageF") -> true
      path.startsWith("LanguageSubstitutor.") -> true
      path.startsWith("LanguageSubstitutors.") -> true
      path.startsWith("LiteralTextEscaper.") -> true
      path.startsWith("Na") -> true
      path.startsWith("O") -> true
      path.startsWith("PlainTextTokenTypes.") -> true
      path.startsWith("PsiAnchor\$S") -> true
      path.startsWith("PsiAnchor.") -> true
      path.startsWith("PsiBinaryF") -> true
      path.startsWith("PsiCodeF") -> true
      path.startsWith("PsiCom") -> true
      path.startsWith("PsiDir") -> true
      path.startsWith("PsiDocu") -> true
      path.startsWith("PsiElement.") -> true
      path.startsWith("PsiElementR") -> true
      path.startsWith("PsiElementVisitor.") -> true
      path.startsWith("PsiEr") -> true
      path.startsWith("PsiFile.") -> true
      path.startsWith("PsiFileF") -> true
      path.startsWith("PsiFileS") -> true
      path.startsWith("PsiInv") -> true
      path.startsWith("PsiLanguageInjectionHost\$I") -> true
      path.startsWith("PsiLanguageInjectionHost.") -> true
      path.startsWith("PsiLargeB") -> true
      path.startsWith("PsiLargeT") -> true
      path.startsWith("PsiLock") -> true
      path.startsWith("PsiMa") -> true
      path.startsWith("PsiMir") -> true
      path.startsWith("PsiNameI") -> true
      path.startsWith("PsiNamed") -> true
      path.startsWith("PsiParserFacade.") -> true
      path.startsWith("PsiPl") -> true
      path.startsWith("PsiPolyVariantReference.") -> true
      path.startsWith("PsiRecursiveElementWalkingVisitor.") -> true
      path.startsWith("PsiRecursiveV") -> true
      path.startsWith("PsiReference.") -> true
      path.startsWith("PsiReferenceBase\$P") -> true
      path.startsWith("PsiReferenceBase.") -> true
      path.startsWith("PsiReferenceC") -> true
      path.startsWith("PsiReferencePr") -> true
      path.startsWith("PsiReferenceR") -> true
      path.startsWith("PsiReferenceS") -> true
      path.startsWith("PsiTa") -> true
      path.startsWith("PsiTreeChangeE") -> true
      path.startsWith("PsiTreeChangeL") -> true
      path.startsWith("PsiWhit") -> true
      path.startsWith("ReferenceR") -> true
      path.startsWith("ResolveR") -> true
      path.startsWith("ResolveState.") -> true
      path.startsWith("Si") -> true
      path.startsWith("SmartP") -> true
      path.startsWith("Stu") -> true
      path.startsWith("SyntaxTraverser\$ApiEx.") -> true
      path.startsWith("SyntaxTraverser.") -> true
      path.startsWith("Synth") -> true
      path.startsWith("TokenType.") -> true
      path.startsWith("codeStyle/Co") -> true
      path.startsWith("filters/A") -> true
      path.startsWith("filters/Cl") -> true
      path.startsWith("filters/ElementFilter.") -> true
      path.startsWith("filters/N") -> true
      path.startsWith("filters/O") -> true
      path.startsWith("im") -> true
      path.startsWith("meta/M") -> true
      path.startsWith("meta/PsiM") -> true
      path.startsWith("meta/PsiP") -> true
      path.startsWith("scope/D") -> true
      path.startsWith("scope/PsiScopeProcessor\$Event.") -> true
      path.startsWith("scope/PsiScopeProcessor.") -> true
      path.startsWith("search/D") -> true
      path.startsWith("search/E") -> true
      path.startsWith("search/GlobalSearchScope.") -> true
      path.startsWith("search/L") -> true
      path.startsWith("search/ProjectScope.") -> true
      path.startsWith("search/PsiElementProcessor\$CollectElements.") -> true
      path.startsWith("search/PsiElementProcessor\$F") -> true
      path.startsWith("search/PsiElementProcessor.") -> true
      path.startsWith("search/PsiF") -> true
      path.startsWith("search/PsiS") -> true
      path.startsWith("search/S") -> true
      path.startsWith("search/i") -> true
      path.startsWith("search/searches/ExtensibleQueryFactory.") -> true
      path.startsWith("stubs/BinaryFileStubBuilder\$") -> true
      path.startsWith("stubs/BinaryFileStubBuilders") -> true
      path.startsWith("stubs/C") -> true
      path.startsWith("stubs/DefaultStubBuilder.") -> true
      path.startsWith("stubs/IL") -> true
      path.startsWith("stubs/IS") -> true
      path.startsWith("stubs/IndexS") -> true
      path.startsWith("stubs/L") -> true
      path.startsWith("stubs/N") -> true
      path.startsWith("stubs/ObjectStubS") -> true
      path.startsWith("stubs/ObjectStubTree.") -> true
      path.startsWith("stubs/PsiF") -> true
      path.startsWith("stubs/Stub.") -> true
      path.startsWith("stubs/StubBa") -> true
      path.startsWith("stubs/StubElement.") -> true
      path.startsWith("stubs/StubI") -> true
      path.startsWith("stubs/StubO") -> true
      path.startsWith("stubs/StubSerialize") -> true
      path.startsWith("stubs/StubTr") -> true
      path.startsWith("templateLanguages/I") -> true
      path.startsWith("templateLanguages/O") -> true
      path.startsWith("templateLanguages/TemplateLanguage.") -> true
      path.startsWith("tex") -> true
      path.startsWith("tree/C") -> true
      path.startsWith("tree/D") -> true
      path.startsWith("tree/IC") -> true
      path.startsWith("tree/IE") -> true
      path.startsWith("tree/IF") -> true
      path.startsWith("tree/IL") -> true
      path.startsWith("tree/IR") -> true
      path.startsWith("tree/IStr") -> true
      path.startsWith("tree/IStubFileElementType.") -> true
      path.startsWith("tree/O") -> true
      path.startsWith("tree/R") -> true
      path.startsWith("tree/S") -> true
      path.startsWith("tree/T") -> true
      path.startsWith("util/Ca") -> true
      path.startsWith("util/Pa") -> true
      path.startsWith("util/PsiA") -> true
      path.startsWith("util/PsiCa") -> true
      path.startsWith("util/PsiEl") -> true
      path.startsWith("util/PsiFormatUtilB") -> true
      path.startsWith("util/PsiMo") -> true
      path.startsWith("util/PsiTr") -> true
      path.startsWith("util/PsiUtilC") -> true
      path.startsWith("util/Q") -> true
      
      else -> false
    }
                  }
                  path.startsWith("openapi/") -> {
                    val path = path.removePrefix("openapi/")
                    when {
      path.startsWith("application/AccessToken\$") -> false
      path.startsWith("application/As") -> false
      path.startsWith("application/BaseA") -> false
      path.startsWith("application/ex/P") -> false
      path.startsWith("application/impl/FlushQueue\$F") -> false
      path.startsWith("command/u") -> false
      path.startsWith("components/ComponentC") -> false
      path.startsWith("components/ServiceD") -> false
      path.startsWith("diagnostic/LoggerRt\$") -> false
      path.startsWith("editor/c") -> false
      path.startsWith("editor/event/B") -> false
      path.startsWith("editor/ex/DocumentEx\$") -> false
      path.startsWith("editor/ex/PrioritizedDocumentListener\$") -> false
      path.startsWith("editor/impl/IntervalTreeImpl\$IntervalNode\$") -> false
      path.startsWith("editor/m") -> false
      path.startsWith("extensions/B") -> false
      path.startsWith("extensions/ExtensionF") -> false
      path.startsWith("extensions/ExtensionPointUtil\$") -> false
      path.startsWith("extensions/LoadingOrder\$1") -> false
      path.startsWith("extensions/S") -> false
      path.startsWith("extensions/impl/ExtensionPointImpl\$4") -> false
      path.startsWith("extensions/impl/XmlExtensionAdapter\$") -> false
      path.startsWith("module/ModuleS") -> false
      path.startsWith("progress/Cancellation\$") -> false
      path.startsWith("progress/EmptyProgressIndicatorB") -> false
      path.startsWith("progress/J") -> false
      path.startsWith("projectR") -> false
      path.startsWith("roots/ContentIteratorE") -> false
      path.startsWith("roots/L") -> false
      path.startsWith("roots/Pa") -> false
      path.startsWith("roots/ProjectRootModificationTracker\$") -> false
      path.startsWith("roots/impl/Pa") -> false
      path.startsWith("util/BuildNumber\$") -> false
      path.startsWith("util/Pair\$3") -> false
      path.startsWith("util/Stax") -> false
      path.startsWith("util/VolatileNullableLazyValue\$") -> false
      path.startsWith("util/io/FileSystemUtil\$I") -> false
      path.startsWith("util/io/FileUtilRt\$N") -> false
      path.startsWith("util/io/w") -> false
      path.startsWith("vfs/StandardFileSystems\$") -> false
      path.startsWith("vfs/Str") -> false
      path.startsWith("vfs/impl/ZipEntryMap\$EntrySet\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("ide/plugins/") -> {
                    val path = path.removePrefix("ide/plugins/")
                    when {
      path.startsWith("C") -> true
      path.startsWith("Di") -> true
      path.startsWith("IdeaPluginDescriptorImpl.") -> true
      path.startsWith("PluginDescriptorLoader.") -> true
      path.startsWith("PluginManagerCore.") -> true
      path.startsWith("PluginU") -> true
      path.startsWith("c") -> true
      
      else -> false
    }
                  }
                  path.startsWith("codeInsight/") -> {
                    val path = path.removePrefix("codeInsight/")
                    when {
      path.startsWith("CodeInsightUtilCore.") -> true
      path.startsWith("Con") -> true
      path.startsWith("F") -> true
      path.startsWith("folding/C") -> true
      
      else -> false
    }
                  }
    path.startsWith("AbstractBundle\$") -> false
    path.startsWith("B") -> false
    path.startsWith("DynamicBundle\$1") -> false
    path.startsWith("DynamicBundle\$D") -> false
    path.startsWith("DynamicBundle.") -> false
    path.startsWith("codeWithMe/ClientId\$") -> false
    path.startsWith("codeWithMe/ClientIdS") -> false
    path.startsWith("core/CoreApplicationEnvironment\$2") -> false
    path.startsWith("core/CoreEncodingP") -> false
    path.startsWith("core/CoreI") -> false
    path.startsWith("core/CoreJ") -> false
    path.startsWith("core/CoreL") -> false
    path.startsWith("core/CoreP") -> false
    path.startsWith("core/J") -> false
    path.startsWith("diagnostic/ActivityI") -> false
    path.startsWith("diagnostic/EventWatcher\$") -> false
    path.startsWith("diagnostic/LoadingState\$") -> false
    path.startsWith("diagnostic/PluginP") -> false
    path.startsWith("diagnostic/ThreadDumper\$") -> false
    path.startsWith("extapi/psi/P") -> false
    path.startsWith("extapi/psi/StubBasedPsiElementBase\$") -> false
    path.startsWith("f") -> false
    path.startsWith("icons/AllIcons.") -> false
    path.startsWith("ide/highlighter/J") -> false
    path.startsWith("lang/j") -> false
    path.startsWith("lexer/J") -> false
    path.startsWith("model/B") -> false
    path.startsWith("model/ModelBranchU") -> false
    path.startsWith("model/psi/PsiE") -> false
    path.startsWith("model/psi/U") -> false
    path.startsWith("navigation/C") -> false
    path.startsWith("navigation/ItemPresentationProvider.") -> false
    path.startsWith("no") -> false
    path.startsWith("patterns/ElementPatternB") -> false
    path.startsWith("patterns/IE") -> false
    path.startsWith("patterns/InitialPatternConditionP") -> false
    path.startsWith("patterns/ObjectPattern\$") -> false
    path.startsWith("patterns/PatternCondition\$") -> false
    path.startsWith("patterns/PatternConditionP") -> false
    path.startsWith("patterns/Pl") -> false
    path.startsWith("patterns/Pr") -> false
    path.startsWith("patterns/PsiElementPattern\$") -> false
    path.startsWith("patterns/PsiJ") -> false
    path.startsWith("patterns/S") -> false
    path.startsWith("patterns/TreeElementPattern\$") -> false
    path.startsWith("patterns/c") -> false
    path.startsWith("pl") -> false
    path.startsWith("pom/Ps") -> false
    path.startsWith("pom/j") -> false
    path.startsWith("serialization/F") -> false
    path.startsWith("serialization/PropertyA") -> false
    path.startsWith("serialization/PropertyCollector\$1") -> false
    path.startsWith("serialization/PropertyCollector\$N") -> false
    path.startsWith("serialization/PropertyCollector\$P") -> false
    path.startsWith("serviceContainer/L") -> false
    path.startsWith("testFramework/LightVirtualFile\$") -> false
    path.startsWith("testFramework/LightVirtualFileB") -> false
    path.startsWith("ui/D") -> false
    path.startsWith("ui/IconManagerH") -> false
    path.startsWith("ui/icons/C") -> false
    path.startsWith("util/Bl") -> false
    path.startsWith("util/Consumer\$") -> false
    path.startsWith("util/Functions\$3") -> false
    path.startsWith("util/Functions\$4") -> false
    path.startsWith("util/Functions\$6") -> false
    path.startsWith("util/Functions\$7") -> false
    path.startsWith("util/Functions\$8") -> false
    path.startsWith("util/NoO") -> false
    path.startsWith("util/NullableFunction\$") -> false
    path.startsWith("util/Ur") -> false
    path.startsWith("util/V") -> false
    path.startsWith("util/X") -> false
    path.startsWith("util/containers/ConcurrentFactoryMap\$CollectionWrapper\$S") -> false
    path.startsWith("util/containers/ConcurrentRefHashMap\$2") -> false
    path.startsWith("util/containers/ContainerUtil\$8\$") -> false
    path.startsWith("util/containers/SLRUMap\$") -> false
    path.startsWith("util/graph/DFSTBuilder\$D") -> false
    path.startsWith("util/io/DirectByteBufferP") -> false
    path.startsWith("util/io/FileChannelU") -> false
    path.startsWith("util/io/Fin") -> false
    path.startsWith("util/io/IOCancellationCallbackHolder\$") -> false
    path.startsWith("util/io/IntToIntBtree\$BtreeD") -> false
    path.startsWith("util/io/IntToIntBtree\$BtreeP") -> false
    path.startsWith("util/io/IntToIntBtree\$K") -> false
    path.startsWith("util/io/M") -> false
    path.startsWith("util/io/OpenChannelsCache\$ChannelP") -> false
    path.startsWith("util/io/Page\$") -> false
    path.startsWith("util/io/Page.") -> false
    path.startsWith("util/io/PageP") -> false
    path.startsWith("util/io/PersistentEnumeratorBase\$C") -> false
    path.startsWith("util/io/PersistentHashMapValueStorage\$O") -> false
    path.startsWith("util/io/PersistentMapWal\$c") -> false
    path.startsWith("util/io/PersistentMapWal\$f") -> false
    path.startsWith("util/io/PersistentMapWal\$s") -> false
    path.startsWith("util/io/Po") -> false
    path.startsWith("util/io/Ra") -> false
    path.startsWith("util/io/UnI") -> false
    path.startsWith("util/io/WriteAheadLogKt\$t") -> false
    path.startsWith("util/keyFMap/MapBackedFMap\$") -> false
    path.startsWith("util/lang/ClasspathCache\$LoaderData.") -> false
    path.startsWith("util/lang/ClasspathCache\$N") -> false
    path.startsWith("util/lang/JdkZipResourceFile\$S") -> false
    path.startsWith("util/lang/Loader\$") -> false
    path.startsWith("util/lang/S") -> false
    path.startsWith("util/lang/f") -> false
    path.startsWith("util/lo") -> false
    path.startsWith("util/messages/impl/CompositeMessageBus\$") -> false
    path.startsWith("util/messages/impl/MessageBusImpl\$MessageP") -> false
    path.startsWith("util/messages/impl/MessageBusImpl\$R") -> false
    path.startsWith("util/messages/impl/MessageBusImpl\$T") -> false
    path.startsWith("util/text/Ca") -> false
    path.startsWith("util/text/K") -> false
    path.startsWith("util/text/L") -> false
    
    else -> true
  }
                }
                path.startsWith("kotlin/script/experimental/") -> {
                  val path = path.removePrefix("kotlin/script/experimental/")
                  when {
                  path.startsWith("jvm") -> {
                    val path = path.removePrefix("jvm")
                    when {
      path.startsWith("/BasicJvmR") -> true
      path.startsWith("/BasicJvmScriptEvaluator.") -> true
      path.startsWith("/BasicJvmScriptEvaluatorK") -> true
      path.startsWith("/CompiledJvmScriptsCache.") -> true
      path.startsWith("/JvmDependency.") -> true
      path.startsWith("/JvmDependencyF") -> true
      path.startsWith("/JvmGetScriptingClass.") -> true
      path.startsWith("/JvmScriptCa") -> true
      path.startsWith("/JvmScriptCompilationConfigurationBuilder.") -> true
      path.startsWith("/JvmScriptCompilationConfigurationK") -> true
      path.startsWith("/JvmScriptCompilationK") -> true
      path.startsWith("/JvmScriptEvaluationConfigurationBuilder.") -> true
      path.startsWith("/JvmScriptEvaluationConfigurationK") -> true
      path.startsWith("/JvmScriptEvaluationK") -> true
      path.startsWith("/JvmScriptingHostConfigurationBuilder.") -> true
      path.startsWith("/JvmScriptingHostConfigurationK") -> true
      path.startsWith("/K") -> true
      path.startsWith("/compat/DiagnosticsUtilKt.") -> true
      path.startsWith("/i") -> true
      path.startsWith("/util/C") -> true
      path.startsWith("/util/D") -> true
      path.startsWith("/util/JvmClassLoaderUtilKt.") -> true
      path.startsWith("/util/JvmClassp") -> true
      path.startsWith("/util/K") -> true
      path.startsWith("/util/RuntimeExceptionReportingKt.") -> true
      path.startsWith("/util/SnippetsHistory.") -> true
      path.startsWith("/util/So") -> true
      
      else -> false
    }
                  }
    path.startsWith("js") -> false
    
    else -> true
  }
                }
                path.startsWith("it/unimi/dsi/fastutil/") -> {
                  val path = path.removePrefix("it/unimi/dsi/fastutil/")
                  when {
                  path.startsWith("objects/Object") -> {
                    val path = path.removePrefix("objects/Object")
                    when {
      path.startsWith("2IntMap\$E") -> true
      path.startsWith("2IntMap.") -> true
      path.startsWith("2IntOpenHashMap.") -> true
      path.startsWith("2ObjectOpenCustomHashMap.") -> true
      path.startsWith("2ObjectOpenHashMap.") -> true
      path.startsWith("2ShortMap\$E") -> true
      path.startsWith("2ShortMap.") -> true
      path.startsWith("2ShortMaps.") -> true
      path.startsWith("2ShortOpenHashMap.") -> true
      path.startsWith("ArrayList.") -> true
      path.startsWith("Collection.") -> true
      path.startsWith("Iterab") -> true
      path.startsWith("Iterator.") -> true
      path.startsWith("OpenCustomHashSet.") -> true
      path.startsWith("OpenHashSet.") -> true
      path.startsWith("Set.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ints/Int") -> {
                    val path = path.removePrefix("ints/Int")
                    when {
      path.startsWith("2IntMap.") -> true
      path.startsWith("2IntOpenHashMap.") -> true
      path.startsWith("2ObjectMap\$E") -> true
      path.startsWith("2ObjectMap.") -> true
      path.startsWith("2ObjectMaps.") -> true
      path.startsWith("2ObjectOpenHashMap.") -> true
      path.startsWith("ArrayList.") -> true
      path.startsWith("LinkedOpenHashSet.") -> true
      path.startsWith("List.") -> true
      path.startsWith("OpenHashSet.") -> true
      path.startsWith("Set.") -> true
      path.startsWith("St") -> true
      
      else -> false
    }
                  }
    path.startsWith("Hash\$") -> true
    path.startsWith("HashC") -> true
    path.startsWith("bytes/ByteArrayList.") -> true
    path.startsWith("bytes/ByteList.") -> true
    path.startsWith("doubles/Double2ObjectMap\$E") -> true
    path.startsWith("doubles/Double2ObjectMap.") -> true
    path.startsWith("doubles/Double2ObjectMaps.") -> true
    path.startsWith("doubles/Double2ObjectOpenHashMap.") -> true
    path.startsWith("doubles/DoubleSet.") -> true
    path.startsWith("longs/LongArrayList.") -> true
    path.startsWith("longs/LongList.") -> true
    path.startsWith("longs/LongOpenHashSet.") -> true
    path.startsWith("longs/LongSet.") -> true
    path.startsWith("objects/Reference2IntOpenHashMap.") -> true
    path.startsWith("objects/Reference2ObjectOpenHashMap.") -> true
    path.startsWith("objects/ReferenceOpenHashSet.") -> true
    
    else -> false
  }
                }
                path.startsWith("kotlin/") -> {
                  val path = path.removePrefix("kotlin/")
                  when {
    path.startsWith("D") -> true
    path.startsWith("E") -> true
    path.startsWith("F") -> true
    path.startsWith("K") -> true
    path.startsWith("L") -> true
    path.startsWith("M") -> true
    path.startsWith("N") -> true
    path.startsWith("P") -> true
    path.startsWith("R") -> true
    path.startsWith("T") -> true
    path.startsWith("U") -> true
    path.startsWith("_") -> true
    path.startsWith("c") -> true
    path.startsWith("e") -> true
    path.startsWith("i") -> true
    path.startsWith("j") -> true
    path.startsWith("p") -> true
    path.startsWith("r") -> true
    path.startsWith("script/d") -> true
    path.startsWith("script/t") -> true
    path.startsWith("se") -> true
    path.startsWith("t") -> true
    
    else -> false
  }
                }
  path.startsWith("com/fasterxml/aalto/W") -> true
  path.startsWith("com/fasterxml/aalto/impl/E") -> true
  path.startsWith("com/fasterxml/aalto/in/ByteS") -> true
  path.startsWith("com/fasterxml/aalto/in/InputB") -> true
  path.startsWith("com/fasterxml/aalto/in/ReaderConfig.") -> true
  path.startsWith("com/fasterxml/aalto/s") -> true
  path.startsWith("com/google/common/base/MoreObjects\$ToStringHelper.") -> true
  path.startsWith("com/google/common/base/MoreObjects.") -> true
  path.startsWith("com/google/common/base/Throwables.") -> true
  path.startsWith("com/google/common/collect/Maps.") -> true
  path.startsWith("com/s") -> true
  path.startsWith("g") -> true
  path.startsWith("javaslang/collection/HashM") -> true
  path.startsWith("javaslang/collection/Map.") -> true
  path.startsWith("org/apache/log4j/L") -> true
  path.startsWith("org/codehaus/stax2/XMLStreamR") -> true
  path.startsWith("org/i") -> true
  path.startsWith("org/jdom/El") -> true
  path.startsWith("org/jetbrains/a") -> true
  path.startsWith("org/jetbrains/c") -> true
  path.startsWith("org/picocontainer/C") -> true
  path.startsWith("org/picocontainer/M") -> true
  path.startsWith("org/picocontainer/PicoC") -> true
  
  else -> false
}
      
      // Pruned:
      fun isPruned(path: String) = when {
                path.startsWith("org/jetbrains/kotlin/") -> {
                  val path = path.removePrefix("org/jetbrains/kotlin/")
                  when {
                  path.startsWith("scripting/compiler/plugin/") -> {
                    val path = path.removePrefix("scripting/compiler/plugin/")
                    when {
      path.startsWith("A") -> false
      path.startsWith("C") -> false
      path.startsWith("E") -> false
      path.startsWith("JvmCliScriptEvaluationExtension\$") -> false
      path.startsWith("JvmCliScriptEvaluationExtensionK") -> false
      path.startsWith("ScriptingCommandLineProcessor\$") -> false
      path.startsWith("ScriptingCompilerConfigurationExtension\$") -> false
      path.startsWith("definitions/CliScriptDefinitionProvider\$") -> false
      path.startsWith("definitions/CliScriptReportSink\$") -> false
      path.startsWith("dependencies/ScriptsCompilationDependenciesKt\$") -> false
      path.startsWith("extensions/ScriptingProcessSourcesBeforeCompilingExtension\$") -> false
      path.startsWith("services/Fir2IrScriptConfiguratorExtensionImpl\$c") -> false
      path.startsWith("services/FirScriptConfigurati") -> false
      path.startsWith("services/FirScriptDefinitionProviderService\$Companion\$") -> false
      path.startsWith("services/S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("fir/") -> {
                    val path = path.removePrefix("fir/")
                    when {
                    path.startsWith("expressions/") -> {
                      val path = path.removePrefix("expressions/")
                      when {
                      path.startsWith("builder/Fir") -> {
                        val path = path.removePrefix("builder/Fir")
                        when {
          path.startsWith("AnnotationArgumentMappingBuilder.") -> true
          path.startsWith("AnnotationBuilder.") -> true
          path.startsWith("Cons") -> true
          
          else -> false
        }
                      }
        path.startsWith("ExhaustivenessStatus\$") -> false
        path.startsWith("ExhaustivenessStatusK") -> false
        path.startsWith("FirArgumentU") -> false
        path.startsWith("FirConte") -> false
        path.startsWith("FirEm") -> false
        path.startsWith("FirOperation\$") -> false
        
        else -> true
      }
                    }
                    path.startsWith("declarations/") -> {
                      val path = path.removePrefix("declarations/")
                      when {
                      path.startsWith("FirDeclaration") -> {
                        val path = path.removePrefix("FirDeclaration")
                        when {
                        path.startsWith("Origin\$") -> {
                          val path = path.removePrefix("Origin\$")
                          when {
            path.startsWith("B") -> true
            path.startsWith("D") -> true
            path.startsWith("I") -> true
            path.startsWith("L") -> true
            path.startsWith("SubstitutionOverride.") -> true
            path.startsWith("Synthetic\$Da") -> true
            path.startsWith("Synthetic\$V") -> true
            path.startsWith("Synthetic.") -> true
            path.startsWith("W") -> true
            
            else -> false
          }
                        }
          path.startsWith(".") -> true
          path.startsWith("A") -> true
          path.startsWith("DataK") -> true
          path.startsWith("DataRegistry\$D") -> true
          path.startsWith("DataRegistry.") -> true
          path.startsWith("Origin.") -> true
          path.startsWith("S") -> true
          
          else -> false
        }
                      }
                      path.startsWith("builder/Fir") -> {
                        val path = path.removePrefix("builder/Fir")
                        when {
          path.startsWith("ConstructorBuilder.") -> true
          path.startsWith("ContextReceiverBuilder.") -> true
          path.startsWith("DefaultSetterValueParameterBuilder.") -> true
          path.startsWith("FieldBuilder.") -> true
          path.startsWith("Fu") -> true
          path.startsWith("ImportBuilder.") -> true
          path.startsWith("PropertyAccessorBuilder.") -> true
          path.startsWith("PropertyBuilder.") -> true
          path.startsWith("ReceiverParameterBuilder.") -> true
          path.startsWith("RegularClassBuilder.") -> true
          path.startsWith("ResolvedImportBuilder.") -> true
          path.startsWith("SimpleFunctionBuilder.") -> true
          path.startsWith("TypeParameterBuilder.") -> true
          path.startsWith("ValueParameterBuilder.") -> true
          path.startsWith("Var") -> true
          
          else -> false
        }
                      }
                      path.startsWith("Fir") -> {
                        val path = path.removePrefix("Fir")
                        when {
          path.startsWith("Contro") -> false
          path.startsWith("De") -> false
          path.startsWith("G") -> false
          path.startsWith("In") -> false
          path.startsWith("ResolvePhaseK") -> false
          path.startsWith("ResolvedToPhaseState\$") -> false
          path.startsWith("To") -> false
          path.startsWith("TypeS") -> false
          path.startsWith("ValueC") -> false
          path.startsWith("VersionRequirementsTableKey.") -> false
          
          else -> true
        }
                      }
        path.startsWith("DeclarationUtilsKt.") -> true
        path.startsWith("DeprecationUtilsKt.") -> true
        path.startsWith("DeprecationsPe") -> true
        path.startsWith("DeprecationsProvider.") -> true
        path.startsWith("G") -> true
        path.startsWith("In") -> true
        path.startsWith("U") -> true
        path.startsWith("i") -> true
        path.startsWith("synthetic/FirSyntheticProperty.") -> true
        path.startsWith("synthetic/FirSyntheticPropertyA") -> true
        path.startsWith("utils/FirD") -> true
        
        else -> false
      }
                    }
      path.startsWith("B") -> true
      path.startsWith("Cl") -> true
      path.startsWith("CopyUtilsKt.") -> true
      path.startsWith("DelegatedWrapperData.") -> true
      path.startsWith("DependencyListForCliModule\$B") -> true
      path.startsWith("DependencyListForCliModule\$Companion.") -> true
      path.startsWith("DependencyListForCliModule.") -> true
      path.startsWith("Ef") -> true
      path.startsWith("FirAn") -> true
      path.startsWith("FirEl") -> true
      path.startsWith("FirExpr") -> true
      path.startsWith("FirFi") -> true
      path.startsWith("FirFunctionTy") -> true
      path.startsWith("FirGenerate") -> true
      path.startsWith("FirImpl") -> true
      path.startsWith("FirLab") -> true
      path.startsWith("FirLanguageSettingsComponentK") -> true
      path.startsWith("FirLook") -> true
      path.startsWith("FirModuleData.") -> true
      path.startsWith("FirModuleDataK") -> true
      path.startsWith("FirN") -> true
      path.startsWith("FirP") -> true
      path.startsWith("FirSession\$C") -> true
      path.startsWith("FirSession.") -> true
      path.startsWith("FirSessionC") -> true
      path.startsWith("FirTarget.") -> true
      path.startsWith("FirVisibilityChecker.") -> true
      path.startsWith("FirVisibilityCheckerKt.") -> true
      path.startsWith("Mu") -> true
      path.startsWith("Se") -> true
      path.startsWith("U") -> true
      path.startsWith("analysis/C") -> true
      path.startsWith("analysis/checkers/CommonL") -> true
      path.startsWith("analysis/checkers/ExtendedL") -> true
      path.startsWith("analysis/checkers/La") -> true
      path.startsWith("analysis/checkers/Op") -> true
      path.startsWith("analysis/checkers/conf") -> true
      path.startsWith("analysis/checkers/context/CheckerContext.") -> true
      path.startsWith("analysis/checkers/expression/FirExpressionChecker.") -> true
      path.startsWith("analysis/checkers/extended/A") -> true
      path.startsWith("analysis/checkers/extended/CanBeR") -> true
      path.startsWith("analysis/collectors/components/A") -> true
      path.startsWith("analysis/collectors/components/L") -> true
      path.startsWith("analysis/diagnostics/FirErrors.") -> true
      path.startsWith("analysis/extensions/FirAdditionalCheckersExtension\$F") -> true
      path.startsWith("analysis/extensions/FirAdditionalCheckersExtension.") -> true
      path.startsWith("backend/ConversionUtilsKt.") -> true
      path.startsWith("backend/Fir2IrA") -> true
      path.startsWith("backend/Fir2IrComponents.") -> true
      path.startsWith("backend/Fir2IrConf") -> true
      path.startsWith("backend/Fir2IrExtensions.") -> true
      path.startsWith("backend/Fir2IrPluginContext.") -> true
      path.startsWith("backend/Fir2IrScriptConfiguratorExtension\$F") -> true
      path.startsWith("backend/Fir2IrScriptConfiguratorExtension.") -> true
      path.startsWith("backend/jvm/FirJvmBackendC") -> true
      path.startsWith("backend/jvm/FirJvmBackendExtension.") -> true
      path.startsWith("backend/jvm/JvmFir2IrExtensions.") -> true
      path.startsWith("builder/FirBuilderDslK") -> true
      path.startsWith("builder/FirScriptConfiguratorExtension\$F") -> true
      path.startsWith("builder/FirScriptConfiguratorExtension.") -> true
      path.startsWith("builder/FirScriptConfiguratorExtensionK") -> true
      path.startsWith("caches/FirCache.") -> true
      path.startsWith("caches/FirCacheW") -> true
      path.startsWith("caches/FirCachesFactory.") -> true
      path.startsWith("caches/FirCachesFactoryKt.") -> true
      path.startsWith("caches/FirL") -> true
      path.startsWith("contracts/F") -> true
      path.startsWith("contracts/i") -> true
      path.startsWith("deserialization/AbstractAnnotationDeserializer.") -> true
      path.startsWith("deserialization/ClassDeserializationKt.") -> true
      path.startsWith("deserialization/FirB") -> true
      path.startsWith("deserialization/FirConstDeserializer.") -> true
      path.startsWith("deserialization/FirDeserializationC") -> true
      path.startsWith("deserialization/FirM") -> true
      path.startsWith("diagnostics/ConeAmbiguousF") -> true
      path.startsWith("diagnostics/ConeDiagnostic.") -> true
      path.startsWith("diagnostics/ConeSi") -> true
      path.startsWith("diagnostics/ConeSt") -> true
      path.startsWith("diagnostics/ConeUne") -> true
      path.startsWith("diagnostics/D") -> true
      path.startsWith("extensions/BunchOfRegisteredExtensions.") -> true
      path.startsWith("extensions/DeclarationGenerationContext\$") -> true
      path.startsWith("extensions/FirAn") -> true
      path.startsWith("extensions/FirAssignExpressionAltererExtension\$F") -> true
      path.startsWith("extensions/FirAssignExpressionAltererExtension.") -> true
      path.startsWith("extensions/FirDeclarationGenerationExtension\$F") -> true
      path.startsWith("extensions/FirDeclarationGenerationExtension.") -> true
      path.startsWith("extensions/FirDeclarationGenerationExtensionK") -> true
      path.startsWith("extensions/FirDeclarationP") -> true
      path.startsWith("extensions/FirDeclarationsForMetadataProviderExtension\$F") -> true
      path.startsWith("extensions/FirDeclarationsForMetadataProviderExtension.") -> true
      path.startsWith("extensions/FirExpressionResolutionExtension\$F") -> true
      path.startsWith("extensions/FirExpressionResolutionExtension.") -> true
      path.startsWith("extensions/FirExtension\$") -> true
      path.startsWith("extensions/FirExtension.") -> true
      path.startsWith("extensions/FirExtensionR") -> true
      path.startsWith("extensions/FirExtensionService.") -> true
      path.startsWith("extensions/FirExtensionServiceK") -> true
      path.startsWith("extensions/FirExtensionSessionComponent\$F") -> true
      path.startsWith("extensions/FirExtensionSessionComponent.") -> true
      path.startsWith("extensions/FirExtensionSessionComponentK") -> true
      path.startsWith("extensions/FirFunctionTypeKindExtension\$F") -> true
      path.startsWith("extensions/FirFunctionTypeKindExtension.") -> true
      path.startsWith("extensions/FirRegisteredPluginAnnotations.") -> true
      path.startsWith("extensions/FirRegisteredPluginAnnotationsK") -> true
      path.startsWith("extensions/FirStatusTransformerExtension\$F") -> true
      path.startsWith("extensions/FirStatusTransformerExtension.") -> true
      path.startsWith("extensions/FirSupertypeGenerationExtension\$F") -> true
      path.startsWith("extensions/FirSupertypeGenerationExtension.") -> true
      path.startsWith("extensions/FirTypeAttributeExtension\$F") -> true
      path.startsWith("extensions/FirTypeAttributeExtension.") -> true
      path.startsWith("extensions/predicate/AbstractPredicate.") -> true
      path.startsWith("i") -> true
      path.startsWith("java/FirJavaElementFinder.") -> true
      path.startsWith("java/FirJavaFacade.") -> true
      path.startsWith("java/FirJavaFacadeF") -> true
      path.startsWith("pipeline/A") -> true
      path.startsWith("pipeline/ConvertToIrKt.") -> true
      path.startsWith("pipeline/F") -> true
      path.startsWith("pipeline/M") -> true
      path.startsWith("references/FirB") -> true
      path.startsWith("references/FirC") -> true
      path.startsWith("references/FirD") -> true
      path.startsWith("references/FirE") -> true
      path.startsWith("references/FirF") -> true
      path.startsWith("references/FirNamedReference.") -> true
      path.startsWith("references/FirR") -> true
      path.startsWith("references/FirS") -> true
      path.startsWith("references/FirT") -> true
      path.startsWith("references/i") -> true
      path.startsWith("resolve/BodyResolveComponents.") -> true
      path.startsWith("resolve/De") -> true
      path.startsWith("resolve/DoubleColonLHS.") -> true
      path.startsWith("resolve/FirDoubleColonExpressionResolver.") -> true
      path.startsWith("resolve/FirQ") -> true
      path.startsWith("resolve/FirSamConversionTransformerExtension\$F") -> true
      path.startsWith("resolve/FirSamConversionTransformerExtension.") -> true
      path.startsWith("resolve/FirSamR") -> true
      path.startsWith("resolve/FirTypeResolu") -> true
      path.startsWith("resolve/FirTypeResolver.") -> true
      path.startsWith("resolve/ImplicitIntegerCoercionK") -> true
      path.startsWith("resolve/L") -> true
      path.startsWith("resolve/ResolveUtilsKt.") -> true
      path.startsWith("resolve/ScopeS") -> true
      path.startsWith("resolve/ScopeUtilsKt.") -> true
      path.startsWith("resolve/SupertypeSupplier.") -> true
      path.startsWith("resolve/SupertypeUtilsKt.") -> true
      path.startsWith("resolve/TypeExpansionUtilsKt.") -> true
      path.startsWith("resolve/calls/AbstractCa") -> true
      path.startsWith("resolve/calls/ArgumentT") -> true
      path.startsWith("resolve/calls/Arguments") -> true
      path.startsWith("resolve/calls/CallI") -> true
      path.startsWith("resolve/calls/Candidate.") -> true
      path.startsWith("resolve/calls/CandidateFactoryK") -> true
      path.startsWith("resolve/calls/CheckerSink.") -> true
      path.startsWith("resolve/calls/Er") -> true
      path.startsWith("resolve/calls/FirN") -> true
      path.startsWith("resolve/calls/InapplicableW") -> true
      path.startsWith("resolve/calls/Nu") -> true
      path.startsWith("resolve/calls/ResolutionC") -> true
      path.startsWith("resolve/calls/ResolutionD") -> true
      path.startsWith("resolve/calls/ResolvedCallArgument.") -> true
      path.startsWith("resolve/calls/Unsa") -> true
      path.startsWith("resolve/calls/Unst") -> true
      path.startsWith("resolve/dfa/DataFlowV") -> true
      path.startsWith("resolve/dfa/Im") -> true
      path.startsWith("resolve/dfa/LogicSystemKt\$") -> true
      path.startsWith("resolve/dfa/RealVariable.") -> true
      path.startsWith("resolve/dfa/Stat") -> true
      path.startsWith("resolve/diagnostics/ConeAmbigui") -> true
      path.startsWith("resolve/diagnostics/ConeFo") -> true
      path.startsWith("resolve/diagnostics/ConeOu") -> true
      path.startsWith("resolve/diagnostics/ConeUnresolvedTypeQualifierError.") -> true
      path.startsWith("resolve/diagnostics/ConeVi") -> true
      path.startsWith("resolve/diagnostics/ConeW") -> true
      path.startsWith("resolve/inference/ConeTypeVariableForLambdaR") -> true
      path.startsWith("resolve/inference/InferenceU") -> true
      path.startsWith("resolve/inference/LambdaW") -> true
      path.startsWith("resolve/inference/PostponedArgumentsK") -> true
      path.startsWith("resolve/inference/PostponedR") -> true
      path.startsWith("resolve/inference/Res") -> true
      path.startsWith("resolve/inference/model/ConeA") -> true
      path.startsWith("resolve/inference/model/ConeExpl") -> true
      path.startsWith("resolve/inference/model/ConeR") -> true
      path.startsWith("resolve/providers/FirCompositeCachedSymbolNamesProvider.") -> true
      path.startsWith("resolve/providers/FirCompositeS") -> true
      path.startsWith("resolve/providers/FirP") -> true
      path.startsWith("resolve/providers/FirSymbolNamesProvider.") -> true
      path.startsWith("resolve/providers/FirSymbolNamesProviderW") -> true
      path.startsWith("resolve/providers/FirSymbolP") -> true
      path.startsWith("resolve/providers/i") -> true
      path.startsWith("resolve/substitution/Ch") -> true
      path.startsWith("resolve/substitution/ConeSubstitutor\$") -> true
      path.startsWith("resolve/substitution/ConeSubstitutor.") -> true
      path.startsWith("resolve/substitution/ConeSubstitutorByMap.") -> true
      path.startsWith("resolve/substitution/S") -> true
      path.startsWith("resolve/transformers/FirImportResolveT") -> true
      path.startsWith("resolve/transformers/Im") -> true
      path.startsWith("resolve/transformers/PackageResolutionResult\$P") -> true
      path.startsWith("resolve/transformers/PackageResolutionResult.") -> true
      path.startsWith("resolve/transformers/Ph") -> true
      path.startsWith("resolve/transformers/R") -> true
      path.startsWith("resolve/transformers/Sc") -> true
      path.startsWith("scopes/FakeOverrideSubstitution.") -> true
      path.startsWith("scopes/FakeOverrideTypeCalculator\$D") -> true
      path.startsWith("scopes/FakeOverrideTypeCalculator.") -> true
      path.startsWith("scopes/FakeOverrideTypeCalculatorK") -> true
      path.startsWith("scopes/FirContainingNamesAwareScope.") -> true
      path.startsWith("scopes/FirKotlinScopeProvider.") -> true
      path.startsWith("scopes/FirKotlinScopeProviderK") -> true
      path.startsWith("scopes/FirN") -> true
      path.startsWith("scopes/FirOverrideC") -> true
      path.startsWith("scopes/FirOverrideService.") -> true
      path.startsWith("scopes/FirOverrideServiceK") -> true
      path.startsWith("scopes/FirScope.") -> true
      path.startsWith("scopes/FirScopeKt.") -> true
      path.startsWith("scopes/FirScopeP") -> true
      path.startsWith("scopes/FirTypeP") -> true
      path.startsWith("scopes/FirTypeScope\$") -> true
      path.startsWith("scopes/FirTypeScope.") -> true
      path.startsWith("scopes/FirTypeScopeKt.") -> true
      path.startsWith("scopes/M") -> true
      path.startsWith("scopes/ProcessorAction.") -> true
      path.startsWith("scopes/i") -> true
      path.startsWith("serialization/FirAd") -> true
      path.startsWith("session/IncrementalC") -> true
      path.startsWith("session/e") -> true
      path.startsWith("symbols/ConeClassL") -> true
      path.startsWith("symbols/ConeClassifierLookupTag.") -> true
      path.startsWith("symbols/ConeT") -> true
      path.startsWith("symbols/FirBasedSymbol.") -> true
      path.startsWith("symbols/FirLazyDeclarationResolverK") -> true
      path.startsWith("symbols/i") -> true
      path.startsWith("types/A") -> true
      path.startsWith("types/CompilerConeAttributes\$ContextFunctionTypeParams.") -> true
      path.startsWith("types/CompilerConeAttributes\$Ext") -> true
      path.startsWith("types/ConeAttributes\$") -> true
      path.startsWith("types/ConeAttributes.") -> true
      path.startsWith("types/ConeB") -> true
      path.startsWith("types/ConeClassLikeT") -> true
      path.startsWith("types/ConeD") -> true
      path.startsWith("types/ConeE") -> true
      path.startsWith("types/ConeFlexibleType.") -> true
      path.startsWith("types/ConeInferenceContext.") -> true
      path.startsWith("types/ConeIntegerConstantOperatorType.") -> true
      path.startsWith("types/ConeIntegerConstantOperatorTypeImpl.") -> true
      path.startsWith("types/ConeIntegerLiteralType.") -> true
      path.startsWith("types/ConeInter") -> true
      path.startsWith("types/ConeKotlinType.") -> true
      path.startsWith("types/ConeL") -> true
      path.startsWith("types/ConeN") -> true
      path.startsWith("types/ConeSi") -> true
      path.startsWith("types/ConeTypeContext.") -> true
      path.startsWith("types/ConeTypeIntersector.") -> true
      path.startsWith("types/ConeTypePa") -> true
      path.startsWith("types/ConeTypeProjection\$") -> true
      path.startsWith("types/ConeTypeProjection.") -> true
      path.startsWith("types/ConeTypeUtilsKt.") -> true
      path.startsWith("types/ConeTypeVariableT") -> true
      path.startsWith("types/FirD") -> true
      path.startsWith("types/FirE") -> true
      path.startsWith("types/FirFunctionTypeKindService.") -> true
      path.startsWith("types/FirFunctionTypeKindServiceK") -> true
      path.startsWith("types/FirFunctionTypeR") -> true
      path.startsWith("types/FirI") -> true
      path.startsWith("types/FirP") -> true
      path.startsWith("types/FirQ") -> true
      path.startsWith("types/FirR") -> true
      path.startsWith("types/FirS") -> true
      path.startsWith("types/FirTypeA") -> true
      path.startsWith("types/FirTypeProjection.") -> true
      path.startsWith("types/FirTypeProjectionW") -> true
      path.startsWith("types/FirTypeRef.") -> true
      path.startsWith("types/FirTypeRefW") -> true
      path.startsWith("types/FirTypeU") -> true
      path.startsWith("types/FirU") -> true
      path.startsWith("types/FunctionalTypeUtilsKt.") -> true
      path.startsWith("types/I") -> true
      path.startsWith("types/TypeComponentsK") -> true
      path.startsWith("types/TypeCon") -> true
      path.startsWith("types/TypeUtilsKt.") -> true
      path.startsWith("types/builder/FirResolvedTypeRefBuilder.") -> true
      path.startsWith("types/i") -> true
      path.startsWith("util/PersistentM") -> true
      path.startsWith("utils/exceptions/FirExceptionUtilsKt.") -> true
      path.startsWith("visitors/FirDefaultVisitor.") -> true
      path.startsWith("visitors/FirT") -> true
      path.startsWith("visitors/FirV") -> true
      
      else -> false
    }
                  }
                  path.startsWith("kotlinx/collections/immutable/") -> {
                    val path = path.removePrefix("kotlinx/collections/immutable/")
                    when {
      path.startsWith("ImmutableList\$") -> false
      path.startsWith("ImmutableM") -> false
      path.startsWith("PersistentC") -> false
      
      else -> true
    }
                  }
                  path.startsWith("library/") -> {
                    val path = path.removePrefix("library/")
                    when {
                    path.startsWith("metadata/KlibMetadata") -> {
                      val path = path.removePrefix("metadata/KlibMetadata")
                      when {
        path.startsWith("Ca") -> true
        path.startsWith("D") -> true
        path.startsWith("F") -> true
        path.startsWith("Mo") -> true
        path.startsWith("PackageFragment.") -> true
        path.startsWith("ProtoBuf\$Header.") -> true
        path.startsWith("Serialize") -> true
        
        else -> false
      }
                    }
                    path.startsWith("KotlinLibrary") -> {
                      val path = path.removePrefix("KotlinLibrary")
                      when {
        path.startsWith(".") -> true
        path.startsWith("K") -> true
        path.startsWith("Layout.") -> true
        path.startsWith("V") -> true
        path.startsWith("W") -> true
        
        else -> false
      }
                    }
      path.startsWith("C") -> false
      path.startsWith("IrKotlinLibraryLayout\$") -> false
      path.startsWith("KotlinAbiVersion\$") -> false
      path.startsWith("KotlinAbiVersionK") -> false
      path.startsWith("KotlinIrSignatureVersion\$") -> false
      path.startsWith("KotlinIrSignatureVersionK") -> false
      path.startsWith("L") -> false
      path.startsWith("MetadataKotlinLibraryLayout\$") -> false
      path.startsWith("R") -> false
      path.startsWith("SearchPathResolver\$") -> false
      path.startsWith("SingleFileResolveKt\$") -> false
      path.startsWith("SingleK") -> false
      path.startsWith("ToolingSingleFileKlibResolveStrategy\$") -> false
      path.startsWith("UnresolvedLibraryK") -> false
      path.startsWith("abi/AbiCompoundName\$") -> false
      path.startsWith("abi/AbiQualifiedName\$") -> false
      path.startsWith("abi/AbiReadingFilter\$E") -> false
      path.startsWith("abi/AbiReadingFilter\$N") -> false
      path.startsWith("abi/AbiRenderingSettings\$") -> false
      path.startsWith("abi/AbiSignatureVersion\$") -> false
      path.startsWith("abi/LibraryAbiR") -> false
      path.startsWith("metadata/C") -> false
      path.startsWith("metadata/DeserializedS") -> false
      path.startsWith("metadata/KlibDeserializedContainerSourceK") -> false
      path.startsWith("metadata/resolver/KotlinLibraryResolverK") -> false
      path.startsWith("resolver/D") -> false
      
      else -> true
    }
                  }
                  path.startsWith("load/java/structure/") -> {
                    val path = path.removePrefix("load/java/structure/")
                    when {
                    path.startsWith("Java") -> {
                      val path = path.removePrefix("Java")
                      when {
        path.startsWith("ElementsKt\$") -> false
        path.startsWith("Lo") -> false
        path.startsWith("N") -> false
        path.startsWith("TypeParameterL") -> false
        path.startsWith("Types") -> false
        
        else -> true
      }
                    }
      
      else -> true
    }
                  }
                  path.startsWith("ir/expressions/") -> {
                    val path = path.removePrefix("ir/expressions/")
                    when {
      path.startsWith("IrStatementC") -> false
      path.startsWith("IrStatementOrigin\$") -> false
      path.startsWith("IrStatementOriginK") -> false
      
      else -> true
    }
                  }
                  path.startsWith("descriptors/") -> {
                    val path = path.removePrefix("descriptors/")
                    when {
                    path.startsWith("annotations/Annotat") -> {
                      val path = path.removePrefix("annotations/Annotat")
                      when {
                      path.startsWith("ion") -> {
                        val path = path.removePrefix("ion")
                        when {
          path.startsWith("Descriptor.") -> true
          path.startsWith("DescriptorI") -> true
          path.startsWith("Us") -> true
          path.startsWith("s\$Companion.") -> true
          path.startsWith("s.") -> true
          path.startsWith("sK") -> true
          
          else -> false
        }
                      }
        path.startsWith("edI") -> true
        
        else -> false
      }
                    }
      path.startsWith("Cal") -> true
      path.startsWith("ClassC") -> true
      path.startsWith("ClassD") -> true
      path.startsWith("ClassKind.") -> true
      path.startsWith("Classi") -> true
      path.startsWith("Constr") -> true
      path.startsWith("DeclarationDescriptor.") -> true
      path.startsWith("DeclarationDescriptorN") -> true
      path.startsWith("DeclarationDescriptorV") -> true
      path.startsWith("DeclarationDescriptorWithS") -> true
      path.startsWith("DeclarationDescriptorWithV") -> true
      path.startsWith("DescriptorVisibilities.") -> true
      path.startsWith("DescriptorVisibility.") -> true
      path.startsWith("EffectiveVisibility\$L") -> true
      path.startsWith("EffectiveVisibility\$Pu") -> true
      path.startsWith("EffectiveVisibility.") -> true
      path.startsWith("Fie") -> true
      path.startsWith("Fu") -> true
      path.startsWith("Inl") -> true
      path.startsWith("InvalidModuleExceptionK") -> true
      path.startsWith("Modality.") -> true
      path.startsWith("ModuleC") -> true
      path.startsWith("ModuleDescriptor\$D") -> true
      path.startsWith("ModuleDescriptor.") -> true
      path.startsWith("NotFoundClasses.") -> true
      path.startsWith("PackageFragmentD") -> true
      path.startsWith("PackageFragmentProvider.") -> true
      path.startsWith("PackageFragmentProviderImpl.") -> true
      path.startsWith("PackageFragmentProviderK") -> true
      path.startsWith("PackageFragmentProviderO") -> true
      path.startsWith("PackageViewDescriptor.") -> true
      path.startsWith("Par") -> true
      path.startsWith("Pr") -> true
      path.startsWith("Rec") -> true
      path.startsWith("Scr") -> true
      path.startsWith("Si") -> true
      path.startsWith("SourceElement.") -> true
      path.startsWith("Sup") -> true
      path.startsWith("TypeA") -> true
      path.startsWith("TypeParameterD") -> true
      path.startsWith("ValueClassRepresentation.") -> true
      path.startsWith("ValueD") -> true
      path.startsWith("ValueP") -> true
      path.startsWith("VariableA") -> true
      path.startsWith("VariableDescriptor.") -> true
      path.startsWith("VariableDescriptorWithAccessors.") -> true
      path.startsWith("Visibilities\$L") -> true
      path.startsWith("Visibilities\$Private.") -> true
      path.startsWith("Visibilities\$Pro") -> true
      path.startsWith("Visibilities\$Pu") -> true
      path.startsWith("Visibilities.") -> true
      path.startsWith("Visibility") -> true
      path.startsWith("deserialization/AdditionalClassPartsProvider.") -> true
      path.startsWith("deserialization/PlatformDependentDeclarationFilter.") -> true
      path.startsWith("i") -> true
      path.startsWith("java/JavaVisibilities\$") -> true
      path.startsWith("runtime/components/ReflectJ") -> true
      path.startsWith("runtime/components/ReflectKotlinClassFinder.") -> true
      path.startsWith("runtime/components/RuntimeE") -> true
      path.startsWith("runtime/components/RuntimeSourceElementFactory.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("cli/common/") -> {
                    val path = path.removePrefix("cli/common/")
                    when {
      path.startsWith("CLICompiler\$") -> false
      path.startsWith("CLICompiler.") -> false
      path.startsWith("CLIT") -> false
      path.startsWith("Cod") -> false
      path.startsWith("CommonCompilerPerformanceManager\$") -> false
      path.startsWith("CompilerI") -> false
      path.startsWith("CompilerSystemProperties\$") -> false
      path.startsWith("Cr") -> false
      path.startsWith("FirSessionConstructionUtilsKt\$") -> false
      path.startsWith("Ga") -> false
      path.startsWith("I") -> false
      path.startsWith("J") -> false
      path.startsWith("M") -> false
      path.startsWith("Pe") -> false
      path.startsWith("Us") -> false
      path.startsWith("UtilsKt\$") -> false
      path.startsWith("arguments/Ab") -> false
      path.startsWith("arguments/Argument\$") -> false
      path.startsWith("arguments/ArgumentU") -> false
      path.startsWith("arguments/CommonCompilerArguments\$") -> false
      path.startsWith("arguments/CommonCompilerArgumentsC") -> false
      path.startsWith("arguments/CommonToolArguments\$") -> false
      path.startsWith("arguments/CommonToolArgumentsC") -> false
      path.startsWith("arguments/D") -> false
      path.startsWith("arguments/F") -> false
      path.startsWith("arguments/G") -> false
      path.startsWith("arguments/J") -> false
      path.startsWith("arguments/K2JSCompilerArguments\$") -> false
      path.startsWith("arguments/K2JSCompilerArgumentsC") -> false
      path.startsWith("arguments/K2JSD") -> false
      path.startsWith("arguments/K2JVMCompilerArguments\$") -> false
      path.startsWith("arguments/K2JVMCompilerArgumentsC") -> false
      path.startsWith("arguments/K2Js") -> false
      path.startsWith("arguments/K2M") -> false
      path.startsWith("arguments/K2N") -> false
      path.startsWith("arguments/L") -> false
      path.startsWith("fir/FirDiagnosticsCompilerResultsReporter\$") -> false
      path.startsWith("fir/K") -> false
      path.startsWith("fir/S") -> false
      path.startsWith("messages/AnalyzerWithCompilerReport\$Companion\$") -> false
      path.startsWith("messages/AnalyzerWithCompilerReport\$M") -> false
      path.startsWith("messages/AnalyzerWithCompilerReport\$a") -> false
      path.startsWith("messages/CompilerMessageLocationW") -> false
      path.startsWith("messages/CompilerMessageSeverity\$") -> false
      path.startsWith("messages/De") -> false
      path.startsWith("messages/F") -> false
      path.startsWith("messages/Gra") -> false
      path.startsWith("messages/GroupingMessageCollector\$") -> false
      path.startsWith("messages/I") -> false
      path.startsWith("messages/MessageCollector\$Companion\$") -> false
      path.startsWith("messages/MessageCollector\$D") -> false
      path.startsWith("messages/MessageCollectorU") -> false
      path.startsWith("messages/MessageRenderer\$") -> false
      path.startsWith("messages/OutputMessageUtil\$") -> false
      path.startsWith("messages/P") -> false
      path.startsWith("messages/X") -> false
      path.startsWith("modules/D") -> false
      path.startsWith("modules/ModuleXmlParser\$") -> false
      path.startsWith("output/N") -> false
      path.startsWith("output/OutputUtilsKt\$") -> false
      path.startsWith("p") -> false
      path.startsWith("repl/CompiledClassData\$") -> false
      path.startsWith("repl/HistoryActionsForRepeatAny\$") -> false
      path.startsWith("repl/HistoryActionsForRepeatRecentOnly\$") -> false
      path.startsWith("repl/LineId\$") -> false
      path.startsWith("repl/ScriptArgsWithTypes\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("cli/jvm/") -> {
                    val path = path.removePrefix("cli/jvm/")
                    when {
      path.startsWith("K") -> false
      path.startsWith("index/JvmDependenciesDynamicCompoundIndex\$") -> false
      path.startsWith("index/JvmDependenciesIndexImpl\$") -> false
      path.startsWith("index/SingleJavaFileRootsIndex\$") -> false
      path.startsWith("javac/M") -> false
      path.startsWith("modules/CliJavaModuleFinder\$") -> false
      path.startsWith("modules/CliJavaModuleResolver\$") -> false
      path.startsWith("modules/CoreJrtVirtualFile\$") -> false
      path.startsWith("modules/Ct") -> false
      path.startsWith("modules/JavaModuleGraph\$") -> false
      path.startsWith("plugins/PluginCliParser\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("resolve/") -> {
                    val path = path.removePrefix("resolve/")
                    when {
                    path.startsWith("calls/") -> {
                      val path = path.removePrefix("calls/")
                      when {
                      path.startsWith("inference/model/") -> {
                        val path = path.removePrefix("inference/model/")
                        when {
                        path.startsWith("Constraint") -> {
                          val path = path.removePrefix("Constraint")
                          when {
            path.startsWith(".") -> true
            path.startsWith("Kind.") -> true
            path.startsWith("Position.") -> true
            path.startsWith("Storage.") -> true
            path.startsWith("Sy") -> true
            
            else -> false
          }
                        }
          path.startsWith("Inc") -> true
          path.startsWith("NewConstraintSystemImpl.") -> true
          path.startsWith("S") -> true
          path.startsWith("V") -> true
          
          else -> false
        }
                      }
                      path.startsWith("tower/") -> {
                        val path = path.removePrefix("tower/")
                        when {
          path.startsWith("CandidateApplicability.") -> true
          path.startsWith("Em") -> true
          path.startsWith("Expr") -> true
          path.startsWith("FunctionE") -> true
          path.startsWith("ImplicitsExtensionsResolutionFilter.") -> true
          path.startsWith("LambdaK") -> true
          path.startsWith("NewCallA") -> true
          path.startsWith("NewResolutionOldInferenceKt.") -> true
          path.startsWith("PSIF") -> true
          path.startsWith("PSIKotlinCallA") -> true
          path.startsWith("ScopeW") -> true
          path.startsWith("SimpleP") -> true
          path.startsWith("Su") -> true
          
          else -> false
        }
                      }
        path.startsWith("A") -> true
        path.startsWith("context/B") -> true
        path.startsWith("context/ResolutionC") -> true
        path.startsWith("inference/ConstraintSystemBuilder.") -> true
        path.startsWith("inference/ConstraintSystemBuilderKt.") -> true
        path.startsWith("model/KotlinCallA") -> true
        path.startsWith("model/PartialCallContainer.") -> true
        path.startsWith("model/PartialCallR") -> true
        path.startsWith("model/Q") -> true
        path.startsWith("model/ReceiverExpressionKotlinCallArgument.") -> true
        path.startsWith("model/ResolutionAtoms") -> true
        path.startsWith("model/ResolvedCallAtom.") -> true
        path.startsWith("smartcasts/DataFlowInfo.") -> true
        path.startsWith("smartcasts/DataFlowInfoF") -> true
        path.startsWith("smartcasts/DataFlowValueFactory.") -> true
        path.startsWith("tasks/E") -> true
        path.startsWith("util/CallUtilKt.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("scopes/") -> {
                      val path = path.removePrefix("scopes/")
                      when {
                      path.startsWith("receivers/") -> {
                        val path = path.removePrefix("receivers/")
                        when {
          path.startsWith("ContextR") -> true
          path.startsWith("ExpressionReceiver\$Companion.") -> true
          path.startsWith("ExpressionReceiver.") -> true
          path.startsWith("Ext") -> true
          path.startsWith("ImplicitC") -> true
          path.startsWith("Q") -> true
          path.startsWith("ReceiverV") -> true
          path.startsWith("Tr") -> true
          
          else -> false
        }
                      }
        path.startsWith("Ch") -> true
        path.startsWith("DescriptorKindExclude\$T") -> true
        path.startsWith("DescriptorKindFilter\$Companion.") -> true
        path.startsWith("DescriptorKindFilter.") -> true
        path.startsWith("HierarchicalScope.") -> true
        path.startsWith("ImportingScope.") -> true
        path.startsWith("In") -> true
        path.startsWith("LazyScopeAdapter.") -> true
        path.startsWith("LexicalScope.") -> true
        path.startsWith("MemberScope\$Companion.") -> true
        path.startsWith("MemberScope\$E") -> true
        path.startsWith("MemberScope.") -> true
        path.startsWith("MemberScopeI") -> true
        path.startsWith("SubstitutingScope.") -> true
        path.startsWith("TypeIntersectionScope.") -> true
        path.startsWith("optimization/OptimizingOptions.") -> true
        path.startsWith("utils/ScopeUtilsKt.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("jvm/modules/JavaModule") -> {
                      val path = path.removePrefix("jvm/modules/JavaModule")
                      when {
        path.startsWith("Info\$Companion\$") -> false
        path.startsWith("Info\$R") -> false
        path.startsWith("K") -> false
        path.startsWith("Resolver\$A") -> false
        
        else -> true
      }
                    }
                    path.startsWith("lazy/declarations/") -> {
                      val path = path.removePrefix("lazy/declarations/")
                      when {
        path.startsWith("C") -> true
        path.startsWith("DeclarationProviderFactory.") -> true
        path.startsWith("DeclarationProviderFactoryService.") -> true
        path.startsWith("FileBasedDeclarationProviderFactory.") -> true
        path.startsWith("Pa") -> true
        
        else -> false
      }
                    }
                    path.startsWith("jvm/") -> {
                      val path = path.removePrefix("jvm/")
                      when {
        path.startsWith("Ja") -> true
        path.startsWith("JvmCl") -> true
        path.startsWith("JvmCod") -> true
        path.startsWith("JvmCom") -> true
        path.startsWith("JvmPr") -> true
        path.startsWith("KotlinC") -> true
        path.startsWith("KotlinJavaPsiFacade.") -> true
        path.startsWith("NotFoundPackagesCachingStrategy\$C") -> true
        path.startsWith("NotFoundPackagesCachingStrategy.") -> true
        path.startsWith("Re") -> true
        path.startsWith("diagnostics/C") -> true
        path.startsWith("diagnostics/ErrorsJvm.") -> true
        path.startsWith("diagnostics/JvmDeclarationOrigin.") -> true
        path.startsWith("diagnostics/JvmDeclarationOriginKi") -> true
        path.startsWith("diagnostics/R") -> true
        path.startsWith("extensions/A") -> true
        path.startsWith("extensions/Pac") -> true
        path.startsWith("extensions/S") -> true
        path.startsWith("multiplatform/OptionalAnnotationPackageFragmentProvider.") -> true
        path.startsWith("platform/JvmPlatformA") -> true
        
        else -> false
      }
                    }
      path.startsWith("BindingContext.") -> true
      path.startsWith("BindingTrace.") -> true
      path.startsWith("BindingTraceContext.") -> true
      path.startsWith("CodeAnalyzerInitializer.") -> true
      path.startsWith("Compiler") -> true
      path.startsWith("DeclarationS") -> true
      path.startsWith("DescriptorEquivalenceForOverrides.") -> true
      path.startsWith("DescriptorFactory.") -> true
      path.startsWith("DescriptorT") -> true
      path.startsWith("DescriptorU") -> true
      path.startsWith("ImplicitIntegerCoercion.") -> true
      path.startsWith("ImportP") -> true
      path.startsWith("InlineClassD") -> true
      path.startsWith("Lan") -> true
      path.startsWith("LazyTopDownAnalyzer.") -> true
      path.startsWith("ModuleA") -> true
      path.startsWith("NonR") -> true
      path.startsWith("OverridingS") -> true
      path.startsWith("OverridingUtil.") -> true
      path.startsWith("PlatformDependentAnalyzerServices.") -> true
      path.startsWith("Se") -> true
      path.startsWith("StatementFilter.") -> true
      path.startsWith("TargetEnvironment.") -> true
      path.startsWith("To") -> true
      path.startsWith("TypeResolver.") -> true
      path.startsWith("checkers/OptInN") -> true
      path.startsWith("constants/ConstantValue.") -> true
      path.startsWith("deprecation/DeprecationI") -> true
      path.startsWith("deprecation/DeprecationL") -> true
      path.startsWith("deprecation/DeprecationResolver.") -> true
      path.startsWith("deprecation/DeprecationSettings.") -> true
      path.startsWith("descriptorUtil/DescriptorUtilsKt.") -> true
      path.startsWith("diagnostics/Diagnostics\$Companion.") -> true
      path.startsWith("diagnostics/Diagnostics.") -> true
      path.startsWith("extensions/AnalysisHandlerExtension.") -> true
      path.startsWith("extensions/As") -> true
      path.startsWith("extensions/ExtraImportsProviderExtension\$Companion.") -> true
      path.startsWith("extensions/ExtraImportsProviderExtension.") -> true
      path.startsWith("extensions/SyntheticResolveExtension\$Companion.") -> true
      path.startsWith("extensions/SyntheticResolveExtension.") -> true
      path.startsWith("lazy/DeclarationScopeProvider.") -> true
      path.startsWith("lazy/FileScopeFactory.") -> true
      path.startsWith("lazy/FileScopeProviderK") -> true
      path.startsWith("lazy/FileScopes") -> true
      path.startsWith("lazy/Fo") -> true
      path.startsWith("lazy/ImportF") -> true
      path.startsWith("lazy/K") -> true
      path.startsWith("lazy/ResolveSession.") -> true
      path.startsWith("lazy/ResolveSessionU") -> true
      path.startsWith("lazy/data/KtClassL") -> true
      path.startsWith("multiplatform/I") -> true
      path.startsWith("rep") -> true
      path.startsWith("sam/SamConversionResolver.") -> true
      path.startsWith("sam/SamConversionResolverImpl.") -> true
      path.startsWith("sam/SamW") -> true
      
      else -> false
    }
                  }
                  path.startsWith("psi/stubs/") -> {
                    val path = path.removePrefix("psi/stubs/")
                    when {
                    path.startsWith("elements/Kt") -> {
                      val path = path.removePrefix("elements/Kt")
                      when {
        path.startsWith("ConstantExpressionElementType\$") -> false
        path.startsWith("ContractEffectL") -> false
        path.startsWith("D") -> false
        path.startsWith("FileS") -> false
        path.startsWith("PlaceHolderW") -> false
        path.startsWith("Pri") -> false
        path.startsWith("Se") -> false
        path.startsWith("Str") -> false
        path.startsWith("StubElementType.") -> false
        path.startsWith("To") -> false
        path.startsWith("UserTypeElementType\$") -> false
        path.startsWith("ValueArgumentL") -> false
        
        else -> true
      }
                    }
      path.startsWith("KotlinStubV") -> false
      path.startsWith("elements/S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("backend/common/serialization/") -> {
                    val path = path.removePrefix("backend/common/serialization/")
                    when {
                    path.startsWith("encodings/") -> {
                      val path = path.removePrefix("encodings/")
                      when {
        path.startsWith("BinaryC") -> false
        path.startsWith("BinaryL") -> false
        path.startsWith("Fi") -> false
        path.startsWith("L") -> false
        path.startsWith("TypeA") -> false
        
        else -> true
      }
                    }
                    path.startsWith("proto/Ir") -> {
                      val path = path.removePrefix("proto/Ir")
                      when {
        path.startsWith("Class.") -> true
        path.startsWith("Constructor.") -> true
        path.startsWith("ConstructorCall.") -> true
        path.startsWith("Declaration\$D") -> true
        path.startsWith("Declaration.") -> true
        path.startsWith("DeclarationBase.") -> true
        path.startsWith("DefinitelyNotNullType.") -> true
        path.startsWith("EnumEntry.") -> true
        path.startsWith("File.") -> true
        path.startsWith("Function.") -> true
        path.startsWith("FunctionBase.") -> true
        path.startsWith("Property.") -> true
        path.startsWith("SimpleType.") -> true
        path.startsWith("SimpleTypeLegacy.") -> true
        path.startsWith("SimpleTypeNullability.") -> true
        path.startsWith("Type\$K") -> true
        path.startsWith("Type.") -> true
        path.startsWith("TypeParameter.") -> true
        path.startsWith("ValueParameter.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("FileDeserializerKt.") -> true
        path.startsWith("Fl") -> true
        path.startsWith("InterningService.") -> true
        path.startsWith("K") -> true
        path.startsWith("L") -> true
        
        else -> false
      }
                    }
      path.startsWith("BasicIrModuleDeserializerK") -> true
      path.startsWith("IdSignatureDeserializer.") -> true
      path.startsWith("metadata/i") -> true
      path.startsWith("proto/FileEntry.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("analysis/providers/") -> {
                    val path = path.removePrefix("analysis/providers/")
                    when {
                    path.startsWith("Kotlin") -> {
                      val path = path.removePrefix("Kotlin")
                      when {
        path.startsWith("Anc") -> false
        path.startsWith("AnnotationsResolverK") -> false
        path.startsWith("DeclarationProviderFactory\$") -> false
        path.startsWith("DeclarationProviderMerger\$") -> false
        path.startsWith("GlobalModificationService\$") -> false
        path.startsWith("MessageBusProvider\$") -> false
        path.startsWith("ModificationTrackerFactoryK") -> false
        path.startsWith("PackageProviderK") -> false
        path.startsWith("ResolutionScopeProvider\$") -> false
        
        else -> true
      }
                    }
      path.startsWith("F") -> false
      path.startsWith("P") -> false
      path.startsWith("topics/KotlinM") -> false
      
      else -> true
    }
                  }
                  path.startsWith("ir/declarations/") -> {
                    val path = path.removePrefix("ir/declarations/")
                    when {
                    path.startsWith("IrDeclarationOrigin") -> {
                      val path = path.removePrefix("IrDeclarationOrigin")
                      when {
        path.startsWith("\$DEFI") -> true
        path.startsWith("\$DELEGATED_P") -> true
        path.startsWith("\$ER") -> true
        path.startsWith("\$FA") -> true
        path.startsWith("\$GENERATED_M") -> true
        path.startsWith("\$GENERATED_SI") -> true
        path.startsWith("\$IR_B") -> true
        path.startsWith("\$MO") -> true
        path.startsWith("\$SYNTHETIC_G") -> true
        path.startsWith(".") -> true
        path.startsWith("I") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("A") -> true
        path.startsWith("C") -> true
        path.startsWith("Declaration.") -> true
        path.startsWith("DeclarationB") -> true
        path.startsWith("DeclarationP") -> true
        path.startsWith("Declarations") -> true
        path.startsWith("E") -> true
        path.startsWith("Factory\$") -> true
        path.startsWith("Factory.") -> true
        path.startsWith("Fi") -> true
        path.startsWith("Fu") -> true
        path.startsWith("L") -> true
        path.startsWith("Mem") -> true
        path.startsWith("Mo") -> true
        path.startsWith("Mu") -> true
        path.startsWith("OverridableD") -> true
        path.startsWith("Pa") -> true
        path.startsWith("Pr") -> true
        path.startsWith("ReturnTarget.") -> true
        path.startsWith("S") -> true
        path.startsWith("T") -> true
        path.startsWith("ValueDeclaration.") -> true
        path.startsWith("ValueP") -> true
        path.startsWith("Var") -> true
        
        else -> false
      }
                    }
      path.startsWith("Id") -> true
      path.startsWith("MetadataSource.") -> true
      path.startsWith("S") -> true
      path.startsWith("i") -> true
      
      else -> false
    }
                  }
                  path.startsWith("backend/jvm/") -> {
                    val path = path.removePrefix("backend/jvm/")
                    when {
      path.startsWith("A") -> true
      path.startsWith("InlineClassAbi.") -> true
      path.startsWith("InlineClassAbiK") -> true
      path.startsWith("Inte") -> true
      path.startsWith("JvmBackendContext.") -> true
      path.startsWith("JvmBackendExtension.") -> true
      path.startsWith("JvmCachedDeclarations.") -> true
      path.startsWith("JvmGeneratorExtensions.") -> true
      path.startsWith("JvmGeneratorExtensionsImpl.") -> true
      path.startsWith("JvmIrCodegenFactory\$I") -> true
      path.startsWith("JvmIrCodegenFactory\$JvmIrB") -> true
      path.startsWith("JvmIrCodegenFactory.") -> true
      path.startsWith("JvmIrD") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$GENERATED_MU") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$INLINE_C") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$MULTI_FIELD_VALUE_CLASS_G") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$ST") -> true
      path.startsWith("JvmLoweredDeclarationOrigin\$SYNTHETIC_I") -> true
      path.startsWith("JvmSymbolsK") -> true
      path.startsWith("L") -> true
      path.startsWith("Me") -> true
      path.startsWith("MfvcNode.") -> true
      path.startsWith("MfvcNodeFactoryKt.") -> true
      path.startsWith("MfvcNodeKt.") -> true
      path.startsWith("MfvcNodeWithSubnodes.") -> true
      path.startsWith("NameableMfvcNode.") -> true
      path.startsWith("ReceiverBasedMfvcNodeInstance.") -> true
      path.startsWith("RootMfvcNode.") -> true
      path.startsWith("extensions/ClassGeneratorE") -> true
      path.startsWith("ir/JvmIrB") -> true
      path.startsWith("ir/JvmIrTypeUtilsKt.") -> true
      path.startsWith("ir/JvmIrUtilsKt.") -> true
      path.startsWith("lower/InheritedDefaultMethodsOnClassesLoweringKt.") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$ValueDeclarationRemapper\$a") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$ValueDeclarationRemapper\$m") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering\$b") -> true
      path.startsWith("lower/JvmMultiFieldValueClassLowering.") -> true
      path.startsWith("lower/Repl") -> true
      path.startsWith("serialization/JvmIdSignatureDescriptor.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/symbols/") -> {
                    val path = path.removePrefix("ir/symbols/")
                    when {
                    path.startsWith("Ir") -> {
                      val path = path.removePrefix("Ir")
                      when {
        path.startsWith("BindableSymbol\$") -> false
        path.startsWith("ClassifierE") -> false
        path.startsWith("ClassifierSymbol\$") -> false
        path.startsWith("D") -> false
        path.startsWith("PackageFragmentSymbol\$") -> false
        path.startsWith("ReturnTargetSymbol\$") -> false
        path.startsWith("Symbol\$") -> false
        path.startsWith("SymbolK") -> false
        path.startsWith("ValueSymbol\$") -> false
        
        else -> true
      }
                    }
      
      else -> true
    }
                  }
                  path.startsWith("serialization/deserialization/") -> {
                    val path = path.removePrefix("serialization/deserialization/")
                    when {
      path.startsWith("AnnotationAndConstantLoader.") -> true
      path.startsWith("AnnotationAndConstantLoaderImpl.") -> true
      path.startsWith("ClassDa") -> true
      path.startsWith("ContractDeserializer.") -> true
      path.startsWith("DeserializationCom") -> true
      path.startsWith("DeserializationConfiguration.") -> true
      path.startsWith("DeserializedC") -> true
      path.startsWith("DeserializedPackageFragment.") -> true
      path.startsWith("EnumEntriesDeserializationSupport.") -> true
      path.startsWith("ErrorReporter.") -> true
      path.startsWith("FlexibleTypeDeserializer.") -> true
      path.startsWith("K") -> true
      path.startsWith("L") -> true
      path.startsWith("N") -> true
      path.startsWith("ProtoBasedClassDataFinder.") -> true
      path.startsWith("builtins/BuiltInS") -> true
      path.startsWith("descriptors/DeserializedContainerS") -> true
      path.startsWith("descriptors/DeserializedMemberD") -> true
      
      else -> false
    }
                  }
                  path.startsWith("config/") -> {
                    val path = path.removePrefix("config/")
                    when {
      path.startsWith("ApiVersion\$") -> false
      path.startsWith("Con") -> false
      path.startsWith("ExplicitApiMode\$") -> false
      path.startsWith("I") -> false
      path.startsWith("JvmDefaultMode\$") -> false
      path.startsWith("LanguageFeature\$C") -> false
      path.startsWith("LanguageFeature\$p") -> false
      path.startsWith("LanguageO") -> false
      path.startsWith("LanguageVersion\$") -> false
      path.startsWith("M") -> false
      path.startsWith("S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("konan/library/") -> {
                    val path = path.removePrefix("konan/library/")
                    when {
      path.startsWith("KonanLibraryK") -> false
      path.startsWith("KonanLibraryP") -> false
      path.startsWith("N") -> false
      path.startsWith("S") -> false
      path.startsWith("TargetedW") -> false
      
      else -> true
    }
                  }
                  path.startsWith("js/backend/ast/") -> {
                    val path = path.removePrefix("js/backend/ast/")
                    when {
                    path.startsWith("Js") -> {
                      val path = path.removePrefix("Js")
                      when {
        path.startsWith("ArrayL") -> true
        path.startsWith("BinaryOperati") -> true
        path.startsWith("Br") -> true
        path.startsWith("Cont") -> true
        path.startsWith("Deb") -> true
        path.startsWith("Expression.") -> true
        path.startsWith("ExpressionS") -> true
        path.startsWith("Function.") -> true
        path.startsWith("Int") -> true
        path.startsWith("La") -> true
        path.startsWith("Loo") -> true
        path.startsWith("Name.") -> true
        path.startsWith("NameR") -> true
        path.startsWith("No") -> true
        path.startsWith("ObjectL") -> true
        path.startsWith("Pa") -> true
        path.startsWith("Ret") -> true
        path.startsWith("Sta") -> true
        path.startsWith("Thi") -> true
        path.startsWith("Vars\$") -> true
        path.startsWith("VisitorWithContextImpl.") -> true
        
        else -> false
      }
                    }
      path.startsWith("HasN") -> true
      path.startsWith("metadata/C") -> true
      path.startsWith("metadata/HasMetadata.") -> true
      path.startsWith("metadata/MetadataProperti") -> true
      
      else -> false
    }
                  }
                  path.startsWith("diagnostics/") -> {
                    val path = path.removePrefix("diagnostics/")
                    when {
                    path.startsWith("Diagnostic") -> {
                      val path = path.removePrefix("Diagnostic")
                      when {
                      path.startsWith("Factory") -> {
                        val path = path.removePrefix("Factory")
                        when {
          path.startsWith("\$") -> true
          path.startsWith(".") -> true
          path.startsWith("0.") -> true
          path.startsWith("1.") -> true
          path.startsWith("2.") -> true
          path.startsWith("W") -> true
          
          else -> false
        }
                      }
        path.startsWith(".") -> true
        path.startsWith("C") -> true
        path.startsWith("Reporter.") -> true
        path.startsWith("ReporterFactory.") -> true
        path.startsWith("Sink\$D") -> true
        path.startsWith("WithParameters1.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractD") -> true
      path.startsWith("AbstractKtDiagnosticF") -> true
      path.startsWith("AbstractSourceElementPositioningStrategy.") -> true
      path.startsWith("Errors.") -> true
      path.startsWith("G") -> true
      path.startsWith("KtDiagnostic.") -> true
      path.startsWith("KtDiagnosticFactory0") -> true
      path.startsWith("KtDiagnosticRenderer.") -> true
      path.startsWith("KtDiagnosticReportH") -> true
      path.startsWith("Se") -> true
      path.startsWith("SimpleD") -> true
      path.startsWith("Unb") -> true
      path.startsWith("i") -> true
      path.startsWith("rendering/Ro") -> true
      
      else -> false
    }
                  }
                  path.startsWith("contracts/description/") -> {
                    val path = path.removePrefix("contracts/description/")
                    when {
                    path.startsWith("Kt") -> {
                      val path = path.removePrefix("Kt")
                      when {
        path.startsWith("ContractDescriptionVa") -> false
        path.startsWith("Er") -> false
        
        else -> true
      }
                    }
      path.startsWith("EventOccurrencesRange.") -> true
      path.startsWith("Lo") -> true
      
      else -> false
    }
                  }
                  path.startsWith("types/") -> {
                    val path = path.removePrefix("types/")
                    when {
                    path.startsWith("ConstantValueKind") -> {
                      val path = path.removePrefix("ConstantValueKind")
                      when {
        path.startsWith("\$E") -> false
        path.startsWith("\$Inte") -> false
        path.startsWith("\$U") -> false
        
        else -> true
      }
                    }
                    path.startsWith("model/") -> {
                      val path = path.removePrefix("model/")
                      when {
                      path.startsWith("Type") -> {
                        val path = path.removePrefix("Type")
                        when {
          path.startsWith("ArgumentM") -> true
          path.startsWith("C") -> true
          path.startsWith("P") -> true
          path.startsWith("SystemCom") -> true
          path.startsWith("SystemContext.") -> true
          path.startsWith("SystemContextH") -> true
          path.startsWith("VariableM") -> true
          
          else -> false
        }
                      }
        path.startsWith("C") -> true
        path.startsWith("F") -> true
        path.startsWith("K") -> true
        path.startsWith("Si") -> true
        
        else -> false
      }
                    }
                    path.startsWith("Type") -> {
                      val path = path.removePrefix("Type")
                      when {
        path.startsWith("Attributes\$") -> true
        path.startsWith("Attributes.") -> true
        path.startsWith("CheckerState.") -> true
        path.startsWith("Constructor.") -> true
        path.startsWith("ConstructorSubstitution.") -> true
        path.startsWith("Projection.") -> true
        path.startsWith("ProjectionI") -> true
        path.startsWith("Substitution.") -> true
        path.startsWith("SubstitutionK") -> true
        path.startsWith("Substitutor.") -> true
        path.startsWith("Utils.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractTypeChecker.") -> true
      path.startsWith("AbstractTypeConstructor.") -> true
      path.startsWith("ClassT") -> true
      path.startsWith("Defa") -> true
      path.startsWith("Des") -> true
      path.startsWith("FlexibleTypes") -> true
      path.startsWith("KotlinType.") -> true
      path.startsWith("KotlinTypeFactory.") -> true
      path.startsWith("KotlinTypeK") -> true
      path.startsWith("SimpleType.") -> true
      path.startsWith("Sm") -> true
      path.startsWith("Sp") -> true
      path.startsWith("Un") -> true
      path.startsWith("Variance.") -> true
      path.startsWith("checker/KotlinTypeRefiner.") -> true
      path.startsWith("checker/NewKotlinTypeChecker.") -> true
      path.startsWith("error/ErrorType.") -> true
      path.startsWith("error/ErrorTypeK") -> true
      path.startsWith("error/ErrorUtils.") -> true
      path.startsWith("expressions/ExpressionTypingCon") -> true
      path.startsWith("expressions/K") -> true
      path.startsWith("expressions/O") -> true
      path.startsWith("typeUtil/TypeUtilsKt.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("psi/Kt") -> {
                    val path = path.removePrefix("psi/Kt")
                    when {
      path.startsWith("Annotated.") -> true
      path.startsWith("AnnotationE") -> true
      path.startsWith("AnnotationU") -> true
      path.startsWith("Ba") -> true
      path.startsWith("BinaryExpression.") -> true
      path.startsWith("CallEx") -> true
      path.startsWith("Calla") -> true
      path.startsWith("Class.") -> true
      path.startsWith("ClassB") -> true
      path.startsWith("ClassL") -> true
      path.startsWith("ClassOrObject.") -> true
      path.startsWith("CodeFragment.") -> true
      path.startsWith("Col") -> true
      path.startsWith("Consta") -> true
      path.startsWith("Constructor.") -> true
      path.startsWith("ContextReceiver.") -> true
      path.startsWith("ContractEffect.") -> true
      path.startsWith("Declaration.") -> true
      path.startsWith("DeclarationC") -> true
      path.startsWith("DeclarationM") -> true
      path.startsWith("Element.") -> true
      path.startsWith("ElementI") -> true
      path.startsWith("EnumEntryS") -> true
      path.startsWith("Expression.") -> true
      path.startsWith("File.") -> true
      path.startsWith("Function.") -> true
      path.startsWith("FunctionL") -> true
      path.startsWith("ImportA") -> true
      path.startsWith("ImportD") -> true
      path.startsWith("LambdaE") -> true
      path.startsWith("ModifierList.") -> true
      path.startsWith("ModifierListOwner.") -> true
      path.startsWith("NameReferenceExpression.") -> true
      path.startsWith("NamedDeclaration.") -> true
      path.startsWith("NamedF") -> true
      path.startsWith("ObjectD") -> true
      path.startsWith("Para") -> true
      path.startsWith("Proj") -> true
      path.startsWith("Property.") -> true
      path.startsWith("PropertyA") -> true
      path.startsWith("PsiUtil.") -> true
      path.startsWith("Ret") -> true
      path.startsWith("Script.") -> true
      path.startsWith("SimpleNameExpression.") -> true
      path.startsWith("TreeVisitorV") -> true
      path.startsWith("TypeAl") -> true
      path.startsWith("TypeParameter.") -> true
      path.startsWith("TypePr") -> true
      path.startsWith("TypeReference.") -> true
      path.startsWith("Us") -> true
      path.startsWith("ValueArgument.") -> true
      path.startsWith("VisitorVoid.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("scripting/definitions/Script") -> {
                    val path = path.removePrefix("scripting/definitions/Script")
                    when {
                    path.startsWith("Definition") -> {
                      val path = path.removePrefix("Definition")
                      when {
        path.startsWith("\$FromConfigurations.") -> true
        path.startsWith("\$FromLegacy.") -> true
        path.startsWith(".") -> true
        path.startsWith("P") -> true
        path.startsWith("sS") -> true
        
        else -> false
      }
                    }
      path.startsWith("CompilationConfigurationFromDefinition.") -> true
      path.startsWith("CompilationConfigurationFromDefinitionK") -> true
      path.startsWith("Dep") -> true
      path.startsWith("EvaluationConfigurationFromDefinition.") -> true
      path.startsWith("P") -> true
      
      else -> false
    }
                  }
                  path.startsWith("codegen/") -> {
                    val path = path.removePrefix("codegen/")
                    when {
                    path.startsWith("state/GenerationState") -> {
                      val path = path.removePrefix("state/GenerationState")
                      when {
        path.startsWith("\$B") -> true
        path.startsWith("\$F") -> true
        path.startsWith("\$GenerateClassFilter.") -> true
        path.startsWith(".") -> true
        path.startsWith("EventCallback\$Companion.") -> true
        path.startsWith("EventCallback.") -> true
        path.startsWith("Kt.") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractClassBuilder.") -> true
      path.startsWith("BytesUrlUtils.") -> true
      path.startsWith("ClassBuilder.") -> true
      path.startsWith("ClassBuilderFactories.") -> true
      path.startsWith("ClassBuilderFactory") -> true
      path.startsWith("ClassBuilderM") -> true
      path.startsWith("ClassFileFactory.") -> true
      path.startsWith("CodegenF") -> true
      path.startsWith("DefaultCodegenFactory.") -> true
      path.startsWith("JvmBackendClassResolver.") -> true
      path.startsWith("JvmC") -> true
      path.startsWith("K") -> true
      path.startsWith("MemberCodegen.") -> true
      path.startsWith("MultifileClassCodegen.") -> true
      path.startsWith("PackageCodegen.") -> true
      path.startsWith("context/CodegenContext.") -> true
      path.startsWith("context/P") -> true
      path.startsWith("extensions/C") -> true
      path.startsWith("extensions/ExpressionCodegenExtension\$Com") -> true
      path.startsWith("extensions/ExpressionCodegenExtension.") -> true
      path.startsWith("state/J") -> true
      path.startsWith("state/KotlinTypeMapper.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/types/") -> {
                    val path = path.removePrefix("ir/types/")
                    when {
      path.startsWith("A") -> false
      path.startsWith("B") -> false
      path.startsWith("Id") -> false
      path.startsWith("IrC") -> false
      path.startsWith("IrTypeC") -> false
      path.startsWith("IrTypeS") -> false
      path.startsWith("IrTypeU") -> false
      path.startsWith("IrTypesKt\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("backend/common/") -> {
                    val path = path.removePrefix("backend/common/")
                    when {
                    path.startsWith("extensions/Ir") -> {
                      val path = path.removePrefix("extensions/Ir")
                      when {
        path.startsWith("A") -> true
        path.startsWith("GenerationExtension\$C") -> true
        path.startsWith("GenerationExtension.") -> true
        path.startsWith("PluginContext.") -> true
        path.startsWith("PluginContextImpl\$D") -> true
        
        else -> false
      }
                    }
      path.startsWith("FileLoweringPass.") -> true
      path.startsWith("actualizer/IrActualized") -> true
      path.startsWith("ir/V") -> true
      path.startsWith("lower/LocalDeclarationsLoweringKt.") -> true
      path.startsWith("lower/loops/Ini") -> true
      path.startsWith("lower/loops/Loo") -> true
      path.startsWith("output/O") -> true
      path.startsWith("output/SimpleOutputFileC") -> true
      path.startsWith("phaser/N") -> true
      path.startsWith("phaser/PhaseBuildersKt.") -> true
      path.startsWith("phaser/PhaseConfig.") -> true
      path.startsWith("phaser/PhaseConfigu") -> true
      
      else -> false
    }
                  }
                  path.startsWith("incremental/components/") -> {
                    val path = path.removePrefix("incremental/components/")
                    when {
      path.startsWith("ImportTracker\$") -> false
      path.startsWith("Loc") -> false
      path.startsWith("LookupI") -> false
      path.startsWith("P") -> false
      path.startsWith("S") -> false
      
      else -> true
    }
                  }
                  path.startsWith("load/kotlin/") -> {
                    val path = path.removePrefix("load/kotlin/")
                    when {
      path.startsWith("DeserializationComponentsForJava.") -> true
      path.startsWith("DeserializationComponentsForJavaKt.") -> true
      path.startsWith("DeserializedDescriptorResolver.") -> true
      path.startsWith("JvmPackagePartProviderBase\$M") -> true
      path.startsWith("JvmPackagePartProviderBase.") -> true
      path.startsWith("KotlinBinaryClassCache.") -> true
      path.startsWith("KotlinClassFinder.") -> true
      path.startsWith("Meta") -> true
      path.startsWith("ModuleM") -> true
      path.startsWith("ModuleN") -> true
      path.startsWith("ModuleVisibilityM") -> true
      path.startsWith("PackagePartProvider.") -> true
      path.startsWith("VirtualFileFinder.") -> true
      path.startsWith("VirtualFileFinderF") -> true
      path.startsWith("incremental/IncrementalPackageFragmentProvider.") -> true
      path.startsWith("incremental/IncrementalPackagePartProvider.") -> true
      path.startsWith("incremental/components/I") -> true
      
      else -> false
    }
                  }
                  path.startsWith("constant/") -> {
                    val path = path.removePrefix("constant/")
                    when {
      path.startsWith("De") -> false
      path.startsWith("ErrorValue\$") -> false
      path.startsWith("Inte") -> false
      path.startsWith("KClassValue\$Value\$L") -> false
      path.startsWith("KClassValue\$Value.") -> false
      path.startsWith("Un") -> false
      
      else -> true
    }
                  }
                  path.startsWith("extensions/") -> {
                    val path = path.removePrefix("extensions/")
                    when {
      path.startsWith("Ap") -> false
      path.startsWith("PreprocessedFileCreator\$") -> false
      path.startsWith("StorageComponentContainerContributor\$D") -> false
      path.startsWith("internal/Cal") -> false
      path.startsWith("internal/I") -> false
      path.startsWith("internal/TypeResolutionInterceptorE") -> false
      
      else -> true
    }
                  }
                  path.startsWith("metadata/") -> {
                    val path = path.removePrefix("metadata/")
                    when {
                    path.startsWith("deserialization/") -> {
                      val path = path.removePrefix("deserialization/")
                      when {
        path.startsWith("B") -> true
        path.startsWith("Flags\$B") -> true
        path.startsWith("Flags\$F") -> true
        path.startsWith("Flags.") -> true
        path.startsWith("NameResolver.") -> true
        path.startsWith("NameResolverImpl.") -> true
        
        else -> false
      }
                    }
                    path.startsWith("ProtoBuf\$") -> {
                      val path = path.removePrefix("ProtoBuf\$")
                      when {
        path.startsWith("Annotation\$Argument\$Value\$Type.") -> true
        path.startsWith("Annotation\$Argument\$Value.") -> true
        path.startsWith("Annotation\$Argument.") -> true
        path.startsWith("Annotation.") -> true
        path.startsWith("Class.") -> true
        path.startsWith("Function.") -> true
        path.startsWith("Package.") -> true
        path.startsWith("PackageFragment.") -> true
        path.startsWith("QualifiedNameTable.") -> true
        path.startsWith("StringTable.") -> true
        path.startsWith("Visibility.") -> true
        
        else -> false
      }
                    }
      path.startsWith("builtins/BuiltInsB") -> true
      path.startsWith("jvm/deserialization/JvmMetadataVersion.") -> true
      path.startsWith("jvm/deserialization/ModuleMapping\$") -> true
      path.startsWith("jvm/deserialization/ModuleMapping.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ir/util/") -> {
                    val path = path.removePrefix("ir/util/")
                    when {
                    path.startsWith("IdSignature") -> {
                      val path = path.removePrefix("IdSignature")
                      when {
        path.startsWith("\$A") -> false
        path.startsWith("\$FileL") -> false
        path.startsWith("\$Low") -> false
        path.startsWith("\$S") -> false
        
        else -> true
      }
                    }
      path.startsWith("AdditionalIrUtilsKt.") -> true
      path.startsWith("DescriptorSymbolTableExtension.") -> true
      path.startsWith("DumpIrTreeKt.") -> true
      path.startsWith("DumpIrTreeO") -> true
      path.startsWith("Em") -> true
      path.startsWith("IrFakeOverrideUtilsKt.") -> true
      path.startsWith("IrTypeUtilsKt.") -> true
      path.startsWith("IrUtilsKt.") -> true
      path.startsWith("KotlinMangler\$DescriptorMangler.") -> true
      path.startsWith("KotlinMangler\$IrMangler.") -> true
      path.startsWith("NameProvider.") -> true
      path.startsWith("PatchDeclarationParentsK") -> true
      path.startsWith("RenderIrElementKt.") -> true
      path.startsWith("SymbolTable.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("asJava/") -> {
                    val path = path.removePrefix("asJava/")
                    when {
                    path.startsWith("classes/") -> {
                      val path = path.removePrefix("classes/")
                      when {
        path.startsWith("ImplUtilsKt.") -> true
        path.startsWith("KtDescriptorBasedFakeLightClass.") -> true
        path.startsWith("KtFakeLightClass.") -> true
        path.startsWith("KtLightClass.") -> true
        path.startsWith("KtLightClassForFacade.") -> true
        path.startsWith("KtLightClassForSourceDeclarationKt.") -> true
        path.startsWith("KtUltraLightClass.") -> true
        path.startsWith("KtUltraLightClassForFacade.") -> true
        path.startsWith("KtUltraLightClassForScript.") -> true
        path.startsWith("KtUltraLightSup") -> true
        path.startsWith("UltraLightUtilsKt.") -> true
        
        else -> false
      }
                    }
      path.startsWith("KotlinAsJavaSupport\$") -> true
      path.startsWith("KotlinAsJavaSupport.") -> true
      path.startsWith("KotlinAsJavaSupportBase\$D") -> true
      path.startsWith("KotlinAsJavaSupportBase.") -> true
      path.startsWith("Kt") -> true
      path.startsWith("LightClassG") -> true
      path.startsWith("S") -> true
      path.startsWith("builder/ClsWrapperStubPsiFactory.") -> true
      path.startsWith("builder/LightElementOrigin.") -> true
      path.startsWith("builder/LightElementOriginK") -> true
      path.startsWith("finder/JavaElementFinder.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("util") -> {
                    val path = path.removePrefix("util")
                    when {
                    path.startsWith("s/") -> {
                      val path = path.removePrefix("s/")
                      when {
        path.startsWith("Collections") -> true
        path.startsWith("DFS\$Ne") -> true
        path.startsWith("DFS.") -> true
        path.startsWith("E") -> true
        path.startsWith("KotlinPaths\$J") -> true
        path.startsWith("KotlinPaths.") -> true
        path.startsWith("KotlinPathsFromHomeDir.") -> true
        path.startsWith("Met") -> true
        path.startsWith("ParametersMapKt.") -> true
        path.startsWith("Pat") -> true
        path.startsWith("Pla") -> true
        path.startsWith("Printe") -> true
        path.startsWith("SmartList.") -> true
        path.startsWith("SmartSet\$C") -> true
        path.startsWith("SmartSet.") -> true
        path.startsWith("SortUtilsKt.") -> true
        path.startsWith("addToStdlib/AddToStdlibKt.") -> true
        path.startsWith("exceptions/ExceptionAttachmentBuilder.") -> true
        path.startsWith("exceptions/KotlinExceptionWithAttachments.") -> true
        path.startsWith("exceptions/KotlinIllegalA") -> true
        path.startsWith("r") -> true
        
        else -> false
      }
                    }
                    path.startsWith("/") -> {
                      val path = path.removePrefix("/")
                      when {
        path.startsWith("AbstractArrayMapOwner.") -> true
        path.startsWith("ArrayMapA") -> true
        path.startsWith("L") -> true
        path.startsWith("OperatorN") -> true
        path.startsWith("Pe") -> true
        path.startsWith("TypeRegistry.") -> true
        path.startsWith("U") -> true
        path.startsWith("WeakPair.") -> true
        path.startsWith("Wi") -> true
        path.startsWith("ca") -> true
        path.startsWith("j") -> true
        path.startsWith("slicedMap/Rea") -> true
        path.startsWith("slicedMap/W") -> true
        
        else -> false
      }
                    }
      
      else -> false
    }
                  }
                  path.startsWith("load/java/") -> {
                    val path = path.removePrefix("load/java/")
                    when {
      path.startsWith("JavaClassFinder\$") -> true
      path.startsWith("JavaClassFinder.") -> true
      path.startsWith("JavaClassFinderImplK") -> true
      path.startsWith("JavaClassesTracker.") -> true
      path.startsWith("JavaTypeEnhancementState\$Companion.") -> true
      path.startsWith("JavaTypeEnhancementState.") -> true
      path.startsWith("JvmAb") -> true
      path.startsWith("JvmAnnotationNamesK") -> true
      path.startsWith("components/FilesByFacadeFqNameIndexer.") -> true
      path.startsWith("components/JavaDeprecati") -> true
      path.startsWith("components/JavaResolverCache.") -> true
      path.startsWith("lazy/LazyJavaPackageFragmentProvider.") -> true
      path.startsWith("lazy/ModuleClassResolver.") -> true
      path.startsWith("lazy/S") -> true
      path.startsWith("sources/JavaSourceElementF") -> true
      
      else -> false
    }
                  }
                  path.startsWith("KtFakeSourceElementKind") -> {
                    val path = path.removePrefix("KtFakeSourceElementKind")
                    when {
                    path.startsWith("\$") -> {
                      val path = path.removePrefix("\$")
                      when {
        path.startsWith("Cont") -> true
        path.startsWith("Dat") -> true
        path.startsWith("Def") -> true
        path.startsWith("ImplicitT") -> true
        path.startsWith("Si") -> true
        path.startsWith("SmartCastE") -> true
        
        else -> false
      }
                    }
      path.startsWith(".") -> true
      
      else -> false
    }
                  }
    path.startsWith("A") -> true
    path.startsWith("KtIo") -> true
    path.startsWith("KtL") -> true
    path.startsWith("KtNodeTypes") -> true
    path.startsWith("KtPsiSourceElement.") -> true
    path.startsWith("KtPsiSourceFile.") -> true
    path.startsWith("KtSourceE") -> true
    path.startsWith("KtSourceFile.") -> true
    path.startsWith("KtSourceFileLinesMapping.") -> true
    path.startsWith("KtV") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInDecompiler.") -> true
    path.startsWith("analysis/decompiler/psi/KotlinBuiltInF") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache\$C") -> true
    path.startsWith("analysis/decompiler/stub/file/ClsKotlinBinaryClassCache.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinClsStubBuilder.") -> true
    path.startsWith("analysis/decompiler/stub/file/KotlinMetadataStubBuilder.") -> true
    path.startsWith("analysis/project/structure/KotlinModuleDependentsProvider.") -> true
    path.startsWith("analysis/project/structure/KtBi") -> true
    path.startsWith("analysis/project/structure/KtModule.") -> true
    path.startsWith("analysis/project/structure/KtModuleUtilsKt.") -> true
    path.startsWith("analysis/project/structure/i") -> true
    path.startsWith("analyzer/AnalysisResult\$Compa") -> true
    path.startsWith("analyzer/AnalysisResult\$R") -> true
    path.startsWith("analyzer/AnalysisResult.") -> true
    path.startsWith("analyzer/K") -> true
    path.startsWith("analyzer/ModuleInfo.") -> true
    path.startsWith("builtins/KotlinBuiltIns.") -> true
    path.startsWith("builtins/PrimitiveType.") -> true
    path.startsWith("builtins/StandardNames.") -> true
    path.startsWith("builtins/functions/FunctionInterfaceF") -> true
    path.startsWith("builtins/functions/FunctionTypeKind\$F") -> true
    path.startsWith("builtins/functions/FunctionTypeKind.") -> true
    path.startsWith("builtins/functions/FunctionTypeKindK") -> true
    path.startsWith("builtins/jvm/JvmBuiltIns\$K") -> true
    path.startsWith("builtins/jvm/JvmBuiltIns.") -> true
    path.startsWith("builtins/jvm/JvmBuiltInsPackageFragmentProvider.") -> true
    path.startsWith("cli/plugins/Plugins") -> true
    path.startsWith("compiler/plugin/CommandLineProcessor.") -> true
    path.startsWith("compiler/plugin/Comp") -> true
    path.startsWith("compiler/plugin/E") -> true
    path.startsWith("compilerRunner/OutputItemsCollector.") -> true
    path.startsWith("container/ComponentP") -> true
    path.startsWith("container/ContainerK") -> true
    path.startsWith("container/Ds") -> true
    path.startsWith("container/StorageComponentContainer.") -> true
    path.startsWith("context/ContextKt.") -> true
    path.startsWith("context/ModuleContext.") -> true
    path.startsWith("context/MutableModuleContext.") -> true
    path.startsWith("context/ProjectContext.") -> true
    path.startsWith("contracts/ContractDeserializerImpl.") -> true
    path.startsWith("fileClasses/JvmFileClassI") -> true
    path.startsWith("fileClasses/JvmFileClassUtil.") -> true
    path.startsWith("fileClasses/JvmFileClassUtilK") -> true
    path.startsWith("fileClasses/O") -> true
    path.startsWith("frontend/java/di/InjectionKt.") -> true
    path.startsWith("idea/KotlinF") -> true
    path.startsWith("idea/KotlinL") -> true
    path.startsWith("idea/MainFunctionDetector.") -> true
    path.startsWith("idea/references/KotlinP") -> true
    path.startsWith("incremental/CompilationTransaction.") -> true
    path.startsWith("incremental/Transa") -> true
    path.startsWith("incremental/js/IncrementalDataProvider.") -> true
    path.startsWith("incremental/js/IncrementalN") -> true
    path.startsWith("incremental/js/IncrementalResultsConsumer.") -> true
    path.startsWith("ir/IrBuiltIns.") -> true
    path.startsWith("ir/IrElement.") -> true
    path.startsWith("ir/IrFileEntry.") -> true
    path.startsWith("ir/IrS") -> true
    path.startsWith("ir/O") -> true
    path.startsWith("ir/backend/js/JsIrBackendContext.") -> true
    path.startsWith("ir/backend/js/ir/JsIrBuilder.") -> true
    path.startsWith("ir/backend/js/lower/SecondaryConstructorLowering\$T") -> true
    path.startsWith("ir/backend/js/lower/calls/CallsT") -> true
    path.startsWith("ir/backend/js/lower/calls/Rep") -> true
    path.startsWith("ir/backend/jvm/JvmLibraryResolverK") -> true
    path.startsWith("ir/backend/jvm/serialization/JvmDescriptorMangler.") -> true
    path.startsWith("ir/backend/jvm/serialization/JvmIrMangler.") -> true
    path.startsWith("ir/builders/E") -> true
    path.startsWith("ir/builders/IrBlockBu") -> true
    path.startsWith("ir/builders/IrBuilderW") -> true
    path.startsWith("ir/builders/IrGeneratorContext.") -> true
    path.startsWith("ir/builders/S") -> true
    path.startsWith("ir/builders/declarations/DeclarationBuildersKt.") -> true
    path.startsWith("ir/builders/declarations/IrFu") -> true
    path.startsWith("ir/builders/declarations/IrP") -> true
    path.startsWith("ir/builders/declarations/IrValueParameterBuilder.") -> true
    path.startsWith("ir/descriptors/IrBasedDescriptorsKt.") -> true
    path.startsWith("ir/descriptors/IrBasedEr") -> true
    path.startsWith("ir/descriptors/IrBasedPropertyD") -> true
    path.startsWith("ir/descriptors/IrBasedS") -> true
    path.startsWith("ir/visitors/IrElementTransformerVoid.") -> true
    path.startsWith("ir/visitors/IrElementVisitor.") -> true
    path.startsWith("ir/visitors/IrElementVisitorV") -> true
    path.startsWith("ir/visitors/IrV") -> true
    path.startsWith("javac/JavaC") -> true
    path.startsWith("javac/JavacWrapper\$C") -> true
    path.startsWith("javac/JavacWrapper.") -> true
    path.startsWith("javac/JavacWrapperK") -> true
    path.startsWith("javac/components/JavacBasedC") -> true
    path.startsWith("javac/components/JavacBasedSourceElementF") -> true
    path.startsWith("javac/components/S") -> true
    path.startsWith("javac/resolve/K") -> true
    path.startsWith("javac/resolve/M") -> true
    path.startsWith("js/config/Ec") -> true
    path.startsWith("js/config/ErrorTolerancePolicy.") -> true
    path.startsWith("js/config/JS") -> true
    path.startsWith("js/config/S") -> true
    path.startsWith("js/config/WasmTarget.") -> true
    path.startsWith("js/coroutine/CoroutineBl") -> true
    path.startsWith("js/coroutine/CoroutineM") -> true
    path.startsWith("js/coroutine/CoroutinePassesKt\$replaceC") -> true
    path.startsWith("js/coroutine/CoroutinePassesKt.") -> true
    path.startsWith("js/coroutine/CoroutineTransformationContext.") -> true
    path.startsWith("js/inline/util/rewriters/C") -> true
    path.startsWith("js/inline/util/rewriters/N") -> true
    path.startsWith("js/inline/util/rewriters/R") -> true
    path.startsWith("js/inline/util/rewriters/T") -> true
    path.startsWith("js/parser/sourcemaps/SourceMapSo") -> true
    path.startsWith("js/resolve/M") -> true
    path.startsWith("js/translate/context/N") -> true
    path.startsWith("js/translate/ext") -> true
    path.startsWith("js/translate/utils/JsAstUtils.") -> true
    path.startsWith("kdoc/lexer/KDocToken.") -> true
    path.startsWith("kdoc/lexer/KDocTokens.") -> true
    path.startsWith("kdoc/parser/KDocE") -> true
    path.startsWith("kdoc/parser/KDocK") -> true
    path.startsWith("kdoc/ps") -> true
    path.startsWith("konan/file/File\$C") -> true
    path.startsWith("konan/file/File.") -> true
    path.startsWith("konan/file/FileK") -> true
    path.startsWith("konan/file/ZipFileSystemA") -> true
    path.startsWith("konan/file/ZipFileSystemI") -> true
    path.startsWith("konan/file/ZipUtilKt.") -> true
    path.startsWith("konan/properties/P") -> true
    path.startsWith("konan/target/KonanTarget.") -> true
    path.startsWith("konan/util/DependencyDownloader\$R") -> true
    path.startsWith("konan/util/S") -> true
    path.startsWith("lexer/KtM") -> true
    path.startsWith("lexer/KtS") -> true
    path.startsWith("lexer/KtTokens") -> true
    path.startsWith("modules/J") -> true
    path.startsWith("modules/M") -> true
    path.startsWith("modules/T") -> true
    path.startsWith("mpp/C") -> true
    path.startsWith("mpp/Fu") -> true
    path.startsWith("mpp/P") -> true
    path.startsWith("mpp/R") -> true
    path.startsWith("mpp/S") -> true
    path.startsWith("mpp/T") -> true
    path.startsWith("mpp/V") -> true
    path.startsWith("name/CallableId.") -> true
    path.startsWith("name/Cl") -> true
    path.startsWith("name/FqName.") -> true
    path.startsWith("name/FqNameUnsafe.") -> true
    path.startsWith("name/FqNamesUtilKt.") -> true
    path.startsWith("name/Name.") -> true
    path.startsWith("name/NativeForwardDeclarationKind.") -> true
    path.startsWith("name/NativeS") -> true
    path.startsWith("name/Sp") -> true
    path.startsWith("name/StandardClassIds\$Ca") -> true
    path.startsWith("name/StandardClassIds.") -> true
    path.startsWith("net/jpountz/lz4/LZ4Compressor.") -> true
    path.startsWith("net/jpountz/lz4/LZ4Fa") -> true
    path.startsWith("one/util/streamex/A") -> true
    path.startsWith("one/util/streamex/E") -> true
    path.startsWith("one/util/streamex/StreamEx.") -> true
    path.startsWith("parsing/KotlinParserDefinition.") -> true
    path.startsWith("platform/CommonPlatforms.") -> true
    path.startsWith("platform/TargetPlatform.") -> true
    path.startsWith("platform/jvm/JvmPlatformK") -> true
    path.startsWith("platform/jvm/JvmPlatforms.") -> true
    path.startsWith("platform/konan/NativePlatforms.") -> true
    path.startsWith("progress/P") -> true
    path.startsWith("protobuf/CodedInputStream.") -> true
    path.startsWith("protobuf/ExtensionRegistryLite.") -> true
    path.startsWith("protobuf/ProtocolS") -> true
    path.startsWith("psi/Ca") -> true
    path.startsWith("psi/ValueArgument.") -> true
    path.startsWith("psi/VisitorWrappersKt.") -> true
    path.startsWith("psi/psiUtil/KtPsiUtilKt.") -> true
    path.startsWith("psi/psiUtil/PsiUtilsKt.") -> true
    path.startsWith("psi2ir/generators/fragments/EvaluatorFragmentInfo.") -> true
    path.startsWith("renderer/DescriptorRenderer.") -> true
    path.startsWith("scripting/U") -> true
    path.startsWith("scripting/con") -> true
    path.startsWith("scripting/definitions/KotlinScriptDefinition.") -> true
    path.startsWith("scripting/extensions/ScriptExtraImportsProviderExtension.") -> true
    path.startsWith("scripting/extensions/Scripti") -> true
    path.startsWith("scripting/resolve/KotlinScriptDefinitionFromAnnotatedTemplate.") -> true
    path.startsWith("scripting/resolve/RefineCompilationConfigurationKt.") -> true
    path.startsWith("scripting/resolve/Rep") -> true
    path.startsWith("scripting/resolve/Res") -> true
    path.startsWith("scripting/resolve/ScriptCompilationConfigurationWrapper.") -> true
    path.startsWith("scripting/resolve/ScriptL") -> true
    path.startsWith("scripting/resolve/ScriptR") -> true
    path.startsWith("scripting/resolve/VirtualFileScriptSource.") -> true
    path.startsWith("serialization/DescriptorSerializerP") -> true
    path.startsWith("serialization/SerializerExtensionP") -> true
    path.startsWith("serialization/js/M") -> true
    path.startsWith("serialization/konan/i") -> true
    path.startsWith("storage/CacheWithNo") -> true
    path.startsWith("storage/La") -> true
    path.startsWith("storage/LockBasedStorageManager.") -> true
    path.startsWith("storage/M") -> true
    path.startsWith("storage/Not") -> true
    path.startsWith("storage/Nu") -> true
    path.startsWith("storage/St") -> true
    
    else -> false
  }
                }
                path.startsWith("com/intellij/") -> {
                  val path = path.removePrefix("com/intellij/")
                  when {
                  path.startsWith("psi/") -> {
                    val path = path.removePrefix("psi/")
                    when {
                    path.startsWith("controlFlow/") -> {
                      val path = path.removePrefix("controlFlow/")
                      when {
                      path.startsWith("ControlFlow") -> {
                        val path = path.removePrefix("ControlFlow")
                        when {
          path.startsWith(".") -> true
          path.startsWith("Factory.") -> true
          path.startsWith("O") -> true
          path.startsWith("P") -> true
          path.startsWith("Util.") -> true
          
          else -> false
        }
                      }
        path.startsWith("AnalysisCanceledE") -> true
        path.startsWith("L") -> true
        
        else -> false
      }
                    }
                    path.startsWith("codeStyle/") -> {
                      val path = path.removePrefix("codeStyle/")
                      when {
        path.startsWith("Co") -> true
        path.startsWith("J") -> true
        path.startsWith("NameUtil\$Matcher.") -> true
        path.startsWith("V") -> true
        
        else -> false
      }
                    }
      path.startsWith("AbstractFileViewProvider\$") -> false
      path.startsWith("B") -> false
      path.startsWith("ClassFileViewProvider\$") -> false
      path.startsWith("CommonR") -> false
      path.startsWith("Cu") -> false
      path.startsWith("De") -> false
      path.startsWith("ElementD") -> false
      path.startsWith("EmptyR") -> false
      path.startsWith("EmptySubstitutor\$") -> false
      path.startsWith("ExternalChangeAction\$") -> false
      path.startsWith("ExternallyA") -> false
      path.startsWith("GenericsUtil\$") -> false
      path.startsWith("JV") -> false
      path.startsWith("JavaCodeFragment\$VisibilityChecker\$1") -> false
      path.startsWith("JavaCodeFragment\$VisibilityChecker\$2") -> false
      path.startsWith("JavaCodeFragmentF") -> false
      path.startsWith("JavaCom") -> false
      path.startsWith("JavaRecursiveElementWalkingVisitor\$") -> false
      path.startsWith("JavaResolveResult\$") -> false
      path.startsWith("JvmC") -> false
      path.startsWith("LambdaUtil\$") -> false
      path.startsWith("LanguageA") -> false
      path.startsWith("LanguageSubstitutors\$") -> false
      path.startsWith("LiteralTextEscaper\$") -> false
      path.startsWith("M") -> false
      path.startsWith("No") -> false
      path.startsWith("Pa") -> false
      path.startsWith("PlainTextTokenTypes\$") -> false
      path.startsWith("PsiAnchor\$1") -> false
      path.startsWith("PsiAnchor\$H") -> false
      path.startsWith("PsiAnchor\$P") -> false
      path.startsWith("PsiAnchor\$T") -> false
      path.startsWith("PsiAnnotationA") -> false
      path.startsWith("PsiAnnotationC") -> false
      path.startsWith("PsiAnnotationE") -> false
      path.startsWith("PsiAnnotationS") -> false
      path.startsWith("PsiCh") -> false
      path.startsWith("PsiClassType\$1") -> false
      path.startsWith("PsiClassType\$ClassResolveResult\$") -> false
      path.startsWith("PsiConstr") -> false
      path.startsWith("PsiDiamondType\$DiamondInferenceResult\$") -> false
      path.startsWith("PsiDiamondTypeImpl\$") -> false
      path.startsWith("PsiDocCommentB") -> false
      path.startsWith("PsiElementFactory\$") -> false
      path.startsWith("PsiElementVisitor\$") -> false
      path.startsWith("PsiExpressionC") -> false
      path.startsWith("PsiFileW") -> false
      path.startsWith("PsiJShellS") -> false
      path.startsWith("PsiJvmC") -> false
      path.startsWith("PsiJvmM") -> false
      path.startsWith("PsiLanguageInjectionHost\$S") -> false
      path.startsWith("PsiLargeF") -> false
      path.startsWith("PsiLis") -> false
      path.startsWith("PsiLiteral.") -> false
      path.startsWith("PsiLiteralV") -> false
      path.startsWith("PsiMig") -> false
      path.startsWith("PsiModifia") -> false
      path.startsWith("PsiNes") -> false
      path.startsWith("PsiParserFacade\$") -> false
      path.startsWith("PsiPolyVariantReferenceB") -> false
      path.startsWith("PsiQualifiedN") -> false
      path.startsWith("PsiQualifiedR") -> false
      path.startsWith("PsiRecursiveElementV") -> false
      path.startsWith("PsiRecursiveElementWalkingVisitor\$") -> false
      path.startsWith("PsiReferenceBase\$I") -> false
      path.startsWith("PsiTe") -> false
      path.startsWith("PsiTreeA") -> false
      path.startsWith("PsiTreeChangeA") -> false
      path.startsWith("PsiType\$") -> false
      path.startsWith("PsiTypeCodeFragment\$") -> false
      path.startsWith("PsiTypeVa") -> false
      path.startsWith("PsiWa") -> false
      path.startsWith("ReferenceP") -> false
      path.startsWith("ResolveState\$") -> false
      path.startsWith("Resolvi") -> false
      path.startsWith("Str") -> false
      path.startsWith("SyntaxTraverser\$1") -> false
      path.startsWith("SyntaxTraverser\$AS") -> false
      path.startsWith("SyntaxTraverser\$Api\$") -> false
      path.startsWith("SyntaxTraverser\$Api.") -> false
      path.startsWith("SyntaxTraverser\$ApiEx\$") -> false
      path.startsWith("SyntaxTraverser\$F") -> false
      path.startsWith("SyntaxTraverser\$L") -> false
      path.startsWith("SyntaxTraverser\$P") -> false
      path.startsWith("TokenType\$") -> false
      path.startsWith("TypeAnnotationProvider\$1") -> false
      path.startsWith("W") -> false
      path.startsWith("augment/PsiE") -> false
      path.startsWith("cs") -> false
      path.startsWith("filters/Cont") -> false
      path.startsWith("filters/ElementFilterB") -> false
      path.startsWith("filters/c") -> false
      path.startsWith("filters/element/ModifierFilter\$") -> false
      path.startsWith("filters/p") -> false
      path.startsWith("infos/MethodCandidateInfo\$ApplicabilityLevel.") -> false
      path.startsWith("meta/PsiW") -> false
      path.startsWith("presentation/java/ClassPresentationProvider\$") -> false
      path.startsWith("presentation/java/ClassPresentationU") -> false
      path.startsWith("presentation/java/J") -> false
      path.startsWith("presentation/java/PackagePresentationProvider\$") -> false
      path.startsWith("presentation/java/VariablePresentationProvider\$") -> false
      path.startsWith("scope/B") -> false
      path.startsWith("scope/ElementClassFilter\$") -> false
      path.startsWith("scope/PatternResolveState\$") -> false
      path.startsWith("scope/Pr") -> false
      path.startsWith("scope/PsiScopeProcessor\$Event\$") -> false
      path.startsWith("scope/conflictResolvers/JavaMethodsConflictResolver\$") -> false
      path.startsWith("scope/processor/FilterE") -> false
      path.startsWith("scope/processor/MethodCandidatesProcessor\$") -> false
      path.startsWith("scope/processor/MethodResolveP") -> false
      path.startsWith("scope/processor/V") -> false
      path.startsWith("search/F") -> false
      path.startsWith("search/GlobalSearchScope\$") -> false
      path.startsWith("search/GlobalSearchScopeU") -> false
      path.startsWith("search/N") -> false
      path.startsWith("search/ProjectA") -> false
      path.startsWith("search/ProjectScopeB") -> false
      path.startsWith("search/ProjectScopeI") -> false
      path.startsWith("search/PsiElementProcessor\$CollectElementsW") -> false
      path.startsWith("search/PsiElementProcessor\$CollectF") -> false
      path.startsWith("search/PsiElementProcessorA") -> false
      path.startsWith("search/PsiN") -> false
      path.startsWith("search/searches/ExtensibleQueryFactory\$") -> false
      path.startsWith("stubs/BinaryFileStubBuilder.") -> false
      path.startsWith("stubs/DefaultStubBuilder\$") -> false
      path.startsWith("stubs/E") -> false
      path.startsWith("stubs/H") -> false
      path.startsWith("stubs/Indexi") -> false
      path.startsWith("stubs/M") -> false
      path.startsWith("stubs/ObjectStubB") -> false
      path.startsWith("stubs/ObjectStubTree\$") -> false
      path.startsWith("stubs/Se") -> false
      path.startsWith("stubs/Str") -> false
      path.startsWith("stubs/StubBu") -> false
      path.startsWith("stubs/StubElementT") -> false
      path.startsWith("stubs/StubF") -> false
      path.startsWith("stubs/StubL") -> false
      path.startsWith("stubs/StubSerializa") -> false
      path.startsWith("stubs/StubSp") -> false
      path.startsWith("stubs/StubTe") -> false
      path.startsWith("stubs/U") -> false
      path.startsWith("ta") -> false
      path.startsWith("templateLanguages/TemplateLanguageF") -> false
      path.startsWith("templateLanguages/TemplateLanguageU") -> false
      path.startsWith("templateLanguages/Tr") -> false
      
      else -> true
    }
                  }
                  path.startsWith("ide/plugins/") -> {
                    val path = path.removePrefix("ide/plugins/")
                    when {
      path.startsWith("C") -> true
      path.startsWith("Di") -> true
      path.startsWith("IdeaPluginDescriptorImpl.") -> true
      path.startsWith("PluginDescriptorLoader.") -> true
      path.startsWith("PluginManagerCore.") -> true
      path.startsWith("PluginU") -> true
      path.startsWith("c") -> true
      
      else -> false
    }
                  }
                  path.startsWith("openapi/") -> {
                    val path = path.removePrefix("openapi/")
                    when {
      path.startsWith("application/AccessToken\$") -> false
      path.startsWith("application/As") -> false
      path.startsWith("application/BaseA") -> false
      path.startsWith("command/u") -> false
      path.startsWith("components/ComponentC") -> false
      path.startsWith("components/ServiceD") -> false
      path.startsWith("diagnostic/LoggerRt\$") -> false
      path.startsWith("editor/c") -> false
      path.startsWith("editor/event/B") -> false
      path.startsWith("editor/ex/DocumentEx\$") -> false
      path.startsWith("editor/ex/PrioritizedDocumentListener\$") -> false
      path.startsWith("editor/m") -> false
      path.startsWith("extensions/B") -> false
      path.startsWith("extensions/ExtensionF") -> false
      path.startsWith("extensions/ExtensionPointUtil\$") -> false
      path.startsWith("extensions/LoadingOrder\$1") -> false
      path.startsWith("extensions/S") -> false
      path.startsWith("module/ModuleS") -> false
      path.startsWith("progress/Cancellation\$") -> false
      path.startsWith("progress/EmptyProgressIndicatorB") -> false
      path.startsWith("progress/JobC") -> false
      path.startsWith("roots/ContentIteratorE") -> false
      path.startsWith("roots/LanguageLevelProjectExtension\$") -> false
      path.startsWith("roots/ProjectRootModificationTracker\$") -> false
      
      else -> true
    }
                  }
                  path.startsWith("patterns/") -> {
                    val path = path.removePrefix("patterns/")
                    when {
      path.startsWith("C") -> true
      path.startsWith("ElementPattern.") -> true
      path.startsWith("ElementPatternC") -> true
      path.startsWith("InitialPatternCondition.") -> true
      path.startsWith("ObjectPattern.") -> true
      path.startsWith("PatternCondition.") -> true
      path.startsWith("PsiElementPattern.") -> true
      path.startsWith("PsiJavaElementPattern\$") -> true
      path.startsWith("PsiJavaP") -> true
      path.startsWith("PsiN") -> true
      path.startsWith("TreeElementPattern.") -> true
      path.startsWith("V") -> true
      
      else -> false
    }
                  }
                  path.startsWith("core/Core") -> {
                    val path = path.removePrefix("core/Core")
                    when {
      path.startsWith("A") -> true
      path.startsWith("B") -> true
      path.startsWith("EncodingR") -> true
      path.startsWith("F") -> true
      path.startsWith("JavaD") -> true
      path.startsWith("JavaF") -> true
      path.startsWith("PsiP") -> true
      
      else -> false
    }
                  }
    path.startsWith("AbstractBundle\$") -> false
    path.startsWith("B") -> false
    path.startsWith("DynamicBundle\$1") -> false
    path.startsWith("DynamicBundle\$D") -> false
    path.startsWith("DynamicBundle.") -> false
    path.startsWith("codeWithMe/ClientId\$") -> false
    path.startsWith("codeWithMe/ClientIdS") -> false
    path.startsWith("diagnostic/ActivityI") -> false
    path.startsWith("diagnostic/EventWatcher\$") -> false
    path.startsWith("diagnostic/LoadingState\$") -> false
    path.startsWith("diagnostic/PluginP") -> false
    path.startsWith("diagnostic/ThreadDumper\$") -> false
    path.startsWith("extapi/psi/P") -> false
    path.startsWith("extapi/psi/StubBasedPsiElementBase\$") -> false
    path.startsWith("f") -> false
    path.startsWith("icons/AllIcons.") -> false
    path.startsWith("model/Branche") -> false
    path.startsWith("model/psi/PsiE") -> false
    path.startsWith("model/psi/U") -> false
    path.startsWith("navigation/C") -> false
    path.startsWith("navigation/ItemPresentationProvider.") -> false
    path.startsWith("no") -> false
    path.startsWith("pom/Ps") -> false
    path.startsWith("serialization/F") -> false
    path.startsWith("serialization/PropertyA") -> false
    path.startsWith("serialization/PropertyCollector\$1") -> false
    path.startsWith("serialization/PropertyCollector\$N") -> false
    path.startsWith("serialization/PropertyCollector\$P") -> false
    path.startsWith("serviceContainer/L") -> false
    path.startsWith("testFramework/LightVirtualFile\$") -> false
    path.startsWith("testFramework/LightVirtualFileB") -> false
    path.startsWith("ui/D") -> false
    path.startsWith("ui/IconManagerH") -> false
    path.startsWith("ui/icons/C") -> false
    
    else -> true
  }
                }
                path.startsWith("kotlin/script/experimental/j") -> {
                  val path = path.removePrefix("kotlin/script/experimental/j")
                  when {
    path.startsWith("vm/BasicJvmScriptEvaluator\$") -> false
    path.startsWith("vm/CompiledJvmScriptsCache\$") -> false
    path.startsWith("vm/G") -> false
    path.startsWith("vm/Js") -> false
    path.startsWith("vm/JvmDependency\$") -> false
    path.startsWith("vm/JvmGetScriptingClass\$") -> false
    path.startsWith("vm/JvmScriptCompilationConfigurationBuilder\$") -> false
    path.startsWith("vm/JvmScriptEvaluationConfigurationBuilder\$") -> false
    path.startsWith("vm/JvmScriptingHostConfigurationBuilder\$") -> false
    path.startsWith("vm/R") -> false
    path.startsWith("vm/compat/DiagnosticsUtilKt\$") -> false
    path.startsWith("vm/compat/E") -> false
    path.startsWith("vm/util/A") -> false
    path.startsWith("vm/util/I") -> false
    path.startsWith("vm/util/JvmClassLoaderUtilKt\$") -> false
    path.startsWith("vm/util/RuntimeExceptionReportingKt\$") -> false
    path.startsWith("vm/util/SnippetsHistory\$") -> false
    path.startsWith("vm/util/SnippetsHistoryK") -> false
    path.startsWith("vmhost/BasicJvmScriptC") -> false
    path.startsWith("vmhost/BasicJvmScriptE") -> false
    path.startsWith("vmhost/BasicJvmScriptJ") -> false
    path.startsWith("vmhost/BasicJvmScriptingHost\$") -> false
    path.startsWith("vmhost/BasicJvmScriptingHost.") -> false
    path.startsWith("vmhost/C") -> false
    path.startsWith("vmhost/JvmScriptC") -> false
    path.startsWith("vmhost/JvmScriptEvaluationConfigurationBuilder\$") -> false
    
    else -> true
  }
                }
                path.startsWith("it/unimi/dsi/fastutil/") -> {
                  val path = path.removePrefix("it/unimi/dsi/fastutil/")
                  when {
                  path.startsWith("objects/Object") -> {
                    val path = path.removePrefix("objects/Object")
                    when {
      path.startsWith("2IntMap\$E") -> true
      path.startsWith("2IntMap.") -> true
      path.startsWith("2IntOpenHashMap.") -> true
      path.startsWith("2ObjectOpenCustomHashMap.") -> true
      path.startsWith("2ObjectOpenHashMap.") -> true
      path.startsWith("2ShortMap\$E") -> true
      path.startsWith("2ShortMap.") -> true
      path.startsWith("2ShortMaps.") -> true
      path.startsWith("2ShortOpenHashMap.") -> true
      path.startsWith("ArrayList.") -> true
      path.startsWith("Collection.") -> true
      path.startsWith("Iterab") -> true
      path.startsWith("Iterator.") -> true
      path.startsWith("OpenCustomHashSet.") -> true
      path.startsWith("OpenHashSet.") -> true
      path.startsWith("Set.") -> true
      
      else -> false
    }
                  }
                  path.startsWith("ints/Int") -> {
                    val path = path.removePrefix("ints/Int")
                    when {
      path.startsWith("2IntMap.") -> true
      path.startsWith("2IntOpenHashMap.") -> true
      path.startsWith("2ObjectMap\$E") -> true
      path.startsWith("2ObjectMap.") -> true
      path.startsWith("2ObjectMaps.") -> true
      path.startsWith("2ObjectOpenHashMap.") -> true
      path.startsWith("ArrayList.") -> true
      path.startsWith("LinkedOpenHashSet.") -> true
      path.startsWith("List.") -> true
      path.startsWith("OpenHashSet.") -> true
      path.startsWith("Set.") -> true
      path.startsWith("St") -> true
      
      else -> false
    }
                  }
    path.startsWith("Hash\$") -> true
    path.startsWith("HashC") -> true
    path.startsWith("bytes/ByteArrayList.") -> true
    path.startsWith("bytes/ByteList.") -> true
    path.startsWith("doubles/Double2ObjectMap\$E") -> true
    path.startsWith("doubles/Double2ObjectMap.") -> true
    path.startsWith("doubles/Double2ObjectMaps.") -> true
    path.startsWith("doubles/Double2ObjectOpenHashMap.") -> true
    path.startsWith("doubles/DoubleSet.") -> true
    path.startsWith("longs/LongArrayList.") -> true
    path.startsWith("longs/LongList.") -> true
    path.startsWith("longs/LongOpenHashSet.") -> true
    path.startsWith("longs/LongSet.") -> true
    path.startsWith("objects/Reference2IntOpenHashMap.") -> true
    path.startsWith("objects/Reference2ObjectOpenHashMap.") -> true
    path.startsWith("objects/ReferenceOpenHashSet.") -> true
    
    else -> false
  }
                }
                path.startsWith("org/objectweb/asm/") -> {
                  val path = path.removePrefix("org/objectweb/asm/")
                  when {
    path.startsWith("AnnotationV") -> true
    path.startsWith("Attribute.") -> true
    path.startsWith("ClassR") -> true
    path.startsWith("ClassV") -> true
    path.startsWith("FieldV") -> true
    path.startsWith("Handle.") -> true
    path.startsWith("L") -> true
    path.startsWith("MethodV") -> true
    path.startsWith("ModuleV") -> true
    path.startsWith("RecordComponentV") -> true
    path.startsWith("T") -> true
    path.startsWith("util/TraceC") -> true
    
    else -> false
  }
                }
                path.startsWith("com/fasterxml/aalto/") -> {
                  val path = path.removePrefix("com/fasterxml/aalto/")
                  when {
    path.startsWith("W") -> true
    path.startsWith("impl/E") -> true
    path.startsWith("in/ByteS") -> true
    path.startsWith("in/InputB") -> true
    path.startsWith("in/ReaderConfig.") -> true
    path.startsWith("s") -> true
    
    else -> false
  }
                }
                path.startsWith("org/") -> {
                  val path = path.removePrefix("org/")
                  when {
                  path.startsWith("jline/reader/") -> {
                    val path = path.removePrefix("jline/reader/")
                    when {
      path.startsWith("En") -> true
      path.startsWith("History.") -> true
      path.startsWith("LineReader\$O") -> true
      path.startsWith("LineReader.") -> true
      path.startsWith("LineReaderB") -> true
      path.startsWith("U") -> true
      
      else -> false
    }
                  }
    path.startsWith("apache/log4j/L") -> true
    path.startsWith("codehaus/stax2/XMLStreamR") -> true
    path.startsWith("jdom/El") -> true
    path.startsWith("jetbrains/c") -> true
    path.startsWith("jline/terminal/Terminal.") -> true
    path.startsWith("jline/terminal/TerminalB") -> true
    path.startsWith("p") -> true
    
    else -> false
  }
                }
  path.startsWith("com/google/common/base/MoreObjects\$ToStringHelper.") -> true
  path.startsWith("com/google/common/base/MoreObjects.") -> true
  path.startsWith("com/google/common/base/Throwables.") -> true
  path.startsWith("com/google/common/collect/Maps.") -> true
  path.startsWith("javaslang/collection/HashM") -> true
  path.startsWith("javaslang/collection/Map.") -> true
  
  else -> false
}