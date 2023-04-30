package com.simiacryptus.skyenet

import com.simiacryptus.openai.ChatMessage
import com.simiacryptus.openai.ChatRequest
import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.util.JsonUtil.toJson
import com.simiacryptus.util.YamlDescriber
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
open class Brain(
    val api: OpenAIClient,
    val hands: java.util.Map<String, Object> = java.util.HashMap<String, Object>() as java.util.Map<String, Object>,
    var model: String = "gpt-3.5-turbo", // "gpt-4-0314"
    var verbose: Boolean = false,
    var maxTokens: Int = 8192,
    var temperature: Double = 0.3,
    var yamlDescriber: YamlDescriber = YamlDescriber(),
    val language: String = "Kotlin",
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


    open fun implement(prompt: String): String {
        val response = _implement(prompt)
        return extractCodeBlock(response)
    }

    open fun respondWithCode(prompt: String): Pair<String, List<Pair<String, String>>> {
        val response = _implement(prompt)
        return Pair(response, extractCodeBlocks(response))
    }

    protected fun _implement(prompt: String): String {
        if (verbose) log.info(prompt)
        val request = ChatRequest()
        val apiDescription = apiDescription(hands, yamlDescriber)
        request.messages = (
                getChatMessages(apiDescription) + listOf(
                    ChatMessage(
                        ChatMessage.Role.user,
                        prompt
                    )
                )).toTypedArray()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        val response = run(request)
        return response
    }

    open fun getChatMessages(apiDescription: String): List<ChatMessage> = listOf(
        ChatMessage(
            ChatMessage.Role.system, """
                        |You will translate natural language instructions into 
                        |an implementation using $language and the script context.
                        |Use ``` code blocks labeled with $language where appropriate.
                        |Defined symbols include ${hands.keySet().joinToString(", ")}.
                        |The runtime context is described below:
                        |
                        |$apiDescription
                        |""".trimMargin().trim()
        )
    )

    open fun fixCommand(prompt: String, previousCode: String, error: Exception, output: String): Pair<String, List<Pair<String, String>>> {
        val response = _fixCommand(prompt, error, previousCode, output)
        return Pair(response, extractCodeBlocks(response))
    }

    private fun _fixCommand(prompt: String, error: Exception, previousCode: String, output: String): String {
        if (verbose) log.info(prompt)
        val request = ChatRequest()
        val apiDescription = apiDescription(hands, yamlDescriber)
        request.messages = (
                listOf(
                    ChatMessage(
                        ChatMessage.Role.system, """
                            |You will translate natural language instructions into 
                            |an implementation using $language and the script context.
                            |Use ``` code blocks labeled with $language where appropriate.
                            |Defined symbols include ${hands.keySet().joinToString(", ")}.
                            |Do not include wrapping code blocks, assume a REPL context.
                            |The runtime context is described below:
                            |
                            |$apiDescription
                            |""".trimMargin().trim()
                    )
                ) + listOf(
                    ChatMessage(
                        ChatMessage.Role.user,
                        prompt
                    ),
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
                )).toTypedArray()
        totalApiDescriptionLength.addAndGet(apiDescription.length)
        val response = run(request)
        return response
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

        fun apiDescription(hands: java.util.Map<String, Object>, yamlDescriber: YamlDescriber): String {
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
                    |    ${joinYamlList(methods.map { yamlDescriber.toYaml(it) }).indent().indent()}
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
                |  ${yamlDescriber.toYaml(it).indent()}
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