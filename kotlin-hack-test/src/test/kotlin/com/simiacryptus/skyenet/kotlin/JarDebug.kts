
import com.simiacryptus.skyenet.kotlin.JarTool

//val requirementMap = ClasspathRelationships.requirementMap(JarTool.pluginClasspath.values.flatten())
//JarTool.classloadLogClasses.parallelStream()
//  .map { it to ClasspathRelationships.allRequirementsOf(requirementMap, it, mutableSetOf(it)) }
//  .collect(Collectors.toList())
//  .sortedBy { it.second.size }
//  .forEach { (k, v) -> println("$k -> ${v.size}") }

val refs = JarTool.pluginClasspath.values.flatten().groupBy { it.to }
  .mapValues { it.value.groupBy { it.from }.mapValues { it.value.map { it.relation }.toSet() } }

fun upstream(to: String, depth: Int): String {
  return refs[to]?.flatMap { (from, references) ->
    listOf("${from} : ${references.joinToString(", ") { it.toString() }} ${classSets(from)}") + listOf(
      if (depth > 0) {
        upstream(from, depth - 1).trim().replace("\n", "\n\t")
      } else {
        ""
      }
    )
  }?.filter { it.isNotBlank() }?.joinToString("\n") { it.trim() } ?: ""
}

fun classSets(name: String) = setOf(
  if (JarTool.requiredClasses.contains(name)) "required" else "",
  if (JarTool.deadWeight.contains(name)) "deadWeight" else "",
  if (JarTool.conflicting.contains(name)) "conflicting" else "",
  if (JarTool.classloadLogClasses.contains(name)) "classloadLogClasses" else "",
  if (JarTool.pluginClasspath.keys.contains(name)) "pluginClasspath" else "",
  if (JarTool.platformClasspath.keys.contains(name)) "platformClasspath" else "",
  if (JarTool.kotlinClasspath.keys.contains(name)) "kotlinDependencyMap" else "",
  if (JarTool.requiredRoots.contains(name)) "requiredRoots" else "",
).filter { it.isNotBlank() }.joinToString(", ")

val missingClass = "kotlin.script.experimental.jvm.util.JvmClasspathUtilKt\$classPathFromGetUrlsMethodOrNull\$\$inlined\$filterIsInstance\$1".replace("/", ".")
upstream(missingClass, 1)
println(
  """
  missingClass = $missingClass ${classSets(missingClass)}
""".trimIndent()
)
