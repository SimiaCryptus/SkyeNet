@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.simiacryptus.skyenet

import com.simiacryptus.openai.models.OpenAIModel
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClient.*
import com.simiacryptus.openai.OpenAIClientBase.Companion.toContentList
import com.simiacryptus.openai.models.OpenAITextModel
import com.simiacryptus.util.JsonUtil.toJson
import com.simiacryptus.util.describe.TypeDescriber
import com.simiacryptus.util.describe.YamlDescriber
import org.intellij.lang.annotations.Language
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

open class Brain(
    val api: OpenAIClient,
    val symbols: java.util.Map<String, Object> = java.util.HashMap<String, Object>() as java.util.Map<String, Object>,
    val model: OpenAITextModel = ChatModels.GPT35Turbo,
    private val verbose: Boolean = false,
    val temperature: Double = 0.3,
    val describer: TypeDescriber = YamlDescriber(),
    val language: String = "Kotlin",
    private val moderated: Boolean = true,
    private val apiDescription: String = apiDescription(symbols, describer),
) {
    private val totalInputLength = AtomicInteger(0)
    private val totalOutputLength = AtomicInteger(0)
    private val totalApiDescriptionLength: AtomicInteger = AtomicInteger(0)

    open fun implement(vararg prompt: String): String {
        if (verbose) log.info("Prompt: \n\t" + prompt.joinToString("\n\t"))
        return implement(*(getChatSystemMessages(apiDescription) +
                prompt.map { ChatMessage(Role.user, it.toContentList()) }).toTypedArray()
        )
    }

    fun implement(
        vararg messages: ChatMessage
    ): String {
        var request = ChatRequest()
        request = request.copy(messages = ArrayList(messages.toList()))
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        return chat(request)
    }

    @Language("TEXT")
    open fun getChatSystemMessages(apiDescription: String): List<ChatMessage> = listOf(
        ChatMessage(
            Role.system, """
                        |You will translate natural language instructions into 
                        |an implementation using $language and the script context.
                        |Use ``` code blocks labeled with $language where appropriate.
                        |Defined symbols include ${symbols.keySet().joinToString(", ")}.
                        |The runtime context is described below:
                        |
                        |$apiDescription
                        |""".trimMargin().trim().toContentList()
        )
    )

    fun fixCommand(
        previousCode: String,
        error: Throwable,
        output: String,
        vararg promptMessages: ChatMessage
    ): Pair<String, List<Pair<String, String>>> {
        val request = ChatRequest(
            messages = ArrayList(
                    promptMessages.toList() + listOf(
                        ChatMessage(
                            Role.assistant,
                            """
                                |```${language.lowercase()}
                                |${previousCode}
                                |```
                                |""".trimMargin().trim().toContentList()
                        ),
                        ChatMessage(
                            Role.system,
                            """
                                |The previous code failed with the following error:
                                |
                                |```
                                |${error.message?.trim() ?: ""}
                                |```
                                |
                                |Output:
                                |```
                                |${output.trim()}
                                |```
                                |
                                |Correct the code and try again.
                                |""".trimMargin().trim().toContentList()
                        )
                    ))
        )
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        val response = chat(request)
        val codeBlocks = extractCodeBlocks(response)
        return Pair(response, codeBlocks)
    }

    private fun chat(_request: ChatRequest): String {
        val request = _request.copy(model = model.modelName, temperature = temperature)
        val json = toJson(request)
        if (moderated) api.moderate(json)
        totalInputLength.addAndGet(json.length)
        val chatResponse = api.chat(request, model)
        var response = chatResponse.choices.first().message?.content.orEmpty()
        if (verbose) log.info(response)
        totalOutputLength.addAndGet(response.length)
        response = response.trim()
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

        private val <T> Class<T>.superclasses: List<Class<*>>
            get() {
                val superclass = superclass
                val supers = if (superclass == null) listOf()
                else superclass.superclasses + listOf(superclass)
                return (interfaces.toList() + supers).distinct()
            }

        fun apiDescription(hands: java.util.Map<String, Object>, yamlDescriber: TypeDescriber): String {
            val types = ArrayList<Class<*>>()

            val apiobjs = hands.entrySet().map { (name, utilityObj) ->
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
                    |    ${joinYamlList(methods.map { yamlDescriber.describe(it) }).indent().indent()}
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
                |  ${yamlDescriber.describe(it).indent()}
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

        fun extractCodeBlocks(response: String): List<Pair<String, String>> {
            val codeBlockRegex = Regex("(?s)```(.*?)\\n(.*?)```")
            val languageRegex = Regex("([a-zA-Z0-9-_]+)")

            val result = mutableListOf<Pair<String, String>>()
            var startIndex = 0

            val matches = codeBlockRegex.findAll(response)
            if (matches.count() == 0) return listOf(Pair("text", response))
            for (match in matches) {
                // Add non-code block before the current match as "text"
                if (startIndex < match.range.first) {
                    result.add(Pair("text", response.substring(startIndex, match.range.first)))
                }

                // Extract language and code
                val languageMatch = languageRegex.find(match.groupValues[1])
                val language = languageMatch?.groupValues?.get(0) ?: "code"
                val code = match.groupValues[2]

                // Add code block to the result
                result.add(Pair(language, code))

                // Update the start index
                startIndex = match.range.last + 1
            }

            // Add any remaining non-code text after the last code block as "text"
            if (startIndex < response.length) {
                result.add(Pair("text", response.substring(startIndex)))
            }

            return result
        }

    }

}