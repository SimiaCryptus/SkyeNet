package com.simiacryptus.skyenet.core.util

import org.objectweb.asm.*
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import java.io.File
import java.util.jar.JarFile

object ClasspathRelationships {
  sealed class Relation
  data object INHERITANCE : Relation() // When a class extends another class
  data object INTERFACE_IMPLEMENTATION : Relation() // When a class implements an interface
  data object FIELD_TYPE : Relation() // When a class has a field of another class type
  data object METHOD_PARAMETER :
    Relation() // When a class has a method that takes another class as a parameter

  data object METHOD_RETURN_TYPE : Relation() // When a class has a method that returns another class
  data object LOCAL_VARIABLE :
    Relation() // When a method within a class declares a local variable of another class

  data object EXCEPTION_TYPE :
    Relation() // When a method declares that it throws an exception of another class

  data object ANNOTATION :
    Relation() // When a class, method, or field is annotated with another class (annotation)

  data object INSTANCE_CREATION : Relation() // When a class creates an instance of another class
  data object METHOD_REFERENCE : Relation() // When a method references another class's method
  data object METHOD_SIGNATURE :
    Relation() // When a method signature references another class

  data object FIELD_REFERENCE : Relation() // When a method references another class's field
  data object DYNAMIC_BINDING :
    Relation() // When a class uses dynamic binding (e.g., invoke dynamic) related to another class

  data object OUTER_CLASS : Relation() // When a class references its outer class

  data object UNKNOWN : Relation() // A fallback for unknown or unclassified dependencies


  class DependencyClassVisitor(
    val dependencies: MutableMap<String, MutableSet<Relation>> = mutableMapOf(),
    var access: Int = 0,
  ) : ClassVisitor(Opcodes.ASM9) {

    override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
    ) {
      this.access = access
      // Add superclass dependency
      superName?.let { addDep(it, INHERITANCE) }
      // Add interface dependencies
      interfaces?.forEach { addDep(it, INTERFACE_IMPLEMENTATION) }
      visitSignature(name, signature)
      super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(
      access: Int,
      name: String?,
      desc: String?,
      signature: String?,
      value: Any?
    ): FieldVisitor? {
      visitSignature(name, signature)
      // Add field type dependency
      addType(desc, FIELD_TYPE)
      return DependencyFieldVisitor(dependencies)
    }

    override fun visitMethod(
      access: Int,
      name: String?,
      desc: String?,
      signature: String?,
      exceptions: Array<out String>?
    ): MethodVisitor {
      visitSignature(name, signature)
      // Add method return type and parameter types dependencies
      addMethodDescriptor(desc, METHOD_PARAMETER, METHOD_RETURN_TYPE)
      // Add exception types dependencies
      exceptions?.forEach { addDep(it, EXCEPTION_TYPE) }
      return DependencyMethodVisitor(dependencies)
    }

    private fun visitSignature(name: String?, signature: String?) {
      // Check if the name indicates an inner class or property accessor
      if (name?.contains("$") == true) {
        // NOTE: This isn't a typically required dependency
        // addDep(name.substringBefore("$"), OUTER_CLASS)
      }
      if (name?.contains("baseClassLoader") == true) {
        signature?.let {
          val signatureReader = SignatureReader(it)
          signatureReader.accept(object : SignatureVisitor(Opcodes.ASM9) {
            override fun visitClassType(name: String?) {
              name?.let { addDep(it, METHOD_PARAMETER) }
            }
          })
        }
        return
      }
      signature?.let {
        val signatureReader = SignatureReader(it)
        signatureReader.accept(object : SignatureVisitor(Opcodes.ASM9) {
          override fun visitClassType(name: String?) {
            name?.let { addDep(it, METHOD_PARAMETER) }
          }
        })
      }
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
      // Add annotation type dependency
      addType(descriptor, ANNOTATION)
      return super.visitAnnotation(descriptor, visible)
    }

    private fun addDep(internalName: String, relationType: Relation) {
      val typeName = internalName.replace('/', '.')
      dependencies.getOrPut(typeName) { mutableSetOf() }.add(relationType)
    }

    private fun addType(type: String?, relationType: Relation) {
      type?.let {
        val typeName = Type.getType(it).className
        addDep(typeName, relationType)
      }
    }

    private fun addMethodDescriptor(
      descriptor: String?,
      paramRelationType: Relation,
      returnRelationType: Relation
    ) {
      descriptor?.let {
        val methodType = Type.getMethodType(it)
        // Add return type dependency
        addType(methodType.returnType.descriptor, returnRelationType)
        // Add parameter types dependencies
        methodType.argumentTypes.forEach { argType ->
          addType(argType.descriptor, paramRelationType)
        }
      }
    }

  }

