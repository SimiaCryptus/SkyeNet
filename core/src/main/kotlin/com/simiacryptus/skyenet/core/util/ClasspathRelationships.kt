package com.simiacryptus.skyenet.core.util

import org.objectweb.asm.*
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import java.util.jar.JarFile

object ClasspathRelationships {
    sealed class Relation {
        open val from_method: String = ""
        open val to_method: String = ""
    }

    data object INHERITANCE : Relation() // When a class extends another class
    data object INTERFACE_IMPLEMENTATION : Relation() // When a class implements an interface
    data class FIELD_TYPE(override val from_method: String) :
        Relation() // When a class has a field of another class type

    data class METHOD_PARAMETER(
        override val from_method: String
    ) : Relation() // When a class has a method that takes another class as a parameter

    data object METHOD_RETURN_TYPE : Relation() // When a class has a method that returns another class
    data class LOCAL_VARIABLE(override val from_method: String) :
        Relation() // When a method within a class declares a local variable of another class

    data class EXCEPTION_TYPE(override val from_method: String) :
        Relation() // When a method declares that it throws an exception of another class

    data class ANNOTATION(override val from_method: String) :
        Relation() // When a class, method, or field is annotated with another class (annotation)

    data class INSTANCE_CREATION(override val from_method: String) :
        Relation() // When a class creates an instance of another class

    data class METHOD_REFERENCE(
        override val from_method: String,
        override val to_method: String
    ) : Relation() // When a method references another class's method

    data class METHOD_SIGNATURE(
        override val from_method: String,
        override val to_method: String
    ) : Relation() // When a method signature references another class

    data class FIELD_REFERENCE(override val from_method: String) :
        Relation() // When a method references another class's field

    data class DYNAMIC_BINDING(override val from_method: String) :
        Relation() // When a class uses dynamic binding (e.g., invoke dynamic) related to another class


    class DependencyClassVisitor(
        val dependencies: MutableMap<String, MutableSet<Relation>> = mutableMapOf(),
        var access: Int = 0,
        var methods: MutableMap<String, DependencyMethodVisitor> = mutableMapOf(),
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
        ): FieldVisitor {
            visitSignature(name, signature)
            // Add field type dependency
            addType(desc, FIELD_TYPE(from_method = ""))
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
            addMethodDescriptor(desc, METHOD_PARAMETER(from_method = name ?: ""), METHOD_RETURN_TYPE)
            // Add exception types dependencies
            exceptions?.forEach { addDep(it, EXCEPTION_TYPE(from_method = name ?: "")) }
            val methodVisitor = DependencyMethodVisitor(name ?: "", dependencies)
            methods[methodVisitor.name] = methodVisitor
            return methodVisitor
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
                            name?.let { addDep(it, METHOD_PARAMETER(from_method = "")) }
                        }
                    })
                }
                return
            }
            signature?.let {
                val signatureReader = SignatureReader(it)
                signatureReader.accept(object : SignatureVisitor(Opcodes.ASM9) {
                    override fun visitClassType(name: String?) {
                        name?.let { addDep(it, METHOD_PARAMETER(from_method = "")) }
                    }
                })
            }
        }

        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            // Add annotation type dependency
            addType(descriptor, ANNOTATION(from_method = ""))
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
            descriptor?.let { addType(it, ANNOTATION(from_method = "")) }
            return super.visitAnnotation(descriptor, visible)
        }

        override fun visitAttribute(attribute: Attribute?) {
            super.visitAttribute(attribute)
        }

        override fun visitTypeAnnotation(
            typeRef: Int,
            typePath: TypePath?,
            descriptor: String?,
            visible: Boolean
        ): AnnotationVisitor? {
            descriptor?.let { addType(it, ANNOTATION(from_method = "")) }
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
        val name: String,
        val dependencies: MutableMap<String, MutableSet<Relation>>,
        var access: Int = 0,
    ) : MethodVisitor(Opcodes.ASM9) {


        override fun visitMethodInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?,
            isInterface: Boolean
        ) {
            access = opcode
            // Add method reference dependency
            owner?.let { addDep(it, METHOD_REFERENCE(from_method = this.name, to_method = name ?: "")) }
            // Add method descriptor dependencies (for parameter and return types)
            descriptor?.let {
                addMethodDescriptor(
                    it,
                    METHOD_SIGNATURE(from_method = this.name, to_method = name ?: "")
                )
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }

        override fun visitParameter(name: String?, access: Int) {
            // Add method parameter type dependency
            name?.let { addType(it, METHOD_PARAMETER(from_method = this.name)) }
            super.visitParameter(name, access)
        }


        override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
            // Add field reference dependency
            owner?.let { addDep(it, FIELD_REFERENCE(from_method = this.name)) }
            // Add field type dependency
            descriptor?.let { addType(it, FIELD_TYPE(from_method = this.name)) }
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }

        override fun visitTypeInsn(opcode: Int, type: String?) {
            // Add instance creation or local variable dependency based on opcode
            type?.let {
                val dependencyType = when (opcode) {
                    Opcodes.NEW -> INSTANCE_CREATION(from_method = this.name)
                    else -> LOCAL_VARIABLE(from_method = this.name)
                }
                addType(it, dependencyType)
            }
            super.visitTypeInsn(opcode, type)
        }

        override fun visitLdcInsn(value: Any?) {
            // Add class literal dependency
            if (value is Type) {
                addType(value.descriptor, LOCAL_VARIABLE(from_method = this.name))
            }
            super.visitLdcInsn(value)
        }

        override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
            // Add local variable dependency for multi-dimensional arrays
            descriptor?.let { addType(it, LOCAL_VARIABLE(from_method = this.name)) }
            super.visitMultiANewArrayInsn(descriptor, numDimensions)
        }

        override fun visitInvokeDynamicInsn(
            name: String?,
            descriptor: String?,
            bootstrapMethodHandle: Handle?,
            vararg bootstrapMethodArguments: Any?
        ) {
            // Add dynamic binding dependency
            descriptor?.let { addMethodDescriptor(it, DYNAMIC_BINDING(from_method = this.name)) }
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
            descriptor?.let { addType(it, LOCAL_VARIABLE(from_method = this.name)) }
            super.visitLocalVariable(name, descriptor, signature, start, end, index)
        }

        override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
            // Add exception type dependency
            type?.let { addType(it, EXCEPTION_TYPE(from_method = this.name)) }
            super.visitTryCatchBlock(start, end, handler, type)
        }

        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            // Add annotation type dependency
            descriptor?.let { addType(it, ANNOTATION(from_method = this.name)) }
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