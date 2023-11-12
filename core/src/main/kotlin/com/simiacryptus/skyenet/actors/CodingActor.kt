package com.simiacryptus.skyenet.actors

import com.simiacryptus.openai.OpenAIClient
import com.simiacryptus.skyenet.Brain
import com.simiacryptus.skyenet.Brain.Companion.indent
import com.simiacryptus.skyenet.Brain.Companion.superMethod
import com.simiacryptus.skyenet.Heart
import com.simiacryptus.skyenet.OutputInterceptor
import com.simiacryptus.skyenet.util.SessionServerUtil.asJava
import com.simiacryptus.skyenet.util.SessionServerUtil.getCode
import com.simiacryptus.skyenet.util.SessionServerUtil.getRenderedResponse
import com.simiacryptus.util.describe.AbbrevWhitelistYamlDescriber
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

class CodingActor(
    private val interpreterClass: KClass<out Heart>,
    private val symbols: Map<String, Any> = mapOf(),
    private val describer: AbbrevWhitelistYamlDescriber = AbbrevWhitelistYamlDescriber(
        "com.simiacryptus",
        "com.github.simiacryptus"
    ),
    name: String? = interpreterClass.simpleName,
    val details: String? = null,
    model: OpenAIClient.Models = OpenAIClient.Models.GPT35Turbo,
    val fallbackModel: OpenAIClient.Models = OpenAIClient.Models.GPT4Turbo,
    temperature: Double = 0.1,
) : BaseActor<CodeResult>(
    prompt = "",
    name = name,
    model = model,
    temperature = temperature,
) {
    val fixIterations = 3
    val fixRetries = 2

    override val prompt: String
        get() {
            val types = ArrayList<Class<*>>()
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
            val apiDescription = """
                    |api_objects:
                    |  ${apiobjs.joinToString("\n").indent()}
                    |components:
                    |  schemas:
                    |    ${typeDescriptions.joinToString("\n").indent().indent()}
                """.trimMargin()
            return """
                        |You will translate natural language instructions into 
                        |an implementation using ${interpreter.getLanguage()} and the script context.
                        |Use ``` code blocks labeled with ${interpreter.getLanguage()} where appropriate.
                        |Defined symbols include ${symbols.keys.joinToString(", ")}.
                        |The runtime context is described below:
                        |
                        |$apiDescription
                        |
                        |${details ?: ""}
                        |""".trimMargin().trim()

        }

    open val interpreter by lazy { interpreterClass.java.getConstructor(Map::class.java).newInstance(symbols) }

    override fun answer(vararg questions: String, api: OpenAIClient): CodeResult = answer(*chatMessages(*questions), api = api)

    override fun answer(vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): CodeResult {
        return CodeResultImpl(*messages, api = api)
    }
    fun answerWithPrefix(codePrefix: String, vararg messages: OpenAIClient.ChatMessage, api: OpenAIClient): CodeResult {
        val prevList = messages.toList()
        val newList = prevList.dropLast(1) + listOf(
            OpenAIClient.ChatMessage(OpenAIClient.ChatMessage.Role.assistant, codePrefix)
        ) + prevList.last()
        return CodeResultImpl(*newList.toTypedArray(), api = api)
    }

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
                Brain(
                    api = api,
                    symbols = symbols.mapValues { it as Object }.asJava,
                    language = interpreter.getLanguage(),
                    describer = describer,
                    model = model,
                    temperature = temperature,
                ), *messages, codePrefix = codePrefix
            )
            if (_status != CodeResult.Status.Success) {
                codedInstruction = implement(
                    Brain(
                        api = api,
                        symbols = symbols.mapValues { it as Object }.asJava,
                        language = interpreter.getLanguage(),
                        describer = describer,
                        model = fallbackModel,
                        temperature = temperature,
                    ), *messages, codePrefix = codePrefix
                )
            }
            if (_status != CodeResult.Status.Success) {
                log.info("Failed to implement ${messages.map { it.content }.joinToString("\n")}")
                _status = CodeResult.Status.Failure
            }
            codedInstruction
        }

        private fun implement(
            brain: Brain,
            vararg messages: OpenAIClient.ChatMessage,
            codePrefix: String = "",
        ): String {
            val response = brain.implement(*messages)
            val codeBlocks = Brain.extractCodeBlocks(response)
            var codedInstruction = ""
            for (codingAttempt in 0..fixRetries) {
                var renderedResponse = getRenderedResponse(codeBlocks)
                codedInstruction = getCode(interpreter.getLanguage(), codeBlocks)
                log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
                log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
                for (fixAttempt in 0..fixIterations) {
                    try {
                        val validate = interpreter.validate((codePrefix + "\n" + codedInstruction).trim())
                        if (validate != null) throw validate
                        log.info("Validation succeeded")
                        _status = CodeResult.Status.Success
                        return codedInstruction
                    } catch (ex: Throwable) {
                        log.info("Validation failed - ${ex.message}")
                        _status = CodeResult.Status.Correcting
                        val respondWithCode = brain.fixCommand(codedInstruction, ex, "", *messages)
                        renderedResponse = getRenderedResponse(respondWithCode.second)
                        codedInstruction = getCode(interpreter.getLanguage(), respondWithCode.second)
                        log.info("Response: \n\t${renderedResponse.replace("\n", "\n\t", false)}".trimMargin())
                        log.info("Code: \n\t${codedInstruction.replace("\n", "\n\t", false)}".trimMargin())
                    }
                }
            }
            return codedInstruction
        }

        override fun getCode(): String {
            return impl
        }

        override fun run(): ExecutionResult {
            //language=HTML
            log.info("Running ${getCode()}")
            OutputInterceptor.clearGlobalOutput()
            val result = interpreter.run(getCode())
            log.info("Result: $result")
            //language=HTML
            val executionResult = ExecutionResult(result.toString(), OutputInterceptor.getGlobalOutput())
            OutputInterceptor.clearGlobalOutput()
            return executionResult
        }
    }

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(CodingActor::class.java)

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
