package com.simiacryptus.skyenet.core.actors

import com.fasterxml.jackson.annotation.JsonIgnore
import com.simiacryptus.jopenai.API
import com.simiacryptus.jopenai.ApiModel.*
import com.simiacryptus.jopenai.ClientUtil.toContentList
import com.simiacryptus.jopenai.OpenAIClient
import com.simiacryptus.jopenai.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.jopenai.describe.TypeDescriber
import com.simiacryptus.jopenai.models.ChatModels
import com.simiacryptus.skyenet.core.Interpreter
import com.simiacryptus.skyenet.core.OutputInterceptor
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
    model: ChatModels = ChatModels.GPT35Turbo,
    val fallbackModel: ChatModels = ChatModels.GPT4Turbo,
    temperature: Double = 0.1,
    val autoEvaluate: Boolean = false,
    private val fixIterations: Int = 3,
    private val fixRetries: Int = 2,
) : BaseActor<CodingActor.CodeResult>(
    prompt = "",
    name = name,
    model = model,
    temperature = temperature,
) {

    data class ExecutionResult(
        val resultValue: String,
        val resultOutput: String
    )
    interface CodeResult {
        enum class Status {
            Coding, Correcting, Success, Failure
        }

        fun getStatus(): Status
        fun getCode(): String
        fun run(): ExecutionResult
    }

    override val prompt: String
        get() = if (symbols.isNotEmpty()) """
            |You will translate natural language instructions into 
            |an implementation using ${interpreter.getLanguage()} and the script context.
            |Use ``` code blocks labeled with ${interpreter.getLanguage()} where appropriate.
            |Defined symbols include ${symbols.keys.joinToString(", ")}.
            |The runtime context is described below:
            |
            |${this.apiDescription}
            |
            |${details ?: ""}
            |""".trimMargin().trim()
        else """
            |You will translate natural language instructions into 
            |an implementation using ${interpreter.getLanguage()} and the script context.
            |Use ``` code blocks labeled with ${interpreter.getLanguage()} where appropriate.
            |
            |${details ?: ""}
            |""".trimMargin().trim()

    open val apiDescription: String
        get() = this.symbols.map { (name, utilityObj) ->
            """
            |$name:
            |    ${this.describer.describe(utilityObj.javaClass).indent("    ")}
            |""".trimMargin().trim()
        }.joinToString("\n")

    open val interpreter: Interpreter by lazy { interpreterClass.java.getConstructor(Map::class.java).newInstance(symbols) }

    protected val language: String by lazy { interpreter.getLanguage() }

    override fun answer(vararg questions: String, api: API): CodeResult =
        if (!autoEvaluate) answer(*chatMessages(*questions), api = api)
        else answerWithAutoEval(*chatMessages(*questions), api = api).first

    override fun answer(vararg messages: ChatMessage, api: API): CodeResult =
        if (!autoEvaluate) CodeResultImpl(*messages, api = (api as OpenAIClient))
        else answerWithAutoEval(*messages, api = api).first

    open fun answerWithPrefix(
        codePrefix: String,
        vararg messages: ChatMessage,
        api: API
    ): CodeResult =
        if (!autoEvaluate) CodeResultImpl(*injectCodePrefix(messages, codePrefix), api = (api as OpenAIClient))
        else answerWithAutoEval(*injectCodePrefix(messages, codePrefix), api = api).first

    open fun answerWithAutoEval(
        vararg messages: String,
        api: API,
        codePrefix: String = ""
    ) = answerWithAutoEval(*injectCodePrefix(chatMessages(*messages), codePrefix), api = api)

    open fun answerWithAutoEval(
        vararg messages: ChatMessage,
        api: API
    ): Pair<CodeResult, ExecutionResult> {
        var result = CodeResultImpl(*messages, api = (api as OpenAIClient))
        var lastError: Throwable? = null
        for (i in 0..fixIterations) try {
            return result to result.run()
        } catch (ex: Throwable) {
            lastError = ex
            result = run {
                val respondWithCode = fixCommand(api, result.getCode(), ex, *messages, model = model)
                val renderedResponse = getRenderedResponse(respondWithCode.second)
                val codedInstruction = getCode(interpreter.getLanguage(), respondWithCode.second)
                log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
                log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
                CodeResultImpl(*messages, codePrefix = codedInstruction, api = api)
            }
        }
        throw RuntimeException(
            """
            |Failed to fix code. Last attempt: 
            |```${interpreter.getLanguage().lowercase()}
            |${result.getCode()}
            |```
            |
            |Last Error:
            |```
            |${lastError?.message}
            |```
            |""".trimMargin().trim()
        )
    }

    open fun implement(
        self: CodeResult,
        api: OpenAIClient,
        messages: Array<out ChatMessage>,
        codePrefix: String,
        model: ChatModels
    ): String {
        var request = ChatRequest()
        request = request.copy(messages = ArrayList(messages.toList()))
        val response = chat(api, request, this.model)
        val codeBlocks = extractCodeBlocks(response)
        for (codingAttempt in 0..fixRetries) {
            val renderedResponse = getRenderedResponse(codeBlocks)
            val codedInstruction = getCode(interpreter.getLanguage(), codeBlocks)
            log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
            log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
            return validateAndFix(self, codedInstruction, codePrefix, api, messages, model) ?: continue
        }
        return ""
    }

    open fun validateAndFix(
        self: CodeResult,
        initialCode: String,
        codePrefix: String,
        api: OpenAIClient,
        messages: Array<out ChatMessage>,
        model: ChatModels
    ): String? {
        var workingCode = initialCode
        for (fixAttempt in 0..fixIterations) {
            try {
                val validate = interpreter.validate((codePrefix + "\n" + workingCode).trim())
                if (validate != null) throw validate
                log.info("Validation succeeded")
                (self as CodeResultImpl)._status = CodeResult.Status.Success
                return workingCode
            } catch (ex: Throwable) {
                log.info("Validation failed - ${ex.message}")
                (self as CodeResultImpl)._status = CodeResult.Status.Correcting
                val respondWithCode = fixCommand(api, workingCode, ex, *messages, model = model)
                val response = getRenderedResponse(respondWithCode.second)
                workingCode = getCode(interpreter.getLanguage(), respondWithCode.second)
                log.info("Response: \n\t${response.replace("\n", "\n\t", false)}".trimMargin())
                log.info("Code: \n\t${workingCode.replace("\n", "\n\t", false)}".trimMargin())
            }
        }
        return null
    }

    open fun execute(code: String): ExecutionResult {
        //language=HTML
        log.info("Running $code")
        OutputInterceptor.clearGlobalOutput()
        val result = try {
            interpreter.run(code)
        } catch (ex: ScriptException) {
            throw RuntimeException(
                """
                        |${ex.message ?: ""} at line ${ex.lineNumber} column ${ex.columnNumber}
                        |  ${code.split("\n")[ex.lineNumber - 1]}
                        |  ${" ".repeat(ex.columnNumber - 1) + "^"}
                        """.trimMargin().trim(), ex
            )
        }
        log.info("Result: $result")
        //language=HTML
        val executionResult = ExecutionResult(result.toString(), OutputInterceptor.getGlobalOutput())
        OutputInterceptor.clearGlobalOutput()
        return executionResult
    }

    private inner class CodeResultImpl(
        vararg messages: ChatMessage,
        codePrefix: String = "",
        api: OpenAIClient,
    ) : CodeResult {
        var _status = CodeResult.Status.Coding
        override fun getStatus(): CodeResult.Status {
            return _status
        }

        private val impl by lazy {
            var codedInstruction = implement(
                this, api, messages, codePrefix = codePrefix, model
            )
            if (_status != CodeResult.Status.Success && fallbackModel != model) {
                codedInstruction = implement(
                    this, api, messages, codePrefix = codePrefix, fallbackModel
                )
            }
            if (_status != CodeResult.Status.Success) {
                log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                _status = CodeResult.Status.Failure
            }
            codedInstruction
        }

        @JsonIgnore
        override fun getCode(): String = impl

        override fun run() = execute(getCode())
    }

    private fun injectCodePrefix(
        messages: Array<out ChatMessage>,
        codePrefix: String
    ) = (messages.dropLast(1) + if (codePrefix.isBlank()) listOf() else listOf(
        ChatMessage(Role.assistant, codePrefix.toContentList())
    ) + messages.last()).toTypedArray()

    private fun fixCommand(
        api: OpenAIClient,
        previousCode: String,
        error: Throwable,
        vararg promptMessages: ChatMessage,
        model: ChatModels
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
                                |Correct the code and try again.
                                |""".trimMargin().trim().toContentList()
                    )
                ))
        )
        val response = chat(api, request, model)
        val codeBlocks = extractCodeBlocks(response)
        return Pair(response, codeBlocks)
    }

    private fun chat(api: OpenAIClient, request: ChatRequest, model: ChatModels) =
        api.chat(request.copy(model = model.modelName, temperature = temperature), model)
            .choices.first().message?.content.orEmpty().trim()

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CodingActor::class.java)

        fun String.indent(indent: String = "  ") = this.replace("\n", "\n$indent")

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

        fun getRenderedResponse(respondWithCode: List<Pair<String, String>>) =
            respondWithCode.joinToString("\n") {
                var language = it.first
                if (language == "code") language = "groovy"
                if (language == "text") {
                    //language=HTML
                    """
                    |<div>
                    |${it.second}
                    |</div>
                    |""".trimMargin().trim()
                } else {
                    //language=HTML
                    """
                    |<pre><code class="language-$language">
                    |${it.second}
                    |</code></pre>
                    |""".trimMargin().trim()
                }
            }

        fun getCode(language: String, textSegments: List<Pair<String, String>>): String {
            if (textSegments.size == 1) return textSegments.joinToString("\n") { it.second }
            return textSegments.joinToString("\n") {
                if (it.first.lowercase() == "code" || it.first.lowercase() == language.lowercase()) {
                    """
                            |${it.second}
                            |""".trimMargin().trim()
                } else {
                    ""
                }
            }
        }

    }

}

