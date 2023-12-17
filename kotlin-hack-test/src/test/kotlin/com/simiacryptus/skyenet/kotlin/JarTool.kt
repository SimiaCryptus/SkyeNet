@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.simiacryptus.skyenet.kotlin

import com.simiacryptus.skyenet.core.util.ClasspathRelationships.analyzeJar
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.classAccessMap
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.classToPath
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.downstream
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.downstreamMap
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.jarFiles
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.readJarClasses
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.readJarFiles
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.upstream
import com.simiacryptus.skyenet.core.util.ClasspathRelationships.upstreamMap
import com.simiacryptus.skyenet.core.util.RuleTreeBuilder.getRuleExpression
import org.objectweb.asm.Opcodes
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

//    "org.jetbrains.kotlin.scripting.compiler.plugin.impl.ErrorReportingKt",
//    "org.jetbrains.kotlin.daemon.KotlinCompileDaemon",
//    "kotlin.script.experimental.jvmhost.BasicJvmScriptingHost",
//    "org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation",
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

  val platformJar by lazy {
    platformLib.jarFiles?.map {
      readJarClasses(it.absolutePath)
    }
  }

  val platformClasspath by lazy {
    platformJar?.flatMap {
      analyzeJar(it)
    }?.apply {
      require(isNotEmpty())
    }?.groupBy { it.from } ?: mapOf()
  }

  private fun isPrivate(to: String) = (pluginAccessMap[to] ?: 0).and(Opcodes.ACC_PRIVATE) != 0
  private fun isPublic(to: String) = (pluginAccessMap[to] ?: 0).and(Opcodes.ACC_PUBLIC) != 0

  val kotlinClasspath by lazy {
    kotlinLib.jarFiles?.flatMap { analyzeJar(it.absolutePath) }?.apply {
      require(isNotEmpty())
    }?.groupBy { it.from } ?: mapOf()
  }

  val pluginJarClasses = readJarClasses(pluginJar)
  val pluginClasspath by lazy {
    analyzeJar(pluginJarClasses)
      .apply {
        require(isNotEmpty())
      }.flatMap {
        when {
          !isPublic(it.to) -> when(it.relation) {
            // Add back-references for non-public classes to ensure they are relocated if needed
            else -> listOf(it, it.copy(to = it.from, from = it.to))
          }
          else -> listOf(it)
        }
      }.groupBy { it.from }
  }

  val pluginAccessMap by lazy {
    classAccessMap(pluginJarClasses).entries.map {
      it.key to it.value
    }.toMap()
  }

  val classloadLogClasses by lazy {
    File(classloadLog).readLines().filter { it.contains("class,load") }.map {
      it.substringAfter("] ").substringBefore(" ").relocateClass()
    }.toSortedSet()
  }

  val requiredClasses: SortedSet<String> by lazy {
    val requirementMap = downstreamMap(pluginClasspath.values.flatten())
    (((requiredRoots).distinct().flatMap {
      downstream(requirementMap, it, mutableSetOf(it))
    } + classloadLogClasses).toSet()).toSortedSet()
  }

  val overrideRoots = platformClasspath.keys
    .filter {
      when {
        it.contains("ApplicationManager") -> true // <-- All this for that one fucking class
        //it.contains("JavaZipFileDataLoader") -> true
        else -> false
      }
    }

  val overrideClasses by lazy {
    val userMap = upstreamMap(pluginClasspath.values.flatten())
    var allUpstream = overrideRoots.flatMap { upstream(userMap, it) }.toSortedSet()
    allUpstream = pluginClasspath.keys.filter { classname -> allUpstream.contains(classname.split("$").first()) }.toSortedSet()
    allUpstream = allUpstream.flatMap { upstream(userMap, it) }.toSortedSet()
    allUpstream
  }

  val conflicting by lazy {
    pluginClasspath.keys.filter {
      when {
        overrideClasses.contains(it) -> false
        platformClasspath.containsKey(it) -> true
        else -> false
      }
    }.toSortedSet()
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
      // Override Classes: ${overrideClasses.size}
      
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
        (deadWeight + conflicting).map { it.classToPath + ".class" }.toSet(),
        (pluginClasspath.keys
          .filter { !deadWeight.contains(it) }
          .filter { !conflicting.contains(it) }
          .map { it.classToPath + ".class" } + requiredResources).toSortedSet(),
        true
      )
    }
    
      // Overrides:
      fun isOverride(path: String) = ${
      getRuleExpression(
        overrideClasses.map { it.classToPath + ".class" }.toSet(),
        pluginClasspath.keys
          .filter { !deadWeight.contains(it) }
          .filter { !conflicting.contains(it) }
          .filter { !overrideClasses.contains(it) }
          .map { it.classToPath + ".class" }.toSortedSet(),
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
