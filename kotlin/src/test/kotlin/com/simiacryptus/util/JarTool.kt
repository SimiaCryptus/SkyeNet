@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.util

import org.objectweb.asm.*
import java.util.jar.JarFile

object JarTool {
  @JvmStatic
  fun main(args: Array<String>) {
    val jarPath = "C:\\Users\\andre\\code\\SkyeNet\\kotlin-hack\\build\\libs\\kotlin-hack-1.0.43-stage2.jar"
    val classDependencies = readDependencies(jarPath)
    //classDependencies.forEach { println(it) }

    println("Required classes:")
    classDependencies.values.flatten()
      .filter { !classDependencies.containsKey(it) }
      .toSet().filter {
        when {
          it.startsWith("java.") -> false
          it.length < 2 -> false
          else -> true
        }
      }.sorted().forEach { println(it) }

    val roots = classDependencies.keys.filter {
      when {
        it.contains("Jsr223") -> true
        else -> false
      }
    }
    println("Required classes for $roots:")
    val requirements = roots.flatMap { findRequiredClasses(classDependencies, it) }.filter {
      when {
        it.startsWith("java.") -> false
        it.startsWith("javax.") -> false
        it.length < 2 -> false
        else -> true
      }
    }.distinct().sorted()
    requirements.forEach { println(it) }

  }

  fun findRequiredClasses(
    classDependencies: MutableMap<String, Set<String>>,
    classname: String,
    chainFromRoot: List<String> = listOf()
  ): Set<String> {
    val dependencies = classDependencies[classname] ?: return setOf()
    return dependencies + dependencies.filter {
      it != classname && !chainFromRoot.contains(it)
    }.flatMap { findRequiredClasses(classDependencies, it, chainFromRoot + classname) }
      .distinct()
  }

  fun readDependencies(jarPath: String): MutableMap<String, Set<String>> {
    val classDependencies = mutableMapOf<String, Set<String>>()
    JarFile(jarPath).use { jarFile ->
      jarFile.entries().asSequence().filter { it.name.endsWith(".class") }.forEach { entry ->
        val className = entry.name.substringBeforeLast(".class").replace('/', '.')
        val dependencyCollector = DependencyCollector()
        jarFile.getInputStream(entry).use { inputStream ->
          inputStream.readBytes().let { bytes ->
            ClassReader(bytes).accept(dependencyCollector, 0)
          }
        }
        classDependencies[className] =
          dependencyCollector.dependencies.map {
            it.replace("""^[\[ILZBCFDJSV]+""".toRegex(), "")
              .removeSuffix(";")
              .replace('/', '.')
          }.toSet()
      }
    }
    return classDependencies
  }

  class DependencyCollector : ClassVisitor(Opcodes.ASM9) {
    val dependencies: MutableSet<String> = mutableSetOf()
    val methodVisitor = object : MethodVisitor(Opcodes.ASM9) {
      override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
      ) {
        addType(owner)
        addMethodDescriptor(descriptor)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      }

      override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        addType(owner)
        addType(descriptor)
        super.visitFieldInsn(opcode, owner, name, descriptor)
      }

      override fun visitTypeInsn(opcode: Int, type: String?) {
        addType(type)
        super.visitTypeInsn(opcode, type)
      }

      override fun visitLdcInsn(value: Any?) {
        if (value is Type) {
          addType(value.descriptor)
        }
        super.visitLdcInsn(value)
      }

      override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        addType(descriptor)
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
      }

      override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
      ) {
        addMethodDescriptor(descriptor)
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
        addType(descriptor)
        super.visitLocalVariable(name, descriptor, signature, start, end, index)
      }

      override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        addType(type)
        super.visitTryCatchBlock(start, end, handler, type)
      }

      override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        addType(descriptor)
        return super.visitAnnotation(descriptor, visible)
      }
    }

    override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
    ) {
      superName?.let { dependencies.add(it) }
      interfaces?.forEach { dependencies.add(it) }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
      addType(desc)
      return super.visitField(access, name, desc, signature, value)
    }

    override fun visitMethod(
      access: Int,
      name: String?,
      desc: String?,
      signature: String?,
      exceptions: Array<out String>?
    ): MethodVisitor? {
      addMethodDescriptor(desc)
      // apply methodVisitor
      return super.visitMethod(access, name, desc, signature, exceptions)
    }

    private fun addType(type: String?) {
      type?.let {
        val typeName = it.substringBefore('<').replace('/', '.')
        dependencies.add(typeName)
      }
    }

    private fun addMethodDescriptor(descriptor: String?) {
      descriptor?.let {
        val returnType = it.substringAfterLast(')').substringAfterLast(')')
        addType(returnType)
        val parameterTypes = it.substringAfter('(').substringBeforeLast(')')
        parameterTypes.split(';').forEach { addType(it) }
      }
    }

  }

}