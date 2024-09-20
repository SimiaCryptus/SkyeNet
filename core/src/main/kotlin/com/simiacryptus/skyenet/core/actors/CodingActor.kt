package com.simiacryptus.skyenet.core.actors

import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.models.ApiModel.*
import com.simiacryptus.jopenai.ChatClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.jopenai.models.OpenAIModels
import com.simiacryptus.jopenai.models.OpenAITextModel
import com.simiacryptus.jopenai.util.ClientUtil.toContentList
import com.simiacryptus.skyenet.core.OutputInterceptor
import com.simiacryptus.skyenet.interpreter.Interpreter
import java.util.*
import javax.script.ScriptException
import kotlin.reflect.KClass

open class CodingActor(
    val interpreterClass: KClass<out Interpreter>,
    val symbols: Map<String, Any> = mapOf(),
    val describer: TypeDescriber = AbbrevWhitelistYamlDescriber(
        "com.simiacryptus",
        "com.github.simiacryptus"
    ),
    name: String? = interpreterClass.simpleName,
    val details: String? = null,
    model: OpenAITextModel = OpenAIModels.GPT4o,
    val fallbackModel: ChatModels = OpenAIModels.GPT4o,
    temperature: Double = 0.1,
    val runtimeSymbols: Map<String, Any> = mapOf()
) : BaseActor<CodingActor.CodeRequest, CodingActor.CodeResult>(
    prompt = "",
    name = name,
    model = model,
    temperature = temperature,
) {
    val interpreter: Interpreter
        get() = interpreterClass.java.getConstructor(Map::class.java).newInstance(symbols + runtimeSymbols)

    data class CodeRequest(
        val messages: List<Pair<String, Role>>,
        val codePrefix: String = "",
        val autoEvaluate: Boolean = false,
        val fixIterations: Int = 1,
        val fixRetries: Int = 1,
    )

    interface CodeResult {
        enum class Status {
            Coding, Correcting, Success, Failure
        }

        val code: String
        val status: Status
        val result: ExecutionResult
        val renderedResponse: String?
    }

    data class ExecutionResult(
        val resultValue: String,
        val resultOutput: String
    )

    var evalFormat = true
    override val prompt: String
        get() {
            val formatInstructions =
                if (evalFormat) """Code should be structured as appropriately parameterized function(s) 
              |with the final line invoking the function with the appropriate request parameters.""" else ""
            return if (symbols.isNotEmpty()) {
                """
                  |You are a coding assistant allows users actions to be enacted using $language and the script context.
                  |Your role is to translate natural language instructions into code as well as interpret the results and converse with the user.
                  |Use ``` code blocks labeled with $language where appropriate. (i.e. ```$language)
                  |Each response should have EXACTLY ONE code block. Do not use inline blocks.
                  |$formatInstructions
                  |
                  |Defined symbols include ${symbols.keys.joinToString(", ")} described below:
                  |
                  |```${this.describer.markupLanguage}
                  |${this.apiDescription}
                  |```
                  |
                  |THESE VARIABLES ARE READ-ONLY: ${symbols.keys.joinToString(", ")}
                  |They are already defined for you.
                  |
                  |${details ?: ""}
                  |""".trimMargin().trim()
            } else """
                |You are a coding assistant allowing users actions to be enacted using $language and the script context.
                |Your role is to translate natural language instructions into code as well as interpret the results and converse with the user.
                |Use ``` code blocks labeled with $language where appropriate. (i.e. ```$language)
                |Each response should have EXACTLY ONE code block. Do not use inline blocks.
                |$formatInstructions
                |
                |${details ?: ""}
                |""".trimMargin().trim()
        }

    open val apiDescription: String
        get() = this.symbols.map { (name, utilityObj) ->
            """
            |$name:
            |    ${this.describer.describe(utilityObj.javaClass).indent("    ")}
            |""".trimMargin().trim()
        }.joinToString("\n")


    val language: String by lazy { interpreter.getLanguage() }

    override fun chatMessages(questions: CodeRequest): Array<ChatMessage> {
        var chatMessages = arrayOf(
            ChatMessage(
                role = Role.system,
                content = prompt.toContentList()
            ),
        ) + questions.messages.map {
            ChatMessage(
                role = it.second,
                content = it.first.toContentList()
            )
        }
        if (questions.codePrefix.isNotBlank()) {
            chatMessages = (chatMessages.dropLast(1) + listOf(
                ChatMessage(Role.assistant, "Code Prefix:\n```\n${questions.codePrefix}\n```".toContentList())
            ) + chatMessages.last()).toTypedArray<ChatMessage>()
        }
        return chatMessages

    }

    override fun respond(
        input: CodeRequest,
        api: API,
        vararg messages: ChatMessage,
    ): CodeResult {
        var result = CodeResultImpl(
            *messages,
            input = input,
            api = (api as ChatClient)
        )
        if (!input.autoEvaluate) return result
        for (i in 0..input.fixIterations) try {
            require(result.result.resultValue.length > -1)
            return result
        } catch (ex: Throwable) {
            if (i == input.fixIterations) {
                log.info(
                    "Failed to implement ${
                        messages.map { it.content?.joinToString("\n") { it.text ?: "" } }.joinToString("\n")
                    }"
                )
                throw ex
            }
            val respondWithCode = fixCommand(api, result.code, ex, *messages, model = model)
            val blocks = extractTextBlocks(respondWithCode)
            val renderedResponse = getRenderedResponse(blocks)
            val codedInstruction = getCode(language, blocks)
            log.debug("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
            log.debug("New Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
            result = CodeResultImpl(
                *messages,
                input = input,
                api = api,
                givenCode = codedInstruction,
                givenResponse = renderedResponse
            )
        }
        throw IllegalStateException()
    }

    open fun execute(prefix: String, code: String): ExecutionResult {
        //language=HTML
        log.debug("Running $code")
        OutputInterceptor.clearGlobalOutput()
        val result = try {
            interpreter.run((prefix + "\n" + code).sortCode())
        } catch (e: Exception) {
            when {
                e is FailedToImplementException -> throw e
                e is ScriptException -> throw FailedToImplementException(
                    cause = e,
                    message = errorMessage(e, code),
                    language = language,
                    code = code,
                    prefix = prefix,
                )

                e.cause is ScriptException -> throw FailedToImplementException(
                    cause = e,
                    message = errorMessage(e.cause!! as ScriptException, code),
                    language = language,
                    code = code,
                    prefix = prefix,
                )

                else -> throw e
            }
        }
        log.debug("Result: $result")
        //language=HTML
        val executionResult = ExecutionResult(result.toString(), OutputInterceptor.getThreadOutput())
        OutputInterceptor.clearThreadOutput()
        return executionResult
    }

    inner class CodeResultImpl(
        vararg val messages: ChatMessage,
        private val input: CodeRequest,
        private val api: ChatClient,
        private val givenCode: String? = null,
        private val givenResponse: String? = null,
    ) : CodeResult {
        private val implementation by lazy {
            if (!givenCode.isNullOrBlank() && !givenResponse.isNullOrBlank()) (givenCode to givenResponse) else try {
                implement(model)
            } catch (ex: FailedToImplementException) {
                if (fallbackModel != model) {
                    try {
                        implement(fallbackModel)
                    } catch (ex: FailedToImplementException) {
                        log.debug("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                        _status = CodeResult.Status.Failure
                        throw ex
                    }
                } else {
                    log.debug("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                    _status = CodeResult.Status.Failure
                    throw ex
                }
            }
        }

        private var _status = CodeResult.Status.Coding

        override val status get() = _status

        override val renderedResponse: String = givenResponse ?: implementation.second
        override val code: String = givenCode ?: implementation.first

        private fun implement(
            model: OpenAITextModel,
        ): Pair<String, String> {
            val request = ChatRequest(messages = ArrayList(this.messages.toList()))
            for (codingAttempt in 0..input.fixRetries) {
                try {
                    val codeBlocks = extractTextBlocks(chat(api, request, model))
                    val renderedResponse = getRenderedResponse(codeBlocks)
                    val codedInstruction = getCode(language, codeBlocks)
                    log.debug("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
                    log.debug("New Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
                    var workingCode = codedInstruction
                    var workingRenderedResponse = renderedResponse
                    for (fixAttempt in 0..input.fixIterations) {
                        try {
                            val validate = interpreter.validate((input.codePrefix + "\n" + workingCode).sortCode())
                            if (validate != null) throw validate
                            log.debug("Validation succeeded")
                            _status = CodeResult.Status.Success
                            return workingCode to workingRenderedResponse
                        } catch (ex: Throwable) {
                            if (fixAttempt == input.fixIterations)
                                throw if (ex is FailedToImplementException) ex else FailedToImplementException(
                                    cause = ex,
                                    message = """
                  |**ERROR**
                  |
                  |```text
                  |${ex.stackTraceToString()}
                  |```
                  |""".trimMargin().trim(),
                                    language = language,
                                    code = workingCode,
                                    prefix = input.codePrefix
                                )
                            log.debug("Validation failed - ${ex.message}")
                            _status = CodeResult.Status.Correcting
                            val respondWithCode = fixCommand(api, workingCode, ex, *messages, model = model)
                            val codeBlocks = extractTextBlocks(respondWithCode)
                            workingRenderedResponse = getRenderedResponse(codeBlocks)
                            workingCode = getCode(language, codeBlocks)
                            log.debug(
                                "Response: \n\t${
                                    workingRenderedResponse.replace(
                                        "\n",
                                        "\n\t",
                                        false
                                    )
                                }".trimMargin()
                            )
                            log.debug("New Code: \n\t${workingCode.replace("\n", "\n\t", false)}".trimMargin())
                        }
                    }
                } catch (ex: FailedToImplementException) {
                    if (codingAttempt == input.fixRetries) {
                        log.debug("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                        throw ex
                    }
                    log.debug("Retry failed to implement ${messages.map { it.content }.joinToString("\n")}")
                    _status = CodeResult.Status.Correcting
                }
            }
            throw IllegalStateException()
        }


        private val executionResult by lazy { execute(input.codePrefix, code) }

        override val result get() = executionResult
    }

    private fun fixCommand(
        api: ChatClient,
        previousCode: String,
        error: Throwable,
        vararg promptMessages: ChatMessage,
        model: OpenAITextModel
    ): String = chat(
        api = api,
        request = ChatRequest(
            messages = ArrayList(
                promptMessages.toList() + listOf(
                    ChatMessage(
                        Role.assistant,
                        """
            |```${language.lowercase()}
            |${previousCode.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
            |```
            |""".trimMargin().trim().toContentList()
                    ),
                    ChatMessage(
                        Role.system,
                        """
            |The previous code failed with the following error:
            |
            |```
            |${error.message?.trim() ?: "".let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}
            |```
            |
            |Correct the code and try again.
            |""".trimMargin().trim().toContentList()
                    )
                )
            )
        ),
        model = model
    )

    private fun chat(api: ChatClient, request: ChatRequest, model: OpenAITextModel) =
        api.chat(request.copy(model = model.modelName, temperature = temperature), model)
            .choices.first().message?.content.orEmpty().trim()


    override fun withModel(model: ChatModels): CodingActor = CodingActor(
        interpreterClass = interpreterClass,
        symbols = symbols,
        describer = describer,
        name = name,
        details = details,
        model = model,
        fallbackModel = fallbackModel,
        temperature = temperature,
        runtimeSymbols = runtimeSymbols
    )

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CodingActor::class.java)

        fun String.indent(indent: String = "  ") = this.replace("\n", "\n$indent")

        fun extractTextBlocks(response: String): List<Pair<String, String>> {
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

        fun getRenderedResponse(respondWithCode: List<Pair<String, String>>, defaultLanguage: String = "") =
            respondWithCode.joinToString("\n") {
                when (it.first) {
                    "code" -> "```$defaultLanguage\n${it.second.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}\n```"
                    "text" -> it.second.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }.toString()
                    else -> "```${it.first}\n${it.second.let { /*escapeHtml4*/(it)/*.indent("  ")*/ }}\n```"
                }
            }

        fun getCode(language: String, textSegments: List<Pair<String, String>>): String {
            if (textSegments.size == 1) return textSegments.joinToString("\n") { it.second }
            return textSegments.joinToString("\n") {
                if (it.first.lowercase() == "code" || it.first.lowercase() == language.lowercase()) {
                    it.second.trimMargin().trim()
                } else {
                    ""
                }
            }
        }

        fun String.sortCode(bodyWrapper: (String) -> String = { it }): String {
            val (imports, otherCode) = this.split("\n").partition { it.trim().startsWith("import ") }
            return imports.distinct().sorted().joinToString("\n") + "\n\n" + bodyWrapper(otherCode.joinToString("\n"))
        }

        fun String.camelCase(locale: Locale = Locale.getDefault()): String {
            val words = fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            return words.first().lowercase(locale) + words.drop(1).joinToString("") {
                it.replaceFirstChar { c ->
                    when {
                        c.isLowerCase() -> c.titlecase(locale)
                        else -> c.toString()
                    }
                }
            }
        }

        fun String.pascalCase(locale: Locale = Locale.getDefault()): String =
            fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("") {
                it.replaceFirstChar { c ->
                    when {
                        c.isLowerCase() -> c.titlecase(locale)
                        else -> c.toString()
                    }
                }
            }

        // Detect changes in the case of the first letter and prepend a space
        private fun String.fromPascalCase(): String = buildString {
            var lastChar = ' '
            for (c in this@fromPascalCase) {
                if (c.isUpperCase() && lastChar.isLowerCase()) append(' ')
                append(c)
                lastChar = c
            }
        }

        fun String.upperSnakeCase(locale: Locale = Locale.getDefault()): String =
            fromPascalCase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }.joinToString("_") {
                it.replaceFirstChar { c ->
                    when {
                        c.isLowerCase() -> c.titlecase(locale)
                        else -> c.toString()
                    }
                }
            }.uppercase(locale)

        fun String.imports(): List<String> {
            return this.split("\n").filter { it.trim().startsWith("import ") }.distinct().sorted()
        }

        fun String.stripImports(): String {
            return this.split("\n").filter { !it.trim().startsWith("import ") }.joinToString("\n")
        }

        fun errorMessage(ex: ScriptException, code: String) = try {
            """
          |```text
          |${ex.message ?: ""} at line ${ex.lineNumber} column ${ex.columnNumber}
          |  ${if (ex.lineNumber > 0) code.split("\n")[ex.lineNumber - 1] else ""}
          |  ${if (ex.columnNumber > 0) " ".repeat(ex.columnNumber - 1) + "^" else ""}
          |```
          """.trimMargin().trim()
        } catch (_: Exception) {
            ex.message ?: ""
        }
    }

    class FailedToImplementException(
        cause: Throwable? = null,
        message: String = "Failed to implement",
        val language: String? = null,
        val code: String? = null,
        val prefix: String? = null,
    ) : RuntimeException(message, cause)
}
