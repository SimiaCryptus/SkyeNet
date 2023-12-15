@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet.kotlin

import com.simiacryptus.skyenet.core.util.ClasspathDependency.Companion.addJar
import com.simiacryptus.skyenet.core.util.ClasspathDependency.Companion.allRequirementsOf
import com.simiacryptus.skyenet.core.util.ClasspathDependency.Companion.allUsersOf
import com.simiacryptus.skyenet.core.util.ClasspathDependency.Companion.classToPath
import com.simiacryptus.skyenet.core.util.ClasspathDependency.Companion.jarFiles
import com.simiacryptus.skyenet.core.util.RuleTreeBuilder.getRuleExpression
import java.io.File

object JarTool {
  @JvmStatic
  fun main(args: Array<String>) {
    val platformDependencyMap = mutableMapOf<String, Set<String>>().apply {
      listOf(
        """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\plugins\Kotlin\lib\""",
        """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\lib\"""
      ).forEach { it.jarFiles?.forEach { addJar(it.absolutePath, this) } }
    }
    require(platformDependencyMap.isNotEmpty())

    val ourDependencyMap = addJar("""C:\Users\andre\code\SkyeNet\kotlin-hack\kotlin-hack-1.0.43-stage2.jar""")
    require(ourDependencyMap.isNotEmpty())

    val ourClasses = ourDependencyMap.keys
    val requiredRoots = listOf(
      "kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEngineFactoryKt",
      "kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl",
      "kotlin.script.experimental.jvmhost.BasicJvmScriptingHostKt",
    ) + ourClasses.filter { name ->
      when {
        name.contains("Jsr223") -> true
        name.contains("Repl") -> true
        name.contains("ArgumentsKt") -> true
        name.contains("JvmClasspathUtilKt") -> true
        name.contains("ErrorReportingKt") -> true
        name.contains("JvmScript.*Kt".toRegex()) -> true
        name.contains("AnalysisFlag") -> true
        name.contains("LanguageVersionSettings") -> true
        name.contains("LanguageParserDefinitions") -> true
        name.contains("KotlinJar") -> true
        name.contains("CLICompiler.*Kt".toRegex()) -> true
        name.contains("""fileTypes\.""".toRegex()) -> true
        name.contains("CoreEnvironment") -> true
        name.contains("JvmReplCompilerState") -> true
        name.contains("UpdateCheckerHelper") -> true
        name.contains("jvm.compiler") -> true
        name.contains("ApplicationEnvironment") -> true
        name.contains("ApplicationManager") -> true
        // com/intellij/core/CoreFileTypeRegistry
        name.contains("FileTypeRegistry") -> true
        name.contains("""psi\..*Registry""".toRegex()) -> true
        name.contains("""openapi\..*Registry""".toRegex()) -> true
        name.contains("util.Disposer") -> true
        name.contains("""util.ObjectTree""".toRegex()) -> true
        name.contains("com.intellij.openapi.diagnostic.Logger\$") -> true
        name.contains("com.intellij.openapi.diagnostic.DefaultLogger") -> true
        name.contains("openapi.vfs") -> true
        name.contains("""openapi\..*\.impl""".toRegex()) -> true
        name.contains(".Mock") -> true
        name.contains(".*Registrar".toRegex()) -> true
        name.contains("""openapi\..*\.Application""".toRegex()) -> true
        name.contains("""intellij.*\.impl""".toRegex()) -> true
        name.contains("""intellij.*\.util""".toRegex()) -> true
        name.contains(""".*ConfigurationKeys""".toRegex()) -> true
        name.contains("CoreJrtFileSystem") -> true
        name.contains("""kotlin.*\.impl""".toRegex()) -> true
        name.contains("ExtensionPointListener") -> true
        name.contains("ExtensionsArea") -> true
        name.contains("progress.Task") -> true
        name.contains("CommandProcessor") -> true
        name.contains("intellij.lang") -> true
        name.contains("intellij.lexer") -> true
        name.contains("org.picocontainer") -> true
        name.contains("psi.tree") -> true
        name.contains("ContainerProvider") -> true
        name.contains("intellij.codeInsight") -> true
        name.contains("com.intellij.psi.search.searches.SuperMethodsSearch") -> true
        name.contains("com.intellij.ide.ui.laf.MnemonicListenerService") -> true

        else -> false
      }
    }
    val requiredClasses = requiredRoots.flatMap { allRequirementsOf(ourDependencyMap, it) }.toSortedSet()

    val overrideRoots = listOf(
      "org.jetbrains.kotlin.cli.common.ArgumentsKt"
    ) + requiredClasses.filter { name ->
      when {
        name.contains("JvmScript.*Kt".toRegex()) -> true
        name.contains("JvmScriptCompilationKt") -> true
        name.contains("JvmClasspathUtilKt") -> true
        name.contains("JvmReplCompilerBase") -> true
        name.contains("CommonConfigurationKeys") -> true
        name.contains("CommonCompilerArguments") -> true
        name.contains("org.jetbrains.kotlin.scripting.compiler.") && name.contains("Kt(?![^$.])".toRegex()) -> true
        name.contains("IgnoredOptionsReportingState") -> true
        name.contains("AnalysisFlag") -> true
        name.contains("KotlinJar") -> true
        name.contains("LanguageVersionSettings") -> true
        name.contains("LanguageParserDefinitions") -> true
        name.contains("PsiManager") -> true
        name.contains("KtFile") -> true
        name.contains("CLICompiler.*Kt".toRegex()) -> true
        name.contains("""fileTypes\.""".toRegex()) -> true
        name.contains("""psi\..*ViewProviders""".toRegex()) -> true
        name.contains("""psi\..*Builders""".toRegex()) -> true
        name.contains("CoreEnvironment") -> true
        name.contains("JvmReplCompilerState") -> true
        name.contains("UpdateCheckerHelper") -> true
        name.contains("jvm.compiler") -> true
        name.contains("ApplicationEnvironment") -> true
        name.contains("ApplicationManager") -> true
        // com/intellij/core/CoreFileTypeRegistry
        name.contains("FileTypeRegistry") -> true
        name.contains("DisabledPluginsState") -> true
        name.contains("ContainerUtil") -> true
        name.contains("util.Disposer") -> true
        name.contains("""psi\..*Registry""".toRegex()) -> true
        name.contains("""openapi\..*Registry""".toRegex()) -> true
        name.contains("""util.ObjectTree""".toRegex()) -> true
        name.contains("com.intellij.openapi.diagnostic.Logger\$") -> true
        name.contains("com.intellij.openapi.diagnostic.DefaultLogger") -> true
        name.contains("CoreJarFileSystem") -> true
        name.contains("LanguageASTFactory") -> true
        name.contains("ItemPresentationProviders") -> true
        name.contains("intellij.*Language[A-Z]".toRegex()) -> true
        name.contains("openapi.vfs") -> true
        name.contains("""openapi\..*\.impl""".toRegex()) -> true
        name.contains(".Mock") -> true
        name.contains(".*Registrar".toRegex()) -> true
        name.contains("""openapi\..*\.Application""".toRegex()) -> true
        name.contains("""intellij.*\.impl""".toRegex()) -> true
        name.contains("""intellij.*\.util""".toRegex()) -> true
        name.contains(""".*ConfigurationKeys""".toRegex()) -> true
        name.contains("CoreJrtFileSystem") -> true
        name.contains("""kotlin.*\.impl""".toRegex()) -> true
        name.contains("ExtensionPointListener") -> true
        name.contains("ExtensionsArea") -> true
        name.contains("progress.Task") -> true
        name.contains("CommandProcessor") -> true
        name.contains("intellij.lang") -> true
        name.contains("intellij.lexer") -> true
        name.contains("org.picocontainer") -> true
        name.contains("psi.tree") -> true
        name.contains("BaseExtensionPointName") -> true
        name.contains("intellij.codeInsight") -> true
        name.contains("com.intellij.psi.search") -> true
        name.contains("com.intellij.ide.ui.laf.MnemonicListenerService") -> true

        else -> false
      }
    }

    val overrides = overrideRoots.flatMap { allUsersOf(ourDependencyMap, it) }
      .filter { requiredClasses.contains(it) }.toSortedSet()
    val deadWeight = ourClasses.filter { !requiredClasses.contains(it) }.toSortedSet()
    val conflicting = requiredClasses.filter {
      platformDependencyMap.containsKey(it)
    }.toSortedSet()

    val code = """      
      // GENERATED CODE

      // Required Classes: ${requiredClasses.size}
      // Overrides: ${overrides.size}
      // Pruned: ${deadWeight.size}
      // Conflicts: ${conflicting.size}
      // Dead Weight: ${deadWeight.size}

      // Overrides:
      fun shouldOverride(path: String) = ${
      getRuleExpression(
        overrides.map { it.classToPath + ".class" }.toSet(),
        ourClasses.filter { !overrides.contains(it) }.map { it.classToPath + ".class" }.toSortedSet(),
        true
      )
    } 
      
      // Conflicts:
      fun isConflicting(path: String) = ${
      getRuleExpression(
        conflicting.map { it.classToPath + ".class" }.toSet(),
        ourClasses.filter { !conflicting.contains(it) }.map { it.classToPath + ".class" }.toSortedSet(),
        true
      )
    }
      
      // Pruned:
      fun isPruned(path: String) = ${
      getRuleExpression(
        deadWeight.map { it.classToPath + ".class" }.toSet(),
        ourClasses.filter { !deadWeight.contains(it) }.map { it.classToPath + ".class" }.toSortedSet(),
        false
      )
    }
      """.trimIndent()

    val buildFile = File("""C:\Users\andre\code\SkyeNet\kotlin-hack\build.gradle.kts""")
    val text = buildFile.readText()
    val start = text.indexOf("// GENERATED CODE")
    buildFile.writeText(text.substring(0, start) + code)

    println(code)

  }

}
