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
  implementation(kGroup, name = "kotlin-scripting-jsr223", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm-host", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-compiler-embeddable", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-scripting-jvm", version = kVersion) { isTransitive = false }
  implementation(kGroup, name = "kotlin-compiler-embeddable", version = kVersion) { isTransitive = false }
}

val verbose = true

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

            path.startsWith("com/intellij/") -> when {
              path.contains("Provider[.$]".toRegex()) -> true

              // Type 'com/intellij/core/CoreFileTypeRegistry' (current frame, stack[0]) is not assignable to 'aicoder/com/intellij/openapi/fileTypes/FileTypeRegistry'
              path.startsWith("com/intellij/core/CoreFileTypeRegistry") -> true
              // com.intellij.psi.search.searches.SuperMethodsSearch
              path.startsWith("com/intellij/psi/search/searches/SuperMethodsSearch") -> true
              // com.intellij.psi.compiled.ClassFileDecompilers
              path.startsWith("com/intellij/psi/compiled/ClassFileDecompilers") -> true
              // com.intellij.psi.compiled.ClassFileDecompilers
              path.startsWith("com/intellij/psi/compiled/ClassFileDecompilers") -> true
              // com.intellij.psi.meta.MetaDataContributor
              path.startsWith("com/intellij/psi/meta/MetaDataContributor") -> true
              // org.jetbrains.kotlin.cli.jvm.compiler.IdeaExtensionPoints
              path.startsWith("org/jetbrains/kotlin/cli/jvm/compiler/IdeaExtensionPoints") -> true
              // com.intellij.psi.JavaModuleSystem
              path.startsWith("com/intellij/psi/JavaModuleSystem") -> true





              // 'void com.intellij.core.CoreProjectScopeBuilder.<init>(aicoder.com.intellij.openapi.project.Project, aicoder.com.intellij.openapi.roots.FileIndexFacade)'
              path.startsWith("com/intellij/core/CoreProjectScopeBuilder") -> true
              // Type 'com/intellij/lang/LanguageASTFactory' (current frame, stack[1]) is not assignable to 'aicoder/com/intellij/lang/LanguageExtension'
              path.startsWith("com/intellij/lang/LanguageASTFactory") -> true
              // com.intellij.lang.MetaLanguage
              path.startsWith("com/intellij/lang/MetaLanguage") -> true
              // Type 'com/intellij/lang/folding/LanguageFolding' (current frame, stack[1]) is not assignable to 'aicoder/com/intellij/lang/LanguageExtension'
              path.startsWith("com/intellij/lang/folding/LanguageFolding") -> true
              // java.lang.IllegalAccessError: class aicoder.com.intellij.lang.LanguageExtension tried to access method 'java.util.Collection com.intellij.lang.LanguageUtil.matchingMetaLanguages(com.intellij.lang.Language)' (aicoder.com.intellij.lang.LanguageExtension is in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @2336f05f; com.intellij.lang.LanguageUtil is in unnamed module of loader com.intellij.util.lang.PathClassLoader @4563e9ab)
              path.startsWith("com/intellij/lang/LanguageUtil") -> true
              // java.lang.NoSuchMethodError: 'void com.intellij.core.CoreJavaPsiImplementationHelper.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/core/CoreJavaPsiImplementationHelper") -> true
              // java.lang.NoSuchMethodError: 'void com.intellij.core.CoreJavaFileManager.<init>(aicoder.com.intellij.psi.PsiManager)'
              path.startsWith("com/intellij/core/CoreJavaFileManager") -> true

              path.startsWith("com/intellij/openapi/fileTypes/") -> when {
                // java.lang.NoSuchFieldError: ourInstanceGetter (defined by com.intellij.openapi.fileTypes.FileTypeRegistry refed from aicoder.com.intellij.openapi.application.ApplicationManager)
                path.endsWith("/FileTypeRegistry.class") -> true
                else -> false
              }

              // (interface com.intellij.openapi.application.ex.ApplicationEx in chain) com.intellij.openapi.components.ComponentManager is interface of com/intellij/openapi/application/Application
              path.startsWith("com/intellij/openapi/application/ex/ApplicationEx") -> true

              // Class aicoder.com.intellij.core.CoreApplicationEnvironment$1 does not implement the requested interface aicoder.com.intellij.openapi.application.Application
              // java.lang.NoSuchMethodError: 'aicoder.com.intellij.openapi.extensions.ExtensionsArea com.intellij.openapi.application.Application.getExtensionArea()'
              path.startsWith("com/intellij/openapi/application/Application") -> true
              path.startsWith("com/intellij/openapi/application/") -> when {
                //path.endsWith("/ApplicationManager.class") -> true
                path.endsWith("/CachedSingletonsRegistry.class") -> true
                else -> false
              }
              // com.intellij.openapi.components.ComponentManager in chain for java.lang.NoSuchMethodError: 'aicoder.com.intellij.openapi.extensions.ExtensionsArea aicoder.com.intellij.openapi.application.Application.getExtensionArea()'
              path.startsWith("com/intellij/openapi/components/ComponentManager") -> true

              // Derive from GlobalSearchScope
              path.contains("Scope") -> when {
                path.startsWith("com/intellij/") -> true
                else -> false
              }

              // com.intellij.DynamicBundle
              path.startsWith("com/intellij/DynamicBundle") -> true

              path.startsWith("com/intellij/openapi/extensions/") -> true // be aggressive
              // class com.intellij.openapi.extensions.ExtensionPointDescriptor cannot be cast to class aicoder.com.intellij.openapi.extensions.ExtensionPointDescriptor
              //path.endsWith("/ExtensionPointDescriptor.class") -> true
              // ERROR loader constraint violation: when resolving field "EP_NAME" of type com.intellij.openapi.extensions.ExtensionPointName, the class loader com.intellij.ide.plugins.cl.PluginClassLoader @68f40e0f of the current class, aicoder.com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl, and the class loader com.intellij.util.lang.PathClassLoader @4563e9ab for the field's defining abstract class, com.intellij.psi.PsiReferenceContributor, have different Class objects for type com.intellij.openapi.extensions.ExtensionPointName
              //path.endsWith("/extensions/ExtensionPointName.class") -> false
              // com.intellij.openapi.extensions.AreaInstance in inheritance chain
              path.endsWith("/AreaInstance.class") -> true
              // internal: java.lang.NoSuchMethodError: 'com.intellij.openapi.extensions.ExtensionPointListener[] com.intellij.openapi.extensions.ExtensionPointListener.emptyArray()'
              path.startsWith("com/intellij/openapi/extensions/ExtensionPointListener") -> true
              // java.lang.NoSuchMethodError: 'com.intellij.openapi.extensions.ExtensionPoint com.intellij.openapi.extensions.ProjectExtensionPointName.getPoint(aicoder.com.intellij.openapi.extensions.AreaInstance)'
              path.startsWith("com/intellij/openapi/extensions/ProjectExtensionPointName") -> true
              path.startsWith("com/intellij/openapi/extensions/Extensions") -> true // needs to be compatible with ExtensionsAreaImpl
              // ERROR class com.intellij.openapi.extensions.ExtensionPointDescriptor cannot be cast to class aicoder.com.intellij.openapi.extensions.ExtensionPointDescriptor
              path.startsWith("com/intellij/openapi/extensions/impl/") -> true // Be aggressive...
              //java.lang.NoSuchMethodError: 'void com.intellij.openapi.extensions.impl.InterfaceExtensionPoint.<init>(java.lang.String, java.lang.String, com.intellij.openapi.extensions.PluginDescriptor, aicoder.com.intellij.openapi.components.ComponentManager, java.lang.Class, boolean)'
              path.startsWith("com/intellij/openapi/extensions/impl/InterfaceExtensionPoint") -> true
              //Type 'com/intellij/openapi/extensions/impl/BeanExtensionPoint' (current frame, locals[7]) is not assignable to 'aicoder/com/intellij/openapi/extensions/impl/ExtensionPointImpl' (stack map, locals[7])
              path.startsWith("com/intellij/openapi/extensions/impl/BeanExtensionPoint") -> true
              //'void com.intellij.openapi.extensions.impl.ExtensionPointImpl.<init>(java.lang.String, java.lang.String, com.intellij.openapi.extensions.PluginDescriptor, aicoder.com.intellij.openapi.components.ComponentManager, java.lang.Class, boolean)'
              path.startsWith("com/intellij/openapi/extensions/impl/ExtensionPointImpl") -> true
              path.startsWith("com/intellij/openapi/extensions/impl/ExtensionsAreaImpl") -> true // in callstack
              //com.intellij.openapi.extensions.BaseExtensionPointName
              path.startsWith("com/intellij/openapi/extensions/BaseExtensionPointName") -> true

              // java.lang.AbstractMethodError: Receiver class aicoder.com.intellij.openapi.extensions.impl.InterfaceExtensionPoint does not define or inherit an implementation of the resolved method 'abstract void registerExtension(java.lang.Object)' of interface com.intellij.openapi.extensions.ExtensionPoint.
              path.startsWith("com/intellij/openapi/extensions/ExtensionPoint") -> true

              // com.intellij.openapi.project.Project in inheritance chain
              path.startsWith("com/intellij/openapi/project/Project") -> true
              // com.intellij.openapi.roots.FileIndexFacade in inheritance chain
              path.startsWith("com/intellij/openapi/roots/FileIndexFacade") -> true

              // com.intellij.psi.PsiReferenceProviderBean
              path.startsWith("com/intellij/psi/PsiReferenceProviderBean") -> true
              // com.intellij.psi.PsiElementFinder
              path.startsWith("com/intellij/psi/PsiElementFinder") -> true
              // java.lang.NoSuchMethodError: 'com.intellij.psi.PsiManager com.intellij.psi.PsiManager.getInstance(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/PsiManager") -> true
              // com.intellij.psi.PsiReferenceContributor
              path.startsWith("com/intellij/psi/PsiReferenceContributor") -> true
              // java.lang.NoSuchMethodError: 'void com.intellij.psi.controlFlow.ControlFlowFactory.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/controlFlow/ControlFlowFactory") -> true
              // java.lang.NoSuchMethodError: 'com.intellij.psi.util.PsiModificationTracker com.intellij.psi.util.PsiModificationTracker$SERVICE.getInstance(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/util/PsiModificationTracker") -> true

              path.startsWith("com/intellij/psi/impl/") -> true // be aggressive
              // java.lang.NoSuchMethodError: 'void com.intellij.psi.impl.PsiManagerImpl.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/impl/PsiManagerImpl") -> true
              // java.lang.NoSuchMethodError: 'void com.intellij.psi.impl.PsiModificationTrackerImpl.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/impl/PsiModificationTrackerImpl") -> true

              //            path.endsWith("components/ComponentManager.class") -> false
              //            path.startsWith("com/intellij/openapi/components/") -> true
              //            path.endsWith("extensions/ExtensionPointName.class") -> false
              //            path.startsWith("com/intellij/openapi/extensions/") -> true
              path.startsWith("com/intellij/psi/impl/source/resolve/") -> true

              // java.lang.NoSuchMethodError: 'com.intellij.psi.search.GlobalSearchScope com.intellij.psi.search.GlobalSearchScope.allScope(aicoder.com.intellij.openapi.project.Project)'
              path.startsWith("com/intellij/psi/search/GlobalSearchScope") -> true
              // Type 'com/intellij/psi/search/EverythingGlobalScope' (current frame, stack[0]) is not assignable to 'aicoder/com/intellij/psi/search/GlobalSearchScope'
              path.startsWith("com/intellij/psi/search/EverythingGlobalScope") -> true
              // Type 'com/intellij/psi/search/ProjectScopeImpl' (current frame, stack[0]) is not assignable to 'aicoder/com/intellij/psi/search/GlobalSearchScope
              path.startsWith("com/intellij/psi/search/ProjectScopeImpl") -> true

              // java.lang.NoSuchMethodError: 'void com.intellij.util.CachedValuesManagerImpl.<init>(aicoder.com.intellij.openapi.project.Project, com.intellij.util.CachedValuesFactory)'
              path.startsWith("com/intellij/util/CachedValuesManagerImpl") -> true

              path.startsWith("com/intellij/util/messages/impl/") -> true

              // com.intellij.psi.impl.compiled.ClsDecompilerImpl does not implement interface aicoder.com.intellij.psi.compiled.ClassFileDecompilers$Decompiler
              //path.startsWith("com/intellij/psi/compiled/") -> true

              // java.lang.NoSuchMethodError: 'void com.intellij.lang.jvm.facade.JvmFacadeImpl.<init>(com.intellij.openapi.project.Project, com.intellij.util.messages.MessageBus)'
              path.endsWith("/JvmFacadeImpl.class") -> true

              path.startsWith("com/intellij/mock/") -> true

              // java.lang.NoSuchMethodError: 'void com.intellij.lang.LanguageExtension.<init>(aicoder.com.intellij.openapi.extensions.ExtensionPointName)'
              path.startsWith("com/intellij/lang/LanguageExtension") -> true
              // Type 'com/intellij/lang/LanguageParserDefinitions' (current frame, stack[1]) is not assignable to 'aicoder/com/intellij/lang/LanguageExtension'
              path.startsWith("com/intellij/lang/LanguageParserDefinitions") -> true

              path.endsWith("/DisabledPluginsState.class") -> true

              path.contains("/MockDocumentCommitProcessor") -> true // MockDocumentCommitProcessor is internal
              path.contains("/CorePsiDocumentManager") -> true // CorePsiDocumentManager is internal
              path.contains("/CoreInjectedLanguageManager") -> true // CoreInjectedLanguageManager is internal
              path.contains("/CoreProjectEnvironment") -> true
              path.contains("/CoreApplicationEnvironment") -> true

              path.contains("/JavaCoreProjectEnvironment") -> true
              path.contains("/JavaCoreApplicationEnvironment") -> true
              else -> false
            }

            path.startsWith("org/jetbrains/kotlin/") -> when {

              path.startsWith("org/jetbrains/kotlin/cli/jvm/") -> true
              // java.lang.ClassNotFoundException: org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter PluginClassLoader(plugin=PluginDescriptor(name=AI Coding Assistant, id=com.github.simiacryptus.intellijopenaicodeassist, descriptorPath=plugin.xml, path=~\code\intellij-aicoder\build\idea-sandbox\plugins-uiTest\intellij-aicoder, version=1.2.24, package=null, isBundled=false), packagePrefix=null, state=active)
              path.startsWith("org/jetbrains/kotlin/cli/") -> true

              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar.registerProjectComponents(aicoder.com.intellij.mock.MockProject, org.jetbrains.kotlin.config.CompilerConfiguration)'
              path.contains("/ComponentRegistrar") -> true

              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.contains("/KotlinAsJavaSupportBase") -> true

              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.asJava.finder.JavaElementFinder.<init>(aicoder.com.intellij.openapi.project.Project)'
              path.contains("/JavaElementFinder") -> true

              // java.lang.NoSuchMethodError: 'org.jetbrains.kotlin.asJava.KotlinAsJavaSupport org.jetbrains.kotlin.asJava.KotlinAsJavaSupport$Companion.getInstance(aicoder.com.intellij.openapi.project.Project)'
              path.contains("/KotlinAsJavaSupport") -> true

              // org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar cannot be cast to class aicoder.org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationExtension.<init>(aicoder.com.intellij.mock.MockProject, kotlin.script.experimental.host.ScriptingHostConfiguration)'
              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.scripting.compiler.plugin.extensions.ScriptingCollectAdditionalSourcesExtension.<init>(aicoder.com.intellij.mock.MockProject)'
              // java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.scripting.compiler.plugin.PluginRegisrarKt.access$registerExtensionIfRequired(org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor, aicoder.com.intellij.mock.MockProject, java.lang.Object)'
              path.startsWith("org/jetbrains/kotlin/scripting/compiler/plugin/") -> true
              //org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor in call stack
              path.contains("/ProjectExtensionDescriptor") -> true
              path.contains("CLICompilerKt") -> true
              path.contains("UtilsKt") -> true
              path.contains("ArgumentsKt") -> true
              path.contains("CompilationContextKt") -> true
              path.contains("JvmReplCompiler") -> true
              path.contains("Jsr223") -> true
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
            path.startsWith("org/picocontainer/") -> false
            else -> true
          }
        ) {
          if (verbose) println("${this.path} excluded from ${file.name} as $path")
          exclude(this.path)
        } else {
          if (verbose) println("${this.path} included from ${file.name} as $path")
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
    val prefix = "aicoder"
    relocate(object : Relocator {

      override fun canRelocatePath(path: String?) = true
      fun shouldRelocatePath(path: String?) =
        path?.removeSuffix(".class")?.let {
          when {
            !inputFiles.contains(path) -> {
              if (verbose) println("""ignoring non-present "$path"""")
              false
            }

            //ERROR loader constraint violation: when resolving field "EP_NAME" of type com.intellij.openapi.extensions.ExtensionPointName, the class loader com.intellij.ide.plugins.cl.PluginClassLoader @59ee1f6f of the current class, aicoder.com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl, and the class loader com.intellij.util.lang.PathClassLoader @4563e9ab for the field's defining abstract class, com.intellij.psi.PsiReferenceContributor, have different Class objects for type com.intellij.openapi.extensions.ExtensionPointName
            it.contains("/ExtensionPointName") -> true //false
            it.contains("/KotlinJsr223") -> false
            it.startsWith("kotlin/script/experimental/jvm") -> false
            //it.startsWith("com/intellij/psi") -> true
            it.startsWith("com/intellij/") -> true
            //org/jetbrains/kotlin/cli/common/extensions/ReplFactoryExtension$Companion
            it.startsWith("org/jetbrains/kotlin/") && it.contains("Extension(?![^.$])".toRegex()) -> false
            it.startsWith("org/jetbrains/") -> true
            it.startsWith("kotlin/") -> true
            it.startsWith("cli/") -> true
            else -> false
          }
        } ?: false

      override fun relocatePath(context: RelocatePathContext?) = context?.path?.let { from ->
        if (shouldRelocatePath(from)) {
          val to = prefix + "/" + from
          if (verbose) println("""path relocate("$from", "$to")""")
          to
        } else {
          if (verbose) println("""leaving path "$from" as-is""")
          from
        }
      }

      override fun canRelocateClass(className: String?) = true
      fun shouldRelocateClass(className: String?) = shouldRelocatePath(className?.replace('.', '/'))

      override fun relocateClass(context: RelocateClassContext?) =
        context?.className?.let { from ->
          if (shouldRelocateClass(from)) {
            val to = prefix + "." + from
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
