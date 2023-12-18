
import com.simiacryptus.skyenet.kotlin.JarTool

val upstream = JarTool.pluginClasspath.values.flatten().groupBy { it.to }
  .mapValues { it.value.groupBy { it.from }.mapValues { it.value.map { it.relation }.toSet() } }

fun upstream(to: String, depth: Int): String = (upstream[to]?.flatMap { (from, references) ->
  listOf("${from} : ${references.joinToString(", ") { it.toString() }} ${classSets(from)}") + listOf(
    if (depth > 0) {
      upstream(from, depth - 1).trim().replace("\n", "\n\t")
    } else {
      ""
    }
  )
}?.filter { it.isNotBlank() }?.joinToString("\n") { it.trim() } ?: "")

val downstream = JarTool.pluginClasspath.values.flatten().groupBy { it.from }
  .mapValues { it.value.groupBy { it.to }.mapValues { it.value.map { it.relation }.toSet() } }

fun downstream(from: String, depth: Int): String = downstream[from]?.flatMap { (to, references) ->
  listOf("${to} : ${references.joinToString(", ") { it.toString() }} ${classSets(to)}") + listOf(
    if (depth > 0) {
      downstream(to, depth - 1).trim().replace("\n", "\n\t")
    } else {
      ""
    }
  )
}?.filter { it.isNotBlank() }?.joinToString("\n") { it.trim() } ?: ""

fun classSets(name: String) = setOf(
  if (JarTool.requiredClasses.contains(name)) "required" else "",
  if (JarTool.deadWeight.contains(name)) "deadWeight" else "",
  if (JarTool.conflicting.contains(name)) "conflicting" else "",
  if (JarTool.classloadLogClasses.contains(name)) "classloadLogClasses" else "",
  if (JarTool.pluginClasspath.keys.contains(name)) "pluginClasspath" else "",
  if (JarTool.platformClasspath.keys.contains(name)) "platformClasspath" else "",
  if (JarTool.kotlinClasspath.keys.contains(name)) "kotlinDependencyMap" else "",
  if (JarTool.requiredRoots.contains(name)) "requiredRoots" else "",
  if (JarTool.overrideRoots.contains(name)) "overrideRoots" else "",
  if (JarTool.overrideClasses.contains(name)) "overrideClasses" else "",

  ).filter { it.isNotBlank() }.joinToString(", ")

fun classInfo(className: String, depth: Int) = """
    |$className ${classSets(className)}
    |  upstream:
    |    ${upstream(className, depth).replace("\n", "\n    ")}
    |  downstream:
    |    ${downstream(className, depth).replace("\n", "\n    ")}
    """.trimMargin()

val interestingClasses = listOf(
  "org.jetbrains.kotlin.scripting.definitions.LazyClasspathWithClassLoader",
  "org.jetbrains.kotlin.scripting.definitions.ScriptiDefinitionsFromClasspathDiscoverySourceKt"
)
interestingClasses.flatMap { classInfo(it, 0).split("\n") }.filter { line ->
  line.isNotBlank() && (interestingClasses.any { line.contains(it) } || line.contains("stream:"))
}.forEach { println(it) }