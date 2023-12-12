@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.util

import org.objectweb.asm.*
import java.io.File
import java.util.jar.JarFile


class JarTool {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      JarTool().main()
    }

    fun calculatePrefixes(
      positiveExamples: Set<String>, negativeExamples: Set<String>,
    ): Set<String> {
      require(negativeExamples.none { it.isEmpty() }) { "Negative examples set should not contain empty strings." }
      val negativePrefixes = computeNegativePrefixes(negativeExamples)
      return positiveExamples.map {
        val prefixExpand = it.prefixExpand()
        prefixExpand.first() { !negativePrefixes.contains(it) }
      }.distinct()
        .toSet()
        .sorted().toSet()
    }

    fun computeNegativePrefixes(negativeExamples: Set<String>) =
      negativeExamples.flatMap { it.prefixExpand() }.toHashSet()

    private fun String.prefixExpand() = (1..length).map { i -> substring(0, i) }
  }

  fun main() {
    val platformDependencyMap = mutableMapOf<String, Set<String>>().apply {
      """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\plugins\Kotlin\lib\""".jarFiles
        ?.forEach { addJar(it.absolutePath, this) }
      """C:\Users\andre\.gradle\caches\modules-2\files-2.1\com.jetbrains.intellij.idea\ideaIC\2023.3\6105b81c6142f62379ad6c5afb542c77350a71eb\ideaIC-2023.3\lib\""".jarFiles
        ?.forEach { addJar(it.absolutePath, this) }
    }
    require(platformDependencyMap.isNotEmpty())

    val ourDependencyMap: MutableMap<String, Set<String>> =
      addJar("""C:\Users\andre\code\kotlin-hack\kotlin-hack-1.0.43-stage2.jar""")
    require(ourDependencyMap.isNotEmpty())

    println("// Overrides:")
    val overrideRoots = listOf(
      "org.jetbrains.kotlin.cli.common.ArgumentsKt"
    ) + ourDependencyMap.keys.filter { name ->
      when {
        name.contains("JvmScriptingHostConfigurationKt") -> true
        name.contains("JvmScriptCompilationKt") -> true
        name.contains("JvmClasspathUtilKt") -> true
        else -> false
      }
    }

    val overrides = overrideRoots.flatMap { allUsersOf(ourDependencyMap, it) }
      .filter { ourDependencyMap.containsKey(it) }.distinct().sorted()

    val ourClasses = ourDependencyMap.keys
    println(getRuleExpression(
      overrides.map { it.classToPath + ".class" },
      ourClasses.filter { !overrides.contains(it) }.map { it.classToPath + ".class" },
      true
    )
    )
    println("\n\n")

    println("// Pruned:")
    val requiredClasses = (
        listOf(
          "kotlin.script.experimental.jsr223.KotlinJsr223DefaultScriptEngineFactoryKt",
          "kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl",
          "kotlin.script.experimental.jvmhost.BasicJvmScriptingHostKt",
        ) + ourClasses.filter { name ->
          when {
            name.contains("Jsr223") -> true
            name.contains("Repl") -> true
            name.contains("ArgumentsKt") -> true
            else -> false
          }
        }
        ).flatMap {
        allRequirementsOf(ourDependencyMap, it)
      }.distinct().sorted()
    val deadWeight = ourClasses.filter { !requiredClasses.contains(it) }.distinct().sorted()
    println(getRuleExpression(
      deadWeight.map { it.classToPath + ".class" },
      requiredClasses.map { it.classToPath + ".class" }
    ))
    println("\n\n")

    println("// Conflicts:")
    val conflicting = requiredClasses.filter {
      platformDependencyMap.containsKey(it)
    }.sorted()
    println(getRuleExpression(
      conflicting.map { it.classToPath + ".class" },
      requiredClasses.filter { !conflicting.contains(it) }.map { it.classToPath + ".class" }
    ))
    println("\n\n")

  }

  private fun getRuleExpression(
    toMatch: List<String>,
    doNotMatch: List<String>,
    result: Boolean = false
  ): String {
    val sb = StringBuilder()
    sb.append("when {\n")

    val exactMatches = toMatch.filter { a -> doNotMatch.any { b -> b.startsWith(a) } }
    exactMatches.forEach {
      sb.append("""  path == "${it.classToPath.escape}" -> $result""" + "\n")
    }

    val toMatchPrefix = toMatch.filter { !exactMatches.contains(it) }.toSet()
    val prefixes = calculatePrefixes(toMatchPrefix, doNotMatch.toSet())
    val dedupedPrefixes = prefixes.filter { a -> prefixes.none { b -> b != a && b.startsWith(a) } }.sorted()

    appendRules(sb, dedupedPrefixes, result)
    sb.append("  else -> ${!result}\n")
    sb.append("}")
    return sb.toString()
  }

  private fun appendRules(
    sb: StringBuilder,
    prefixes: List<String>,
    result: Boolean
  ) {
    val bestPrefixes = getSharedPrefixes(prefixes)
    prefixes.groupBy {
      bestPrefixes.firstOrNull { prefix -> it.startsWith(prefix) } ?: ""
    }.forEach { (commonPrefix, items) ->
      if (commonPrefix.isNotEmpty() && items.size > 3) {
        sb.append(
          """  path.startsWith("${
            commonPrefix.classToPath.escape
          }") -> {
          val path = path.removePrefix("${commonPrefix.classToPath.escape}")
          when {""" + "\n"
        )
        val subItems = items.map { it.removePrefix(commonPrefix) }
        appendRules(sb, subItems, result)
        sb.append("    else -> ${!result}\n")
        sb.append("  }\n}\n")
      } else {
        items.forEach {
          sb.append("""  path.startsWith("${it.classToPath.escape}") -> $result""" + "\n")
        }
      }
    }
  }

  private fun getSharedPrefixes(prefixes: List<String>): MutableSet<String> {
    val allPossiblePrefixes = prefixes.flatMap { it.prefixExpand() }.toHashSet()
    val bestPrefixes = mutableSetOf<String>()
    while (true) {
      val remaining = allPossiblePrefixes.filter { a -> bestPrefixes.none(a::startsWith) }
      if (remaining.isEmpty()) break
      val scores = remaining.associate { a -> a to remaining.filter { it.startsWith(a) }.sumOf { a.length } - a.length }
        .filter { it.value > 32 }
      bestPrefixes.add(scores.maxByOrNull { it.value }?.key ?: break)
    }
    return bestPrefixes
  }

  private fun allRequirementsOf(
    dependencies: MutableMap<String, Set<String>>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ): Set<String> {
    val filter = synchronized(buffer) { buffer.toMutableSet().apply { add(className) }.toHashSet() }
    val required = (dependencies[className] ?: emptySet())
      .filter { !filter.contains(it) }.toSet()
    buffer.addAll(required)
    required.parallelStream().flatMap { allRequirementsOf(dependencies, it, buffer).stream() }
    return buffer.filter { it.isNotBlank() }.toHashSet()
  }

  private fun allUsersOf(
    dependencies: MutableMap<String, Set<String>>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ): Set<String> {
    val filter = synchronized(buffer) { buffer.toMutableSet().apply { add(className) }.toHashSet() }
    val required = dependencies
      .filter { it.value.contains(className) }
      .keys
      .filter { !filter.contains(it) }.toSet()
    buffer.addAll(required)
    required.parallelStream().flatMap { allUsersOf(dependencies, it, buffer).stream() }
    return buffer.filter { it.isNotBlank() }.toHashSet()
  }

  private fun addJar(
    jarPath: String,
    pluginDependencies: MutableMap<String, Set<String>> = mutableMapOf()
  ): MutableMap<String, Set<String>> {
    readJar(jarPath).forEach { (className, classData) ->
      pluginDependencies[className] = run {
        val dependencyClassVisitor = DependencyClassVisitor()
        ClassReader(classData).accept(dependencyClassVisitor, 0)
        dependencyClassVisitor.dependencies.keys.map { it.symbolName }.toSet()
      }
    }
    return pluginDependencies
  }

  private fun readJar(jarPath: String) = JarFile(jarPath).use { jarFile ->
    jarFile.entries().asSequence().filter { it.name.endsWith(".class") }.map { entry ->
      val className = entry.name.replace('/', '.').removeSuffix(".class")
      className to jarFile.getInputStream(entry)?.readBytes()
    }.toMap()
  }
}

sealed class DependencyType {
  data object INHERITANCE : DependencyType() // When a class extends another class
  data object INTERFACE_IMPLEMENTATION : DependencyType() // When a class implements an interface
  data object FIELD_TYPE : DependencyType() // When a class has a field of another class type
  data object METHOD_PARAMETER : DependencyType() // When a class has a method that takes another class as a parameter
  data object METHOD_RETURN_TYPE : DependencyType() // When a class has a method that returns another class
  data object LOCAL_VARIABLE :
    DependencyType() // When a method within a class declares a local variable of another class

  data object EXCEPTION_TYPE : DependencyType() // When a method declares that it throws an exception of another class
  data object ANNOTATION :
    DependencyType() // When a class, method, or field is annotated with another class (annotation)

  data object INSTANCE_CREATION : DependencyType() // When a class creates an instance of another class
  data object METHOD_REFERENCE : DependencyType() // When a method references another class's method
  data object FIELD_REFERENCE : DependencyType() // When a method references another class's field
  data object DYNAMIC_BINDING :
    DependencyType() // When a class uses dynamic binding (e.g., invoke dynamic) related to another class

  data object UNKNOWN : DependencyType() // A fallback for unknown or unclassified dependencies

}


class DependencyClassVisitor(
  val dependencies: MutableMap<String, MutableSet<DependencyType>> = mutableMapOf()
) : ClassVisitor(Opcodes.ASM9) {

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    // Add superclass dependency
    superName?.let { addDep(it, DependencyType.INHERITANCE) }
    // Add interface dependencies
    interfaces?.forEach { addDep(it, DependencyType.INTERFACE_IMPLEMENTATION) }
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitField(
    access: Int,
    name: String?,
    desc: String?,
    signature: String?,
    value: Any?
  ): FieldVisitor? {
    // Add field type dependency
    addType(desc, DependencyType.FIELD_TYPE)
    return super.visitField(access, name, desc, signature, value)
  }

  override fun visitMethod(
    access: Int,
    name: String?,
    desc: String?,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    // Add method return type and parameter types dependencies
    addMethodDescriptor(desc, DependencyType.METHOD_PARAMETER, DependencyType.METHOD_RETURN_TYPE)
    // Add exception types dependencies
    exceptions?.forEach { addDep(it, DependencyType.EXCEPTION_TYPE) }
    return DependencyMethodVisitor(dependencies)
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    // Add annotation type dependency
    addType(descriptor, DependencyType.ANNOTATION)
    return super.visitAnnotation(descriptor, visible)
  }

  private fun addDep(internalName: String, dependencyType: DependencyType) {
    val typeName = internalName.replace('/', '.')
    dependencies.getOrPut(typeName) { mutableSetOf() }.add(dependencyType)
  }

  private fun addType(type: String?, dependencyType: DependencyType) {
    type?.let {
      val typeName = Type.getType(it).className
      addDep(typeName, dependencyType)
    }
  }

  private fun addMethodDescriptor(
    descriptor: String?,
    paramDependencyType: DependencyType,
    returnDependencyType: DependencyType
  ) {
    descriptor?.let {
      val methodType = Type.getMethodType(it)
      // Add return type dependency
      addType(methodType.returnType.descriptor, returnDependencyType)
      // Add parameter types dependencies
      methodType.argumentTypes.forEach { argType ->
        addType(argType.descriptor, paramDependencyType)
      }
    }
  }
}

class DependencyMethodVisitor(
  val dependencies: MutableMap<String, MutableSet<DependencyType>>
) : MethodVisitor(Opcodes.ASM9) {

  override fun visitMethodInsn(
    opcode: Int,
    owner: String?,
    name: String?,
    descriptor: String?,
    isInterface: Boolean
  ) {
    // Add method reference dependency
    owner?.let { addDep(it, DependencyType.METHOD_REFERENCE) }
    // Add method descriptor dependencies (for parameter and return types)
    descriptor?.let { addMethodDescriptor(it) }
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  }

  override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
    // Add field reference dependency
    owner?.let { addDep(it, DependencyType.FIELD_REFERENCE) }
    // Add field type dependency
    descriptor?.let { addType(it, DependencyType.FIELD_TYPE) }
    super.visitFieldInsn(opcode, owner, name, descriptor)
  }

  override fun visitTypeInsn(opcode: Int, type: String?) {
    // Add instance creation or local variable dependency based on opcode
    type?.let {
      val dependencyType = when (opcode) {
        Opcodes.NEW -> DependencyType.INSTANCE_CREATION
        else -> DependencyType.LOCAL_VARIABLE
      }
      addType(it, dependencyType)
    }
    super.visitTypeInsn(opcode, type)
  }

  override fun visitLdcInsn(value: Any?) {
    // Add class literal dependency
    if (value is Type) {
      addType(value.descriptor, DependencyType.LOCAL_VARIABLE)
    }
    super.visitLdcInsn(value)
  }

  override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
    // Add local variable dependency for multi-dimensional arrays
    descriptor?.let { addType(it, DependencyType.LOCAL_VARIABLE) }
    super.visitMultiANewArrayInsn(descriptor, numDimensions)
  }

  override fun visitInvokeDynamicInsn(
    name: String?,
    descriptor: String?,
    bootstrapMethodHandle: Handle?,
    vararg bootstrapMethodArguments: Any?
  ) {
    // Add dynamic binding dependency
    descriptor?.let { addMethodDescriptor(it, DependencyType.DYNAMIC_BINDING) }
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
  }

  override fun visitLocalVariable(
    name: String?,
    descriptor: String?,
    signature: String?,
    start: Label?,
    end: Label?,
    index: Int
  ) {
    // Add local variable dependency
    descriptor?.let { addType(it, DependencyType.LOCAL_VARIABLE) }
    super.visitLocalVariable(name, descriptor, signature, start, end, index)
  }

  override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
    // Add exception type dependency
    type?.let { addType(it, DependencyType.EXCEPTION_TYPE) }
    super.visitTryCatchBlock(start, end, handler, type)
  }

  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
    // Add annotation type dependency
    descriptor?.let { addType(it, DependencyType.ANNOTATION) }
    return super.visitAnnotation(descriptor, visible)
  }

  private fun addDep(internalName: String, dependencyType: DependencyType) {
    val typeName = internalName.replace('/', '.')
    dependencies.getOrPut(typeName) { mutableSetOf() }.add(dependencyType)
  }

  private fun addType(type: String, dependencyType: DependencyType): Unit {
    addDep(getTypeName(type) ?: return, dependencyType)
  }

  private fun getTypeName(type: String): String? = try {
    val name = when {
      // For array types, get the class name
      type.startsWith("L") && type.endsWith(";") -> getTypeName(type.substring(1, type.length - 1))
      // Handle the case where the descriptor appears to be a plain class name
      !type.startsWith("[") && !type.startsWith("L") && !type.endsWith(";") -> Type.getObjectType(
        type.classToPath
      ).className
      // Handle the case where the descriptor is missing 'L' and ';'
      type.contains("/") && !type.startsWith("L") && !type.endsWith(";") -> Type.getObjectType(type).className
      // For primitive types, use the descriptor directly
      type.length == 1 && "BCDFIJSZ".contains(type[0]) -> type
      type.endsWith("$") -> type.substring(0, type.length - 1)
      else -> Type.getType(type).className
    }
    name
  } catch (e: Exception) {
    println("Error adding type: $type (${e.message})")
    null
  }

  private fun addMethodDescriptor(descriptor: String, dependencyType: DependencyType = DependencyType.UNKNOWN) {
    val methodType = Type.getMethodType(descriptor)
    // Add return type dependency
    addType(methodType.returnType.descriptor, dependencyType)
    // Add parameter types dependencies
    methodType.argumentTypes.forEach { argType ->
      addType(argType.descriptor, dependencyType)
    }
  }
}

val String.jarFiles
  get() = File(this).listFiles()?.filter {
    it.isFile && it.name.endsWith(".jar")
  }
val CharSequence.symbolName
  get() =
    replace("""^[\[ILZBCFDJSV]+""".toRegex(), "")
      .removeSuffix(";").replace('/', '.')
      .removeSuffix(".class")


val String.escape get() = replace("$", "\\$")

val String.classToPath
  get() = replace('.', '/')
    .removeSuffix(".class")
