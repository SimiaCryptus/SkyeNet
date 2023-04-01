@file:Suppress("MemberVisibilityCanBePrivate")

package com.simiacryptus.skyenet

import java.lang.reflect.Modifier


/**
 * The body is the physical manifestation of the brain.
 * It connects the heart and the brain, providing a framework for action
 */
class Body(
    val brain: Brain,
    val apiObjects: Map<String, Any> = mapOf(),
) {
    val heart: Heart by lazy { Heart("", apiObjects) }

    fun commandToCode(
        command: String,
    ): Brain.AssistantInstruction {
        return brain.interpretCommand(commandWithContext(command))
    }

    fun commandWithContext(command: String): Brain.AssistantCommand {
        var methods: List<String> = listOf()
        var types: Map<String, String> = mapOf()
        apiObjects.forEach { (name, utilityObj) ->
            val clazz = Class.forName(utilityObj.javaClass.typeName)
            methods = methods + clazz.methods
                .filter { Modifier.isPublic(it.modifiers) }
                .filter { it.declaringClass == clazz }
                .filter { !it.isSynthetic }
                .map {
                    val methodDefinition = "${name}.${it.name}(${
                        it.parameters.joinToString(",") {
                            it.type.name + " " + it.name
                        }
                    }): ${it.returnType.name}"
                    val typesToAdd = listOf(it.returnType) + it.parameters.map { it.type }
                        .filter { !it.isPrimitive }
                        .filter { it != clazz }
                        .filter { !it.isSynthetic }
                        .distinct()
                    types = types + typesToAdd.map {
                        it.name to "(${
                            it.declaredFields
                                .filter { Modifier.isPublic(it.modifiers) }
                                .joinToString(",") {
                                    it.type.name + " " + it.name
                                }
                        })"
                    }
                    methodDefinition
                }
            types = types + clazz.declaredClasses.filter { Modifier.isPublic(it.modifiers) }.associate {
                it.name to "(${
                    it.declaredFields
                        .filter { Modifier.isPublic(it.modifiers) }
                        .joinToString(",") {
                            it.type.name + " " + it.name
                        }
                })"
            }
        }
        types = types
            .filter { !it.key.startsWith("java.") }
            .filter { !setOf("void").contains(it.key) }
        val assistantCommand = Brain.AssistantCommand(
            command = command,
            methods = methods,
            types = types,
        )
        return assistantCommand
    }
}