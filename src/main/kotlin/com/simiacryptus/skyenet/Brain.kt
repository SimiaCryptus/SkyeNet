package com.simiacryptus.skyenet

import com.simiacryptus.openai.ChatMessage
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.DescriptorUtil.toYaml
import com.simiacryptus.util.JsonUtil.toJson
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class Brain(
    val api: OpenAIClient,
    val apiObjects: Map<String, Any> = mapOf(),
    var model: String = "gpt-3.5-turbo", // "gpt-4-0314"
    var verbose: Boolean = false,
    var maxTokens: Int = 8192,
    var temperature: Double = 0.3,
    private val moderated: Boolean = true,
) {
    val metrics: Map<String, Any>
        get() = hashMapOf(
            "totalInputLength" to totalInputLength.get(),
            "totalOutputLength" to totalOutputLength.get(),
            "totalApiDescriptionLength" to totalApiDescriptionLength.get(),
            "totalExamplesLength" to totalExamplesLength.get(),
        ) + api.metrics
    protected val totalInputLength = AtomicInteger(0)
    protected val totalOutputLength = AtomicInteger(0)
    protected val totalExamplesLength = AtomicInteger(0)
    protected val totalApiDescriptionLength: AtomicInteger = AtomicInteger(0)


    fun implement(prompt: String): String {
        if (verbose) log.info(prompt)
        val request = ChatRequest()
        val apiDescription = apiDescription(apiObjects)
        request.messages = (
                listOf(
                    ChatMessage(
                        ChatMessage.Role.system, """
                |You will translate natural language instructions into 
                |an implementation using Kotlin and the script context.
                |Do not include explaining text outside of the code blocks.
                |Defined symbols include ${apiObjects.keys.joinToString(", ")}.
                |The runtime context is described below:
                |
                |$apiDescription
                |""".trimMargin().trim()
                    )
                ) + listOf(
                    ChatMessage(
                        ChatMessage.Role.user,
                        prompt
                    )
                )).toTypedArray()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        return run(request)
    }

    fun fixCommand(prompt: String, previousCode: String, errorMessage: String): String {
        if (verbose) log.info(prompt)
        val request = ChatRequest()
        val apiDescription = apiDescription(apiObjects)
        request.messages = (
                listOf(
                    ChatMessage(
                        ChatMessage.Role.system, """
                |You will translate natural language instructions into 
                |an implementation using Kotlin and the script context.
                |Do not include explaining text outside of the code blocks.
                |Defined symbols include ${apiObjects.keys.joinToString(", ")}.
                |The runtime context is described below:
                |
                |$apiDescription
                |""".trimMargin().trim()
                    )
                ) + listOf(
                    ChatMessage(
                        ChatMessage.Role.user,
                        prompt
                    )
                ) + listOf(
                    ChatMessage(
                        ChatMessage.Role.system,
                        """
                |The previous code failed with the following error:
                |```kotlin
                |${errorMessage.indent()}
                |```
                |The previous code was:
                |```kotlin
                |${previousCode.indent()}
                |```
                |""".trimMargin().trim()
                    )
                )).toTypedArray()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        return run(request)
    }

    private fun run(request: ChatRequest): String {
        request.model = model
        request.max_tokens = maxTokens
        request.temperature = temperature
        val json = toJson(request)
        if (moderated) api.moderate(json)
        totalInputLength.addAndGet(json.length)
        var response = api.chat(request).response.get().toString()
        if (verbose) log.info(response)
        totalOutputLength.addAndGet(response.length)
        response = response.trim()

        // If the response is wrapped in a code block, remove it
        if(response.contains("```")) {
            val startIndex = response.indexOf('\n', response.indexOf("```"))
            val endIndex = response.lastIndexOf("```")
            val trim = response.substring(startIndex, endIndex).trim()
            response = trim
        }

        return response
    }


    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(Brain::class.java)
        fun String.indent() = this.replace("\n", "\n  ")
        fun joinYamlList(typeDescriptions: List<String>) = typeDescriptions.joinToString("\n") {
            "- " + it.indent()
        }

        fun Method.superMethod(): Method? {
            val superMethod = declaringClass.superclasses.flatMap { it.methods.toList() }
                .find { it.name == name && it.parameters.size == parameters.size }
            return superMethod?.superMethod() ?: superMethod
        }

        val <T> Class<T>.superclasses: List<Class<*>>
            get() {

                val superclass = superclass
                val supers = if (superclass == null) listOf()
                else superclass.superclasses + listOf(superclass)
                return (interfaces.toList() + supers).distinct()
            }

        fun apiDescription(apiObjects: Map<String, Any>): String {
            val types = ArrayList<Class<*>>()
            val apiobjs = apiObjects.map { (name, utilityObj) ->
                val clazz = Class.forName(utilityObj.javaClass.typeName)
                val methods = clazz.methods
                    .filter { Modifier.isPublic(it.modifiers) }
                    .filter { it.declaringClass == clazz }
                    .filter { !it.isSynthetic }
                    .map { it.superMethod() ?: it }
                    .filter { it.declaringClass != Object::class.java }
                types.addAll(methods.flatMap { (listOf(it.returnType) + it.parameters.map { it.type }).filter { it != clazz } })
                types.addAll(clazz.declaredClasses.filter { Modifier.isPublic(it.modifiers) })
                """
                    |$name:
                    |  operations:
                    |    ${joinYamlList(methods.map { it.toYaml(true) }).indent().indent()}
                    |""".trimMargin().trim()
            }.toTypedArray()
            val typeDescriptions = types
                .filter { !it.isPrimitive }
                .filter { !it.isSynthetic }
                .filter { !it.name.startsWith("java.") }
                .filter { !setOf("void").contains(it.name) }
                .distinct().map {
                    """
                |${it.simpleName}:
                |  ${it.toYaml().indent()}
                """.trimMargin().trim()
                }.toTypedArray()
            return """
                |api_objects:
                |  ${apiobjs.joinToString("\n").indent()}
                |components:
                |  schemas:
                |    ${typeDescriptions.joinToString("\n").indent().indent()}
            """.trimMargin()
        }

    }

}