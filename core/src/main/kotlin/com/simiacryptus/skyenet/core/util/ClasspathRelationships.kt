package com.simiacryptus.skyenet.core.util

import java.util.jar.JarFile

object ClasspathRelationships {
  sealed class Relation {
    open val from_method : String = ""
    open val to_method : String = ""
  }

  data class Reference(
    val from: String,
    val to: String,
    val relation: Relation
  )


  fun readJarClasses(jarPath: String) = JarFile(jarPath).use { jarFile ->
    jarFile.entries().asSequence().filter { it.name.endsWith(".class") }.map { entry ->
      val className = entry.name.replace('/', '.').removeSuffix(".class")
      className to jarFile.getInputStream(entry)?.readBytes()
    }.toMap()
  }

  fun readJarFiles(jarPath: String) = JarFile(jarPath).use { jarFile ->
    jarFile.entries().asSequence().map { it.name }.toList().toTypedArray()
  }

  fun downstreamMap(dependencies: List<Reference>) =
    dependencies.groupBy { it.from }

}