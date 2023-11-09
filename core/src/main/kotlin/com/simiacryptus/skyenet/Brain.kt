package com.simiacryptus.skyenet

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClient.ChatMessage
import com.simiacryptus.openai.OpenAIClient.ChatRequest
import com.simiacryptus.util.JsonUtil.toJson
import com.simiacryptus.util.describe.TypeDescriber
import com.simiacryptus.util.describe.YamlDescriber
import org.intellij.lang.annotations.Language
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
open class Brain(
    val api: OpenAIClient,
    val symbols: java.util.Map<String, Object> = java.util.HashMap<String, Object>() as java.util.Map<String, Object>,
    var model: OpenAIClient.Model = OpenAIClient.Models.GPT35Turbo,
    var verbose: Boolean = false,
    var temperature: Double = 0.3,
    var describer: TypeDescriber = YamlDescriber(),
    val language: String = "Kotlin",
    private val moderated: Boolean = true,
    val apiDescription: String = apiDescription(symbols, describer),
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


    open fun implement(vararg prompt: String): String {
        if (verbose) log.info("Prompt: \n\t" + prompt.joinToString("\n\t"))
        return implement(*getChatMessages(*prompt).toTypedArray())
    }

    fun getChatMessages(vararg prompt: String) = getChatSystemMessages(apiDescription) +
            prompt.map { ChatMessage(ChatMessage.Role.user, it) }

    fun implement(
        vararg messages: ChatMessage
    ): String {
        val request = ChatRequest()
        request.messages = messages.toList().toTypedArray()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        val response = chat(request)
        return response
    }

    @Language("TEXT")
    open fun getChatSystemMessages(apiDescription: String): List<ChatMessage> = listOf(
        ChatMessage(
            ChatMessage.Role.system, """
                        |You will translate natural language instructions into 
                        |an implementation using $language and the script context.
                        |Use ``` code blocks labeled with $language where appropriate.
                        |Defined symbols include ${symbols.keySet().joinToString(", ")}.
                        |The runtime context is described below:
                        |
                        |$apiDescription
                        |""".trimMargin().trim()
        )
    )

    open fun fixCommand(
        previousCode: String,
        error: Throwable,
        output: String,
        vararg prompt: String
    ): Pair<String, List<Pair<String, String>>> {
        val promptMessages = listOf(
            ChatMessage(
                ChatMessage.Role.system, """
                            |You will translate natural language instructions into 
                            |an implementation using $language and the script context.
                            |Use ``` code blocks labeled with $language where appropriate.
                            |Defined symbols include ${symbols.keySet().joinToString(", ")}.
                            |Do not include wrapping code blocks, assume a REPL context.
                            |The runtime context is described below:
                            |
                            |$apiDescription
                            |""".trimMargin().trim()
            )
        ) + prompt.map {
            ChatMessage(ChatMessage.Role.user, it)
        }
        if (verbose) log.info("Prompt: \n\t" + prompt.joinToString("\n\t"))
        return fixCommand(previousCode, error, output, *promptMessages.toTypedArray())
    }

    fun fixCommand(
        previousCode: String,
        error: Throwable,
        output: String,
        vararg promptMessages: ChatMessage
    ): Pair<String, List<Pair<String, String>>> {
        val request = ChatRequest()
        request.messages = (
                promptMessages.toList() + listOf(
                    ChatMessage(
                        ChatMessage.Role.assistant,
                        """
                                |```${language.lowercase()}
                                |${previousCode}
                                |```
                                |""".trimMargin().trim()
                    ),
                    ChatMessage(
                        ChatMessage.Role.system,
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
                                |""".trimMargin().trim()
                    )
                )).toTypedArray<ChatMessage>()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        val response = chat(request)
        val codeBlocks = extractCodeBlocks(response)
        return Pair(response, codeBlocks)
    }

    private fun chat(request: ChatRequest): String {
        request.model = model.modelName
        request.temperature = temperature
        val json = toJson(request)
        if (moderated) api.moderate(json)
        totalInputLength.addAndGet(json.length)
        val chatResponse = api.chat(request, model)
        var response = chatResponse.choices.first()?.message?.content.orEmpty()
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

        val <T> Class<T>.superclasses: List<Class<*>>
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

        /***
         * The input stream is parsed based on ```language\n...\n``` blocks.
         * A list of tuples is returned, where the first element is the language and the second is the code block
         * For intermediate non-code blocks, the language is "text"
         * For unlabeled code blocks, the language is "code"
         */
        fun extractCodeBlocks(response: String): List<Pair<String, String>> {
            val codeBlockRegex = Regex("(?s)```(.*?)\\n(.*?)```")
            val languageRegex = Regex("([a-zA-Z0-9-_]+)")

            val result = mutableListOf<Pair<String, String>>()
            var startIndex = 0

            for (match in codeBlockRegex.findAll(response)) {
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

        fun extractCodeBlock(response: String): String {
            var response1 = response
            if (response1.contains("```")) {
                val startIndex = response1.indexOf('\n', response1.indexOf("```"))
                val endIndex = response1.lastIndexOf("```")
                val trim = response1.substring(startIndex, endIndex).trim()
                response1 = trim
            }
            return response1
        }
    }

}