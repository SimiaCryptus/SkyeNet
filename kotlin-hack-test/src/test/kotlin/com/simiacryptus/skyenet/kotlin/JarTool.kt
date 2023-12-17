@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.simiacryptus.skyenet.kotlin

import com.simiacryptus.skyenet.core.util.ClasspathRelationships.allRequirementsOf
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.analyzeJar
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.classToPath
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.jarFiles
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.readJarFiles
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.requirementMap
import com.simiacryptus.skyenet.core.util.RuleTreeBuilder.getRuleExpression
import java.io.File
import java.util.*

object JarTool {
  val buildFile = File("""C:\Users\andre\code\SkyeNet\kotlin-hack\build.gradle.kts""")
  val classloadLog = "C:\\Users\\andre\\code\\SkyeNet\\kotlin\\classloader.log"
  val platformLib =
    """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\lib\"""
  val kotlinLib =
    """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\plugins\Kotlin\lib\"""
  val pluginJar = """C:\Users\andre\code\SkyeNet\kotlin-hack\kotlin-hack-1.0.43-full.jar"""

  val requiredRoots = listOf(
    "kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEngineFactoryKt",
    "kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl",
    "org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineBase",
    "kotlin.script.experimental.jvmhost.BasicJvmScriptingHostKt",
    "org.jetbrains.kotlin.jsr223.KotlinJsr223JvmScriptEngine4Idea",

    "org.jetbrains.kotlin.scripting.compiler.plugin.impl.ErrorReportingKt",
    "org.jetbrains.kotlin.daemon.KotlinCompileDaemon",
    "kotlin.script.experimental.jvmhost.BasicJvmScriptingHost",
    "org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation",
  )


  val requiredResources: List<String> by lazy {
    readJarFiles(pluginJar).filter {
      when {
        it.endsWith(".class") -> false
        it.startsWith("META-INF/services/") -> true
        it.startsWith("META-INF/extensions/compiler.xml") -> true
        else -> false
      }
    }
  }

  fun String.relocateClass() = when {
    startsWith("org.jetbrains.kotlin.com.") -> removePrefix("org.jetbrains.kotlin.")
    startsWith("org.jetbrains.kotlin.org.") -> removePrefix("org.jetbrains.kotlin.")
    startsWith("org.jetbrains.kotlin.it.") -> removePrefix("org.jetbrains.kotlin.")
    startsWith("org.jetbrains.org.") -> removePrefix("org.jetbrains.")
    startsWith("org.jetbrains.com.") -> removePrefix("org.jetbrains.")
    else -> this
  }

  val platformClasspath by lazy {
    platformLib.jarFiles?.flatMap { analyzeJar(it.absolutePath) }?.apply {
      require(isNotEmpty())
    }?.groupBy { it.from } ?: mapOf()
  }

  val kotlinClasspath by lazy {
    kotlinLib.jarFiles?.flatMap { analyzeJar(it.absolutePath) }?.apply {
      require(isNotEmpty())
    }?.groupBy { it.from } ?: mapOf()
  }

  val pluginClasspath by lazy {
    analyzeJar(pluginJar)
      .apply {
        require(isNotEmpty())
      }.groupBy { it.from }
  }

  val classloadLogClasses by lazy {
    File(classloadLog).readLines().filter { it.contains("class,load") }.map {
      it.substringAfter("] ").substringBefore(" ").relocateClass()
    }.toSortedSet()
  }

  val requiredClasses: SortedSet<String> by lazy {
    val requirementMap = requirementMap(pluginClasspath.values.flatten())
    (((requiredRoots).distinct().flatMap {
      allRequirementsOf(requirementMap, it, mutableSetOf(it))
    } + classloadLogClasses).toSet()).toSortedSet()
  }

  val conflicting by lazy {
    pluginClasspath.keys
      .filter {
        when {
          it.contains("ApplicationManager") -> false
          platformClasspath.containsKey(it) -> true
          else -> false
        }
      /*|| kotlinDependencyMap.containsKey(it)*/
      }
      .toSortedSet()
  }

  val deadWeight by lazy {
    pluginClasspath.keys
      .filter { !requiredClasses.contains(it) }
      .filter { !classloadLogClasses.contains(it) }
      .filter { !kotlinClasspath.contains(it) }
      .toSortedSet()
  }

  @JvmStatic
  fun main(args: Array<String>) {
    val code = """      
      // GENERATED CODE

      // Conflicts: ${conflicting.size}
      // Pruned: ${deadWeight.size}
      // Required Classes: ${requiredClasses.size}
      
      // Conflicts:
      fun isConflicting(path: String) = ${
      getRuleExpression(
        conflicting.map { it.classToPath + ".class" }.toSet(),
        pluginClasspath.keys
          .filter { !conflicting.contains(it) }
          .map { it.classToPath + ".class" }.toSortedSet(),
        true
      )
    }
      
      // Pruned:
      fun isPruned(path: String) = ${
      getRuleExpression(
        (deadWeight+conflicting).map { it.classToPath + ".class" }.toSet(),
        (pluginClasspath.keys
          .filter { !deadWeight.contains(it) }
          .filter { !conflicting.contains(it) }
          .map { it.classToPath + ".class" }
            + requiredResources
            ).toSortedSet(),
        true
      )
    }
      """.trimIndent()

    val text = buildFile.readText()
    val start = text.indexOf("// GENERATED CODE")
    buildFile.writeText(text.substring(0, start) + code)
    println(code)
  }

}