  class DependencyFieldVisitor(
    val dependencies: MutableMap<String, MutableSet<Relation>>
  ) : FieldVisitor(Opcodes.ASM9) {

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
      descriptor?.let { addType(it, ANNOTATION) }
      return super.visitAnnotation(descriptor, visible)
    }

    override fun visitTypeAnnotation(
      typeRef: Int,
      typePath: TypePath?,
      descriptor: String?,
      visible: Boolean
    ): AnnotationVisitor? {
      descriptor?.let { addType(it, ANNOTATION) }
      return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
    }

    private fun addDep(internalName: String, relationType: Relation) {
      val typeName = internalName.replace('/', '.')
      dependencies.getOrPut(typeName) { mutableSetOf() }.add(relationType)
    }

    private fun addType(type: String, relationType: Relation) {
      addDep(getTypeName(type) ?: return, relationType)
    }

  }

  class DependencyMethodVisitor(
    val dependencies: MutableMap<String, MutableSet<Relation>>
  ) : MethodVisitor(Opcodes.ASM9) {

    override fun visitMethodInsn(
      opcode: Int,
      owner: String?,
      name: String?,
      descriptor: String?,
      isInterface: Boolean
    ) {
      // Add method reference dependency
      owner?.let { addDep(it, METHOD_REFERENCE) }
      // Add method descriptor dependencies (for parameter and return types)
      descriptor?.let { addMethodDescriptor(it, METHOD_SIGNATURE) }
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitParameter(name: String?, access: Int) {
      // Add method parameter type dependency
      name?.let { addType(it, METHOD_PARAMETER) }
      super.visitParameter(name, access)
    }


    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
      // Add field reference dependency
      owner?.let { addDep(it, FIELD_REFERENCE) }
      // Add field type dependency
      descriptor?.let { addType(it, FIELD_TYPE) }
      super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
      // Add instance creation or local variable dependency based on opcode
      type?.let {
        val dependencyType = when (opcode) {
          Opcodes.NEW -> INSTANCE_CREATION
          else -> LOCAL_VARIABLE
        }
        addType(it, dependencyType)
      }
      super.visitTypeInsn(opcode, type)
    }

    override fun visitLdcInsn(value: Any?) {
      // Add class literal dependency
      if (value is Type) {
        addType(value.descriptor, LOCAL_VARIABLE)
      }
      super.visitLdcInsn(value)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
      // Add local variable dependency for multi-dimensional arrays
      descriptor?.let { addType(it, LOCAL_VARIABLE) }
      super.visitMultiANewArrayInsn(descriptor, numDimensions)
    }

    override fun visitInvokeDynamicInsn(
      name: String?,
      descriptor: String?,
      bootstrapMethodHandle: Handle?,
      vararg bootstrapMethodArguments: Any?
    ) {
      // Add dynamic binding dependency
      descriptor?.let { addMethodDescriptor(it, DYNAMIC_BINDING) }
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
      descriptor?.let { addType(it, LOCAL_VARIABLE) }
      super.visitLocalVariable(name, descriptor, signature, start, end, index)
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
      // Add exception type dependency
      type?.let { addType(it, EXCEPTION_TYPE) }
      super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
      // Add annotation type dependency
      descriptor?.let { addType(it, ANNOTATION) }
      return super.visitAnnotation(descriptor, visible)
    }

    private fun addDep(internalName: String, relationType: Relation) {
      val typeName = internalName.replace('/', '.')
      dependencies.getOrPut(typeName) { mutableSetOf() }.add(relationType)
    }

    private fun addType(type: String, relationType: Relation): Unit {
      addDep(getTypeName(type) ?: return, relationType)
    }

    private fun addMethodDescriptor(
      descriptor: String,
      relationType: Relation
    ) {
      val methodType = Type.getMethodType(descriptor)
      // Add return type dependency
      addType(methodType.returnType.descriptor, relationType)
      // Add parameter types dependencies
      methodType.argumentTypes.forEach { addType(it.descriptor, relationType) }
    }
  }

  private fun getTypeName(type: String): String? = try {
    val name = when {
      // For array types, get the class name
      type.startsWith("L") && type.endsWith(";") -> getTypeName(type.substring(1, type.length - 1))
      // Handle the case where the descriptor appears to be a plain class name
      !type.startsWith("[") && !type.startsWith("L") && !type.endsWith(";") -> Type.getObjectType(type.classToPath).className
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


  val String.classToPath
    get() = removeSuffix(".class").replace('.', '/')


  val String.jarFiles
    get() = File(this).listFiles()?.filter {
      it.isFile && it.name.endsWith(".jar")
    }
  val CharSequence.symbolName
    get() =
      replace("""^[\[ILZBCFDJSV]+""".toRegex(), "")
        .removeSuffix(";").replace('/', '.')
        .removeSuffix(".class")

  data class Reference(
    val from: String,
    val to: String,
    val relation: Relation
  )

  fun analyzeJar(jarPath: String) = analyzeJar(readJarClasses(jarPath))
  fun analyzeJar(
    jar: Map<String, ByteArray?>,
  ) = jar.flatMap { (className, classData) ->
    val dependencyClassVisitor = DependencyClassVisitor()
    ClassReader(classData).accept(dependencyClassVisitor, 0)
    val dependencies = dependencyClassVisitor.dependencies
    dependencies.flatMap { (to, dependencies) ->
      dependencies.map { Reference(className, to, it) }
    }
  }

  fun classAccessMap(jarPath: String) = classAccessMap(readJarClasses(jarPath))

  fun classAccessMap(
    jar: Map<String, ByteArray?>,
  ): Map<String, Int> = jar.map { (className, classData) ->
    val dependencyClassVisitor = DependencyClassVisitor()
    ClassReader(classData).accept(dependencyClassVisitor, 0)
    className to dependencyClassVisitor.access
  }.toMap()


  fun readJarClasses(jarPath: String) = JarFile(jarPath).use { jarFile ->
    jarFile.entries().asSequence().filter { it.name.endsWith(".class") }.map { entry ->
      val className = entry.name.replace('/', '.').removeSuffix(".class")
      className to jarFile.getInputStream(entry)?.readBytes()
    }.toMap()
  }

  fun readJarFiles(jarPath: String) = JarFile(jarPath).use { jarFile ->
    jarFile.entries().asSequence().map { it.name }.toList().toTypedArray()
  }

  fun upstream(
    dependencies: List<Reference>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ) = upstream(upstreamMap(dependencies), className, buffer)

  fun upstreamMap(dependencies: List<Reference>) =
    dependencies.groupBy { it.to }

  fun upstream(
    dependencies: Map<String, List<Reference>>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ): Set<String> {
    val required = (dependencies[className] ?: listOf())
      .map { it.from }
      .filter { className != it }
      .filter { !buffer.contains(it) }
      .filter { it.isNotBlank() }
      .toTypedArray()
    synchronized(buffer) { buffer.addAll(required) }
    required.toList().parallelStream().forEach { upstream(dependencies, it, buffer).stream() }
    return buffer
  }

  fun downstream(
    dependencies: Map<String, List<Reference>>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ): Set<String> {
    val required = (dependencies[className] ?: listOf())
      .map { it.to }
      .filter { className != it }
      .filter { !buffer.contains(it) }
      .filter { it.isNotBlank() }
      .toTypedArray()
    synchronized(buffer) { buffer.addAll(required) }
    required.toList().parallelStream().forEach { downstream(dependencies, it, buffer).stream() }
    return buffer
  }

  fun downstream(
    dependencies: List<Reference>,
    className: String,
    buffer: MutableSet<String> = mutableSetOf(className)
  ) = downstream(downstreamMap(dependencies), className, buffer)

  fun downstreamMap(dependencies: List<Reference>) =
    dependencies.groupBy { it.from }

}