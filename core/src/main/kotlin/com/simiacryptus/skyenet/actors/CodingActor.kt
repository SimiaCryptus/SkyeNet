package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.openai.OpenAIClientBase.Companion.toContentList
import com.simiacryptus.openai.models.ChatModels
import com.simiacryptus.openai.models.OpenAITextModel
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Brain.Companion.indent
import com.simiacryptus.skyenet.Brain.Companion.superMethod
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.util.describe.AbbrevWhitelistYamlDescriber
import com.simiacryptus.util.describe.TypeDescriber
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class CodingActor(
    private val interpreterClass: KClass<out Heart>,
    private val symbols: Map<String, Any> = mapOf(),
    private val describer: TypeDescriber = AbbrevWhitelistYamlDescriber(
        "com.simiacryptus",
        "com.github.simiacryptus"
    ),
    name: String? = interpreterClass.simpleName,
    val details: String? = null,
    model: OpenAITextModel = ChatModels.GPT35Turbo,
    val fallbackModel: OpenAITextModel = ChatModels.GPT4Turbo,
    temperature: Double = 0.1,
    val autoEvaluate: Boolean = false,
) : BaseActor<CodeResult>(
    prompt = "",
    name = name,
    model = model,
    temperature = temperature,
) {
    val fixIterations = 3
    val fixRetries = 2

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
        get() = apiDescription(this.symbols, this.describer)

    open val interpreter by lazy { interpreterClass.java.getConstructor(Map::class.java).newInstance(symbols) }

    override fun answer(vararg questions: String, api: OpenAIClient): CodeResult =
        if (!autoEvaluate) answer(*chatMessages(*questions), api = api)
        else answerWithAutoEval(*chatMessages(*questions), api = api).first

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): CodeResult =
        if (!autoEvaluate) CodeResultImpl(*messages, api = api)
        else answerWithAutoEval(*messages, api = api).first

    fun answerWithPrefix(codePrefix: String, vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): CodeResult =
        if (!autoEvaluate) CodeResultImpl(*injectCodePrefix(messages, codePrefix), api = api)
        else answerWithAutoEval(*injectCodePrefix(messages, codePrefix), api = api).first

    fun answerWithAutoEval(
        vararg messages: String,
        api: OpenAIClient,
        codePrefix: String = ""
    ) = answerWithAutoEval(*injectCodePrefix(chatMessages(*messages), codePrefix), api = api)

    fun answerWithAutoEval(
        vararg messages: OpenAIClient.ChatMessage,
        api: OpenAIClient
    ): Pair<CodeResult, ExecutionResult> {
        var result = CodeResultImpl(*messages, api = api)
        for (i in 0..fixIterations) try {
            return result to result.run()
        } catch (ex: Throwable) {
            result = fix(api, messages, result, ex)
        }
        throw RuntimeException("Failed to fix ${messages.map { it.content }.joinToString("\n")}")
    }

    private fun injectCodePrefix(
        messages: Array<out OpenAIClient.ChatMessage>,
        codePrefix: String
    ) = (messages.dropLast(1) + if (codePrefix.isBlank()) listOf() else listOf(
        OpenAIClient.ChatMessage(OpenAIClient.Role.assistant, codePrefix.toContentList())
    ) + messages.last()).toTypedArray()

    private fun fix(
        api: OpenAIClient,
        messages: Array<out OpenAIClient.ChatMessage>,
        result: CodingActor.CodeResultImpl,
        ex: Throwable
    ): CodingActor.CodeResultImpl {
        val respondWithCode = brain(api, model).fixCommand(result.getCode(), ex, "", *messages)
        val renderedResponse = getRenderedResponse(respondWithCode.second)
        val codedInstruction = getCode(interpreter.getLanguage(), respondWithCode.second)
        log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
        log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
        return CodeResultImpl(*messages, codePrefix = codedInstruction, api = api)
    }

    fun brain(api: OpenAIClient, model: OpenAITextModel) = Brain(
        api = api,
        symbols = symbols.mapValues { it as Object }.asJava,
        language = interpreter.getLanguage(),
        describer = describer,
        model = model,
        temperature = temperature,
    )

    private inner class CodeResultImpl(
        vararg messages: OpenAIClient.ChatMessage,
        codePrefix: String = "",
        api: OpenAIClient,
    ) : CodeResult {
        var _status = CodeResult.Status.Coding
        override fun getStatus(): CodeResult.Status {
            return _status
        }

        val impl by lazy {
            var codedInstruction = implement(
                brain(api, model), *messages, codePrefix = codePrefix
            )
            if (_status != CodeResult.Status.Success) {
                codedInstruction = implement(
                    brain(api, fallbackModel), *messages, codePrefix = codePrefix
                )
            }
            if (_status != CodeResult.Status.Success) {
                log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                _status = CodeResult.Status.Failure
            }
            codedInstruction
        }


        fun implement(
            brain: Brain,
            vararg messages: OpenAIClient.ChatMessage,
            codePrefix: String = "",
        ): String {
            val response = brain.implement(*messages)
            val codeBlocks = Brain.extractCodeBlocks(response)
            for (codingAttempt in 0..fixRetries) {
                var renderedResponse = getRenderedResponse(codeBlocks)
                val codedInstruction = getCode(interpreter.getLanguage(), codeBlocks)
                log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
                log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
                return validateAndFix(brain, codePrefix, codedInstruction, messages) ?: continue
            }
            return ""
        }

        fun validateAndFix(
            brain: Brain,
            codePrefix: String,
            initialCode: String,
            messages: Array<out OpenAIClient.ChatMessage>
        ): String? {
            var workingCode = initialCode
            for (fixAttempt in 0..fixIterations) {
                try {
                    val validate = interpreter.validate((codePrefix + "\n" + workingCode).trim())
                    if (validate != null) throw validate
                    log.info("Validation succeeded")
                    _status = CodeResult.Status.Success
                    return workingCode
                } catch (ex: Throwable) {
                    log.info("Validation failed - ${ex.message}")
                    _status = CodeResult.Status.Correcting
                    val respondWithCode = brain.fixCommand(workingCode, ex, "", *messages)
                    val response = getRenderedResponse(respondWithCode.second)
                    workingCode = getCode(interpreter.getLanguage(), respondWithCode.second)
                    log.info("Response: \n\t${response.replace("\n", "\n\t", false)}".trimMargin())
                    log.info("Code: \n\t${workingCode.replace("\n", "\n\t", false)}".trimMargin())
                }
            }
            return null
        }

        override fun getCode(): String {
            return impl
        }

        override fun run(): ExecutionResult {
            //language=HTML
            log.info("Running ${getCode()}")
            OutputInterceptor.clearGlobalOutput()
            val result = try {
                interpreter.run(getCode())
            } catch (ex: javax.script.ScriptException) {
                throw RuntimeException(errorMessage(getCode(), ex.lineNumber, ex.columnNumber, ex.message ?: ""), ex)
            }
            log.info("Result: $result")
            //language=HTML
            val executionResult = ExecutionResult(result.toString(), OutputInterceptor.getGlobalOutput())
            OutputInterceptor.clearGlobalOutput()
            return executionResult
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CodingActor::class.java)
        fun errorMessage(
            code: String,
            line: Int,
            column: Int,
            message: String
        ) = """
                |$message at line ${line} column ${column}
                |  ${code.split("\n")[line - 1]}
                |  ${" ".repeat(column - 1) + "^"}
                """.trimMargin().trim()

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

        operator fun <K, V> java.util.Map<K, V>.plus(mapOf: Map<K, V>): java.util.Map<K, V> {
            val hashMap = java.util.HashMap<K, V>()
            this.forEach(hashMap::put)
            hashMap.putAll(mapOf)
            return hashMap as java.util.Map<K, V>
        }

        val <K, V> Map<K, V>.asJava: java.util.Map<K, V>
            get() {
                return java.util.HashMap<K, V>().also { map ->
                    this.forEach { (key, value) ->
                        map[key] = value
                    }
                } as java.util.Map<K, V>
            }

        fun apiDescription(
            symbols: Map<String, Any>,
            describer: TypeDescriber
        ): String {
            val types: ArrayList<Class<*>> = ArrayList()
            val apiobjs = symbols.map { (name, utilityObj) ->
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
                                    |    ${Brain.joinYamlList(methods.map { describer.describe(it) }).indent().indent()}
                                    |""".trimMargin().trim()
            }.toTypedArray<String>()
            val typeDescriptions = types
                .filter { !it.isPrimitive }
                .filter { !it.isSynthetic }
                .filter { !it.name.startsWith("java.") }
                .filter { !setOf("void").contains(it.name) }
                .distinct().map {
                    """
                                |${it.simpleName}:
                                |  ${describer.describe(it).indent()}
                                """.trimMargin().trim()
                }.toTypedArray<String>()
            return """
                |api_objects:
                |  ${apiobjs.joinToString("\n").indent()}
                |components:
                |  schemas:
                |    ${typeDescriptions.joinToString("\n").indent().indent()}
                |""".trimMargin().trim()
        }

    }
}

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
